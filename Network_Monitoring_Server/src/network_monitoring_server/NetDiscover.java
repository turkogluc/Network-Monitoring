package network_monitoring_server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

/**
 *
 * @author cemal
 */
public class NetDiscover {

    private String localAddress = null;
    private String defaultInterface = null;
    private ArrayList<String> deviceList;
    private int timeOut = 50;

    /**
     * First Constructor takes no parameter Assumes connection is made via eth0
     * interface First finds the IP address of Local machine at "eth0" interface
     * (Ex. 192.168.1.6) Then scans the network with iterating last part of
     * local IP (192.168.1.{1-255}), to find out which IP addresses are up.
     */
    NetDiscover() throws SocketException, IOException {
        defaultInterface = "eth0";
        localAddress = FindLocalIP(defaultInterface);
        deviceList = new ArrayList<>();
    }

    /**
     * Second constructor takes interface name as parameter Finds the IP address
     * of Local machine at that interface Scans the network to find IP addresses
     * that are up.
     *
     * @param defaulInterface
     * @throws SocketException
     */
    NetDiscover(String defaulInterface) throws SocketException {
        this.defaultInterface = defaulInterface;
        localAddress = FindLocalIP(this.defaultInterface);
        deviceList = new ArrayList<>();
    }

    /**
     * "NetworkInterface.getNetworkInterfaces()" method returns all network
     * interfaces. Return type of that method is Enumeration Interface. In
     * foreach loop all interfaces are taken one by one as iface variable
     * Information of interfaces are printed. An interface may contain more than
     * one InetAddress (IP Address) So to find each IP address in the interface
     * is checked one by one via foreach loop When the the iface is matched with
     * the interfaceName, then IP is found and host is assigned
     */
    public String FindLocalIP(String interfaceName) throws SocketException {
        String host = null;
        System.out.println("## Your Network Interfaces: ##");
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

        for (NetworkInterface iface : Collections.list(nets)) {
            System.out.printf("Display name: %s\n", iface.getDisplayName());
            System.out.printf("Name: %s\n", iface.getName());
            Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                System.out.printf("InetAddress: %s\n", inetAddress);
                if (iface.getName().contains(interfaceName)) {
                    host = inetAddress.toString();
                }
            }
            System.out.printf("\n");
            System.out.println("Your Local IP: " + host);

        }
        if (host == null) {
            return "Error";
        } else {
            return host.substring(1);   // to erase the "/" character from beginnig of InetAddress
        }

    }

    public ArrayList<String> ScanNetwork() throws UnknownHostException, IOException {

        // hostname will be converted into "192.168.10." format for scanning /24 subnet
        // timeout could be increased
        System.out.println("Your network is scanning.. Please wait");
        int count = 0, index = 0;
        System.out.println(localAddress);
        for (int i = 1; i <= localAddress.length(); i++) {
            if (localAddress.charAt(i) == '.') {
                count++;
                if (count == 3) {
                    index = i;
                    break;
                }
            }

        }
        String tempAddr = localAddress.substring(0, index + 1);

        for (int i = 1; i < 255; i++) {
            String tempHostName = tempAddr + Integer.toString(i);
            // String concetanation: "192.168.1." + i

            if (InetAddress.getByName(tempHostName).isReachable(timeOut)) {
                deviceList.add(tempHostName);
                // is reachable function checks whether the remote host is up or down

            }
        }
        System.out.println("Scan is completed..!");
        return deviceList;
    }

    public void printNetList() {
        for (String node : deviceList) {
            System.out.println(node);
        }
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public ArrayList<String> getDeviceList() {
        return deviceList;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public void setDefaultInterface(String defaultInterface) {
        this.defaultInterface = defaultInterface;
    }

    /**
     * @param args the command line arguments
     * @throws java.net.UnknownHostException
     */
//    public static void main(String[] args) throws UnknownHostException, IOException {
//        // TODO code application logic here
//
//        NetDiscover test = new NetDiscover("wlan0");
//        System.out.println("Burda=" + test.FindLocalIP("wlan0"));
//        test.ScanNetwork();
//        test.printNetList();
//
////          if(InetAddress.getByName("192.168.1.3").isReachable(5)){
////              System.out.println("Yep it is reachable");
////          }
//
//    }
}
