#!/usr/bin/env bash
args_str="-id $1 -servers p1:localhost:8080,p2:localhost:8081,p3:localhost:8082"
clear
mvn exec:java -Dexec.mainClass="com.github.zgseed.ClusterStarter"  -Dexec.args="$args_str"
