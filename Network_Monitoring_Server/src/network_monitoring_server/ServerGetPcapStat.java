/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lab;

/**
 *
 * @author cemal
 */
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerGetPcapStat extends Thread {

    private ServerSocket ss = null;

    public ServerGetPcapStat(int port) {
        try {
            ss = new ServerSocket(port);
        } catch (IOException ex) {
            Logger.getLogger(ServerGetPcapStat.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("constructor");
    }

    @Override
    public void run() {
        if (ss == null) {
            return;
        } else {
            while (true) {
                System.out.println("run method");
                this.listen();
            }
        }

    }

    public String listen() {
        try {
            Socket s = ss.accept();
            
            
            String remoteAddr= s.getRemoteSocketAddress().toString();
            remoteAddr = remoteAddr.substring(1);
            remoteAddr = remoteAddr.split(":")[0];
            System.out.println(remoteAddr);
            
            DataInputStream in = new DataInputStream(s.getInputStream());
            
            String result = in.readUTF();
            if (s.isClosed()) {
                System.out.println("Connection is closing");
            }
            System.out.println(result);
            
            // parse operation
            String MacAddr = null;
            String date;
            String timeBegin;
            String timeEnd;
            int totalNumOfPackets = 0;
            int packetsSend = 0;
            int packetsRcv = 0;
            int arpPackets = 0;
            int TCPPackets = 0;
            int SSLPackets = 0;
            int UDPPackets = 0;
            int DNSPackets = 0;
            int HTTPPackets = 0;
            int getRequests = 0;
            int postRequests = 0;
            
            
            String[] arr = result.split("\n");
            int i=0;
            for (i = 0; i < 10; i++) {
                if(arr[i].contains("begin")){
                    break;
                }
            } 
            i++;
            date = arr[i].split(";")[1];
            timeBegin = arr[i].split(";")[2];
            timeEnd = arr[i].split(";")[5];
            i++;

            if(arr[i].contains("PART1")){
                totalNumOfPackets = Integer.parseInt(arr[i+1].split(";")[1]) ;
                MacAddr = arr[5].split(";")[1];
                packetsSend =  Integer.parseInt(arr[i+2].split(";")[2]);
                packetsRcv = Integer.parseInt(arr[i+3].split(";")[2]);
                arpPackets = Integer.parseInt(arr[i+4].split(";")[1]);
                TCPPackets = Integer.parseInt(arr[i+5].split(";")[1]);
                SSLPackets = Integer.parseInt(arr[i+6].split(";")[1]);
                UDPPackets = Integer.parseInt(arr[i+7].split(";")[1]);
                DNSPackets = Integer.parseInt(arr[i+8].split(";")[1]);
                HTTPPackets = Integer.parseInt(arr[i+9].split(";")[1]);
                getRequests = Integer.parseInt(arr[i+10].split(";")[1]);
                postRequests = Integer.parseInt(arr[i+11].split(";")[1]);
                
                i+=12;
                
                if(arr[i].contains("IP ADDRESSES VISITED")){
                    
                    
                    
                    
                }else{
                    System.out.println("Bozuk Paket : IP ADDRESSES VISITED bulunamadı!!");
                }

            }else{
                System.out.println("Bozuk Paket : PART1 bulunamadı!!");
            }
                
            
            System.out.println(MacAddr);
            System.out.println(date);
            System.out.println(timeBegin);
            System.out.println(timeEnd);
            System.out.println(totalNumOfPackets);
            System.out.println(packetsRcv);
            System.out.println(packetsSend);
            System.out.println(TCPPackets);
            System.out.println(SSLPackets);
            System.out.println(UDPPackets);
            System.out.println(HTTPPackets);
            System.out.println(getRequests);
            System.out.println(postRequests);
            
            
            
            
            return result;
            
            
        } catch (IOException ex) {
            Logger.getLogger(ServerGetPcapStat.class.getName()).log(Level.SEVERE, null, ex);
            return "Error";
        }
        
    }

//    /**
//     * @param args the command line arguments
//     * @throws java.io.IOException
//     */
    public static void main(String[] args) throws IOException {
        // TODO code application logic here  
        ServerGetPcapStat test = new ServerGetPcapStat(4444);
        test.start();
        
        

    }

}
