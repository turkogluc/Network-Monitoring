package network_monitoring_server;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;


/**
 *
 * @author cemal
 */

public class SNMPManager {

    private Snmp snmp = null;
    private String address = null;
    private TransportMapping transport = null;
    private int ifaceIndex = 0;

    /**
    * Constructor
    * @param add
         * @throws java.io.IOException
    */
    public SNMPManager(String add) throws IOException{
        address = "udp:"+add+"/161";
        start();
        setifIndex();
    }

    /**
    * Start the Snmp session. If you forget the listen() method you will not
    * get any answers because the communication is asynchronous
    * and the listen() method listens for answers.
    * @throws IOException
    */
    private void start() throws IOException {
        transport = new DefaultUdpTransportMapping();
        snmp = new Snmp(transport);
        transport.listen();
    }

    /**
    * Method which takes a single OID and returns the response from the agent as a String.
    * @param oid
    * @return
    * @throws IOException
    */
    public String getAsString(OID oid) throws IOException {
        ResponseEvent event = get(new OID[] { oid });
        return event.getResponse().get(0).getVariable().toString();
    }
    
    public String getNextAsString(OID oid) throws IOException {
        ResponseEvent event = getNext(new OID[] { oid });
        return event.getResponse().get(0).getVariable().toString();
    }

    /**
    * This method is capable of handling multiple OIDs
    * @param oids
    * @return
    * @throws IOException
    */
    public ResponseEvent get(OID oids[]) throws IOException {
        PDU pdu = new PDU();
        for (OID oid : oids) {
            pdu.add(new VariableBinding(oid));
        }
        pdu.setType(PDU.GET);
        ResponseEvent event = snmp.send(pdu, getTarget(), transport);
        if(event != null) {
            return event;
        }
        throw new RuntimeException("GET timed out");
    }
    
    public ResponseEvent getNext(OID oids[]) throws IOException {
        PDU pdu = new PDU();
        for (OID oid : oids) {
            pdu.add(new VariableBinding(oid));
        }
        pdu.setType(PDU.GETNEXT);
        ResponseEvent event = snmp.send(pdu, getTarget(), transport);
        if(event != null) {
            return event;
        }
        throw new RuntimeException("GET timed out");
    }

    /**
    * This method returns a Target, which contains information about
    * where the data should be fetched and how.
    * @return
    */
    private Target getTarget() {
        Address targetAddress = GenericAddress.parse(address);
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("public"));
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(SnmpConstants.version2c);
        return target;
    }



    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = "udp:"+address+"/161";
    }
    //Title:System Description
    public String getsysDescr() throws IOException{
        ResponseEvent event = get(new OID[] { new OID(".1.3.6.1.2.1.1.1.0") });
        return event.getResponse().get(0).getVariable().toString();
    }
    //Title:System Object ID
    public String getsysObjectID() throws IOException{
        ResponseEvent event = get(new OID[] { new OID(".1.3.6.1.2.1.1.2.0") });
        return event.getResponse().get(0).getVariable().toString();
    }
    //Title:System Up time
    public String getsysUpTime() throws IOException{
        ResponseEvent event = get(new OID[] { new OID(".1.3.6.1.2.1.1.3.0") });
        return event.getResponse().get(0).getVariable().toString();
    }
    //Title:System Name
    public String getsysName() throws IOException{
        ResponseEvent event = get(new OID[] { new OID(".1.3.6.1.2.1.1.5.0") });
        return event.getResponse().get(0).getVariable().toString();
    }
    
    
    private void setifIndex() throws IOException{
        
         //remove beginning & end of address : (udp:192.168.10.100/161)
        String target = address.replace("udp:", "");
        target = target.replace("/161", "");
        
        ResponseEvent event2 = get(new OID[] { new OID(".1.3.6.1.2.1.4.20.1.2."+ target) });
        ifaceIndex = Integer.parseInt(event2.getResponse().get(0).getVariable().toString());
            
    }
    
    public int getifIndex(){
        return ifaceIndex ;
    }
    
    
    public String getifPhysAddress() throws IOException{
        
        ResponseEvent event = get(new OID[] {new OID(".1.3.6.1.2.1.2.2.1.6."+Integer.toString(ifaceIndex))});
        return event.getResponse().get(0).getVariable().toString();
        
    }
    //Title: Interface Description
    public String getifDescr() throws IOException{
                
        ResponseEvent event = get(new OID[] {new OID(".1.3.6.1.2.1.2.2.1.2."+Integer.toString(ifaceIndex))});
        return event.getResponse().get(0).getVariable().toString();

    }
    public String getBasicInfo() throws IOException{
        String content;
        content=this.getsysDescr()+"/n"+this.getsysName()+"\n"+this.getsysObjectID()+"\n"+this.getifDescr()+"\n"+this.getifPhysAddress()+"\n"+
        this.getsysUpTime();
        return content;
    }
    
    public static void main(String[] args) throws IOException {
        /**
        * Port 161 is used for Read and Other operations
        * Port 162 is used for the trap generation
        */
        SNMPManager client = new SNMPManager("192.168.10.187");
        
        System.out.println(client.getsysDescr());
        System.out.println(client.getsysObjectID());
        System.out.println(client.getsysUpTime());
        System.out.println(client.getsysName());
        System.out.println(client.getifPhysAddress());
        System.out.println(client.getifDescr());
        System.out.println(client.getifIndex());
                
        
    }

}