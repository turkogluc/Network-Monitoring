/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network_monitoring_client;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import static network_monitoring_client.WriteReportProcess.MIN_TIME_VALUE;
import static network_monitoring_client.WriteReportProcess.report_fw;
import static network_monitoring_client.WriteReportProcess.time_factor;

/**
 *
 * @author yagmur
 */
public class SendReportProcessViaTCP implements Runnable {

    private Thread send_t;
    private String sending_report_thread;

    public SendReportProcessViaTCP(String threadName) {
        this.sending_report_thread = threadName;
    }

    @Override
    public synchronized void run() {
        System.out.println("In sending run");
        while (true) {

            try {
                Thread.sleep(3000); //to guarentee Writer access first
                Client.lock.lock();//for critical section usage
                BufferedReader br = new BufferedReader(new FileReader(new File(WriteReportProcess.file_name)));
                //FileWriter bw = new FileWriter(WriteReportProcess.file_name);
                String line = "",report="";
                while ((line = br.readLine()) != null) {
                    report=report+"\n"+line;
                }
                BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
                Socket clientSocket = new Socket("192.168.10.187", 4444);
                DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                outToServer.writeUTF(report + "\n");
                br.close();
                WriteReportProcess.report_fw.close();
            } catch (FileNotFoundException ex) {
               Logger.getLogger(SendReportProcessViaTCP.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(SendReportProcessViaTCP.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(SendReportProcessViaTCP.class.getName()).log(Level.SEVERE, null, ex);
            }finally {
                Client.lock.unlock();
            }
        }
    }

    public void start() {
        if (send_t == null) {
            send_t = new Thread(this, sending_report_thread);
            send_t.start();
        }

    }

}
