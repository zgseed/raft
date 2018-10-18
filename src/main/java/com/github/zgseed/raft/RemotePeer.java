package com.github.zgseed.raft;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author:zhuge
 * @date:2018/10/11
 */
@Data
@Slf4j
public class RemotePeer {
    private final PeerEndPoint endPoint;
    private boolean voteGranted;
    private volatile long nextIndex;
    private volatile long matchIndex;
    private final RpcClient rpcClient;

    public RemotePeer(PeerEndPoint endPoint) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(endPoint.getPeerId()), "Invalid candidate id");
        this.endPoint = endPoint;
        this.rpcClient = new RpcClient(this);
        reset();
    }

    public void reset() {
        voteGranted = false;
        this.nextIndex = 0;
        this.matchIndex = 0;

    }


    public void updateVoteStatus(boolean voteGranted) {
        log.info("peer {} grant vote {}", endPoint,voteGranted);
        this.voteGranted = voteGranted;
    }

    public long getLatestLogEntryIndex() {
        return nextIndex - 1;
    }

    public long getLatestLogEntryTerm() {
        return matchIndex;
    }

}
