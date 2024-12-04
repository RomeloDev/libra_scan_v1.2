package com.example.librascanapp;

public class LogEntry {

    private String logID, timestamp, purpose;

    public LogEntry(){

    }

    public LogEntry(String logID, String timestamp, String purpose){
        this.logID = logID;
        this.timestamp = timestamp;
        this.purpose = purpose;
    }

    public String getLogID(){
        return logID;
    }

    public String getTimestamp(){
        return timestamp;
    }
    public String getPurpose(){return purpose;}
}
