package com.github.zgseed.raft.service;

/**
 * @author:zhuge
 * @date:2018/10/10
 */
public interface PeerServerService {
    void electionTimeOut();
    void leaderVoteHandler();
    void voteForLeader();
    boolean logAppendHandler();
    boolean logCommitHandler();
}
