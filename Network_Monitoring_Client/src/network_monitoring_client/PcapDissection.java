/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network_monitoring_client;

/**
 *
 * @author yagmur
 */
import java.io.File;
import org.apache.commons.net.whois.WhoisClient;
import org.jnetpcap.Pcap;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.packet.PcapPacketHandler;
import org.jnetpcap.packet.format.FormatUtils;
import org.jnetpcap.protocol.application.WebImage;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Icmp;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.network.Ip6;
import org.jnetpcap.protocol.tcpip.Http;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;

import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jnetpcap.PcapIf;

class PcapDissection implements Runnable {

    private Thread pcap_t;
    private String pcap_analyzing;
    static Pcap pcap;
    static final Ethernet ETHERNET = new Ethernet();
    static final Http HTTP = new Http();
    static final Tcp TCP = new Tcp();
    static final Udp UDP = new Udp();
    static final Ip4 IP = new Ip4();
    static final Icmp ICMP = new Icmp();
    static final Ip6 IP6 = new Ip6();
    static final WebImage WEB_IMAGE = new WebImage();

    static int numberOfPackets;
    static int numberOfPacketsSent;
    static int numberOfPacketsReceived;

    static int numberOfARPpackets;
    static int numberOfICMPpackets;

    static int numberOfIPpackets;

    static int numberOfTcpPackets;
    static int numberOfSYN;
    static int numberOfSYNACK;
    static int numberOfACK;
    static int numberOfPSHACK;
    static int numberOfFINPSHACK;
    static int numberOfFINACK;
    static int numberOfRST;

    static int numberOfSslTls;
    static int numberOfUdpPackets;
    static int numberOfDNS;

    static int numberOfHTTPpackets;
    static int numberOfGETS;
    static int numberOfPosts;
    static int numberOfImages;
    static HashMap<String, String> ipAddressesVisited = new HashMap<String, String>();
    static TreeSet<Integer> clientPortsUsed = new TreeSet<Integer>();
    static TreeSet<Integer> serversPortsUsed = new TreeSet<Integer>();
    static HashMap<String, Integer> imageTypes = new HashMap<String, Integer>();
    static HashMap<String, Long> bandwidth = new HashMap<String, Long>();
    static HashMap<String,Integer> received_packets_with_size=new HashMap<>();
    static HashMap<String,Integer> sent_packets_with_size=new HashMap<>();
    static String macAddress = "";
    static String agentIpAddress = "";
    static PrintWriter writer;
    static String destIP;
    static PcapPacket pcap_packet;
    static int totalSizeReceived=0;
    static int totalSizeSent=0;

    public PcapDissection(String threadName) {
        this.pcap_analyzing = threadName;
    }

    @Override
    public void run() {
        System.out.println("In pcap run");
        try {
            List<PcapIf> alldevs = new ArrayList();

            // For any error msgs
            StringBuilder errbuf = new StringBuilder();

            //Getting a list of devices
            int r = Pcap.findAllDevs(alldevs, errbuf);
            System.out.println(r);
            if (r != Pcap.OK) {
                System.err.printf("Can't read list of devices, error is %s", errbuf
                        .toString());
                return;
            }
            System.out.println("Network devices found:");
            int i = 0;int ch=0;
            for (PcapIf device : alldevs) {
                String description = (device.getDescription() != null) ? device.getDescription() : "No description available";
                System.out.printf("#%d: %s [%s]\n", i++, device.getName(), description);
                if(device.getName().contains("eth0")){
                    ch=alldevs.indexOf(device);
                }
            }

            System.out.println("choose the one device from above list of devices");
            PcapIf device = alldevs.get(ch);

            int snaplen = 64 * 1024;           // Capture all packets, no trucation
            int flags = Pcap.MODE_PROMISCUOUS; // capture all packets
            int timeout = 1;
            findIpInNetworkInterface();
            macAddress = getMacAddress();
            //Open the selected device to capture packets
            Pcap pcap = Pcap.openLive(device.getName(), snaplen, flags, timeout, errbuf);

            if (pcap == null) {
                System.err.printf("Error while opening device for capture: "
                        + errbuf.toString());
                return;
            }
            System.out.println("device opened");

            if (pcap == null) {
                System.err.println(errbuf);
                return;
            }
            PcapPacketHandler<String> jpacketHandler = new PcapPacketHandler<String>() {
                @Override
                public void nextPacket(PcapPacket packet, String user) {
                    pcap_packet = packet;
                    numberOfPackets++;
                    System.out.printf("Received packet at\n",
                            new Date(packet.getCaptureHeader().timestampInMillis()));
                    if (packet.hasHeader(ETHERNET)) {
                        boolean flag=processEthernetheader(packet.getTotalSize());

                        if (packet.hasHeader(IP)) {
                            processIPheader();
                            if(flag){
                                if(sent_packets_with_size.containsKey(destIP))
                                    sent_packets_with_size.put(destIP,(sent_packets_with_size.get(destIP)+packet.getTotalSize()));
                                else{
                                    sent_packets_with_size.put(destIP,packet.getTotalSize());
                                }
                             }
                            else{
                                if(received_packets_with_size.containsKey(destIP))
                                    received_packets_with_size.put(destIP,(received_packets_with_size.get(destIP)+packet.getTotalSize()));
                                else{
                                    received_packets_with_size.put(destIP,packet.getTotalSize());
                                }
                            }
                            String destDomain;
                            try {
                                destDomain = resolveNetname(destIP);
                                System.out.println("DESTip: "+destIP);
                                long size = (long) packet.size();
                                if (bandwidth.containsKey(destDomain)) {
                                    size = bandwidth.get(destDomain);
                                    size = size + packet.size();
                                    bandwidth.put(destDomain, size);
                                }
                                bandwidth.put(destDomain, size);
                            } catch (Exception ex) {
                                Logger.getLogger(PcapDissection.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            if (packet.hasHeader(ICMP)) {
                                numberOfICMPpackets++;
                            } else if (packet.hasHeader(TCP)) {
                                processTCPheader();
                            } else if (packet.hasHeader(UDP)) {
                                processUDPheader();
                            }

                            if (packet.hasHeader(HTTP)) {
                                processHTTPheader();
                            }

                            if (packet.hasHeader(WEB_IMAGE)) {
                                processImage();
                            }
                        }
                    }
                }
            };

            pcap.loop(Pcap.LOOP_INFINITE, jpacketHandler, " *");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //writer.close();
        }
    }

    public void start() {
        if (pcap_t == null) {
            pcap_t = new Thread(this, pcap_analyzing);
            pcap_t.start();
        }

    }

    static String findIpInNetworkInterface() {
        agentIpAddress="";
        Enumeration e;
        String pattern="^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$";
        try {
            e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress i = (InetAddress) ee.nextElement();
                   if(i.getHostAddress().matches(pattern)&&!i.getHostAddress().startsWith("127.0.0"))
                        agentIpAddress = i.getHostName();
                    
                }
            }
        } catch (SocketException ex) {
            Logger.getLogger(PcapDissection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return agentIpAddress;
    }

    /**
     * Returns the MAC address of the current machine in 00:00:00:00:00:00
     * format
     *
     * @return
     */
    static String getMacAddress() {
        try {

            InetAddress ip2 = InetAddress.getByName(agentIpAddress);

            NetworkInterface network = NetworkInterface.getByInetAddress(ip2);

            byte[] mac = network.getHardwareAddress();

            if (mac != null) {
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < mac.length; i++) {
                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                }
                return sb.toString().replaceAll("-", ":");
            }
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Processes the ETHERNET header of this packet
     */
    static boolean processEthernetheader(int packetSize) {
        if ((FormatUtils.hexdump(ETHERNET.getHeader()).substring(45, 50)).equals("08 06")) {
            numberOfARPpackets++;
        }
        String sourceMac = FormatUtils.mac(ETHERNET.source());
        String destinationMac = FormatUtils.mac(ETHERNET.destination());
        return separateIngoingOutgoing(sourceMac, destinationMac,packetSize);
    }

    /**
     * Processes the IP header of this packet
     */
    static void processIPheader() {
        numberOfIPpackets++;
        String sourceMac = FormatUtils.mac(ETHERNET.source());
        String destinationIP = FormatUtils.ip(IP.destination());
        getDestinationAddress(sourceMac, destinationIP);
    }

    /**
     * Separates ingoing from outgoing traffic based on the MAC addresses of the
     * ETHERNET header
     *
     * @param sourceMac
     * @param destinationMac
     */
    public static boolean separateIngoingOutgoing(String sourceMac, String destinationMac,int packetSize) {
        boolean ret_flag=true;
        if (sourceMac.equalsIgnoreCase(macAddress)) {
            numberOfPacketsSent++;
            totalSizeSent=totalSizeSent+packetSize;
            ret_flag= true;

        } else if(destinationMac.equalsIgnoreCase(macAddress)) {
            numberOfPacketsReceived++;
            totalSizeReceived=totalSizeReceived+packetSize;
            ret_flag= false;
        }
        return ret_flag;
    }

    /**
     * Processes the TCP header of this packet
     */
    static void processTCPheader() {
        numberOfTcpPackets++;
        int sport = TCP.source();
        int dport = TCP.destination();
        addPorts(sport, dport);
        processTCPflags();
        processPorts(sport, dport);
    }

    /**
     * Processes the flags of this packet's TCP header TCP Flags include: [SYN],
     * [SYN ACK], [ACK], [PSH ACK] [FIN PSH ACK], [FIN ACK], [RST]
     */
    static void processTCPflags() {
        if (TCP.flags_SYN() && (!TCP.flags_ACK())) {
            numberOfSYN++;
        } else if (TCP.flags_SYN() && TCP.flags_ACK()) {
            numberOfSYNACK++;
        } else if (TCP.flags_ACK() && (!TCP.flags_SYN()) && (!TCP.flags_PSH()) && (!TCP.flags_FIN()) && (!TCP.flags_RST())) {
            numberOfACK++;
        } else if (TCP.flags_PSH() && (TCP.flags_ACK() && (!TCP.flags_FIN()))) {
            numberOfPSHACK++;
        } else if (TCP.flags_FIN() && TCP.flags_ACK() && (!TCP.flags_PSH())) {
            numberOfFINACK++;
        } else if (TCP.flags_PSH() && (TCP.flags_ACK() && (TCP.flags_FIN()))) {
            numberOfFINPSHACK++;
        } else if (TCP.flags_RST()) {
            numberOfRST++;
        }
    }

    /**
     * Processes the ports of a packet using transport layer protocols (TCP,
     * UDP)
     *
     * @param sport sourcePort
     * @param dport destinationPort
     */
    static void processPorts(int sport, int dport) {
        if (sport == 53 || dport == 53) {
            numberOfDNS++;
        } else if (sport == 443 || dport == 443) {
            numberOfSslTls++;
        }
    }

    /**
     * Adds the source and destination ports to the appropriate Treeset based on
     * the source and destination mac addresses of the packet
     *
     * @param sport
     * @param dport
     */
    static void addPorts(int sport, int dport) {
        String sourceMac = FormatUtils.mac(ETHERNET.source());
        String destinationMac = FormatUtils.mac(ETHERNET.destination());
        if (sourceMac.equals(macAddress)) {
            clientPortsUsed.add(sport);
            serversPortsUsed.add(dport);
        } else if (destinationMac.equals(macAddress)) {
            clientPortsUsed.add(dport);
            serversPortsUsed.add(sport);
        }
    }

    /**
     * Processes the UDP header of this packet
     */
    static void processUDPheader() {
        numberOfUdpPackets++;
        int sport = UDP.source();
        int dport = UDP.destination();
        addPorts(sport, dport);
        processPorts(sport, dport);
    }

    /**
     * Processes the HTTP header of this packet
     */
    static void processHTTPheader() {
        numberOfHTTPpackets++;
        if (HTTP.header().contains("GET")) {
            numberOfGETS++;
        } else if (HTTP.header().contains("POST")) {
            numberOfPosts++;
        }
    }

    /**
     * Processes images transferred over HTTP Images transferred over SSL/TLS
     * are not processed
     */
    static void processImage() {
        numberOfImages++;
        String imageType = HTTP.contentTypeEnum().toString();
        Integer count = imageTypes.get(imageType);
        if (count == null) {
            imageTypes.put(imageType, 1);
        } else {
            imageTypes.put(imageType, count + 1);
        }
    }

    /**
     * Prints the distributions among the different image types that have been
     * downloaded in the machine
     */
    static void printImageTypes() {
        System.out.printf("%s %d %s \n", "Found ", numberOfImages, " images (images transferred over SSL/TLS not included):");
        for (Map.Entry entry : imageTypes.entrySet()) {
            System.out.printf("%-4s %s %d \n", entry.getKey(), " ", entry.getValue());
        }
    }

    /**
     * Adds the IP destination address to the Map of IP addresses visited
     *
     * @param sourceMac
     * @param destinationIP
     */
    static void getDestinationAddress(String sourceMac, String destinationIP) {
        try {
            if (sourceMac.equals(macAddress)) {
                destIP = destinationIP;
                ipAddressesVisited.put(destinationIP, "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Resolves the IP addresses of the input Map and assigns the netname as the
     * value of each entry
     *
     * @param ipAddressesVisited
     * @throws Exception
     */
    static void resolveIPaddresses(HashMap<String, String> ipAddressesVisited) throws Exception {
        Iterator it = ipAddressesVisited.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry item = (Map.Entry) it.next();
            String ip = item.getKey().toString();
            String netname = resolveNetname(ip);
            item.setValue(netname);
        }
       
    }

    /**
     * Resolves the netname of the input IP address using the WhoIs Protocol The
     * first WhoIs server queried is whois.iana.org
     *
     * @param IPaddress the IP address to be resolved
     * @return
     * @throws Exception
     */
    static String resolveNetname(String IPaddress) throws Exception {
        try {
            findIpInNetworkInterface();
            if (IPaddress.equals(agentIpAddress)) {
                return "Local Address";
            }
            String netname = "";
            WhoisClient whoisClient = new WhoisClient();
            whoisClient.connect("whois.iana.org", 43);
            String queryResult = whoisClient.query(IPaddress);
            whoisClient.disconnect();
            String[] s = queryResult.split("\n");
            String serverToQuery = "";
            for (int i = 0; i < s.length; i++) {
                if (s[i].contains("whois:")) {
                    serverToQuery = s[i].substring(14);

                    break;
                }
            }
            String actualServer = serverToQuery;
            String tld = IPaddress.substring(IPaddress.lastIndexOf(".") + 1).trim().toLowerCase();
            whoisClient.connect(actualServer, 43);
            if (tld.equals("com")) {
                queryResult = whoisClient.query("domain " + IPaddress);
            } else {
                queryResult = whoisClient.query(IPaddress);
            }
            whoisClient.disconnect();
            String[] reply = queryResult.split("\n");
            for (int i = 0; i < reply.length; i++) {
                if (reply[i].startsWith("%")) {
                    continue;
                }
                if (reply[i].startsWith("NetName")) {
                    netname = reply[i].substring(16);
                    break;
                } else if (reply[i].startsWith("netname")) {
                    netname = reply[i].substring(16);
                    break;
                } else if (reply[i].startsWith("status")) {
                    netname = reply[i].substring(16);
                    break;
                } else if (reply[i].startsWith("Organization")) {
                    netname = reply[i].substring(16);
                    break;
                }
                if (reply[i].startsWith("OrgName")) {
                    netname = reply[i].substring(16);
                    break;
                }
            }

            if (!netname.equals("")) {
                return netname;
            } else {
                return "Not resolved";
            }
        } catch (Exception e) {
            return "Not resolved";
        }
    }

    public static void clearAllData() {
        ipAddressesVisited.clear();
        received_packets_with_size.clear();
        sent_packets_with_size.clear();
        bandwidth.clear();
        clientPortsUsed.clear();
        imageTypes.clear();
        serversPortsUsed.clear();
        numberOfPackets=1;
        numberOfPacketsSent=0;
        numberOfPacketsReceived=0;
        numberOfARPpackets=0;
        numberOfICMPpackets=0;
        numberOfIPpackets=0;
        numberOfTcpPackets=0;
        numberOfSYN=0;
        numberOfSYNACK=0;
        numberOfACK=0;
        numberOfPSHACK=0;
        numberOfFINPSHACK=0;
        numberOfFINACK=0;
        numberOfRST=0;
        numberOfSslTls=0;
        numberOfUdpPackets=0;
        numberOfDNS=0;
        numberOfHTTPpackets=0;
        numberOfGETS=0;
        numberOfPosts=0;
        numberOfImages=0;
        totalSizeReceived=0;
        totalSizeSent=0;

    }
}
