package com.github.zgseed.raft.rpc;

import com.github.zgseed.raft.PeerServer;
import com.github.zgseed.rpc.AppendEntryGrpc;
import com.github.zgseed.rpc.Raft;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

/**
 * @author:zhuge
 * @date:2018/10/11
 */
@Slf4j
public class AppendEntryServiceImpl extends AppendEntryGrpc.AppendEntryImplBase {

    private PeerServer peerServer;

    public AppendEntryServiceImpl(PeerServer peerServer) {
        this.peerServer = peerServer;
    }

    @Override
    public void appendEntry(Raft.AppendEntryRequest request,
                            StreamObserver<Raft.AppendEntryResponse> responseObserver){
        try {
            final Raft.AppendEntryResponse response = peerServer.logAppendRequestHandler(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }catch (Throwable t){
            responseObserver.onError(t);
        }
    }
}
