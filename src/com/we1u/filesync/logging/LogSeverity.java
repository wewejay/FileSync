package com.we1u.filesync.logging;

public enum LogSeverity{
    DEBUG("DEBUG"),
    INFO("INFO"),
    ERROR("ERROR");

    private final String value;

    LogSeverity(String _value){
        this.value = _value;
    }

    public String getValue() {
        return value;
    }
}