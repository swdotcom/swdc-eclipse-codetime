package com.swdc.codetime.models;

public class CommitChangeStats {
    public int insertions = 0;
    public int deletions = 0;
    public int fileCount = 0;
    public int commitCount = 0;
    public boolean committed = false;

    public CommitChangeStats(boolean committed) {
        this.committed = committed;
    }

}