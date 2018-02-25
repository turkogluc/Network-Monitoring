/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network_monitoring_client;

import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author yagmur
 */
public class Client {
    static ReentrantLock lock = new ReentrantLock(); //for controling critical section of project e.g report.txt
    public static void main(String[] args) {
        CommunicateWithBroadcast broadcast_thread = new CommunicateWithBroadcast("Broadcast via UDP Thread");
        broadcast_thread.start();
        PcapDissection pcap_thread = new PcapDissection("Packet Analyzing Thread");
        pcap_thread.start();
        WriteReportProcess writing_thread = new WriteReportProcess("Writing Report Thread");
        writing_thread.start();
        SendReportProcessViaTCP sending_thread = new SendReportProcessViaTCP("Sending Report via TCP Thread");
        sending_thread.start();
    }
}
