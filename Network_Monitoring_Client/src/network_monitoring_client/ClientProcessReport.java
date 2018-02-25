/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network_monitoring_client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yagmur
 */
public class ClientProcessReport {
    JsonFile json_file;
    public ClientProcessReport() {
        json_file=new JsonFile();
    }
    
   
   public void processReport() throws IOException{
       BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(new File(WriteReportProcess.file_name)));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ClientProcessReport.class.getName()).log(Level.SEVERE, null, ex);
        }
                //FileWriter bw = new FileWriter(WriteReportProcess.file_name);
                String line = "",report="";
                while ((line = br.readLine()) != null) {
                    report=report+"\n"+line;
                }
   }
}
