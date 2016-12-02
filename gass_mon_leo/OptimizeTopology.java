package net.floodlightcontroller.gass_mon_leo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.topology.ITopologyService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizeTopology{

	protected ITopologyService topologiaOriginal;
	protected IFloodlightProviderService floodlightProviderOriginal;
	protected ILinkDiscoveryService linkDiscoveryOriginal;
	
	//lista de switches y enlaces originales de la topología
	protected  Map<Long, Set<Link>> switchLinksOriginalMap;
	//lista de switches y enlaces con nuevo tipo LinkGass antes de optimizacion
	protected  Map<Long, Set<LinkGass>> switchLinksIntermediateMap;
	//lista de switches y enlaces optimizados de la topología con nuevo tipo LinkGass
	protected  Map<Long, Set<LinkGass>> switchLinksOptimizedMap;
	//lista de switches ids
	protected Set<Long> listaIDswitches;
	protected int newOrchestatorTimer;
	protected static Logger log = LoggerFactory.getLogger(MonLeo.class);
	
	
	//constructora
	public OptimizeTopology(IFloodlightProviderService ProviTest,ITopologyService topoTest, ILinkDiscoveryService linkTest){
		this.topologiaOriginal = topoTest;
		this.floodlightProviderOriginal = ProviTest; 
		this.linkDiscoveryOriginal = linkTest;
		this.switchLinksOriginalMap = new HashMap<Long, Set<Link>>(); 
		this.switchLinksIntermediateMap = new HashMap<Long, Set<LinkGass>>(); 
		this.switchLinksOptimizedMap = new HashMap<Long, Set<LinkGass>>(); 
		this.newOrchestatorTimer=0;
	}
	
	//Convertir Topologia a Mapa de SwitchLinks Original (SwitchLinksOriginalMap)
	public void doConvertTopologyToSLMap(){
		listaIDswitches = this.floodlightProviderOriginal.getAllSwitchDpids();
		Map<Long, Set<Link>> lk_temp = new HashMap<Long, Set<Link>>();
		
		lk_temp = this.linkDiscoveryOriginal.getSwitchLinks(); //obtengo la lista de switches y enlaces
		////log.info("lk_temp: {}",lk_temp.toString());

		
		this.switchLinksOriginalMap = lk_temp; // guardo la lista obtenida en la variable interna
		convertToGassLink();
    }
	public void convertToGassLink(){
		for (Map.Entry<Long, Set<Link>> entry : this.switchLinksOriginalMap.entrySet()){
        	////log.info("el enlace {} tiene el valor {}", entry.getKey(), entry.getValue());
            Long switchID = entry.getKey();
            Set<Link> listaEnlacesLink = entry.getValue();
            
            Set<LinkGass> listaEnlacesLinkGass = new HashSet<LinkGass>();
            
            //recorremos la lista de enlaces
            Iterator<Link> iter = listaEnlacesLink.iterator();
            while (iter.hasNext()) {
              Link enl = iter.next();
              LinkGass enlaceLinkGass = new LinkGass(enl.getSrc(),enl.getSrcPort(),enl.getDst(),enl.getDstPort());
              listaEnlacesLinkGass.add(enlaceLinkGass);
            }
            this.switchLinksIntermediateMap.put(switchID, listaEnlacesLinkGass);
            		
		}
		//log.info("transformacion ok");
	}
	
	//Optimizar Topologia a Mapa de SwitchLinks Optimizado (SwitchLinksOriginalMap)
	public void doOptimizeTopologyToSLMap(){
		this.switchLinksOptimizedMap = this.switchLinksIntermediateMap; // ToDo: Optimizar Mapa de switch y enlaces
		////log.info("lk_temp: {}",this.switchLinksOptimizedMap.toString());

    }
	
	//Optimizar Topologia a Mapa de SwitchLinks Optimizado (SwitchLinksOriginalMap)
	public Map<Long, Set<LinkGass>> getOptimizeTopologyToSLMap(){
		return this.switchLinksOptimizedMap;
    }
	
	//toString
	public String toString(){
    	return ("lista Switches: "+floodlightProviderOriginal.getAllSwitchDpids().toString());
    }

	
	//getters and setters
	public Set<Long> getListaIDswitches() {
		return listaIDswitches;
	}
	
	public int getOptimizedOrchestatorTime(int originalTimer, double controllerCapacity) {
		
		double newTimer;
		
		double numeroEnlaces,numeroSwitches;
		
		numeroSwitches=this.floodlightProviderOriginal.getAllSwitchDpids().size();
		numeroEnlaces= this.linkDiscoveryOriginal.getLinks().size();
		//log.info("num switches:{}",numeroSwitches);
		//log.info("num links:{}",numeroEnlaces);
		
		if(controllerCapacity==1){
			newTimer = originalTimer; 
		}else{
			if(numeroEnlaces<numeroSwitches){
				newTimer=numeroSwitches*originalTimer;
			}else{
				newTimer=originalTimer*(numeroSwitches+(numeroEnlaces/numeroSwitches));
			}	
			//log.info("newTimersinFActorAjuste:{}",newTimer);
			newTimer= ((1-controllerCapacity)*newTimer);
		}
		
		//log.info("newTimerconFactorAjuste:{}",newTimer);
		
		this.newOrchestatorTimer =  (int)newTimer;
		//log.info("timer calculado:{}",this.newOrchestatorTimer);
		
		return this.newOrchestatorTimer;
	}

	
}