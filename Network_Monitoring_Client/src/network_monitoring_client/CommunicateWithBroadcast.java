/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network_monitoring_client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;

/**
 *
 * @author asafgwe
 */
public class CommunicateWithBroadcast implements Runnable{

    private DatagramSocket socket;
    private byte[] sendData;
    private int port = 8888;
    private DatagramPacket receivePacket;
    static int sec=30;
    String message;
    String broadcast_thread;
    Thread broadcast_t;

    public CommunicateWithBroadcast(String broadcast_thread) {
        this.broadcast_thread = broadcast_thread;
    }
    
    @Override
    public void run() {
        message= PcapDissection.findIpInNetworkInterface();
        broadcastPeriodicly(message);

    }
    public void broadcastPeriodicly(String message) {
        while (true) {
            try {
                broadcast(message);
                Thread.sleep((long) (sec * 1000));
            } catch (InterruptedException ex) {
                System.err.println(ex);
                break;
            }
        }
    }

    public void start() {
        if (broadcast_t == null) {
            broadcast_t = new Thread(this, broadcast_thread);
            broadcast_t.start();
        }

    }
    public void broadcast(String message) {
        sendData = message.getBytes();
        openSocket();
        try {
            sendPacket(InetAddress.getByName("255.255.255.255")); // default 255.255.255.255
        } catch (UnknownHostException ex) {
            System.err.println(ex);
        }
        broadcastToAllNetworkInterfaces();
        closeSocket();
    }

    private void openSocket() {
        try {

            socket = new DatagramSocket();
            socket.setBroadcast(true);
        } catch (SocketException ex) {
            System.err.println(ex);
        }
    }

    private void broadcastToAllNetworkInterfaces() {
        try {
            Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = (NetworkInterface) interfaces.nextElement();

                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) {
                        continue;
                    }
                    sendPacket(broadcast);
                }
            }
            System.out.println("Broadcast message over all network interfaces.");
        } catch (SocketException ex) {
            System.err.println(ex);
        }
    }

    private void sendPacket(InetAddress address) {
        try {
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(sendPacket);

        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    private void waitForResponse() {
        try {
            byte[] resBuf = new byte[15000];
            receivePacket = new DatagramPacket(resBuf, resBuf.length);
            socket.receive(receivePacket);
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    private boolean isMessageCorrect() {
        String message = new String(receivePacket.getData()).trim();
        return message.equals(Arrays.toString(sendData));
    }

    private void closeSocket() {
        if (!socket.isClosed()) {
            socket.close();
        }
    }

    public void setSendData(String sendData) {
        this.sendData = sendData.getBytes();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
