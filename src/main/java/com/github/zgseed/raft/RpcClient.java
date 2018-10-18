package com.github.zgseed.raft;

import com.github.zgseed.rpc.AppendEntryGrpc;
import com.github.zgseed.rpc.LeaderVoteGrpc;
import com.github.zgseed.rpc.Raft;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author:zhuge
 * @date:2018/10/10
 */
@Slf4j
public class RpcClient implements Closeable {


    private RemotePeer remotePeer;

    private final ManagedChannel channel;
    private final LeaderVoteGrpc.LeaderVoteFutureStub leaderVoteRpcasyncStub;
    private final AppendEntryGrpc.AppendEntryFutureStub appendEntryRpcAsynStub;

    public RpcClient(RemotePeer remotePeer) {
        this.remotePeer = remotePeer;
        final PeerEndPoint peerEndPoint = remotePeer.getEndPoint();
        final ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(peerEndPoint.getHost(), peerEndPoint.getPort()).usePlaintext();

        channel = channelBuilder.build();
        leaderVoteRpcasyncStub = LeaderVoteGrpc.newFutureStub(channel);
        appendEntryRpcAsynStub = AppendEntryGrpc.newFutureStub(channel);
    }


    private void shutdownQuietly() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            if (!channel.isTerminated()) channel.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Raft.LeaderVoteResponse> leaderVoteRequest(Raft.LeaderVoteRequest request) {
        log.info("Send a vote request candidateId = {} ,term = {},lastLogTerm = {},lastLogIndex = {},",request.getCandidateId(),request.getTerm(),request.getLastLogTerm(),request.getLastLogIndex());
        final ListenableFuture<Raft.LeaderVoteResponse> leaderVoteListenableFuture = leaderVoteRpcasyncStub.leaderRequest(request);
        return FutureConverter.toCompletableFuture(leaderVoteListenableFuture);
    }

    public CompletableFuture<Raft.AppendEntryResponse> appendLogEntryRequest(Raft.AppendEntryRequest request) {
        final ListenableFuture<Raft.AppendEntryResponse> appendEntryListenableFuture = appendEntryRpcAsynStub.appendEntry(request);
        return FutureConverter.toCompletableFuture(appendEntryListenableFuture);
    }


    @Override
    public void close() {
        this.shutdownQuietly();
    }
}
