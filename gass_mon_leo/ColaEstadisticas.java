package net.floodlightcontroller.gass_mon_leo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import net.floodlightcontroller.qoe_jesus.EstadisticasSwitch;


public class ColaEstadisticas {

	protected static Logger log = LoggerFactory.getLogger(MonLeo.class);
	
	private int numElementos;
	private ArrayList<Map<Long,ArrayList<EstadisticasSwitch>>> cola;
	
	public ColaEstadisticas(){
		this.numElementos = 0;
		this.cola = new ArrayList<Map<Long,ArrayList<EstadisticasSwitch>>>();
	}

	//añade un conjunto de estadisticas nuevo a la cola
	public void encolar(Map<Long,ArrayList<EstadisticasSwitch>> newList){
		if(this.numElementos==2)
			desencolar();
		this.cola.add(newList);
		this.numElementos++;
	}
	
	//elimina un conjunto de estadisticas de la cola
	public void desencolar(){
		this.cola.remove(0);
		this.numElementos--;
	}
	
	//getters and setters
	public int getNumElementos() {return numElementos;}
	public ArrayList<Map<Long,ArrayList<EstadisticasSwitch>>> getCola() {return cola;}
	public void setNumElementos(int numElementos) {this.numElementos = numElementos;}
	public void setCola(ArrayList<Map<Long,ArrayList<EstadisticasSwitch>>> cola) {this.cola = cola;}
	
	//Devuelven lista de estadistica en cada posicion de la cola
	public Map<Long,ArrayList<EstadisticasSwitch>> getPrimeraEstadistica(){return this.cola.get(0);}
	public Map<Long,ArrayList<EstadisticasSwitch>> getSegundaEstadistica(){return this.cola.get(1);}
	
	//calcula las metricas ErrorRate y dataRate
	public Map<Long, Set<LinkGass>> getMetrisResults(Map<Long, Set<LinkGass>> topologia, Map<LinkGass, Long> listaDelaysEnlaces,int STATS_REQUEST_INTERVAL_MS){
		
		
		/*if(listaDelaysEnlaces.size()!=0){
			for (Map.Entry<LinkGass,Long> entry : listaDelaysEnlaces.entrySet()){
	        	LinkGass e = entry.getKey();
	        	long del = entry.getValue();
	        	log.info("Delay en el enlace fuente: "+e.getSrc()+" puerto: " + e.getSrcPort()+" destino: "+e.getDst()+" puerto: "+e.getDstPort()+" delay: "+del);
	    	}
			log.info("pasado en metricresults");
		}
		else{log.info("lista delays vacia");}*/
		Map<Long, Set<LinkGass>> topologiaCalculada = new HashMap<Long, Set<LinkGass>>();
		Set<LinkGass> listaCompletaEnlacesYaCalculados = new HashSet<LinkGass>();

        for (Map.Entry<Long, Set<LinkGass>> entry : topologia.entrySet()){
        	
            Long switchID = entry.getKey();
            Set<LinkGass> enlacesSwitchId = entry.getValue();
            Set<LinkGass> enlacesCalculados = new HashSet<LinkGass>();
            
			//log.info("Switch: "+switchID);
			
            Iterator<LinkGass> iter = enlacesSwitchId.iterator();
            while (iter.hasNext()) {
            	
            	LinkGass enl = iter.next();
            	//fuente
	            Long src_aux_id =enl.getSrc();
	            short srcPort_aux = enl.getSrcPort();
	            //destino
	            Long dst_aux_id = enl.getDst();
	            short dstPort_aux = enl.getDstPort();
	 
	            //calculo del coste de arista
	            long bytesTransAnterioresSrc = 0;
	            long bytesTransActualesSrc = 0;
	            long tasaEnviadosbps = 0;
	            
	            long bytesRecAnterioresDst = 0;
	            long bytesRecActualesDst = 0;
	            
	            long numeroBytesEnviados = 0;
	            long numeroBytesRecibidos = 0;
	            long tasaBytesPerdidosbps = 0;
	          
//	            double anchoBanda = (CAPACIDAD* Math.pow(2, 20))/8;
//	            double coste = 0;
			
	            if(switchID ==src_aux_id){
	            //if(!compruebaEnlacesRepetidos(listaCompletaEnlacesYaCalculados,enl)){
	            
					ArrayList<EstadisticasSwitch> estadisticasPuertoAnterioresFuente = getPrimeraEstadistica().get(src_aux_id);
			        ArrayList<EstadisticasSwitch> estadisticasPuertoActualesFuente   = getSegundaEstadistica().get(src_aux_id);
			        
			        ArrayList<EstadisticasSwitch> estadisticasPuertoAnterioresDestino = getPrimeraEstadistica().get(dst_aux_id);
			        ArrayList<EstadisticasSwitch> estadisticasPuertoActualesDestino   = getSegundaEstadistica().get(dst_aux_id);
			        ////log.info("////////////////////////////////////////////////////////////////////////////////////");
			        ////log.info("Switch fuente: "+src_aux_id+"Puerto fuente : "+srcPort_aux +" ---------  Switch destino: "+dst_aux_id+"  Puerto destino: "+ dstPort_aux);
			        ////log.info("////////////////////////////////////////////////////////////////////////////////////");
//DATA RATE
			        for(int i = 0; i< estadisticasPuertoAnterioresFuente.size();i++){
			        	if(srcPort_aux==estadisticasPuertoAnterioresFuente.get(i).getIdPuerto()){
			        		//////log.info("Cooncordancia de puertofuente: "+srcPort_aux+"-"+estadisticasPuertoAnterioresFuente.get(i).getIdPuerto());
			        		
			        		bytesTransAnterioresSrc = estadisticasPuertoAnterioresFuente.get(i).getBytesTransmitidos();
			        		bytesTransActualesSrc = estadisticasPuertoActualesFuente.get(i).getBytesTransmitidos();
			        		////log.info("Bytes transmitidos fuente (T-1): "+bytesTransAnterioresSrc);
			        		////log.info("Bytes transmitidos fuente (T): "+bytesTransActualesSrc);
			        		
			        		if(bytesTransActualesSrc >= bytesTransAnterioresSrc){
			        		
			        			long numeroBytesTotalesTransmitidos = Math.abs(bytesTransActualesSrc-bytesTransAnterioresSrc);
			        			//log.info("BYTES TOTALES ENVIADOS: {}",numeroBytesTotalesTransmitidos);
			        			//long intervaloTiempoSegundos = (long)(STATS_REQUEST_INTERVAL_MS/1000); 
			        			long intervaloTiempoSegundos = (long)(STATS_REQUEST_INTERVAL_MS); //test
			        			tasaEnviadosbps = ((numeroBytesTotalesTransmitidos*8*1000) / intervaloTiempoSegundos);
			        			//log.info("Tasa de datos (bps): "+tasaEnviadosbps);
			        			enl.setDataRate(tasaEnviadosbps);
			        			break;
			        		}
			        		else{
			        			//log.info("Dato recibido Src no válido");
			        			enl.setDataRate(0);
			        			break;
			        		}
			
			        	}    
			        }
//ERROR RATE
			        for(int i = 0; i< estadisticasPuertoAnterioresDestino.size();i++){
			        	if(dstPort_aux==estadisticasPuertoAnterioresDestino.get(i).getIdPuerto()){ 
			        		////log.info("Cooncordancia de puerto destino: "+dstPort_aux+" con "+estadisticasPuertoAnterioresDestino.get(i).getIdPuerto());
				
			        		bytesRecAnterioresDst = estadisticasPuertoAnterioresDestino.get(i).getBytesRecibidos();
				            bytesRecActualesDst = estadisticasPuertoActualesDestino.get(i).getBytesRecibidos();      	       
				            //log.info("Bytes recibidos destino (T-1): "+bytesRecAnterioresDst);
				            //log.info("Bytes recibidos destino (T): "+bytesRecActualesDst);
				            
				            if(bytesRecActualesDst>=bytesRecAnterioresDst){
				            	numeroBytesEnviados = Math.abs((bytesTransAnterioresSrc-bytesTransActualesSrc)) ;
				            	numeroBytesRecibidos = Math.abs(bytesRecActualesDst-bytesRecAnterioresDst);
				            	//log.info("BYTES TOTALES RECIBIDOS: {}",numeroBytesRecibidos);

				            
				            	long numeroBytesPerdidos = Math.abs((numeroBytesRecibidos-numeroBytesEnviados));
			        			//long intervaloTiempoS = (long)(STATS_REQUEST_INTERVAL_MS/1000);
			        			long intervaloTiempoS = (long)(STATS_REQUEST_INTERVAL_MS);// test
			        			tasaBytesPerdidosbps = ((numeroBytesPerdidos*8*1000)/intervaloTiempoS);
			        			
			        			
			        			if(tasaBytesPerdidosbps>tasaEnviadosbps)
			        			{
			        				tasaBytesPerdidosbps=0;
			        			}
			        			//log.info("Tasa de error (bps): "+tasaBytesPerdidosbps);
			        			enl.setErrorRate(tasaBytesPerdidosbps);
			        				if(numeroBytesEnviados>0){
			        					//log.info("porcentaje  error/enviados:{} por ciento ",(numeroBytesPerdidos*100/numeroBytesEnviados));
			        				}
			        			break;
				            }
				            else{
				            	//log.info("Dato recibido Dst no válido");
				            	enl.setErrorRate(0);
			        			break;
				            }
			        	}
			        	//log.info("////////////////////////////////////////////////////////////////////////////////////");
			            
			    	}
//DELAY RATE			        
//			        if(listaDelaysEnlaces.containsKey(enl)){
//			        	enl.setDelay(listaDelaysEnlaces.get(enl));
//			        }
			        for (Map.Entry<LinkGass,Long> entry2 : listaDelaysEnlaces.entrySet()){
			        	LinkGass e1 = entry2.getKey();
			        	long del = entry2.getValue();
			        	if((e1.getSrc()==src_aux_id)&&(e1.getSrcPort()==srcPort_aux)&&(e1.getDst()==dst_aux_id)&&(e1.getDstPort()==dstPort_aux)){
			        		enl.setDelay(del);
			        		break;
			        	}
			        	//log.info("Delay en el enlace fuente: "+e.getSrc()+" puerto: " + e.getSrcPort()+" destino: "+e.getDst()+" puerto: "+e.getDstPort()+" delay: "+del);
			    	}

			        
			        //anchoBandaAct = calculaAnchoBanda(bytesTransActualesSrc, bytesTransAnterioresSrc);
			        
			        //cij= 1 - (x(A )+(1-x)(B)) donde:
			        //A = ancho de banda / LINK_BANDWIDTH           ancho de banda = (bytestransmitidos(T) - bytestransmitidos(T-1))/T
			        //B = Infoperdida/LINK_BANDWIDTH                Infoperdida=(bytesperdidos(T)-bytesperdidos(T-1))/T
			        
			        //coste = 1 + ( (PARAMETRO_COSTE*(bytesEnviados/anchoBanda)  + (1-PARAMETRO_COSTE)*(bytesCerrados/anchoBanda)));
			        
			        enlacesCalculados.add(enl);
			        listaCompletaEnlacesYaCalculados.add(enl);
	            }
            }
            topologiaCalculada.put(switchID, enlacesCalculados);
        }
		return topologiaCalculada;
	
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
