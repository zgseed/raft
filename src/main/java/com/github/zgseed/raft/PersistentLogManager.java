package com.github.zgseed.raft;

import com.github.zgseed.rpc.Raft;
import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;
import java.util.Optional;

/**
 * @author:zhuge
 * @date:2018/10/11
 */
@Data
public class PersistentLogManager {

    private List<Raft.Entry> logEntries;

    public PersistentLogManager(List<Raft.Entry> logEntries) {
        this.logEntries = logEntries;
    }

    public PersistentLogManager() {
        logEntries = Lists.newArrayList();
    }

    private Optional<Raft.Entry> lastEntry(){
        if(logEntries.isEmpty()){
            return Optional.empty();
        }
        return Optional.of(logEntries.get(logEntries.size()-1));
    }

    public Long getLastLogIndex(){
        return lastEntry().map(Raft.Entry::getIndex).orElse(0L);
    }

    public Long getLastLogTerm(){
        return lastEntry().map(Raft.Entry::getTerm).orElse(0L);
    }


    public Raft.Entry previousEntry(Raft.Entry entry){
        return null;
    }

    public Optional<Raft.Entry> getLogEntryByIndex(long index) {

        return null;
    }

    public void deleteFromEntry(Raft.Entry entry) {
    }
}
