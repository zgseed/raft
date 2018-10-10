package com.github.zgseed.raft;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.github.zgseed.raft.service.PeerServerService;
import com.github.zgseed.rpc.Raft;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.*;

/**
 * @author:zhuge
 * @date:2018/10/10
 */
@Slf4j
public class PeerServer implements PeerServerService {
    /**
     * states on all peer
     */
    private PeerState peerState;
    //how to
    private int candidateId;
    private int currentTerm;
    private int votedFor;
    private volatile long commitedIndex;
    private volatile long lastApplied;

    /**
     * stages only on leader
     * re-init after election as leader
     */
    private volatile long nextIndex[];
    private volatile long matchIndex[];


    //    private long electionTimeOutInMillis;
    private volatile boolean isElectionTimeOut;
    private ScheduledThreadPoolExecutor electionTimeoutScheduleService;
    ScheduledFuture<?> electionTimeoutJobFuture;


    public PeerServer(Properties properties) {
        peerState = PeerState.FOLLOWER;

        isElectionTimeOut = false;
        electionTimeoutScheduleService = new ScheduledThreadPoolExecutor(2, new ThreadFactoryBuilder().setNameFormat("ElectionTimeout-Thread-%s")
                .setDaemon(true)
                .setUncaughtExceptionHandler((t, e) -> log.error("Failed to check election timeout in {}", t.getName(), e)).build());
        resetElectionTimer();
    }


    private long getElectionTimeOutInMillis() {
        return new Random().nextInt(150) + 150L;  //
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

    @Override
    public void electionTimeOut() {


        voteForLeader();

    }

    @Override
    public void leaderVoteHandler() {

    }

    @Override
    public void voteForLeader() {
        this.peerState = PeerState.CANDIDATE;
        this.currentTerm++;

    }


    private Raft.LeaderVoteRequest createLeaderVoteRequest(){
        Raft.LeaderVoteRequest.newBuilder().setTerm(currentTerm)
                .setCandidateId(this.candidateId)

    }


    @Override
    public boolean logAppendHandler() {
        return false;
    }

    @Override
    public boolean logCommitHandler() {
        return false;
    }


    private enum PeerState {
        FOLLOWER, CANDIDATE, LEADER
    }
}
