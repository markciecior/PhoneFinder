/*Copyright (C) 2013 Mark Ciecior

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package com.markciecior.snmp.phonefinder;


import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;


public class SNMPManager {

static Snmp snmp = null;
private String VLAN_OID = ".1.3.6.1.4.1.9.9.46.1.3.1.1.2.1";
private String MAC_OID = ".1.3.6.1.2.1.17.4.3.1.1";
private String BRIDGEPORT_OID = ".1.3.6.1.2.1.17.4.3.1.2";
private String IFINDEX_OID = ".1.3.6.1.2.1.17.1.4.1.2";
private String IFNAME_OID = ".1.3.6.1.2.1.31.1.1.1.1";
private String ARP_OID = ".1.3.6.1.2.1.4.22.1.2";
private String CDP_ADDRESS_OID = ".1.3.6.1.4.1.9.9.23.1.2.1.1.4";
private String CDP_CAPABILITY_OID = ".1.3.6.1.4.1.9.9.23.1.2.1.1.9";
private String CDP_NAME_OID = ".1.3.6.1.4.1.9.9.23.1.2.1.1.6";

public SNMPManager(){
	
}

/*First we need to get the VLANs active in the switch so we can poll the MAC table for each VLAN
*/
public LinkedList<String> getVlans(String addr, String community){
List<TreeEvent> myVLANs = getBulkTree(snmp, new OID(VLAN_OID), addr, community);
LinkedList<String> vlanList = new LinkedList<String>();
Iterator<TreeEvent> iter = myVLANs.iterator();
VariableBinding[] bind;

	while (iter.hasNext()){
	bind = iter.next().getVariableBindings();
	for (int i=0; i < bind.length; i++){
		String VLAN = bind[i].getOid().toString().substring(VLAN_OID.length());
		vlanList.add(VLAN);		
		}
	}
	return vlanList;
}

/*Getting the MAC table is a four-step process:
	the MAC address is tied to a crazy ID number
	the crazy ID number is tied to a bridgeport number
	the bridgeport number is tied to an ifIndex
	the ifIndex is tied to a recognizable interface name
*/	

public HashMap<String,String> getMACToCrazyId(String addr, String community){
List<TreeEvent> myMACs = getBulkTree(snmp, new OID(MAC_OID), addr, community);
HashMap<String,String> MAC_TO_CRAZYID = new HashMap<String,String>();
Iterator<TreeEvent> iter = myMACs.iterator();
VariableBinding[] bind;

	while (iter.hasNext()){
	bind = iter.next().getVariableBindings();
	for (int i=0; i < bind.length; i++){
		String crazyID = bind[i].getOid().toString().substring(MAC_OID.length());
		String MAC = bind[i].getVariable().toString();
		MAC_TO_CRAZYID.put(MAC, crazyID);		
		}
	
	}
	return MAC_TO_CRAZYID;
}
	
public HashMap<String,String> getCrazyIdToBridgePort(String addr, String community){
	List<TreeEvent> myPorts = getBulkTree(snmp, new OID(BRIDGEPORT_OID), addr, community);
	HashMap<String,String> CRAZYID_TO_BRIDGEPORT = new HashMap<String,String>();
	Iterator<TreeEvent> iter = myPorts.iterator();
	VariableBinding[] bind;

	while (iter.hasNext()){
		bind = iter.next().getVariableBindings();
		for (int i=0; i < bind.length; i++){
			String crazyID = bind[i].getOid().toString().substring(BRIDGEPORT_OID.length());
			String bridgePort = bind[i].getVariable().toString();
			CRAZYID_TO_BRIDGEPORT.put(crazyID, bridgePort);		
		}
		
	}
	return CRAZYID_TO_BRIDGEPORT;
}
	
public HashMap<String,String> getBridgePortToIfIndex(String addr, String community){
	List<TreeEvent> myIndexes = getBulkTree(snmp, new OID(IFINDEX_OID), addr, community);
	HashMap<String,String> BRIDGEPORT_TO_IFINDEX = new HashMap<String,String>();
	Iterator<TreeEvent> iter = myIndexes.iterator();
	VariableBinding[] bind;

	while (iter.hasNext()){
		bind = iter.next().getVariableBindings();
		for (int i=0; i < bind.length; i++){
			String bridgePort = bind[i].getOid().toString().substring(IFINDEX_OID.length());
			String ifIndex = bind[i].getVariable().toString();
			BRIDGEPORT_TO_IFINDEX.put(bridgePort, ifIndex);		
		}
		
	}
	return BRIDGEPORT_TO_IFINDEX;
}
	
public HashMap<String,String> getIfIndexToIfName(String addr, String community){
	List<TreeEvent> myNames = getBulkTree(snmp, new OID(IFNAME_OID), addr, community);
	HashMap<String,String> IFINDEX_TO_IFNAME = new HashMap<String,String>();
	Iterator<TreeEvent> iter = myNames.iterator();
	VariableBinding[] bind;

	while (iter.hasNext()){
		bind = iter.next().getVariableBindings();
		for (int i=0; i < bind.length; i++){
			String ifIndex = bind[i].getOid().toString().substring(IFNAME_OID.length());
			String ifName = bind[i].getVariable().toString();
			IFINDEX_TO_IFNAME.put(ifIndex, ifName);		
		}
		
	}
	return IFINDEX_TO_IFNAME;
}
	

/*Go through the four hashMaps and put together the info we really want: MAC address and interface name
*/
public HashMap<String,String> getMACToIfName(HashMap<String,String> macToCrazyId, HashMap<String,String> CrazyIdtoBridgePort, HashMap<String,String> BridgePortToIfIndex, HashMap<String,String> IfIndexToIfName){
	Iterator<Map.Entry<String,String>> iter = macToCrazyId.entrySet().iterator();
	HashMap<String,String> MAC_TO_IFNAME = new HashMap<String,String>();
	
	while (iter.hasNext()){
		Map.Entry<String,String> pairs = (Map.Entry<String,String>)iter.next();
		String myMAC = (String)pairs.getKey();
		String myCrazyId = macToCrazyId.get(myMAC);
		String myBridgePort = CrazyIdtoBridgePort.get(myCrazyId);
		String myIfIndex = BridgePortToIfIndex.get(myBridgePort);
		String myIfName = IfIndexToIfName.get(myIfIndex);
		
		MAC_TO_IFNAME.put(myMAC, myIfName);
	}
	return MAC_TO_IFNAME;
}
	

public HashMap<String,LinkedList<String>> getArpTable(String addr, String community){
	List<TreeEvent> myARPs = getBulkTree(snmp, new OID(ARP_OID), addr, community);
	HashMap<String,LinkedList<String>> MAC_TO_IP = new HashMap<String,LinkedList<String>>();
	Iterator<TreeEvent> iter = myARPs.iterator();
	VariableBinding[] bind;
	LinkedList<String> listIP;

	while (iter.hasNext()){
		bind = iter.next().getVariableBindings();
		for (int i=0; i < bind.length; i++){
			String myIPArray[] = bind[i].getOid().toString().split("[.]");
			String myIP = myIPArray[myIPArray.length - 4] + "." + myIPArray[myIPArray.length - 3] + "." + myIPArray[myIPArray.length - 2] + "." + myIPArray[myIPArray.length - 1];
			String myMAC = bind[i].getVariable().toString();
			//Multiple IP addresses might point to the same MAC address, so we used a LinkedList to store all the IP addresses
			if (!MAC_TO_IP.containsValue(myMAC)){
				listIP = new LinkedList<String>();
			} else {
				listIP = MAC_TO_IP.get(myMAC);
			}
			listIP.add(myIP);
			MAC_TO_IP.put(myMAC, listIP);	
		}
		
	}
	return MAC_TO_IP;
}

public HashMap<String,String> getCDPAddress(String addr, String community){
	List<TreeEvent> myAdds = getBulkTree(snmp, new OID(CDP_ADDRESS_OID), addr, community);
	HashMap<String,String> IFINDEX_TO_CDPADDRESS = new HashMap<String,String>();
	Iterator<TreeEvent> iter = myAdds.iterator();
	VariableBinding[] bind;

	while (iter.hasNext()){
		bind = iter.next().getVariableBindings();
		for (int i=0; i < bind.length; i++){
			String ifIndex = bind[i].getOid().toString().substring(CDP_ADDRESS_OID.length()).split("[.]")[0];
			String cdpAddress = bind[i].getVariable().toString();
			IFINDEX_TO_CDPADDRESS.put(ifIndex, cdpAddress);		
		}
		
	}
	return IFINDEX_TO_CDPADDRESS;
}

public HashMap<String,String> getCDPCapability(String addr, String community){
	List<TreeEvent> myCaps = getBulkTree(snmp, new OID(CDP_CAPABILITY_OID), addr, community);
	HashMap<String,String> IFINDEX_TO_CDPCAPABILITY = new HashMap<String,String>();
	Iterator<TreeEvent> iter = myCaps.iterator();
	VariableBinding[] bind;

	while (iter.hasNext()){
		bind = iter.next().getVariableBindings();
		for (int i=0; i < bind.length; i++){
			String ifIndex = bind[i].getOid().toString().substring(CDP_CAPABILITY_OID.length()).split("[.]")[0];
			String myCap = bind[i].getVariable().toString();
			IFINDEX_TO_CDPCAPABILITY.put(ifIndex, myCap);		
		}
		
	}
	return IFINDEX_TO_CDPCAPABILITY;
}

public HashMap<String,String> getCDPName(String addr, String community){
	List<TreeEvent> myNames = getBulkTree(snmp, new OID(CDP_NAME_OID), addr, community);
	HashMap<String,String> IFINDEX_TO_CDPNAME = new HashMap<String,String>();
	Iterator<TreeEvent> iter = myNames.iterator();
	VariableBinding[] bind;

	while (iter.hasNext()){
		bind = iter.next().getVariableBindings();
		for (int i=0; i < bind.length; i++){
			String ifIndex = bind[i].getOid().toString().substring(CDP_NAME_OID.length()).split("[.]")[0];
			String myName = bind[i].getVariable().toString();
			IFINDEX_TO_CDPNAME.put(ifIndex, myName);		
		}
		
	}
	return IFINDEX_TO_CDPNAME;
}

public HashMap<String,String> getIfNameToCapability(HashMap<String,String> ifIndexToCap, HashMap<String,String> IfIndexToIfName){
	Iterator<Map.Entry<String,String>> iter = ifIndexToCap.entrySet().iterator();
	HashMap<String,String> MAC_TO_IFNAME = new HashMap<String,String>();
	
	while (iter.hasNext()){
		Map.Entry<String,String> pairs = (Map.Entry<String,String>)iter.next();
		String myIfIndex = (String)pairs.getKey();
		String myCap = ifIndexToCap.get(myIfIndex);
		String myIfName = IfIndexToIfName.get(myIfIndex);
		
		MAC_TO_IFNAME.put(myIfName, myCap);
	}
	return MAC_TO_IFNAME;
}

public String hexToAddress(String hexAddress){
	String retVal = "";
	
	String[] temp = hexAddress.split("[:]");
	for (int i=0; i < temp.length; i++) {
		if (i != 0) {retVal += ".";}
		try {
			retVal += Integer.parseInt(temp[i], 16);
		} catch (NumberFormatException n) {
			return "";
		}
	}

	
	return retVal;
}

/*The gateway's ARP table will contain all the ARP entries for all connected switches, not just
	the one we're interested in.  We need to iterate through the access switch's MACTable alongside
	the ARPTable and just pick out the addresses corresponding to MAC addresses connected to our
	access switch.  The rest of the ARP table is useless to us right now.
*/	
@SuppressWarnings("rawtypes")
public HashMap<String,LinkedList<String>> getConnectedIPs(HashMap<String,String> macToIfName, HashMap<String,LinkedList<String>> arpTable){
	HashMap<String,LinkedList<String>> MAC_TO_IP = new HashMap<String,LinkedList<String>>();
	Iterator iter = macToIfName.entrySet().iterator();
	
	while (iter.hasNext()){
		Map.Entry pairs = (Map.Entry)iter.next();
		String myMAC = (String) pairs.getKey();
		
		//If no IP address points to a particular MAC, we still need to return the MAC address will a null value for the IP address
		if (!arpTable.containsKey(myMAC)){
			LinkedList<String> myIPList = new LinkedList<String>();
			myIPList.add(null);
			MAC_TO_IP.put(myMAC, myIPList);
		} else {
		//If we do find a match, return the MAC with a LinkedList of all the IP addresses referencing it	
			MAC_TO_IP.put(myMAC, arpTable.get(myMAC));
		}
	}
	
	return MAC_TO_IP;
}

/*public HashMap<String,String> getIpToDns(HashMap<String,String> MacToIfName, HashMap<String,String> MacToIp, String nameserver){
	HashMap<String,String> IP_TO_NAME = new HashMap<String,String>();
	Iterator<Map.Entry<String,String>> iter;

	DNSLookup dns = null;
	try {
		dns = new DNSLookup(nameserver);
		
	} catch (Exception e){
		System.out.println(e.getMessage() + "Couldn't create a DNSLookup.");
		System.exit(1);
	}
	iter = MacToIfName.entrySet().iterator();
	String name;
	while (iter.hasNext()){
		Map.Entry<String,String> pairs = (Map.Entry<String,String>)iter.next();
		String myMAC = (String)pairs.getKey();
		String myIP = MacToIp.get(myMAC);
		
		try {
		name = "";
		} catch (Exception e) {
			name = "N/A";
		}
		IP_TO_NAME.put(myIP, name);
	}
	return IP_TO_NAME;
}*/


/**
* Start the Snmp session. If you forget the listen() method you will not
* get any answers because the communication is asynchronous
* and the listen() method listens for answers.
* @throws IOException
*/
public void start() throws IOException {
TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
snmp = new Snmp(transport);
// Do not forget this line!
transport.listen();
}

/*This method actually performs the SNMPWalk operation and returns a list of VariableBinding arrays.
	By default the walk operation returns at most 10 OIDs in each array
*/
public List<TreeEvent> getBulkTree(Snmp mySnmp, OID myOid, String myAddress, String community) {
	Address address = GenericAddress.parse("udp:" + myAddress + "/161");
	PDUFactory factory = new DefaultPDUFactory(PDU.GETBULK);
	TreeUtils tree  = new TreeUtils(mySnmp, factory);
	return tree.getSubtree(getTarget(address, community), myOid);
	
}

/**
* Method which takes a single OID and returns the response from the agent as a String.
* @param oid
* @return
* @throws IOException
*/
/*
public String getAsString(OID oid) throws IOException {
ResponseEvent event = get(new OID[] { oid });
return event.getResponse().get(0).getVariable().toString();
}

/**
* This method is capable of handling multiple OIDs
* @param oids
* @return
* @throws IOException
*/
/*
public ResponseEvent get(OID oids[]) throws IOException {
PDU pdu = new PDU();
for (OID oid : oids) {
pdu.add(new VariableBinding(oid));
pdu.setMaxRepetitions(10);
}
pdu.setType(PDU.GETBULK);
ResponseEvent event = snmp.getBulk(pdu, getTarget(address, "MMC-ROV1ew"));
//ResponseEvent event = snmp.send(pdu, getTarget(), null);
if(event != null) {
return event;
}
throw new RuntimeException("GET timed out");
}
*/
/**
* This method returns a Target, which contains information about
* where the data should be fetched and how.
* @return
*/
private Target getTarget(Address address, String community) {
//Address targetAddress = GenericAddress.parse(address);
//Address targetAddress = address;
CommunityTarget target = new CommunityTarget();
target.setCommunity(new OctetString(community));
//target.setCommunity(community);
target.setAddress(address);
target.setRetries(4);
target.setTimeout(5000);
target.setVersion(SnmpConstants.version2c);
return target;
}

}