package com.github.zgseed.raft;

import lombok.Builder;
import lombok.Data;

/**
 * @author:zhuge
 * @date:2018/10/10
 */
@Data
public class PeerEndPoint {
    private String peerId;
    private String host;
    private int port;

    @Builder
    public PeerEndPoint(String peerId, String host, int port) {
        this.peerId = peerId;
        this.host = host;
        this.port = port;
    }
}
