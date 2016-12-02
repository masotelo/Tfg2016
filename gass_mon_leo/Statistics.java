package net.floodlightcontroller.gass_mon_leo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

//import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.ImmutablePort;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
//import net.floodlightcontroller.packet.UDP;
//import net.floodlightcontroller.qoe_jesus.EstadisticasSwitch;

import org.openflow.protocol.OFMatch;
//import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.statistics.OFAggregateStatisticsRequest;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFPortStatisticsRequest;
import org.openflow.protocol.statistics.OFQueueStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.openflow.protocol.statistics.OFTableStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Statistics{

	Future<List<OFStatistics>> future;
	//IOFSwitch sw = null;
	//Log to use to inform about the operations
	protected static Logger log = LoggerFactory.getLogger(MonLeo.class);
	protected int test = 0;
	//List<OFStatistics> values = null;
	protected OFStatisticsType statType = null;
	protected Map<Long, Set<LinkGass>> topologia;
	//protected ICounterStoreService counterStore;
	protected IFloodlightProviderService floodlightProvider;
	
	public static final byte PROTOCOL_EXP = (byte)253;
		
	//constructora
	public Statistics(Map<Long,Set<LinkGass>> topologia, OFStatisticsType OFstatType, IFloodlightProviderService floodlightprovider){
		//this.sw = switchid;
		this.topologia = topologia;
		this.statType= OFstatType;
		this.test = 5;
		this.floodlightProvider = floodlightprovider;
	}
	
	//getters and setters
	public int getTest(){
		return test;
	};

	//lee estadisticas de un switch en concreto todos los puertos de los switches
	private List<OFStatistics> leerDelSwitchStatistics(long switchId, OFStatisticsType statType) {
	 IOFSwitch sw = floodlightProvider.getSwitch(switchId);
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
	 
	 ////log.info("imprimiendo estadisticas!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	// //log.info("stats:  {} \n", values.toString());
	 return values;
	}

	
	//lee estadisticas de un switch en concreto puertos se pueden escoger arbitrariamente
	private List<OFStatistics> leerDelSwitchStatistics2(long switchId, OFStatisticsType statType) {
	 IOFSwitch sw = this.floodlightProvider.getSwitch(switchId);
	 Collection<Short> num_pt = sw.getEnabledPortNumbers();
	 
	 //short puertoTest;
	 
	 Future<List<OFStatistics>> future;
	 List<OFStatistics> values = new ArrayList<OFStatistics>();
	 //Set<Long> num_sw = floodlightProvider.getAllSwitchDpids();
	 
	 if (sw != null) {
	 
		 if (num_pt.isEmpty()) {
			//log.info("El Switch no tiene puertos");
	
		 } else {

			for ( Short puertoTest : num_pt) {
				
				List<OFStatistics> tempValues = null;
				
				   OFStatisticsRequest req = new OFStatisticsRequest();
				     req.setStatisticType(statType);
				     int requestLength = req.getLengthU();
				     if (statType == OFStatisticsType.PORT) {
				         OFPortStatisticsRequest specificReq = new OFPortStatisticsRequest();
				         specificReq.setPortNumber(puertoTest);
				         req.setStatistics(Collections.singletonList((OFStatistics)specificReq));
				         requestLength += specificReq.getLength();
				     } else if (statType == OFStatisticsType.DESC ||
				                statType == OFStatisticsType.TABLE) {
				         // pass - nothing todo besides set the type above
				     }
				     req.setLengthU(requestLength);
				     try {
				         future = sw.queryStatistics(req);
				         tempValues = future.get(10, TimeUnit.SECONDS);
				     } catch (Exception e) {
				         log.error("Failure retrieving statistics from switch " + sw, e);
				     }
				
				 // tempValues.iterator();
				 if(!tempValues.isEmpty()){    
					 for (Iterator<OFStatistics> itemp = tempValues.iterator(); itemp.hasNext(); )
					 {
						 OFStatistics leotest=itemp.next(); 
						 //log.info("leotest:{}",leotest.toString());
						 values.add(leotest);
					  
					 }
				 }
			}
	  
		 }
	
	 }
	 
		 ////log.info("imprimiendo estadisticas!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	// //log.info("stats:  {} \n", values.toString());
	 return values;
	}	
	
	
	//calcula las estadisticas de toda la topologia en concreto
	public Map<Long,ArrayList<EstadisticasSwitch>> getSwitchStatistics(OFStatisticsType statType){
		//calcular las metricas necesarias para cada enlace
		Map<Long,ArrayList<EstadisticasSwitch>> listaMetricas = new HashMap<Long,ArrayList<EstadisticasSwitch>>();
		//recorrer la lista de estadisticas
		//para cada enlace recoger su tasa de datos y tasa de fallos
		ArrayList<EstadisticasSwitch> listaEstados = null;
		
		Set<Long> num_sw = floodlightProvider.getAllSwitchDpids();
		if (num_sw.isEmpty()) {
			//log.info("Switch no conectado");
		} else {

			for (Long d : num_sw) {
				
				IOFSwitch sw_test = this.floodlightProvider.getSwitch(d);
//				Collection<ImmutablePort> cole = sw_test.getPorts();
				////log.info("			El switch {} tiene {} puertos",d,cole.size());
				
				//lista de estadisticas para el switch d
				listaEstados = new ArrayList<EstadisticasSwitch>();
				
				List<OFStatistics> stats = leerDelSwitchStatistics(d, statType);//se puede poner leer del switch statistics2 para puertos selectivos
				OFStatisticsRequest req = new OFStatisticsRequest();
				int requestLength = req.getLengthU();
				if (sw_test != null) {

					if (statType.equals(OFStatisticsType.PORT)) {

						for (Iterator<OFStatistics> it = stats.iterator(); it.hasNext(); )
						{
							OFStatistics stat = it.next();
							// Variable para recoger las estadísticas del puerto al inicio.
							OFPortStatisticsReply portStat = (OFPortStatisticsReply) stat;

							if (stat instanceof OFPortStatisticsReply) {


								EstadisticasSwitch estadisticasSW = new EstadisticasSwitch(d, 
										portStat.getPortNumber(), 
										portStat.getreceivePackets(), 
										portStat.getTransmitPackets(), 
										portStat.getCollisions(), 
										portStat.getReceiveBytes(),
										portStat.getTransmitBytes(), 
										portStat.getReceiveDropped(),
										portStat.getTransmitDropped());
								
								listaEstados.add(estadisticasSW);
							
								
							}
						}
					} 
					
				}
				listaMetricas.put(d, listaEstados);
			}
			
		}

		return listaMetricas;
	}

	//envia el paquete que calcula el delay
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
   
		packetOutMessage.setBufferId(OFPacketOut.BUFFER_ID_NONE);
    	packetOutMessage.setActionsLength((short)OFActionOutput.MINIMUM_LENGTH);
	    packetOutMessage.setInPort(OFPort.OFPP_NONE.getValue());
	      
	      
    	List<OFAction> actions = new ArrayList<OFAction>();
		
		//Colocación del puerto por donde se enviará
    	actions.add(new OFActionOutput(portToSend,(short) 0xffff));
    	
    	packetOutMessage.setActions(actions);
    	

    	//marca de tiempo
    	java.util.Date date= new java.util.Date();
   	    Timestamp actualTime = new Timestamp(date.getTime());
   	   	//log.info("time:{}",actualTime.toString());  	    
   	   
   	   	long testSend = actualTime.getTime(); //test
   	   	//log.info("long data send:{}",testSend);
   	   	
   	   	byte[] bytes = ByteBuffer.allocate(8).putLong(testSend).array();
    	//creando payload y agregando marca de tiempo 
    	String agentInfo = null;
    	agentInfo =actualTime.toString();
    	
	    //enviar paquete 
    	
		Data payloadData = new Data();
		    
		//l2.setPayload(l3.setPayload(l4.setPayload(payloadData.setData(bytes))));    
		l2.setPayload(l3.setPayload(payloadData.setData(bytes))); 
		short packetOutLength = (short) (OFPacketOut.MINIMUM_LENGTH + packetOutMessage.getActionsLength());
		packetOutMessage.setPacketData(l2.serialize());        
    	packetOutLength = (short) (packetOutLength + l2.serialize().length);
    	packetOutMessage.setLength(packetOutLength);
    
 
		try{
			////log.info("packetOutMessage:{}",packetOutMessage.getPacketData());
			switchToSend.write(packetOutMessage, null);
			switchToSend.flush();
		}catch(IOException e){
			log.error("Failed to write {} to switch {}: {}", new Object[]{ packetOutMessage, switchToSend, e });
		}
	        
	   return;
	
	}
	
	//Recorre los switches y enlaces y envía el paquete de Delay de muestra
	public void enviarDelayPacketToTopology(){
		Set<LinkGass> listaCompletaEnlacesDelayPacketYaEnviados = new HashSet<LinkGass>();

		// if(!compruebaEnlacesRepetidos(listaCompletaEnlacesYaCalculados,enl)){}
		
		
		//iniciando iterador de switches y LinkGass
		for (Map.Entry<Long, Set<LinkGass>> entry : this.topologia.entrySet()){
			Long switchID = entry.getKey();
            Set<LinkGass> enlacesSwitchId = entry.getValue();
            Iterator<LinkGass> iter = enlacesSwitchId.iterator();
            
            while (iter.hasNext()) {
            	        	
            	LinkGass enl = iter.next(); // Recorriendo enlaces
  
            	 if(!compruebaEnlacesRepetidos(listaCompletaEnlacesDelayPacketYaEnviados,enl)){
            	
            	//crear paquete Packet Out
            	OFPacketOut packetOutMessage = (OFPacketOut) this.floodlightProvider.getOFMessageFactory().getMessage(OFType.PACKET_OUT);
        		
            	//Enviar Packete en el switch y puerto 
            	sendDelayPacket(packetOutMessage,this.floodlightProvider.getSwitch(enl.getSrc()) , enl.getSrcPort());
            	
            	//Informar 
            	//log.info("Paquete para calculo Delay enviado: Swich {} Puerto {}", enl.getSrc(),enl.getSrcPort());
            	//log.info("--hacia el: Swich {} Puerto {}", enl.getDst(),enl.getDstPort());
            	
            	listaCompletaEnlacesDelayPacketYaEnviados.add(enl); 
            	
            }
            	
            }
		}	
	}
	
	public boolean compruebaEnlacesRepetidos(Set<LinkGass> enlacesCalculados, LinkGass en){
		boolean esta = false;
		
		Iterator<LinkGass> iter = enlacesCalculados.iterator();
        while (iter.hasNext()) {
        	LinkGass e= iter.next();
        	if((e.getSrc()==en.getSrc())&&(e.getSrcPort()==en.getSrcPort())&&(e.getDst()==en.getDst())&&(e.getDstPort()==en.getDstPort())){
        		esta=true;
        		break;
        	}
        }
		return esta;
	}

	
}