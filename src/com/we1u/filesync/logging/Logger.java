package com.we1u.filesync.logging;


import javax.swing.JTextArea;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Logger {

    private final boolean date;
    private final boolean time;
    private final boolean severity;
    private ConcurrentLinkedQueue<LogMessage> logQueue;
    private Thread loggingThread;

    private final String datePattern = "dd/MM/yyyy";
    private final String timePattern = "HH:mm:ss";

    private final JTextArea output;

    public Logger(JTextArea _output, boolean _date, boolean _time, boolean _severity){
        this.output = _output;
        this.date = _date;
        this.time = _time;
        this.severity = _severity;
        this.logQueue = new ConcurrentLinkedQueue<>();
        loggingThread = new Thread(() -> {
            while(!loggingThread.isInterrupted()){
                if (!logQueue.isEmpty()){
                    output(logQueue.poll());
                }
            }
        });
        loggingThread.start();
    }

    public void log(String text, LogSeverity logSeverity){
        logQueue.add(new LogMessage(text, logSeverity, LocalDateTime.now()));
    }

    private void output(LogMessage logMessage){
        StringBuilder sb = new StringBuilder();
        if (severity) {
            sb.append(logMessage.logSeverity);
            sb.append(" ");
        }
        if (date){
            sb.append("[");
            String date = DateTimeFormatter.ofPattern(datePattern).format(logMessage.time);
            sb.append(date);
            sb.append("] ");
        }
        if (time){
            sb.append("[");
            String time = DateTimeFormatter.ofPattern(timePattern).format(logMessage.time);
            sb.append(time);
            sb.append("] ");
        }
        sb.append("- ");
        sb.append(logMessage.text);
        sb.append("\n");
        output.append(sb.toString());
    }
}
