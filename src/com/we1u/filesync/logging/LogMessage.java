package com.we1u.filesync.logging;

import java.time.LocalDateTime;

public class LogMessage {
    public String text;
    public LogSeverity logSeverity;
    public LocalDateTime time;

    public LogMessage(String _text, LogSeverity _logSeverity, LocalDateTime _time){
        text = _text;
        logSeverity = _logSeverity;
        time = _time;
    }
}
