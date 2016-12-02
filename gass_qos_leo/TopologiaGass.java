package net.floodlightcontroller.gass_qos_leo;
 
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
 
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
 
 
import net.floodlightcontroller.gass_mon_leo.LinkGass;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.topology.NodePortTuple;
 
public class TopologiaGass {
 
    Map<Long, Set<LinkGass>> topologia;
     
    public TopologiaGass(Map<Long, Set<LinkGass>> top){
        this.topologia = new HashMap<Long, Set<LinkGass>>(); 
        for (Map.Entry<Long, Set<LinkGass>> entry : top.entrySet()){
             
            Long switchId = entry.getKey();
            Set<LinkGass> setMonitor = entry.getValue();
             
            HashSet<LinkGass> linksSwitch = new HashSet<LinkGass>();
            Iterator<LinkGass> iter = setMonitor.iterator();
            while (iter.hasNext()) {
                LinkGass enl = iter.next();
                linksSwitch.add(enl);
            }
             
            this.topologia.put(switchId, linksSwitch);
             
        }
    }
     
    public Map<Long, Set<LinkGass>> getTopologiaGass() {return topologia;}
 
    public void setTopologiaTipoGass(Map<Long, Set<LinkGass>> topologia) {this.topologia = topologia;}
 
 
    public Route getDijkstraRoute(long srcId, short srcPort, long dstId, short dstPort, double xfrpl, double xjitter) {
 
        Dijkstra dijkstra = new Dijkstra(this.topologia);   
        List<ParSwitchPuerto> routeFound = dijkstra.ejecutaAlgortimo(srcId,srcPort,dstId, dstPort, xfrpl, xjitter);
         
         
        //aqui agregamos la ruta que encontramos en formato List<NodePortTuple>
        //La Ruta debe incluir puerto entrada y salida en el switch
        //NodePortTuple = Switch + Puerto
        //Ejm: Ruta (sw=00:00:00:00:00:00:00:05, port=1,sw=00:00:00:00:00:00:00:01, port=1 )
        //tendría Ruta pt1_SW5--SW5_pt2-----SW4_pt3--SW4_pt1------SW2_pt3--SW2_pt1------SW1_pt2--sw1_pt1
        //[[id=00:00:00:00:00:00:00:05, port=1], [id=00:00:00:00:00:00:00:05, port=2], [id=00:00:00:00:00:00:00:04, port=3], [id=00:00:00:00:00:00:00:04, port=1]       
        //[id=00:00:00:00:00:00:00:02, port=3], [id=00:00:00:00:00:00:00:02, port=1], [id=00:00:00:00:00:00:00:01, port=2], [id=00:00:00:00:00:00:00:01, port=1]]]
         
         
        Route routeOut = new Route(srcId, dstId);
        //Route routeOut = null; //TODO BORRAR CUANDO DIJSKTRA FUNCIONE
        if(!routeFound.isEmpty()){  //Si es que existe Ruta
         
            List<NodePortTuple> switchPorts = new ArrayList<NodePortTuple>();       
            NodePortTuple npt;
             
            //agrego el switch y puerto de llegada del paquete
            npt = new NodePortTuple(srcId, srcPort);
            switchPorts.add(0, npt);
             

            // si la ruta esta al reves, es decir, el switch Fuente esta al final de routeFound, invertimos la lista
            if (srcId == routeFound.get(routeFound.size()-1).getSwID()){
            	Collections.reverse(routeFound);
            }

            Iterator<ParSwitchPuerto> iter = routeFound.iterator();

             
             
            while (iter.hasNext()) {
           
            	ParSwitchPuerto aux = iter.next();
                  
                Long switchId = aux.getSwID();
                Short puertoSw = aux.getPuerto();
                 
                npt = new NodePortTuple(switchId,puertoSw);
                switchPorts.add(npt);                         //guardo Ruta
                 
                //Agregar el NodePortTuple a donde llega el Link
                if(switchId!=dstId){ //Si el Switch no es el switch de llegada 
                    //TODO: Buscar a que puerto y cual switch se conecta 
                     
                    npt=findSrcPt(switchId,puertoSw);
                    switchPorts.add(npt);  
                     
                }
                 
                //this.topologia.
                 
                //System.out.println("Switch:"+switchId+" por el puerto:"+puertoSw);
                 
                 
            }
             
            //aqui guardamos la ruta encontrada
            routeOut.setPath(switchPorts);
             
            //aqui le decimos que hay solo una ruta
            routeOut.setRouteCount(1);  
             
/*      Route routeOut = new Route(srcId, dstId);
        //Agrego la primera y último par de switch ports (basicamente puerto de entrada y puerto de salida)
        NodePortTuple npt;
         
        //agrego puerto de entrada
        npt = new NodePortTuple(srcId, srcPort);
        switchPorts.add(0, npt); // add src port to the front
         
        //agrego puerto de salida
        npt = new NodePortTuple(dstId, dstPort);
        switchPorts.add(npt); // add dst port to the end
         
 
*/      }
         
        return routeOut;
    }
/*
tiene que devolverte a donde llega el paquete (switch llegada, puerto llegada)
NodePortTuple
es un par Switch-Puerto*/
    private NodePortTuple findSrcPt(Long switchId, Short puertoSw) {
         
        ArrayList<Long> switchesYaLeidos = new ArrayList<Long>();
        NodePortTuple npt=null;
         
        for (Map.Entry<Long, Set<LinkGass>> entry : this.topologia.entrySet()){
            long swId = entry.getKey();
            if(!switchesYaLeidos.contains(swId)){
                switchesYaLeidos.add(swId);
                Set<LinkGass> enlacesSwitchId = entry.getValue();
                Iterator<LinkGass> iter = enlacesSwitchId.iterator();
                while (iter.hasNext()) {
                    LinkGass enl = iter.next();
                     
                    Long dst_aux_id = enl.getDst();
                    short dst_port_aux=enl.getDstPort();
                    Long src_aux_id=enl.getSrc();
                    short src_port_aux=enl.getSrcPort();
                     
                    if((puertoSw==src_port_aux)&&(switchId==src_aux_id)){
                        npt = new NodePortTuple(dst_aux_id,dst_port_aux);
                         
                    }                   
                }               
            }
                         
        }
             
        // TODO Auto-generated method stub
        return npt;
    }
     
     
}