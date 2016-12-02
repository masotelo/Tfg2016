package net.floodlightcontroller.gass_mon_leo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IFloodlightProviderService.Role;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.SingletonTask;
import net.floodlightcontroller.counter.ICounterStoreService;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.floodlightcontroller.topology.ITopologyService;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFStatisticsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MonLeo implements IOFMessageListener, IFloodlightModule, MonLeoService {
    
	protected static Logger log = LoggerFactory.getLogger(MonLeo.class);
	protected IFloodlightProviderService floodlightProvider;
	protected IDeviceService deviceManager;
	protected ITopologyService topology;
	protected IThreadPoolService threadPool;
	protected IRoutingService routingEngine;
	protected ILinkDiscoveryService linkDiscovery;
	
	protected IRestApiService restApi;
	protected int STATS_REQUEST_INTERVAL_MS = 1000; //200 al principio
	protected SingletonTask newInstanceTask;
	
	protected static List<OFFlowStatisticsReply> statsReply;
	
	//lista de switches y puertos optimizado para el monitoring
	protected Map<Long, Set<LinkGass>> topologiaMonitorizada = null;
	//objeto de estadisticas
	protected Statistics leyendoStatistics;
	//lista de Enlaces con sus delays
	protected Map<LinkGass, Long> listaDelaysEnlaces = null;
	//cola de estadisticas
	protected ColaEstadisticas  colaEstadisticas = new ColaEstadisticas();
	
	
	//Agregar para delay con soporte learningswtich
	public static final byte PROTOCOL_EXP = (byte)253;
	protected ICounterStoreService counterStore;
	public static final int LEARNING_SWITCH_APP_ID = 1;
    public static final int APP_ID_BITS = 12;
    public static final int APP_ID_SHIFT = (64 - APP_ID_BITS);
    public static final long LEARNING_SWITCH_COOKIE = (long) (LEARNING_SWITCH_APP_ID & ((1 << APP_ID_BITS) - 1)) << APP_ID_SHIFT;
    protected static short FLOWMOD_VIDEO_PRIORITY = 1000;
    
    //Flow-mod timeouts (defaults)
    protected static short FLOWMOD_DEFAULT_IDLE_TIMEOUT_VIDEO = 5; // in seconds -> 0 es infinito //para QoS
    protected static short FLOWMOD_DEFAULT_HARD_TIMEOUT_VIDEO = 0;
	
	
	  /**
     * Role of the controller.
     */
    private Role role;
	
	@Override
	public String getName() {
		return MonLeo.class.getPackage().getName();
		//return null;
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
	    l.add(MonLeoService.class);
	    return l;
		//return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(MonLeoService.class, this);
	    return m;
		//return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(ITopologyService.class);
		 l.add(IDeviceService.class);
		  l.add(IRoutingService.class);
		  l.add(ILinkDiscoveryService.class);
		  l.add(IThreadPoolService.class);
		  l.add(ICounterStoreService.class);
		  l.add(IRestApiService.class);
		return l;
		//return null;
	}

	@Override
	public void init(FloodlightModuleContext context)throws FloodlightModuleException {
		floodlightProvider=context.getServiceImpl(IFloodlightProviderService.class);
		 topology = context.getServiceImpl(ITopologyService.class);
		 deviceManager = context.getServiceImpl(IDeviceService.class);
		 routingEngine = context.getServiceImpl(IRoutingService.class);
		 linkDiscovery = context.getServiceImpl(ILinkDiscoveryService.class);
		 threadPool = context.getServiceImpl(IThreadPoolService.class);
		 restApi = context.getServiceImpl(IRestApiService.class);
		 counterStore = context.getServiceImpl(ICounterStoreService.class);
		 topologiaMonitorizada = new HashMap<Long,Set<LinkGass>>();
		 listaDelaysEnlaces = new HashMap<LinkGass, Long>();
	

	}

	@Override
	public void startUp(FloodlightModuleContext context)throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		//floodlightProvider.addOFMessageListener(OFType.PACKET_OUT, this);
		floodlightProvider.addOFMessageListener(OFType.PORT_STATUS, this);
		floodlightProvider.addOFMessageListener(OFType.ERROR, this);
		
		  //Iniciar REST Service
		 restApi.addRestletRoutable(new MonLeoWebRoutable());
		 
		 
		this.role = floodlightProvider.getRole();
		
		ScheduledExecutorService ses = threadPool.getScheduledExecutor();
		newInstanceTask = new SingletonTask(ses, new UpdateTopologyWorker());
		
		 if (role != Role.SLAVE)
	            newInstanceTask.reschedule(STATS_REQUEST_INTERVAL_MS, TimeUnit.MILLISECONDS);
		
	  

	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		//introducir la lectura de la topologia
		////log.info("INICIANDO TEST");
	    //log.info("paquete nuevo llega type: {}", msg.getType() );
		
	    switch (msg.getType()){
		case PACKET_IN:
						
			//handleMiscellaneousPeriodicEvents();
			return this.processPacketInMessage(sw,(OFPacketIn)msg,cntx);
			
			//return Command.CONTINUE;
			
		case PORT_STATUS:
		    //return this.processFlowRemovedMessage(sw,(OFFlowRemoved)msg);
			log.error("Error en enlace!");
			//log.info("   Switch {} link: {} ",sw, msg.toString());
			return Command.CONTINUE;
		case ERROR:
			
			//log.info("received an error {} on switch {}",msg,sw);
		    return Command.CONTINUE;
		default:
			break;
		}
		log.error("received an unexpected message {} from switch {}",msg,sw);
		
	
		return Command.CONTINUE;
	}

	
	protected class UpdateTopologyWorker implements Runnable {
        @Override
        public void run() {
            try {
            	
            	//log.info("TIMER ACTIVADO CADA 2 SEGUNDOS");
                handleMiscellaneousPeriodicEvents(); //descomentar si se desea recolectar estadísticas de la red
            }
            catch (Exception e) {
                log.error("Error in topology instance task thread", e);
            } finally {
                if (floodlightProvider.getRole() != Role.SLAVE)
                    newInstanceTask.reschedule(STATS_REQUEST_INTERVAL_MS, TimeUnit.MILLISECONDS);
            }
        }
    }
	
	protected void handleMiscellaneousPeriodicEvents() {
		
		Set<Long> num_sw = floodlightProvider.getAllSwitchDpids();
		//topology.getPortsWithLinks(sw)
		
			
		if (num_sw.isEmpty()){
			//log.info("switch no conectado");			
		}else{
			
			if(this.colaEstadisticas.getNumElementos() == 2){
				this.colaEstadisticas.desencolar();
			}
			//log.info("Iniciando TEST MON LEO:");
			//Iniciar tareas de optimizacion, iniciar clase OptimizeTopology
			OptimizeTopology topologiaSinMonitorizar = new OptimizeTopology(floodlightProvider, topology, linkDiscovery);
			//Map<Long, Set<LinkGass>> topologiaMonitorizada = null;
			
			//1. Leer y guardar Topología
			topologiaSinMonitorizar.doConvertTopologyToSLMap();
			
			//2. Optimizar y guardar Topología Mejorada (con el fin de reducir el número de requests)
			topologiaSinMonitorizar.doOptimizeTopologyToSLMap();
			
			//3. TODO Leer los datos de la topología mejorada 
			Map<Long, Set<LinkGass>> topoMonitorizar = topologiaSinMonitorizar.getOptimizeTopologyToSLMap();
			
			//4. TODO: Crear clase Orchestator y agregar método calculo nuevo timer (entrada: STATS_REQUEST_INTERVAL_MS -- SALIDA SERA el nuevo timer optimizado STATS_REQUEST_INTERVAL_MS)
			//ModificarTimer = new Orchestator (STATS_REQUEST_INTERVAL_MS,topología(numero de switches y links))
			//STATS_REQUEST_INTERVAL_MS = Orchestator.getNewTimerValue()
			
			
			//Envía paquetes para reconocer delay paquetes y enviarlos al controlador
			instalarSwitchPrioridadParaDelayPackets();
			
			//5.- Lee estadisticas y calcula metricas (dataRate, errorRate, delay)		
			leyendoStatistics = new Statistics(topoMonitorizar,OFStatisticsType.PORT,topologiaSinMonitorizar.floodlightProviderOriginal); 
			leyendoStatistics.enviarDelayPacketToTopology();
			Map<Long,ArrayList<EstadisticasSwitch>> listaEstadisticas = leyendoStatistics.getSwitchStatistics(leyendoStatistics.statType);        
			this.colaEstadisticas.encolar(listaEstadisticas); //insertas las metricas del instante t
			if(this.colaEstadisticas.getNumElementos() == 2){
				this.topologiaMonitorizada = this.colaEstadisticas.getMetrisResults(topoMonitorizar,this.listaDelaysEnlaces, STATS_REQUEST_INTERVAL_MS); //pondera las metricas de t y t-1
				imprimeTopologiaFinal(this.topologiaMonitorizada);
			}
			//	else TODO: preguntar leo
			
			
    
			//6.-ToDo: insertar método para enviar paquete Delay --> LEO
			
	         }
	
        return ;
    }

	//imprime la topologia final con todas las estadisticas de los enlaces
	public void imprimeTopologiaFinal(Map<Long, Set<LinkGass>> topoImprimir){
		
		//M	log.info("					IMPRIMIENDO TOPOLOGIA");
        for (Map.Entry<Long, Set<LinkGass>> entry : topoImprimir.entrySet()){
        	
 //           Long switchID = entry.getKey();
            Set<LinkGass> enlacesSwitchId = entry.getValue();
            ////log.info("		SWITCH: "+switchID);
            Set<LinkGass> sortedset = new TreeSet<LinkGass>(enlacesSwitchId);
       
            
            Iterator<LinkGass> iter = sortedset.iterator();
            while (iter.hasNext()) {
            	LinkGass e = iter.next();
            	//M	log.info("{DATOS_ARCHIVO}"+" "+e.getSrc()+" "+e.getSrcPort()+" "+e.getDst()+" "+e.getDstPort()+" "+e.getDataRate()+" "+e.getErrorRate()+" "+e.getDelay());

            }
            
        }
      //M	log.info("				MONITORIZACION FINALIZADA");  
	}
	
	
	private Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		OFMatch match = new OFMatch();
		
		match.loadFromPacket(pi.getPacketData(), pi.getInPort());
		//log.info("data layer IP protocol {}",match.getNetworkProtocol() );
		
		
		////log.info("data readed Port Src: {}  Port Dst {}", match.getTransportSource(),match.getTransportDestination());
		
		if(match.getNetworkProtocol()==(byte)253){
			//log.info("Leyendo informacion de delay...");
	    	//ToDo: Insertar método para calcular el delay
			calculaDelay(sw, pi);
		}
		//return;
		return Command.CONTINUE;
	};    
	
	//calcula el delay que hay entre dos switches
	public void calculaDelay(IOFSwitch sw, OFPacketIn pi){
		long diff=0;
		
		//log.info("data:{}",pi.getPacketData());
		//abriendo información del paquete
		byte[] piData = pi.getPacketData();
		
		Ethernet eth =  new Ethernet();
		eth.deserialize(piData, 0, piData.length);
		//eth.setPayload(payloadData.setData(piData));
		IPv4 ipv4 = (IPv4) eth.getPayload();
		//UDP udp_pkt = (UDP) ipv4.getPayload();
		Data dataPkt=(Data)ipv4.getPayload();
		byte[] arr=dataPkt.getData();

		//convirtiendo dato recibido byte to long
		long dataSwitchPacket = ByteBuffer.wrap(arr).getLong();
		//log.info("long data received:{}",dataSwitchPacket);

		java.util.Date dateSwitch= new java.util.Date();
		dateSwitch.setTime(dataSwitchPacket);
   	    Timestamp timeOfPacket = new Timestamp(dateSwitch.getTime());

    	//obteniendo time stamp actual del controlador    	
    	java.util.Date dateController= new java.util.Date();
   	    Timestamp actualTimeController = new Timestamp(dateController.getTime());
    	//log.info("timeController:{}",actualTimeController.toString());
    	
    	//calculando delay
    	diff = actualTimeController.getTime() - timeOfPacket.getTime();	
    	
    	
    	//log.info("delay in miliseconds:{}",diff);
	    
    	
		//guardar el delay en su enlace correspondiente
    	short puertoDestino=pi.getInPort();
    	long switchDestino = sw.getId();
    	
    	//log.info("sw:"+switchDestino+" port:"+puertoDestino + "delay:"+diff);
    	
		ArrayList<Long> switchesYaLeidos = new ArrayList<Long>();
		 
    	for (Map.Entry<Long, Set<LinkGass>> entry : this.topologiaMonitorizada.entrySet()){
        	long swId = entry.getKey();
			if(!switchesYaLeidos.contains(swId)){
				switchesYaLeidos.add(swId);
				Set<LinkGass> enlacesSwitchId = entry.getValue();
//				Set<LinkGass>  newLinksList = new HashSet<LinkGass>();
				Iterator<LinkGass> iter = enlacesSwitchId.iterator();
				while (iter.hasNext()) {
					LinkGass enl = iter.next();

					//destino
					Long dst_aux_id = enl.getDst();
					short dstPort_aux = enl.getDstPort();
					
					//log.info("comparando con link:{}",enl.toString());
					
					if((puertoDestino==dstPort_aux)&&(switchDestino==dst_aux_id)){
//						LinkGass aux = new LinkGass(enl.getSrc(),enl.getSrcPort(),enl.getDst(),enl.getDstPort(),enl.getDataRate(),enl.getErrorRate(),diff);
//						newLinksList.add(aux);
//						//log.info("delay guardado en su enlace"+aux.toString());
						this.listaDelaysEnlaces.put(enl,diff);					
						break;
					}
				}
				//this.topologiaMonitorizada.put(swId,newLinksList);
			}
    	}
		//return Command.CONTINUE;
    	/*for (Map.Entry<LinkGass,Long> entry : this.listaDelaysEnlaces.entrySet()){
        	LinkGass e = entry.getKey();
        	long del = entry.getValue();
        	log.info("Delay en el enlace fuente: "+e.getSrc()+" puerto: " + e.getSrcPort()+" destino: "+e.getDst()+" puerto: "+e.getDstPort()+" delay: "+del);
    	}*/
	}
	
	
	private void instalarSwitchPrioridadParaDelayPackets (){
		
		for (Map.Entry<Long, Set<LinkGass>> entry : this.topologiaMonitorizada.entrySet()){
        	
          Long switchID = entry.getKey();
          IOFSwitch switchIDTipoIOFSwitch = floodlightProvider.getSwitch(switchID);
          /////////////////////////
          
			OFMatch match_aux = new OFMatch() ;
			
			match_aux.setDataLayerSource("00:24:8c:B1:14:51")
			.setDataLayerDestination("00:24:8c:B1:14:5E")
			.setDataLayerType(Ethernet.TYPE_IPv4)
			.setNetworkDestination(IPv4.toIPv4Address("192.168.1.3"))
			.setNetworkSource(IPv4.toIPv4Address("140.113.88.187"))
			.setNetworkProtocol(PROTOCOL_EXP);
			
	
			
			//match_aux.loadFromPacket(pi.getPacketData(), pi.getInPort());

			match_aux.setWildcards(OFMatch.OFPFW_ALL
					//& ~OFMatch.OFPFW_IN_PORT & ~OFMatch.OFPFW_DL_TYPE
					
					& ~OFMatch.OFPFW_DL_TYPE
                  & ~OFMatch.OFPFW_NW_SRC_MASK & ~OFMatch.OFPFW_NW_DST_MASK
                  & ~OFMatch.OFPFW_NW_PROTO & ~OFMatch.OFPFW_TP_SRC & ~OFMatch.OFPFW_TP_DST);

			//log.info("				Setwildcards del switch: {} ",switchID);

			//short pu = (short)listaSwitchesPuerto.get(i).intValue();
			//log.info("			Escribiendo flowmod en el switch: "+switchID);
			//setSwOFMatchPortRegister(sw_aux,match,pu);
			this.writeFlowMod(switchIDTipoIOFSwitch, OFFlowMod.OFPFC_ADD, OFPacketOut.BUFFER_ID_NONE, match_aux, OFPort.OFPP_CONTROLLER.getValue(),FLOWMOD_VIDEO_PRIORITY);
          
          /////////////////////////
      
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
    private void writeFlowMod(IOFSwitch sw, short command, int bufferId, OFMatch match, short outPort, short priori) {

        OFFlowMod flowMod = (OFFlowMod) floodlightProvider.getOFMessageFactory().getMessage(OFType.FLOW_MOD);
        flowMod.setMatch(match);
        flowMod.setCookie(MonLeo.LEARNING_SWITCH_COOKIE);
        flowMod.setCommand(command);
        flowMod.setIdleTimeout(MonLeo.FLOWMOD_DEFAULT_IDLE_TIMEOUT_VIDEO);
        flowMod.setHardTimeout(MonLeo.FLOWMOD_DEFAULT_HARD_TIMEOUT_VIDEO);
        flowMod.setPriority(priori);
        flowMod.setBufferId(bufferId);
        flowMod.setOutPort((command == OFFlowMod.OFPFC_DELETE) ? outPort : OFPort.OFPP_NONE.getValue());
        flowMod.setFlags((command == OFFlowMod.OFPFC_DELETE) ? 0 : (short) (1 << 0)); // OFPFF_SEND_FLOW_REM
        flowMod.setActions(Arrays.asList((OFAction) new OFActionOutput(outPort, (short) 0xffff)));
        flowMod.setLength((short) (OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH));
        //log.info("se ha instalado el siguiente flow:{}",flowMod.toString());
        if (log.isTraceEnabled()) {
            log.trace("{} {} flow mod {}", new Object[]{ sw, (command == OFFlowMod.OFPFC_DELETE) ? "deleting" : "adding", flowMod });
        }

        counterStore.updatePktOutFMCounterStoreLocal(sw, flowMod);

        // and write it out
        try {
            sw.write(flowMod, null);
        } catch (IOException e) {
            log.error("Failed to write {} to switch {}", new Object[]{ flowMod, sw }, e);
        }
    }
    
    //Buffer para leer datos de la topología
    
    @Override
	public Map<Long, Set<LinkGass>> getBuffer() {
    	Map<Long, Set<LinkGass>> exportResults = new HashMap<Long, Set<LinkGass>>();
    	
	    
        for (Map.Entry<Long, Set<LinkGass>> entry : topologiaMonitorizada.entrySet()){
        	
        	Long switchId = entry.getKey();
        	Set<LinkGass> setMonitor = entry.getValue();
        	
        	HashSet<LinkGass> linksSwitch = new HashSet<LinkGass>();
            Iterator<LinkGass> iter = setMonitor.iterator();
            while (iter.hasNext()) {
            	LinkGass enl = iter.next();
            	linksSwitch.add(enl);
            }
            
            exportResults.put(switchId, linksSwitch);
            
        }
        return exportResults;
	}
	  
}