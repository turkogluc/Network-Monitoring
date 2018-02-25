/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network_monitoring_server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 *
 * @author cemal
 */
class UDPServerListener implements Runnable {

    //private BufferedReader inFromUser = null;
    private DatagramSocket socket = null;
    private DatagramPacket receivePacket = null;
    private int PORT = 0;
    private byte[] receiveData = null;
    private String receivedSentence = null;
    private ArrayList<String> Liste;
    private Thread thread;

    UDPServerListener(int PORT) {
        try {
            this.PORT = PORT;
            socket = new DatagramSocket(PORT, InetAddress.getByName("0.0.0.0"));
            //inFromUser = new BufferedReader(new InputStreamReader(System.in));
            //socket.getBroadcast();
            receiveData = new byte[1024];
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            Liste = new ArrayList<>();
        } catch (SocketException | UnknownHostException ex) {
            System.err.println(ex);
        }
    }

    /* socket.receive() methodu paket gelene kadar bloklayacak
     * Bunu engellemek için Listen methodu ayrı bir thread'de yapılmalı
     *
     */
    private void Listen() {
        while (true) {
            try {
                socket.receive(receivePacket);
                String receivedIP = receivePacket.getAddress().toString();
                System.out.println("asfa");
                if (!Liste.contains(receivedIP)) {
                    Liste.add(receivedIP);
                    receivedSentence = new String(receivePacket.getData());
                    System.out.println("FROM SERVER:" + receivedSentence);
                    break;
                }
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
    }

    public byte[] getReceiveData() {
        return receiveData;
    }

    public ArrayList<String> getClientList() {
        return Liste;
    }

//    public static void main(String[] args) throws UnknownHostException, SocketException, IOException{
//        
//        UDPServerListener test = new UDPServerListener(8888);
//        test.Listen();
//        ArrayList<String> l = test.getClientList();
//        for(String s:l){
//            System.out.println(s);
//        }
//        
//        
//    }
    @Override
    public void run() {
        while (true) {
            System.out.println("asaf");
            try {
                System.out.println("taha");
                socket.receive(receivePacket);
                String receivedIP = receivePacket.getAddress().toString();
                System.out.println("123");
                if (!Liste.contains(receivedIP)) {
                    Liste.add(receivedIP);
                    receivedSentence = new String(receivePacket.getData());
                    System.out.println("FROM SERVER:" + receivedSentence);
                    break;
                }
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this, "Listen Thread");
            thread.start();
        }
    }

}
