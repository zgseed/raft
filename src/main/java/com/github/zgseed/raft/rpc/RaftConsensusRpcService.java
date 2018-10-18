package com.github.zgseed.raft.rpc;

import com.github.zgseed.rpc.Raft;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public interface RaftConsensusRpcService extends Closeable {
    CompletableFuture<Raft.AppendEntryResponse> appendLogEntry(Raft.AppendEntryRequest request);

    CompletableFuture<Raft.LeaderVoteRequest> leaderVoteRequest(Raft.LeaderVoteRequest request);


//    void installSnapshot();

}
