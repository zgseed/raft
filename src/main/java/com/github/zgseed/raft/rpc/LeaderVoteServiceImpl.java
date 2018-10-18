package com.github.zgseed.raft.rpc;

import com.github.zgseed.raft.PeerServer;
import com.github.zgseed.rpc.LeaderVoteGrpc;
import com.github.zgseed.rpc.Raft;
import io.grpc.stub.StreamObserver;

/**
 * @author:zhuge
 * @date:2018/10/11
 */
public class LeaderVoteServiceImpl extends LeaderVoteGrpc.LeaderVoteImplBase {
    private PeerServer peerServer;

    public LeaderVoteServiceImpl(PeerServer peerServer) {
        this.peerServer = peerServer;
    }

    @Override
    public void leaderRequest(Raft.LeaderVoteRequest request,
                              StreamObserver<Raft.LeaderVoteResponse> responseObserver) {
        try {
            final Raft.LeaderVoteResponse response = peerServer.leaderVoteRequestHandler(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }catch (Throwable t){
            responseObserver.onError(t);
        }
    }
}
