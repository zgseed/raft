package com.github.zgseed;

import com.beust.jcommander.Parameter;
import lombok.Data;

/**
 * @author:zhuge
 * @date:2018/10/17
 */
@Data
public class Args {
    @Parameter(names = "-servers",required = true,description = "Clusters peer list")
    private String servers;

    @Parameter(names ="-id",required = true,description = "Local peer server id")
    private String peerId;
}
