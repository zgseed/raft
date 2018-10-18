package com.github.zgseed.raft;

import com.github.zgseed.raft.rpc.RaftConsensusRpcService;
import com.github.zgseed.raft.rpc.RpcServer;
import com.github.zgseed.rpc.Raft;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author:zhuge
 * @date:2018/10/10
 */
@Slf4j
@Data
public class PeerServer {
    /**
     * states on all peer
     */
    private PeerState peerState;

    // need to be persistent into disk
    //todo how to set the server id
    private String candidateId;
    private String voteFor;
    private long currentTerm;
    private PersistentLogManager persistentLogManager;
    private PeerEndPoint peerEndPoint;
    private volatile long committedIndex;
    private volatile long lastApplied;


    private StateMachine stateMachine;
    private RaftConsensusRpcService rpcServer;
    /**
     * stages only on leader
     * re-init after election as leader
     */
    private Cluster cluster;

    private long electionTimeOutInMillis;
    private ScheduledThreadPoolExecutor electionTimeoutScheduleService;
    private ScheduledFuture<?> electionTimeoutJobFuture;
    private ScheduledExecutorService rpcExecutors;
    private ScheduledThreadPoolExecutor heartbeatExecutors;
    private ScheduledFuture<?> heartbeatScheduledFuture;

    private Lock lock;


    public PeerServer(String peerId, RaftConfiguration raftConfiguration) throws IOException {
        peerState = PeerState.FOLLOWER;
        this.currentTerm = 0;
        this.candidateId = peerId;
        electionTimeoutScheduleService = new ScheduledThreadPoolExecutor(2, new ThreadFactoryBuilder().setNameFormat("ElectionTimeout-Thread-%s")
                .setDaemon(true)
                .setUncaughtExceptionHandler((t, e) -> log.error("Failed to check election timeout in {}", t.getName(), e)).build());
        rpcExecutors = new ScheduledThreadPoolExecutor(5, new ThreadFactoryBuilder().setDaemon(true
        ).setNameFormat("RPCThread-%s")
                .setUncaughtExceptionHandler((t, e) -> log.error("Failed to execute rpc", e)).build());
        heartbeatExecutors = new ScheduledThreadPoolExecutor(raftConfiguration.getPeerSize());
        persistentLogManager = new PersistentLogManager();
        cluster = new Cluster(raftConfiguration);
        this.peerEndPoint = raftConfiguration.getLocalPeer(candidateId).orElseThrow(InvalidParameterException::new);
        rpcServer = new RpcServer(this);
        lock = new ReentrantLock(true);
        resetElectionTimer();
    }

    public void close() {
    }

    private long getElectionTimeOutInMillis() {
        if (electionTimeOutInMillis == 0) {
            electionTimeOutInMillis = new Random().nextInt(350);
        }
        return 3000L + electionTimeOutInMillis;
    }


    /**
     * 重置election timer
     */
    private void resetElectionTimer() {
        if (!Objects.isNull(electionTimeoutJobFuture)) {
            electionTimeoutJobFuture.cancel(true);

        }
        electionTimeoutJobFuture = electionTimeoutScheduleService.schedule(this::electionTimeOut, getElectionTimeOutInMillis(), TimeUnit.MILLISECONDS);
    }

    private void stopElectionTimer() {
        if (!Objects.isNull(electionTimeoutJobFuture)) {
            electionTimeoutJobFuture.cancel(true);
            electionTimeoutJobFuture = null;
        }
    }

    public void electionTimeOut() {
        voteForLeader();
    }

    /**
     * The RequestVote RPC implements this restriction: the RPC includes information about the candidate’s log,
     * and the voter denies its vote if its own log is more up-to-date than that of the candidate
     * Raft determines which of two logs is more up-to-date by comparing the index and term of the last entries in the logs.
     * If the logs have last entries with different terms, then the log with the later term is more up-to-date.
     * If the logs end with the same term, then whichever log is longer is more up-to-date
     */
    public Raft.LeaderVoteResponse leaderVoteRequestHandler(Raft.LeaderVoteRequest request) {
        log.info("Received vote request,candidateId = {}, request term = {}, lastLog =({},{})", request.getCandidateId(), request.getTerm(), request.getLastLogTerm(), request.getLastLogIndex());
        this.lock.lock();
        try {
            resetElectionTimer();
            final boolean isCandidateValid = checkCandidateValid(request.getCandidateId(), request.getTerm(), request.getLastLogIndex());
            if (isCandidateValid) {
                this.voteFor = request.getCandidateId();
                log.info("vote grant for candidate {}", request.getCandidateId());
            }
            return Raft.LeaderVoteResponse.newBuilder()
                    .setTerm(this.currentTerm)
                    .setVoteGranted(isCandidateValid)
                    .build();
        } finally {
            this.lock.unlock();
        }
    }

    private boolean checkCandidateValid(String candidateId, long candidateTerm, long candidateLogIndex) {
        if (candidateTerm < this.currentTerm) {
            return false;
        }
        if (candidateTerm > this.currentTerm) {
            backToFollower(candidateTerm);
            return true;
        }
        if (this.voteFor == null || this.voteFor.equals(candidateId)) {
            if (candidateLogIndex >= this.persistentLogManager.getLastLogIndex()) {
                return true;
            }
        }
        return false;
    }


    public void leaderVoteResponseHandler(RemotePeer peer, Raft.LeaderVoteResponse response) {
        log.info("Vote response received {}", response.getVoteGranted());
        lock.lock();
        try {

            if (this.currentTerm > response.getTerm()) {
                log.info("Ignore the response, as it is from previous RPC request ");
                return;
            }
//        if (this.peerState != PeerState.CANDIDATE) {
//            log.info("Ignore the response, as it is not on election stage");
//            return;
//        }
            // 此处是否需要 降为follower 存疑
            if (this.currentTerm < response.getTerm()) {
                backToFollower(response.getTerm());
            }
            if (response.getVoteGranted()) {
                peer.updateVoteStatus(true);
                if (cluster.isValidLeader()) {
                    becomeLeader();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void voteForSelf() {
        this.voteFor = candidateId;
        this.cluster.getLocalPeer(this.peerEndPoint).setVoteGranted(true);
    }

    public void voteForLeader() {
        this.lock.lock();
        try {
            this.peerState = PeerState.CANDIDATE;
            this.voteFor = this.candidateId;
            this.cluster.resetStatus();
            voteForSelf();
            this.currentTerm++;
            log.info("Election timeout, start to vote for leader in term {}", currentTerm);
            cluster.getRemotePeer(this.peerEndPoint)
                    .forEach(peer -> {
                        final CompletableFuture<Raft.LeaderVoteResponse> leaderVoteResponseCompletableFuture = peer.getRpcClient().leaderVoteRequest(createLeaderVoteRequest());
                        leaderVoteResponseCompletableFuture.thenAcceptAsync((response) -> leaderVoteResponseHandler(peer, response), rpcExecutors);
                    });
        } finally {
            this.lock.unlock();
        }
    }


    public void becomeLeader() {
        this.lock.lock();
        try {
            if (this.peerState == PeerState.LEADER) {
                return; // prevent afterward vote response cause duplicated execution
            }
            log.info("Become as a leader");
            this.peerState = PeerState.LEADER;
            cluster.leaderSetup(this.persistentLogManager.getLastLogIndex() + 1);
            stopElectionTimer();
            resetHeartbeatTimer();
        } finally {
            this.lock.unlock();
        }
    }


    private Raft.LeaderVoteRequest createLeaderVoteRequest() {
        return Raft.LeaderVoteRequest.newBuilder().setTerm(currentTerm)
                .setCandidateId(this.candidateId)
                .setLastLogIndex(persistentLogManager.getLastLogIndex())
                .setLastLogTerm(persistentLogManager.getLastLogTerm())
                .build();


    }


    public boolean logAppendResponseHandler(RemotePeer peer, Raft.AppendEntryResponse response) {

        return false;
    }


    public boolean logCommitResponseHandler(Raft.AppendEntryResponse response) {
        return false;
    }


    public boolean logAppend() {
        return false;
    }

    public void resetHeartbeatTimer() {
        heartbeatScheduledFuture = heartbeatExecutors.scheduleWithFixedDelay(() -> sendHeartbeat(), 0, 1000, TimeUnit.MILLISECONDS);
    }

    public void sendHeartbeat() {
        cluster.getRemotePeer(this.peerEndPoint)
                .forEach(peer -> {
                    final CompletableFuture<Raft.AppendEntryResponse> appendEntryResponseCompletableFuture = peer.getRpcClient().appendLogEntryRequest(createHeartbeatRequest(peer));
                    appendEntryResponseCompletableFuture.thenAcceptAsync(response -> logAppendResponseHandler(peer, response), rpcExecutors);
                });
    }

    private Raft.AppendEntryRequest createHeartbeatRequest(RemotePeer peer) {
        return Raft.AppendEntryRequest.newBuilder()
                .setTerm(this.currentTerm)
                .setLeaderId(this.candidateId)
                .setPreLogIndex(peer.getLatestLogEntryIndex())
                .setPreLogTerm(peer.getLatestLogEntryTerm())
                .setLeaderCommit(committedIndex)
                .build();

    }

    public Raft.AppendEntryResponse logAppendRequestHandler(Raft.AppendEntryRequest request) {
        log.info("Received heartbeat from leader {} in term {},({},{},{})", request.getLeaderId(), request.getTerm(), request.getPreLogTerm(), request.getPreLogIndex(), request.getLeaderCommit());
        resetElectionTimer();
        final Raft.AppendEntryResponse.Builder responseBuilder = Raft.AppendEntryResponse.newBuilder();
        responseBuilder.setTerm(this.getCurrentTerm());
        // when local server in candidate state, waiting for vote response,
        //If the term in the append log RPC is smaller than the candidate’s current term, then the candidate rejects the RPC and continues in candidate state
        if (request.getTerm() < this.currentTerm) {
            responseBuilder.setSuccess(false);
            return responseBuilder.build();
        }
        //If the leader’s term (included in its RPC) is at least as large as the candidate’s current term, then the candidate recognizes the leader as legitimate and returns to follower state.
        backToFollower(request.getTerm());
        final Optional<Raft.Entry> logEntryByIndexOpt = persistentLogManager.getLogEntryByIndex(request.getPreLogIndex());
        if (!logEntryByIndexOpt.isPresent()) {
            responseBuilder.setSuccess(false);
            return responseBuilder.build();
        }
        final Raft.Entry entry = logEntryByIndexOpt.get();
        if (entry.getTerm() != request.getTerm()) {
            persistentLogManager.deleteFromEntry(entry);
        }
        if (request.getEntriesList().size() == 0) {
            //heartbeat rpc
            responseBuilder.setSuccess(true);
        } else {

        }
        // todo commitIndex update

        return responseBuilder.build();
    }

    public boolean logCommit() {
        return false;
    }


    private enum PeerState {
        FOLLOWER, CANDIDATE, LEADER
    }


    /**
     * pre requirement:
     * 1. local.term <= request.term
     * 2. in lock
     */
    public void backToFollower(long requestTerm) {
        this.currentTerm = requestTerm;
        if (this.peerState != PeerState.FOLLOWER) {
            if (this.peerState == PeerState.CANDIDATE) {
                this.peerState = PeerState.FOLLOWER;
            } else {
                if (this.peerState == PeerState.LEADER) {
                    stopHeartBeat();
                }
            }
        }
        resetElectionTimer();
    }

    private void stopHeartBeat() {
        if (heartbeatScheduledFuture != null && !heartbeatScheduledFuture.isDone()) {
            heartbeatScheduledFuture.cancel(true);
        }
    }
}
