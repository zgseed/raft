package com.github.zgseed.raft;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author:zhuge
 * @date:2018/10/10
 */
@Data
@Slf4j
public class Cluster {
    private Map<String, RemotePeer> peerList;


    public Cluster() {
        this.peerList = Maps.newHashMap();
    }

    public Cluster(RaftConfiguration raftConfiguration) {
        this.peerList = Maps.newHashMap();
        raftConfiguration.allPeerEndPoint().stream().forEach(this::addPeer);
    }

    public void addPeer(PeerEndPoint endPoint) {
        this.peerList.putIfAbsent(endPoint.getPeerId(),new RemotePeer(endPoint));
    }


    public boolean isValidLeader() {
        return peerList.values().stream().filter(RemotePeer::isVoteGranted).count() > getClusterSize() / 2;
    }

    public int getClusterSize() {
        return peerList.size();
    }

    public void resetStatus() {
        peerList.values().forEach(RemotePeer::reset);
    }

    public Set<RemotePeer> getRemotePeer(PeerEndPoint local) {
        return this.peerList.values().stream().filter(remotePeer -> !remotePeer.getEndPoint().equals(local)).collect(Collectors.toSet());
    }

    public RemotePeer getLocalPeer(PeerEndPoint local) {
        return this.peerList.values().stream().filter(remotePeer -> remotePeer.getEndPoint().equals(local)).findFirst().get();

    }

    public void leaderSetup(long nextIndex) {
        peerList.values().forEach(peer -> {
            peer.setNextIndex(nextIndex);
        });
    }
}
