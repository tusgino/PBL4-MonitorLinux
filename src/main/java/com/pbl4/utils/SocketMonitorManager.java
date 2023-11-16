package com.pbl4.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

import com.pbl4.models.SocketMonitor;

public class SocketMonitorManager {
    private static final HashMap<String, SocketMonitor> sockets = new HashMap<>();

    public SocketMonitorManager() {
        
        Test();
    }

    private static void initInfos(List<SocketMonitor> socketsList, HashMap<String, SocketMonitor> socketsHashmap){
        try {
            String socketLine;
            Process p;
            p = Runtime.getRuntime().exec("ss -tulpn");
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            socketLine = input.readLine();
            while ((socketLine = input.readLine()) != null) {
                //System.out.println(socketLine);

                SocketMonitor socketMonitor = new SocketMonitor();

                String[] socketsInfos = socketLine.split("\\s+");


                socketMonitor.setNetID(socketsInfos[0].toUpperCase());
                socketMonitor.setstateProp(socketsInfos[1]);
                System.out.println(socketMonitor.getstateProp());
                socketMonitor.setRecv_Q(Long.parseLong(socketsInfos[2]));
                socketMonitor.setSend_Q(Long.parseLong(socketsInfos[3]));
                {
                    String[] addressInfo = getPort(socketsInfos[4]);
                    //System.out.println(addressInfo[0] + "\t\t" + Integer.parseInt(addressInfo[1]) + "\t\t" + "*".equals(addressInfo[1]));
                    socketMonitor.setLocalAddress(addressInfo[0]);
                    socketMonitor.setLocalPort("*".equals(addressInfo[1])?0:Integer.parseInt(addressInfo[1]));
                }

                {
                    String[] addressInfo = getPort(socketsInfos[5]);
                    
                    socketMonitor.setPeerAddress(addressInfo[0]);
                    socketMonitor.setPeerPort(Integer.parseInt(addressInfo[1].trim().equals("*")?"0":addressInfo[1]));
                }

                if(socketsInfos.length == 7)
                    socketMonitor.setHostProcess(socketsInfos[6]);

                socketsList.add(socketMonitor);
                socketsHashmap.put(socketsInfos[4], socketMonitor);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



    public static void main(String[] args) {
        new SocketMonitorManager();
    }

    private static String[] getPort(String str){
        int index = str.lastIndexOf(':');

        String[] strs = new String[2];

        strs[0] = str.substring(0, index);
        strs[1] = str.substring(index + 1);

        

        return strs;
    }

    private void Test(){
        for (String key : sockets.keySet()) {
            SocketMonitor socket = sockets.get(key);

            System.out.print(socket.getLocalAddress()+ ":\t" +socket.getLocalPort());
            // System.out.print(" " +socket.getHostProcess());

            System.out.println();
        }
    }

    public static void getSocketsInfo(List<SocketMonitor> socketsList, HashMap<String, SocketMonitor> socketsHashmap){
        initInfos(socketsList, socketsHashmap);
    }
}
