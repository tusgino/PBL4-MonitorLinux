package com.pbl4.models;

public class SocketMonitor {
    private String netID; // TCP = 0 or UDP = 1
    public String getNetID() {
        return netID;
    }

    public void setNetID(String netID) {
        this.netID = netID;
    }
    public String temp = "2222"; 
    private String stateProp; 
    public String getstateProp() {
        return stateProp;
    }

    public void setstateProp(String stateProp) {
        this.stateProp = stateProp;
    }

    public Long getSend_Q() {
        return send_Q;
    }

    public void setSend_Q(Long send_Q) {
        this.send_Q = send_Q;
    }

    private Long send_Q;
    public Long getRecv_Q() {
        return recv_Q;
    }

    public void setRecv_Q(Long recv_Q) {
        this.recv_Q = recv_Q;
    }

    private Long recv_Q;
    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    private String localAddress;
    private Integer localPort;
    public Integer getLocalPort() {
        return this.localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public String getPeerAddress() {
        return peerAddress;
    }

    public void setPeerAddress(String peerAddress) {
        this.peerAddress = peerAddress;
    }

    private String peerAddress;
    private Integer peerPort;

    public Integer getPeerPort() {
        return peerPort;
    }

    public void setPeerPort(Integer peerPort) {
        this.peerPort = peerPort;
    }

    

    //private HostProcess hostProcess = new HostProcess();
    private String hostProcess;
    public String getHostProcess() {
        return hostProcess;
    }

    public void setHostProcess(String hostProcess) {
        this.hostProcess = hostProcess;
    }



 }
