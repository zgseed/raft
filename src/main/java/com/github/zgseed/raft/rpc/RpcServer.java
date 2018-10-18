package com.github.zgseed.raft.rpc;

import com.github.zgseed.raft.PeerEndPoint;
import com.github.zgseed.raft.PeerServer;
import com.github.zgseed.rpc.Raft;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author:zhuge
 * @date:2018/10/11
 */
public class RpcServer implements RaftConsensusRpcService {

    private PeerEndPoint endPoint;
    private PeerServer peerServer;
    private Server server;

    public RpcServer(PeerServer peerServer) throws IOException {
        this.endPoint = peerServer.getPeerEndPoint();
        this.peerServer = peerServer;
        start();
    }

    public void start() throws IOException {
        server = ServerBuilder
                .forPort(endPoint.getPort())
                .addService(new LeaderVoteServiceImpl(peerServer))
                .addService(new AppendEntryServiceImpl(peerServer))
                .build();

        server.start();
    }

    public void stop() throws InterruptedException {
        server.shutdown();
        server.awaitTermination(5, TimeUnit.SECONDS);
        server.shutdownNow();
    }


    @Override
    public CompletableFuture<Raft.AppendEntryResponse> appendLogEntry(Raft.AppendEntryRequest request) {
        return null;
    }


    @Override
    public CompletableFuture<Raft.LeaderVoteRequest> leaderVoteRequest(Raft.LeaderVoteRequest request) {
        return null;
    }

    @Override
    public void close() throws IOException {
        try {
            this.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
