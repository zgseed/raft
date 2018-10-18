package com.github.zgseed.raft;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author:zhuge
 * @date:2018/10/17
 */
@Data
@AllArgsConstructor
public class RaftConfiguration {
    private Map<String, PeerEndPoint> clusterPeerMap;


    public Optional<PeerEndPoint> getLocalPeer(String peerId) {
        return Optional.ofNullable(clusterPeerMap.get(peerId));
    }

    public int getPeerSize() {
        return clusterPeerMap.size();
    }


    public Set<PeerEndPoint> allPeerEndPoint() {
        return Sets.newHashSet(clusterPeerMap.values());
    }
}
