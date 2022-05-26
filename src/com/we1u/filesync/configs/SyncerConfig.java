package com.we1u.filesync.configs;

public class SyncerConfig {

    public boolean oneToTwo;
    public boolean twoToOne;

    public SyncerConfig(boolean _oneToTwo, boolean _twoToOne){
        this.oneToTwo = _oneToTwo;
        this.twoToOne = _twoToOne;
    }
}
