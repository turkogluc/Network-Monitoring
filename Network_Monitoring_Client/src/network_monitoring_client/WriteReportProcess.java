/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network_monitoring_client;



import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

/**
 *
 * @author yagmur
 */
public class WriteReportProcess implements Runnable {

    private Thread writing_t;
    private String writing_report_thread;
    static FileWriter report_fw;
    static PrintWriter writer;
    static String file_name = "report.json";
    static long MIN_TIME_VALUE = 1000 * 10 * 1; // miliseconds*seconds*minute 10 minutes
    static int time_factor = 1; //should be finalized by server's user by indicating period
    static String timeStampEnd;
    static String timeStampStart;
    static JSONObject data;
    private Date start_time;
    private Date internal_time;
    static long TIME_UNTIL_SEND=1000*60*60;
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public WriteReportProcess(String threadName) {
        this.writing_report_thread = threadName;
    }

    @Override
    public synchronized void run() {
        System.out.println("In write run");
        while (true) {
            try {
                
                Client.lock.lock(); //for critical section usage
                report_fw = new FileWriter(file_name);
                timeStampStart = new SimpleDateFormat("yyyy/MM/dd;HH:mm:ss").format(Calendar.getInstance().getTime());
                start_time=new Date();
                
                Thread.sleep(time_factor * MIN_TIME_VALUE);
                BufferedWriter bw = new BufferedWriter(report_fw);
                writer = new PrintWriter(bw);

                // update
                data = new JSONObject();
                printTrafficStatistics();
                PcapDissection.resolveIPaddresses(PcapDissection.ipAddressesVisited);
                printIPaddressesVisited(PcapDissection.ipAddressesVisited);
                printBandwidth();
                writer.print(data.toJSONString());
                //
                
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(WriteReportProcess.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(WriteReportProcess.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                internal_time=new Date();
                long diff=internal_time.getTime()-start_time.getTime();
                if(diff==TIME_UNTIL_SEND){
                    Client.lock.unlock();
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                PcapDissection.clearAllData();
            }
        }
    }

    public void start() {
        if (writing_t == null) {
            writing_t = new Thread(this, writing_report_thread);
            writing_t.start();
        }
    }

    /**
     * Prints traffic statistics related to different protocols from different
     * OSI layers Protocols include: Ethernet, ARP, IP, TCP/UDP, SSL/TLS, DNS,
     * HTTP
     */
    static void printTrafficStatistics() {
        try {
            //for avoiding divide by zero exception
            if (PcapDissection.numberOfPackets == 0) {
                PcapDissection.numberOfPackets = 1;
            }

            timeStampEnd = new SimpleDateFormat("yyyy/MM/dd;H:mm:ss").format(Calendar.getInstance().getTime());

            data.put("timeStampStart", timeStampStart);
            data.put("timeStampEnd", timeStampEnd);
            data.put("numberOfPackets", PcapDissection.numberOfPackets);
            data.put("numberOfPacketsSent", PcapDissection.numberOfPacketsSent);
            data.put("numberOfPacketsReceived", PcapDissection.numberOfPacketsReceived);
            data.put("numberOfARPpackets", PcapDissection.numberOfARPpackets);
            data.put("numberOfTcpPackets", PcapDissection.numberOfTcpPackets);
            data.put("numberOfSslTls", PcapDissection.numberOfSslTls);
            data.put("numberOfUdpPackets", PcapDissection.numberOfUdpPackets);
            data.put("numberOfDNS", PcapDissection.numberOfDNS);
            data.put("numberOfHTTPpackets", PcapDissection.numberOfHTTPpackets);
            data.put("numberOfGETS", PcapDissection.numberOfGETS);
            data.put("numberOfPosts", PcapDissection.numberOfPosts);

//            writer.println("THE NETWORK REPORT FOR IN RANGE;" + timeStampStart + ";TO;" + timeStampEnd);
//            writer.println("PART1");
//            writer.printf("%s%s%d\n", "Total number of packets in pcap", ";", PcapDissection.numberOfPackets);
//            writer.printf("%s%s%s%d%s%f%s\n", "Number of packets sent from;", PcapDissection.macAddress, ";", PcapDissection.numberOfPacketsSent, ";", ((float) PcapDissection.numberOfPacketsSent / PcapDissection.numberOfPackets) * 100, "%");
//            writer.printf("%s%s%s%d%s%f%s\n", "Number of packets sent to;", PcapDissection.macAddress, ";", PcapDissection.numberOfPacketsReceived, ";", ((float) PcapDissection.numberOfPacketsReceived / PcapDissection.numberOfPackets) * 100, "%");
//            writer.printf("%s%s%d\n", "ARP packets", ";", PcapDissection.numberOfARPpackets);
//
//            writer.printf("%s%s%d\n", "TCP packets", ";", PcapDissection.numberOfTcpPackets);
//            writer.printf("%s%s%d\n", "SSL/TLS packets", ";", PcapDissection.numberOfSslTls);
//
//            writer.printf("%s%s%d\n", "UDP packets", ";", PcapDissection.numberOfUdpPackets);
//            writer.printf("%s%s%d\n", "DNS packets", ";", PcapDissection.numberOfDNS);
//            writer.printf("%s%s%d\n", "HTTP packets", ";", PcapDissection.numberOfHTTPpackets);
//            writer.printf("%s%s%d\n", "Number of  GET requests", ";", PcapDissection.numberOfGETS);
//            writer.printf("%s%s%d\n", "Number of POST requests", ";", PcapDissection.numberOfPosts);
        } catch (ArithmeticException e) {
            Logger.getLogger("Divided by zero");
        }
    }

    private static void printBandwidth() throws Exception {
        //writer.println("PART2-SENT");
        
        
        data.put("SentPacketsSize",PcapDissection.sent_packets_with_size);
        data.put("ReceivedPacketsSize",PcapDissection.received_packets_with_size);
        
//        Set<String> keys1 = PcapDissection.sent_packets_with_size.keySet();
//        for (int i = 0; i < keys1.size(); i++) {
//            String ip_key = keys1.iterator().next();
//            String domain_name = PcapDissection.resolveNetname(ip_key);
//            writer.println(ip_key + ";" + domain_name + ";" + PcapDissection.sent_packets_with_size.get(ip_key));
//        }
//        
//        //writer.println("PART3-RECEIVED");
//        Set<String> keys2 = PcapDissection.received_packets_with_size.keySet();
//        for (int i = 0; i < keys2.size(); i++) {
//            String ip_key = keys2.iterator().next();
//            String domain_name = PcapDissection.resolveNetname(ip_key);
//            writer.println(ip_key + ";" + domain_name + ";" + PcapDissection.received_packets_with_size.get(ip_key));
//        }
    }

    /**
     * Prints the IP addresses that were visited along with their netnames
     *
     * @param ipAddressesVisited
     */
    static void printIPaddressesVisited(HashMap<String, String> ipAddressesVisited) throws Exception {

        JSONObject temp = new JSONObject();

        temp.putAll(ipAddressesVisited);
        data.put("ipAddressesVisited", temp);

    }

}

/**
 * Prints the ports that have been used
 *
 * @param portsUsed
 */
/*static void printPortsUsed(String machine, TreeSet<Integer> portsUsed) {
        writer.println();
        writer.println(machine + " PORTS UTILISED");
        int i = 0;
        for (int port : portsUsed) {
            i++;
            writer.printf("%d", port);
            if (i % 18 == 0) {
                writer.println();
            }
        }
        writer.println();
    }*/
/**
 * Prints the distributions among different TCP flags TCP Flags include: [SYN],
 * [SYN ACK], [ACK], [PSH ACK] [FIN PSH ACK], [FIN ACK], [RST]
 */
/*static void printTCPflagsStatistics() {
        writer.println();
        writer.println("TCP Flags distribution: ");
        writer.printf("%-12s %s %8d %5.2f %s\n", "SYN", ";", PcapDissection.numberOfSYN, ((float) PcapDissection.numberOfSYN) / PcapDissection.numberOfTcpPackets * 100, "%");
        writer.printf("%-12s %s %8d %5.2f %s\n", "SYN ACK", ";", PcapDissection.numberOfSYNACK, ((float) PcapDissection.numberOfSYNACK) / PcapDissection.numberOfTcpPackets * 100, "%");
        writer.printf("%-12s %s %8d %5.2f %s\n", "ACK", ";", PcapDissection.numberOfACK, ((float) PcapDissection.numberOfACK) / PcapDissection.numberOfTcpPackets * 100, "%");
        writer.printf("%-12s %s %8d %5.2f %s\n", "PSH ACK", ";", PcapDissection.numberOfPSHACK, ((float) PcapDissection.numberOfPSHACK) / PcapDissection.numberOfTcpPackets * 100, "%");
        writer.printf("%-12s %s %8d %5.2f %s\n", "FIN PSH ACK", ";", PcapDissection.numberOfFINPSHACK, ((float) PcapDissection.numberOfFINPSHACK) / PcapDissection.numberOfTcpPackets * 100, "%");
        writer.printf("%-12s %s %8d %5.2f %s\n", "FIN ACK", ";", PcapDissection.numberOfFINACK, ((float) PcapDissection.numberOfFINACK) / PcapDissection.numberOfTcpPackets * 100, "%");
        writer.printf("%-12s %s %8d %5.2f %s\n", "RST", ";", PcapDissection.numberOfRST, ((float) PcapDissection.numberOfRST) / PcapDissection.numberOfTcpPackets * 100, "%");
        writer.println();
    }*/
