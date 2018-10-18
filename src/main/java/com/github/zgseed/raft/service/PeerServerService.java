package com.github.zgseed.raft.service;

import com.github.zgseed.rpc.Raft;

/**
 * @author:zhuge
 * @date:2018/10/10
 */
public interface PeerServerService {
    void electionTimeOut();

    Raft.LeaderVoteResponse leaderVoteRequestHandler(Raft.LeaderVoteRequest request);
    void leaderVoteResponseHandler(Raft.LeaderVoteResponse response);

    void voteForLeader();
    void becomeLeader();

    boolean logAppendResponseHandler(Raft.AppendEntryResponse response);
    Raft.AppendEntryResponse logAppendRequestHandler(Raft.AppendEntryRequest request);

    boolean logCommitResponseHandler(Raft.AppendEntryResponse response);
}
