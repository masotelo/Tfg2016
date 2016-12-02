package net.floodlightcontroller.gass_mon_leo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
//import java.util.PriorityQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

//import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFSwitch;
//import net.floodlightcontroller.core.annotations.LogMessageDoc;


import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFStatisticsRequest;
//import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.statistics.OFAggregateStatisticsReply;
import org.openflow.protocol.statistics.OFAggregateStatisticsRequest;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFPortStatisticsRequest;
import org.openflow.protocol.statistics.OFQueueStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.protocol.statistics.OFTableStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import net.floodlightcontroller.counter.ICounterStoreService;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
//import net.floodlightcontroller.packet.TCP;
//import net.floodlightcontroller.packet.UDP;

public class DelaySent{

	Future<List<OFStatistics>> future;
	IOFSwitch sw = null;
	//Log to use to inform about the operations
	protected static Logger log = LoggerFactory.getLogger(MonLeo.class);
	int test = 0;
	List<OFStatistics> values = null;
	OFStatisticsType statType = null;
	public static final byte PROTOCOL_EXP = (byte)253;
	//OFPacketOut packetOutMessage = null;
	//protected ICounterStoreService counterStore;
		
	public DelaySent(IOFSwitch switchid, OFStatisticsType OFstatType){
		this.sw = switchid;
		this.test = 5;
		this.statType= OFstatType;
	}
	

	public int getTest(){
		return test;
	};
	
	public List<OFStatistics> getSwitchStatistics(){
		List<OFStatistics> valores = leerDelSwitchStatistics();
		return valores;
	};
	
	public void printSwitchStatistics(){
		List<OFStatistics> statis = leerDelSwitchStatistics();
		
    	if(statType.equals(OFStatisticsType.PORT)){       		
    		for (Iterator<OFStatistics> it = statis.iterator(); it.hasNext();) {
    			OFStatistics stat = it.next();
    			if (stat instanceof OFPortStatisticsReply) {
    				OFPortStatisticsReply portStat = (OFPortStatisticsReply) stat;
    				log.info("  puerto: {} ",portStat.getPortNumber());
    				log.info("    paquetes recibidos:{} ",portStat.getreceivePackets());
    			//	log.info("    paquetes transmitidos: {} ",portStat.getTransmitBytes());
    				//log.info("    colisiones: {} ",portStat.getCollisions());
    			}	        		
    		}
    	}else if(statType.equals(OFStatisticsType.FLOW)){
    		for (Iterator<OFStatistics> it = statis.iterator(); it.hasNext();) {
    			OFStatistics stat = it.next();
    			if (stat instanceof OFFlowStatisticsReply) {
    				OFFlowStatisticsReply flowStat = (OFFlowStatisticsReply) stat;
    				//if(flowStat.getMatchedCount()!=0 || flowStat.getActiveCount()!=0){
    				log.info("  flowStat packet count: {} ",flowStat.getPacketCount());
    			//	log.info("    matchedcount:{} ",tableStat.getMatchedCount());
    			//	log.info("    activecount: {} ",tableStat.getActiveCount());
    				//}
    			}
             }
        }else if(statType.equals(OFStatisticsType.TABLE)){
        	for (Iterator<OFStatistics> it = statis.iterator(); it.hasNext();) {
    			OFStatistics stat = it.next();
    			if (stat instanceof OFTableStatistics) {
    				OFTableStatistics tableStat = (OFTableStatistics) stat;
    				if(tableStat.getMatchedCount()!=0 || tableStat.getActiveCount()!=0){
    			//	log.info("  tabla: {} ",tableStat.getName());
    			//	log.info("    matchedcount:{} ",tableStat.getMatchedCount());
    			//	log.info("    activecount: {} ",tableStat.getActiveCount());
    				}
    			}
             }
        }else if(statType.equals(OFStatisticsType.AGGREGATE)){
        	for (Iterator<OFStatistics> it = statis.iterator(); it.hasNext();) {
    			OFStatistics stat = it.next();
    			if (stat instanceof OFAggregateStatisticsReply) {
    				OFAggregateStatisticsReply agreStat = (OFAggregateStatisticsReply) stat;
    				//if(tableStat.!=0 || tableStat.getActiveCount()!=0){
    				log.info("  agrestat flowcont: {} ",agreStat.getFlowCount());
    				log.info("    agrestat packetcount:{} ",agreStat.getPacketCount());
    			//	log.info("    activecount: {} ",tableStat.getActiveCount());
    				//}
    			}
        	}
        }
		
	};
	
	private List<OFStatistics> leerDelSwitchStatistics() {
	 //IOFSwitch sw = floodlightProvider.getSwitch(switchId);
	 Future<List<OFStatistics>> future;
	 List<OFStatistics> values = null;
	 if (sw != null) {
	     OFStatisticsRequest req = new OFStatisticsRequest();
	     req.setStatisticType(statType);
	     int requestLength = req.getLengthU();
	     if (statType == OFStatisticsType.FLOW) {
	         OFFlowStatisticsRequest specificReq = new OFFlowStatisticsRequest();
	         OFMatch match = new OFMatch();
	         match.setWildcards(0xffffffff);
	         specificReq.setMatch(match);
	         specificReq.setOutPort(OFPort.OFPP_NONE.getValue());
	         specificReq.setTableId((byte) 0xff);
	         req.setStatistics(Collections.singletonList((OFStatistics)specificReq));
	         requestLength += specificReq.getLength();
	     } else if (statType == OFStatisticsType.AGGREGATE) {
	         OFAggregateStatisticsRequest specificReq = new OFAggregateStatisticsRequest();
	         OFMatch match = new OFMatch();
	         match.setWildcards(0xffffffff);
	         specificReq.setMatch(match);
	         specificReq.setOutPort(OFPort.OFPP_NONE.getValue());
	         specificReq.setTableId((byte) 0xff);
	         req.setStatistics(Collections.singletonList((OFStatistics)specificReq));
	         requestLength += specificReq.getLength();
	     } else if (statType == OFStatisticsType.PORT) {
	         OFPortStatisticsRequest specificReq = new OFPortStatisticsRequest();
	         specificReq.setPortNumber(OFPort.OFPP_NONE.getValue());
	         req.setStatistics(Collections.singletonList((OFStatistics)specificReq));
	         requestLength += specificReq.getLength();
	     } else if (statType == OFStatisticsType.QUEUE) {
	         OFQueueStatisticsRequest specificReq = new OFQueueStatisticsRequest();
	         specificReq.setPortNumber(OFPort.OFPP_ALL.getValue());
	         // LOOK! openflowj does not define OFPQ_ALL! pulled this from openflow.h
	         // note that I haven't seen this work yet though...
	         specificReq.setQueueId(0xffffffff);
	         req.setStatistics(Collections.singletonList((OFStatistics)specificReq));
	         requestLength += specificReq.getLength();
	     } else if (statType == OFStatisticsType.DESC ||
	                statType == OFStatisticsType.TABLE) {
	         // pass - nothing todo besides set the type above
	     }
	     req.setLengthU(requestLength);
	     try {
	         future = sw.queryStatistics(req);
	         values = future.get(10, TimeUnit.SECONDS);
	     } catch (Exception e) {
	         log.error("Failure retrieving statistics from switch " + sw, e);
	     }
	 }
	 //log.info("stats:  {} \n", values.toString());
	 return values;
	}
	
	public void sendDelayPacket(OFPacketOut Packet, IOFSwitch switchToSend, short portToSend ){
		
		//construir paquete para enviar
		OFPacketOut packetOutMessage = new OFPacketOut();
		packetOutMessage = Packet;
		
    	Ethernet l2 = new Ethernet().setSourceMACAddress("00:24:8c:B1:14:51")
    			                    .setDestinationMACAddress("00:24:8c:B1:14:5E")   
								    .setEtherType(Ethernet.TYPE_IPv4);
		
		IPv4 l3 = new IPv4().setDestinationAddress("192.168.1.3")
							.setSourceAddress("140.113.88.187")
							.setProtocol(PROTOCOL_EXP)
							.setTtl((byte)64);
		
		// UDP l4 = new UDP().setSourcePort((short)5532)
			//	 			.setDestinationPort((short)5537);
	    
		 
		 
		    
		packetOutMessage.setBufferId(OFPacketOut.BUFFER_ID_NONE);
    	packetOutMessage.setActionsLength((short)OFActionOutput.MINIMUM_LENGTH);
	    packetOutMessage.setInPort(OFPort.OFPP_NONE.getValue());
	      
	      
    	List<OFAction> actions = new ArrayList<OFAction>();
    	actions.add(new OFActionOutput(portToSend,(short) 0xffff));
    	
    	packetOutMessage.setActions(actions);
    	

    	//marca de tiempo
    	
    	java.util.Date date= new java.util.Date();
   	    Timestamp actualTime = new Timestamp(date.getTime());
   	   	log.info("time:{}",actualTime.toString());  	    
   	   
   	   	long testSend = actualTime.getTime(); //test
   	   	log.info("long data send:{}",testSend);
   	   	
   	   	byte[] bytes = ByteBuffer.allocate(8).putLong(testSend).array();
    	//creando payload y agregando marca de tiempo 
    	//String agentInfo = null;
    	//agentInfo =actualTime.toString();
    	
	    //enviar paquete 
    	
	      Data payloadData = new Data();
	        
	      //l2.setPayload(l3.setPayload(l4.setPayload(payloadData.setData(bytes))));    
	      l2.setPayload(l3.setPayload(payloadData.setData(bytes))); 
	        short packetOutLength = (short) (OFPacketOut.MINIMUM_LENGTH + packetOutMessage.getActionsLength());
	        packetOutMessage.setPacketData(l2.serialize());        
	        packetOutLength = (short) (packetOutLength + l2.serialize().length);
	        packetOutMessage.setLength(packetOutLength);
	        
	     
	        try{
	        	log.info("packetOutMessage:{}",packetOutMessage.getPacketData());
	    		switchToSend.write(packetOutMessage, null);
	    		switchToSend.flush();
	    	}catch(IOException e){
	    		log.error("Failed to write {} to switch {}: {}", new Object[]{ packetOutMessage, switchToSend, e });
	    	}
	        
	   return;
	
	}
	
}