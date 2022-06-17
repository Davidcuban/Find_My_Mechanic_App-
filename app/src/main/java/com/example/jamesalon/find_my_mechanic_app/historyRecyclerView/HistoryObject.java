package com.example.jamesalon.find_my_mechanic_app.historyRecyclerView;

/**
 * Created by James Alon on 02-Mar-20.
 */

public class HistoryObject {
    private String historyId;
    private String time;

    public HistoryObject(String historyId,String time){
        this.historyId = historyId;
        this.time = time;
    }
    public String getHistoryId(){
        return historyId;
    }
    public void setHistoryId(String historyId){
        this.historyId = historyId;
    }

    public String getTime(){
        return time;
    }
    public void setTime(String time){
        this.time = time;
    }
}
