package com.pbl4.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import com.pbl4.models.SocketMonitor;

public class SocketMonitorManager {
    private final HashMap<String, SocketMonitor> sockets = new HashMap<>();

    public SocketMonitorManager() {
        getSocketsInfo();
        Test();
    }

    private void getSocketsInfo(){
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


                socketMonitor.setNetID(socketsInfos[0].toLowerCase()=="tcp"?0:1);
                socketMonitor.setstate(socketsInfos[1]);
                socketMonitor.setRecv_Q(Long.parseLong(socketsInfos[2]));
                socketMonitor.setSend_Q(Long.parseLong(socketsInfos[3]));
                {
                    String[] addressInfo = getPort(socketsInfos[4]);
                    
                    socketMonitor.setLocalAddress(addressInfo[0]);
                    socketMonitor.setLocalPort(Integer.parseInt(addressInfo[1].equals("*")?"0":addressInfo[1]));
                }

                {
                    String[] addressInfo = getPort(socketsInfos[5]);

                    socketMonitor.setPeerAddress(addressInfo[0]);
                    socketMonitor.setLocalPort(Integer.parseInt(addressInfo[1].equals("*")?"0":addressInfo[1]));
                }

                if(socketsInfos.length >= 7)
                    socketMonitor.setHostProcess(socketsInfos[6]);

                sockets.put(socketsInfos[4], socketMonitor);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



    public static void main(String[] args) {
        new SocketMonitorManager();
    }

    private String[] getPort(String str){
        int index = str.lastIndexOf(':');

        String[] strs = new String[2];

        strs[0] = str.substring(0, index);
        strs[1] = str.substring(index + 1);

        return strs;
    }

    private void Test(){
        for (String key : sockets.keySet()) {
            SocketMonitor socket = sockets.get(key);

            System.out.print(socket.getLocalAddress()+ ":" +socket.getLocalPort());
            System.out.println();
        }
    }
}
