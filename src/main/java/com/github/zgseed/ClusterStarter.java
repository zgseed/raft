package com.github.zgseed;

import com.beust.jcommander.JCommander;
import com.github.zgseed.raft.PeerEndPoint;
import com.github.zgseed.raft.PeerServer;
import com.github.zgseed.raft.RaftConfiguration;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

/**
 * @author:zhuge
 * @date:2018/10/17
 */
public class ClusterStarter {


    public static void main(String[] args) throws IOException {
        final Args commandArgs = new Args();
        JCommander.newBuilder()
                .addObject(commandArgs)
                .build().parse(args);
        final ClusterStarter main = new ClusterStarter();
        final Map<String, PeerEndPoint> clusterPeersMap = main.parse(commandArgs.getServers());
        final RaftConfiguration raftConfiguration = new RaftConfiguration(clusterPeersMap);
        new PeerServer(commandArgs.getPeerId(), raftConfiguration);
//        for (String peerId : clusterPeersList.keySet()) {
//            final PeerServer peerServer = new PeerServer(peerId, raftConfiguration);
//        }
        LockSupport.park();
    }

//    private Properties readProperties(String filePath){
//        final File configFile = new File(filePath);
//        if(!configFile.exists()|| !configFile.isFile()){
//            System.err.println(String.format("Config file %s does not exist or not a file", filePath));
//            System.exit(1);
//        }
//        final Properties properties = new Properties();
//        try {
//            properties.load(new FileReader(configFile));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return properties;
//    }

    private Map<String,PeerEndPoint> parse(String servers){
        final String[] serverStrList = servers.split(",");
        Preconditions.checkArgument(serverStrList.length>2,"Peers size should at least 3");
       return Arrays.stream(serverStrList).map(s ->{
            final String[] parts = s.split(":");
            Preconditions.checkArgument(parts.length==3,"Invalid format of endpoint");
            int port =0;
            try {
                port = Integer.parseInt(parts[2]);
            }catch (NumberFormatException e){
                System.err.println("Invalid port format of endpoint");
                System.exit(1);
            }
            return new PeerEndPoint(parts[0],parts[1] ,port );
        }).collect(Collectors.toMap(PeerEndPoint::getPeerId, endPoint -> endPoint));
    }
}
