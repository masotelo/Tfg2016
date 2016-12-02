//Protocol to Support User Level Information
package net.floodlightcontroller.gass_qos_leo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.gass_mon_leo.LinkGass;
import net.floodlightcontroller.gass_mon_leo.MonLeoService;
import net.floodlightcontroller.gass_mon_leo.MonLeoWebRoutable;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.pktinhistory.IPktinHistoryService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.topology.NodePortTuple;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import net.floodlightcontroller.core.types.SwitchMessagePair;


public class QosLeo implements IFloodlightModule, IOFMessageListener, QoSLeoService {
	
	//Dependencias
	protected IFloodlightProviderService floodlightProvider;
	protected MonLeoService gassmonitor;
	protected IPktinHistoryService history;
	//protected ConcurrentCircularBuffer<SwitchMessagePair> buffer;
	protected static Logger log = LoggerFactory.getLogger(QosLeo.class);
	protected IRestApiService restApi;
	protected IDeviceService deviceManager;
	protected IRoutingService routingEngine;
	protected ILinkDiscoveryService linkDiscovery;
	
	//puerto monitorización (para descartar paquetes)
	public static final byte PROTOCOL_EXP = (byte)253;
	
	//puerto utilizado para pruebas
	//protected static short USENET_UDP_PROTOCOL = 5532;
	protected static short USENET_UDP_PROTOCOL_RTP = 5532;	//Paquetes RTP	
	protected static short USENET_UDP_PROTOCOL_RTCP = 5533;  //Paquetes RTCP
	
	protected double frpl = 0;  //paquetes perdidos acumulados
	//protected double delayslr = 0;  //delay desde el último reporte rtcp recibido
	protected double jitter = 0;  //jitter
	
	
	//Prioridad de configuración de los paquetes con QoS
	protected static short FLOWMOD_PRIORITY = 100;
	
	protected static double CAPACIDAD = 1;
	
    //Parametro funcion de coste
    protected final static double PARAMETRO_COSTE = 0.7;
    //puerto video
    //protected static short VIDEO_PORT = 5532;
    
    //puertos Gstreamer
    protected static int XMLRPC_PORT = 5500;
    protected static int RTSP_PORT = 5501;
    
	
	// IDLE Y HARD TIMMEOUT PARA LOS FLOWMOD
    // LOOK! This should probably go in some class that encapsulates the app cookie management
    public static final int LEARNING_SWITCH_APP_ID = 1;
    public static final int APP_ID_BITS = 12;
    public static final int APP_ID_SHIFT = (64 - APP_ID_BITS);
    public static final long LEARNING_SWITCH_COOKIE = (long) (LEARNING_SWITCH_APP_ID & ((1 << APP_ID_BITS) - 1)) << APP_ID_SHIFT;
    protected static short FLOWMOD_DEFAULT_IDLE_TIMEOUT_BASIC = 0; // in seconds -> 0 es infinito
    protected static short FLOWMOD_DEFAULT_HARD_TIMEOUT_BASIC = 5;
    protected int STATS_REQUEST_INTERVAL_MS = 2000; //5 seg
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "PktInHistory";
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		// TODO Auto-generated method stub
		switch(msg.getType()){
		case PACKET_IN:
			//buffer.add(new SwitchMessagePair(sw, msg));
			//log.info("New PacketIn received on switch: {}",sw.getId());
			//log.info("Iniciando prueba de impresion de variables monitorizados en servicio gass");
			//PrintVariableMonitor();
			
			//log.info("Mensaje Recibido, Leyendo Mensaje:");
			return this.processPacketInMessage(sw,(OFPacketIn)msg,cntx);
			
			//break;
		default:
			break;
		}
		return Command.CONTINUE;
	}



	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(QoSLeoService.class);
	    return l;
		
		//return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(QoSLeoService.class, this);
	    return m;
		//return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList <Class<? extends IFloodlightService>> ();
		l.add(IFloodlightProviderService.class);
		l.add(IPktinHistoryService.class);
		l.add(MonLeoService.class);
		l.add(IRestApiService.class);
		l.add(IDeviceService.class);
		l.add(IRoutingService.class);
		l.add(ILinkDiscoveryService.class);
		return l;
		// TODO Auto-generated method stub
		//return null;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        gassmonitor = context.getServiceImpl(MonLeoService.class);
       // logger = LoggerFactory.getLogger(PktInHistory.class);
        history = context.getServiceImpl(IPktinHistoryService.class);
        restApi = context.getServiceImpl(IRestApiService.class);
        deviceManager = context.getServiceImpl(IDeviceService.class);
		routingEngine = context.getServiceImpl(IRoutingService.class);
		linkDiscovery = context.getServiceImpl(ILinkDiscoveryService.class);
        
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
        
        //Iniciar REST Service
		 restApi.addRestletRoutable(new QoSLeoWebRoutable());
        //Instalar FlowMod para evitar repetición de paquetes UseNet
        
        
	}

	
	private Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		//PrintVariableMonitor();
		
		//Read packet in data headers by using OFMatch
		OFMatch match = new OFMatch();
		match.loadFromPacket(pi.getPacketData(), pi.getInPort());
		
		//leer source IP
		int sourceIP = match.getNetworkSource();
		//leer IP destino
		int destIP = match.getNetworkDestination();
	
		//if(destIP != IPv4.toIPv4Address("192.168.1.3")){
		//if(destIP == IPv4.toIPv4Address("10.0.0.1") || sourceIP == IPv4.toIPv4Address("10.0.0.1")){
			
		//leer puerto destino
		//short portDest = match.getTransportDestination();
		short portDest = match.getTransportDestination();
		
		//leer puerto origen
		//short portSrc = match.getTransportSource();
		short portSrc = match.getTransportSource();
		
		//log.info("el paquete tiene como destino:"+IPv4.fromIPv4Address(destIP));
	
		//short portSrc = match.getTransportSource();
	
		//procesar unicamente paquetes de prueba		
		//if(portSrc==USENET_UDP_PROTOCOL || (sourceIP > 0) & (destIP > 0)  ){
		if((sourceIP > 0) & (destIP > 0)  )
		{
			long swFuente= sw.getId();
			//log.info("paquete recibido en switch:"+sw.getId());
			//TFMlog.info("paquete recibido en switch:"+swFuente);
			//TFMlog.info("el paquete tiene como destino:"+IPv4.fromIPv4Address(destIP));
			
			//TFMlog.info(" Puerto Origen: "+ portSrc);
			//TFMlog.info(" Puerto Destino: "+ portDest);
			
			//si el paquete es de prueba, entonces leer el IP destino
			//int destIP = match.getNetworkDestination();
			
			
			//busco si el IP destino existe en la topología
			Iterator<? extends IDevice> buscando = null;
			//long swFuente= sw.getId();
			short ptFuente=pi.getInPort();
			long swDest=0;
	    	short ptDest=0;
			buscando =  deviceManager.queryDevices(null, null, destIP, null, null);
			
			//Si el IP destino existe dentro de la topología, 
			//obtengo el switch y puerto donde se encuentra
			
			while(buscando.hasNext()) {
	    		SwitchPort[] dstDap = buscando.next().getAttachmentPoints();
	    		swDest = dstDap[0].getSwitchDPID();
				ptDest = (short)dstDap[0].getPort();
				//TFMlog.info("host encontrado en switch:"+swDest+" port:"+ptDest);
	    	}
			
			//si es que existe hostdestino
			//if(swDest!=0){
			//if(swDest!=0 && (swFuente==1 || swFuente==5)){
			if(swDest!=0 && swFuente==1){
				log.info("buscando ruta...");
				Route routeOut = null;
				//if(portDest == VIDEO_PORT)
				if(sourceIP == IPv4.toIPv4Address("10.0.0.1") && destIP == IPv4.toIPv4Address("10.0.0.2") && portDest == USENET_UDP_PROTOCOL_RTP)	
				//if((sourceIP == IPv4.toIPv4Address("10.0.0.1") && destIP == IPv4.toIPv4Address("10.0.0.2") && swFuente==1 && swDest==5) || (sourceIP == IPv4.toIPv4Address("10.0.0.2") && destIP == IPv4.toIPv4Address("10.0.0.1") && swFuente==5 && swDest==1))
				//if((sourceIP == IPv4.toIPv4Address("10.0.0.1") && destIP == IPv4.toIPv4Address("10.0.0.2") && swFuente==1 && swDest==5) || (sourceIP == IPv4.toIPv4Address("10.0.0.2") && destIP == IPv4.toIPv4Address("10.0.0.1") && swFuente==5 && swDest==1))
				{
					//Buscar Ruta entre fuente y destino
					log.info("PAQUETE VIDEO WITH QOS");
					routeOut = calculaRutaGASS(swFuente, ptFuente, swDest, ptDest);
				}
				else{ 
					routeOut = routingEngine.getRoute(swFuente, ptFuente, swDest, ptDest, 0); //utiliza Ruteo Normal Floodlight
				}
				
				
				if(routeOut != null) {
					// Configurar Switches con Ruta Encontrada
					ConfigureRoute(routeOut, match, pi,sw);
					
				}else{
					log.info("No existe Ruta entre fuente y destino!!!");
				}
	
			}
			
		}
		//}
		
		return Command.CONTINUE;
	}

	//Configurar RutaEncontrada
	private void ConfigureRoute(Route routeOut, OFMatch match, OFPacketIn pi, IOFSwitch sw) {
		//TFMlog.info("ruta encontrada: {}",routeOut.toString()); 
		//TFMlog.info("Configurando Ruta en switches...");
		
		//Saco la lista de pares Switch-Port de fuente a destino
		ListIterator<NodePortTuple> switchIterator = routeOut.getPath().listIterator(routeOut.getPath().size());
		
		//Creo un paquete tipo OFMatch para configurar a los switches
		OFMatch matchIter = new OFMatch() ;
		matchIter.loadFromPacket(pi.getPacketData(), pi.getInPort());		            
		matchIter.setWildcards(OFMatch.OFPFW_ALL
					//& ~OFMatch.OFPFW_IN_PORT 
				    
					& ~OFMatch.OFPFW_DL_TYPE
                    & ~OFMatch.OFPFW_NW_SRC_MASK 
                    & ~OFMatch.OFPFW_NW_DST_MASK
                    & ~OFMatch.OFPFW_NW_PROTO 
                    & ~OFMatch.OFPFW_TP_SRC 
                    & ~OFMatch.OFPFW_TP_DST
                    );
		
		//Puerto por el que debo enviar este paquete luego de configurar switches
		short portPropio=0;
		
		//La Ruta tiene puerto de entrada y puerto de Salida del Switch
		//esta variable voy a utilizar para seleccionar unicamente el puerto de salida del paquete
		int tempCounter=0; //
		
		//Voy iterando desde el switch final hasta el switch origen
		while(switchIterator.hasPrevious()) {
			
			//Obtengo el par Switch-Port
			NodePortTuple portTemp = switchIterator.previous();
			
			//Leo el Switch y port a configurar
			long swtich_it = portTemp.getNodeId();
			IOFSwitch sw_it = floodlightProvider.getSwitch(swtich_it); 
			short port_it = portTemp.getPortId(); 
			
			//Verifico que sea el puerto de salida
			if(tempCounter==0){
				tempCounter++;

				//log.info("Configurando Sw:"+swtich_it+" Port:"+port_it);
				
				//si el switch es el switch actual, guardo el puerto por el que debo enviar el paquete
				if(swtich_it==sw.getId()){portPropio=port_it;}; 				
				
				this.writeFlowModBasic(sw_it, OFFlowMod.OFPFC_ADD, OFPacketOut.BUFFER_ID_NONE, matchIter, port_it, FLOWMOD_PRIORITY);	
			
			}else{tempCounter=0;}
		}
		
		//Enviar paquete por puerto de Salida
		if(portPropio!=0){
			//enviando paquete por el puerto asignado
			log.info("Enviando paquete recibido en el switch {} por el puerto:{}",sw,portPropio);
			this.writePacketOutForPacketIn(sw,pi,portPropio);
			
		};
		
	}
 

	private void PrintVariableMonitor() {
		
		gassmonitor.getBuffer();
		
		Map<Long, Set<LinkGass>> topoImprimir = new HashMap<Long, Set<LinkGass>> ();
		topoImprimir = gassmonitor.getBuffer();
		
		//TFMlog.info("					IMPRIMIENDO TOPOLOGIA");
        for (Map.Entry<Long, Set<LinkGass>> entry : topoImprimir.entrySet()){
        	
 //           Long switchID = entry.getKey();
            Set<LinkGass> enlacesSwitchId = entry.getValue();
            ////log.info("		SWITCH: "+switchID);
            Set<LinkGass> sortedset = new HashSet<LinkGass>(enlacesSwitchId);
       
            
            Iterator<LinkGass> iter = sortedset.iterator();
            while (iter.hasNext()) {
            	LinkGass e = iter.next();
            	//TFMlog.info("{DATOS_ARCHIVO EXTERNAL}"+" "+e.getSrc()+" "+e.getSrcPort()+" "+e.getDst()+" "+e.getDstPort()+" "+e.getDataRate()+" "+e.getErrorRate()+" "+e.getDelay());

            }
            
        }

	}

	/**
     * Writes a OFFlowMod to a switch.
     * @param sw The switch to write the flowmod to.
     * @param command The FlowMod actions (add, delete, etc).
     * @param bufferId The buffer ID if the switch has buffered the packet.
     * @param match The OFMatch structure to write.
     * @param outPort The switch port to output it to.
     */
    private void writeFlowModBasic(IOFSwitch sw, short command, int bufferId, OFMatch match, short outPort, short priori) {

        OFFlowMod flowMod = (OFFlowMod) floodlightProvider.getOFMessageFactory().getMessage(OFType.FLOW_MOD);
        flowMod.setMatch(match);
        flowMod.setCookie(QosLeo.LEARNING_SWITCH_COOKIE);
        flowMod.setCommand(command);
        flowMod.setIdleTimeout(QosLeo.FLOWMOD_DEFAULT_IDLE_TIMEOUT_BASIC);
        flowMod.setHardTimeout(QosLeo.FLOWMOD_DEFAULT_HARD_TIMEOUT_BASIC);
        flowMod.setPriority(priori);
        flowMod.setBufferId(bufferId);
        flowMod.setOutPort((command == OFFlowMod.OFPFC_DELETE) ? outPort : OFPort.OFPP_NONE.getValue());
        flowMod.setFlags((command == OFFlowMod.OFPFC_DELETE) ? 0 : (short) (1 << 0)); // OFPFF_SEND_FLOW_REM
        flowMod.setActions(Arrays.asList((OFAction) new OFActionOutput(outPort, (short) 0xffff)));
        flowMod.setLength((short) (OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH));
        //TFMlog.info("se ha instalado el siguiente flow:{}",flowMod.toString());
        if (log.isTraceEnabled()) {
            log.trace("{} {} flow mod {}", new Object[]{ sw, (command == OFFlowMod.OFPFC_DELETE) ? "deleting" : "adding", flowMod });
        }

        //counterStore.updatePktOutFMCounterStoreLocal(sw, flowMod);

        // and write it out
        try {
            sw.write(flowMod, null);
        } catch (IOException e) {
            log.error("Failed to write {} to switch {}", new Object[]{ flowMod, sw }, e);
        }
    }  	
	

    /**
     * Writes an OFPacketOut message to a switch.
     * @param sw The switch to write the PacketOut to.
     * @param packetInMessage The corresponding PacketIn.
     * @param egressPort The switchport to output the PacketOut.
     */
    private void writePacketOutForPacketIn(IOFSwitch sw, OFPacketIn packetInMessage,short egressPort) {

    	OFPacketOut packetOutMessage = (OFPacketOut) floodlightProvider.getOFMessageFactory().getMessage(OFType.PACKET_OUT);
    	short packetOutLength = (short)OFPacketOut.MINIMUM_LENGTH;//starting length
    	
    	//Set buffer_id, in_port, actions_len
    	packetOutMessage.setBufferId(packetInMessage.getBufferId());
    	packetOutMessage.setInPort(packetInMessage.getInPort());
    	packetOutMessage.setActionsLength((short)OFActionOutput.MINIMUM_LENGTH);
    	packetOutLength +=  OFActionOutput.MINIMUM_LENGTH;
    	
    	//set actions
    	List<OFAction> actions = new ArrayList<OFAction>(1);
    	actions.add(new OFActionOutput(egressPort, (short)0));
    	packetOutMessage.setActions(actions);
    	
    	//set data - only if buffer_id == -1
    	if(packetInMessage.getBufferId() == OFPacketOut.BUFFER_ID_NONE){
    		byte[] packetData = packetInMessage.getPacketData();
    		packetOutMessage.setPacketData(packetData);
    		packetOutLength += (short) packetData.length;
    	}
    	
    	//finally, set the local length
    	packetOutMessage.setLength(packetOutLength);
    	
    	//and write it out
    	try{
    		//counterStore.updatePktOutFMCounterStoreLocal(sw, packetOutMessage);
    		sw.write(packetOutMessage, null);
    	}catch(IOException e){
    		log.error("Failed to write {} to switch {}: {}", new Object[]{ packetOutMessage, sw, e });
    	}
    	
	}
    
    
    private Route calculaRutaGASS(long swFuente, short ptFuente, long swDest, short ptDest){
    	//Buscar Ruta entre fuente y destino
		Route routeOut = null;
		//START Algoritmo Jesus routeOut = //Encontrar Ruta utilizando Algoritmo Jesus
		//Obtengo una topología con métricas
		Map<Long, Set<LinkGass>> topoGassMonitorizada  = gassmonitor.getBuffer();

		//Creo un objeto Tipo Topologia Gass y le paso las estadisticas
		TopologiaGass topologiaGass = new TopologiaGass(topoGassMonitorizada);
		
		//busco la mejor ruta
		double xfrpl = frpl;
		double xjitter = jitter;
		
		routeOut = topologiaGass.getDijkstraRoute(swFuente, ptFuente, swDest, ptDest, xfrpl,xjitter);

		//FIN Algoritmo Jesus
		return routeOut;
    }
    
    @Override
   	public int getUsenetValue() {
    	log.info("Valor Tomado x REST API: ESTE ES EL QUE FUNCIONA");
    	int leo = 5;
    	return leo;
    }
    
    
    public void setHostValue(String hostValues){
    	//log.info("----Valor del Host RECIBIDO!!!!: {}", hostValue);
    	String[] partes = hostValues.split(",");
    	String sjitter = partes[0];
    	String sfraction = partes[1];
    	
    	double xjitter = Double.parseDouble(sjitter);
    	double xfraction = Double.parseDouble(sfraction);
    	
    	jitter = xjitter;
    	frpl = xfraction;
    	
    	log.info("----Valor del Jitter RECIBIDO!!!!: {}", Double.toString(jitter));
    	log.info("----Valor del Frpl RECIBIDO!!!!: {}", Double.toString(frpl));
    }
}