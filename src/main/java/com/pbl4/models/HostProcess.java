/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.pbl4.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;

/*
 * Properties of a process in linux OS
 */
public class HostProcess {

    private String user;
    private String pid;
    private String command;
    private String CPU;
    private String MEM;
    private String start;
    private String time;
    private String readableName;

    private boolean monitored;
    private Long timeStartFollow;

    public Long getTimeStartFollow() {
        return timeStartFollow;
    }

    public void setTimeStartFollow(Long timeStartFollow) {
        this.timeStartFollow = timeStartFollow;
    }

    private List<Long> memoryLogs;
    private BooleanProperty mProperty;

    public HostProcess() {
        monitored = false;
        mProperty = new SimpleBooleanProperty(false);
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCPU() {
        return CPU;
    }

    public void setCPU(String CPU) {
        this.CPU = CPU;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPID() {
        return pid;
    }

    public void setPID(String pid) {
        this.pid = pid;
    }

    public String getMEM() {
        return MEM;
    }

    public void setMEM(String MEM) {
        this.MEM = MEM;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getReadableName() {
        return readableName;
    }

    public void setReadableName(String readableName) {
        this.readableName = readableName;
    }

    public boolean isMonitored() {
        return monitored;
    }

    public BooleanProperty monitoredProperty() {
        return mProperty;
    }

    public void setMonitored(boolean monitored) {
        if (monitored) {
            memoryLogs = new ArrayList<>();
        } else {
            memoryLogs = null;
        }

        mProperty.setValue(monitored);
        this.monitored = monitored;
    }

    public List<Long> getMemoryLogs() {
        return memoryLogs;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof HostProcess)) {
            return false;
        }

        HostProcess another = (HostProcess) obj;

        return getKey().equals(another.getKey());
    }

    public void addMemoryLog(long log) {
        memoryLogs.add(log);
    }

    public String getKey() {
        return String.valueOf(pid) + String.valueOf(command);
    }

}
