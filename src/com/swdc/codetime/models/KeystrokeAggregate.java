package com.swdc.codetime.models;

import com.swdc.codetime.util.KeystrokePayload.FileInfo;

public class KeystrokeAggregate {
    public int add = 0;
    public int close = 0;
    public int delete = 0;
    public int linesAdded = 0;
    public int linesRemoved = 0;
    public int open = 0;
    public int paste = 0;
    public int charsPasted = 0;
    public int keystrokes = 0;
    public String directory = "";

    public void aggregate(FileInfo fileInfo) {
        this.add += fileInfo.add;
        this.keystrokes += fileInfo.keystrokes;
        this.paste += fileInfo.paste;
        this.charsPasted += fileInfo.charsPasted;
        this.open += fileInfo.open;
        this.close += fileInfo.close;
        this.delete += fileInfo.delete;
        this.linesAdded += fileInfo.linesAdded;
        this.linesRemoved += fileInfo.linesRemoved;
    }
}