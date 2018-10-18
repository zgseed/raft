package com.github.zgseed.raft.service;

/**
 * @author:zhuge
 * @date:2018/10/10
 */
public interface LeaderService {
    boolean logAppend();
    void sendHeartbeat();
    boolean logCommit();
}
