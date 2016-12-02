package net.floodlightcontroller.gass_qos_leo;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
 
import net.floodlightcontroller.gass_mon_leo.LinkGass;
import net.floodlightcontroller.packet.IPv4;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class Dijkstra{
	//protected static Logger log = LoggerFactory.getLogger(QosLeo.class);
    private Map<Long, Set<LinkGass>> topologia;
     
    //lista con los vertices visitados
    private Map<Long,Boolean> vertVisitado = null;
     
    //matriz de adyacencia con la distancia entre par de vertices (i,j)
    //private ArrayList< ArrayList< VerticeSwitch > > tablaAdyacencia = null;
     
    // distancia de vertice inicial a vertice con ID = clave, distancia = valor
    private Map<Long,Double> distancia = null;          
     
    //cola de prioridad 
    private PriorityQueue<VerticePrioridad> colaPrioridad = new PriorityQueue<VerticePrioridad>(); 
     
    //numero de vertices
    private int numV = -1;
     
    //para la impresion de caminos (i,j) i sera el vertice del camino por el puerto j
    //primera fila los identificadores de swtiches
    //segunda fila puertos entre los enlaces de switches
    //tercera fila puerto del switch destino con el host destino
    private List<ParSwitchPuerto> previo = null;             
         
         
    public Dijkstra (Map<Long, Set<LinkGass>> top){
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
        init(topologia);
    }
 
     
    //inicializa todos los componentes del grafo
    public void init(Map<Long, Set<LinkGass>> topologia){
        this.numV = topologia.size();
        this.distancia = new HashMap<Long,Double>(this.numV);
        this.vertVisitado = new HashMap<Long,Boolean>(this.numV);
        this.previo = new ArrayList<ParSwitchPuerto>();
         
        Iterator<Map.Entry<Long, Set<LinkGass>>> it = topologia.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Set<LinkGass>> e = (Map.Entry<Long, Set<LinkGass>>)it.next();
            this.distancia.put(e.getKey(), Double.MAX_VALUE);
            this.vertVisitado.put(e.getKey(),false);
        }
         
    }
         
         
    public List<ParSwitchPuerto> ejecutaAlgortimo(long origen,short puertoOrigen, long destino, short puertoDestino, double xfrpl, double xjitter){
        if(origen != destino){
 
            this.colaPrioridad.add( new VerticePrioridad(origen,0.0) ); //Insertamos el vertice inicial en la Cola de Prioridad
            //this.distancia[ origen ] = 0;      //Este paso es importante, inicializamos la distancia del inicial como 0
            this.distancia.put(origen, 0.0);
            long actual , adyacente;
            long retardo;
            short puerto_desde;
            double peso, velocidad,error,porcentaje_error,bw,alfa,delay_max;
            
            bw = 20000;
            alfa = 0.7;
            //delay_max = 10;
            
            //long id_adyacente;
            while( !colaPrioridad.isEmpty() ){                   //Mientras cola no este vacia
                 
                System.out.println("*******Contenido de la cola prioridad***********");
                Iterator<VerticePrioridad> iterCola = colaPrioridad.iterator();
 
                while (iterCola.hasNext()){
                    VerticePrioridad vAux = iterCola.next();
                    System.out.println("Switch con id:  "+vAux.getId_switch()+"     y prioridad:  "+vAux.getPrioridad() );
                }
                System.out.println("********fin de la cola de prioridad*********");
                actual = colaPrioridad.element().getId_switch();       //Obtengo de la cola el nodo con menor peso, en un comienzo sera el inicial
                System.out.println("\n Vertice actual: "+ actual);
                colaPrioridad.remove();//Sacamos el elemento de la cola
                if(vertVisitado.get(actual))continue;
                //if( vertVisitado[ actual ] ) continue; //Si el vertice actual ya fue visitado entonces sigo sacando elementos de la cola
                //vertVisitado[ actual ] = true;         //Marco como visitado el vertice actual
                vertVisitado.put((long)actual,true);
                 
                Set<LinkGass> verticesAdyacentes= topologia.get(actual);
                 
                Iterator<LinkGass> iterAdyacentes = verticesAdyacentes.iterator();
                while (iterAdyacentes.hasNext()) {
                    LinkGass linkGass= iterAdyacentes.next();
                    adyacente = linkGass.getDst();
                 
                //for( int i = 1 ; i < tablaAdyacencia.get( actual ).size() ; ++i ){ //reviso sus adyacentes del vertice actual
                    //adyacente = tablaAdyacencia.get( actual ).get( i ).getIdSwitch();   //id del vertice adyacente
                    if(actual != adyacente ){
                        System.out.println("Vertice actual: +"+actual+      "    Vertice adyacente: "+ adyacente);
                        //TODO el peso es nuestra funcion de costo (cambiar cuando se quiera)
                        //1- velocidad
                        //2- 0.6*velocidad mas 0.4*retardo
                        //3- f(QoS)
                         
                        //peso= linkGass.getDataRate();
                        //peso= 1;
                        
                        //id_adyacente = linkGass.getDst();
                        velocidad = linkGass.getDataRate();
                        retardo = linkGass.getDelay();
                        error=linkGass.getErrorRate();
                        //porcentaje_error = error/velocidad;
                        puerto_desde = linkGass.getSrcPort();
                        
                        //peso = 0.1 + (error + xcnpl)/velocidad;
                        //peso = velocidad/bw + error/velocidad + (xfrpl/256)*alfa + xjitter*(1.0 - alfa)/1000;
                        peso = velocidad/bw + error/velocidad + (xfrpl/256)*alfa/(error/velocidad) + xjitter*(0.7)/100;
                        
                        //+ alfa*(error + xcnpl)/velocidad;
                        //+ (1-alfa)*(retardo/delay_max);
                        
                        System.out.println("peso calculado: "+Double.toString(peso));
                        
                        //System.out.println("velocidad link:"+velocidad);
                        //System.out.println("error link:"+error);
                        //System.out.println("delay link:"+retardo);
                         
 
                        if( !vertVisitado.get(adyacente) ){        //si el vertice adyacente no fue visitado
                            //System.out.println(adyacente+":     Vertice no visitado \n");
                            //relajacion( actual , adyacente , 1/peso, velocidad, retardo, puerto_desde, destino,puertoDestino);
                            relajacion( actual , adyacente , peso, velocidad, retardo, puerto_desde, destino,puertoDestino);
                        }
                        //else System.out.println(adyacente+":        Vertice ya visitado \n");
                    }
                }
            }
           System.out.println("------------------------------------------------------------");
            System.out.println("**************Impresion de camino mas corto**************");
            //imprimeCamino(); //fin es el vertice destino
            //System.out.printf( "Distancias mas cortas iniciando en vertice origen:%d hacia destino:%d\n" , origen,destino );
            //for( int i = 1 ; i <= numV ; ++i ){
                //System.out.println("Vertice "+i+" distancia mas corta = "+ distancia.get(i) );
            //}
            //System.out.println("\n------------------------------------------------------------");
            //System.out.println("\n");
        }
        else{
            //previo[2][ origen ] = puertoDestino;
            previo.add(new ParSwitchPuerto(origen, puertoDestino));
        }
        return previo;
    }
     
    public  void imprimeCamino(){
       /* for (Map.Entry<Long, Short> entry : previo.entrySet()){
             
            Long switchId = entry.getKey();
            Short puertoSw = entry.getValue();
            System.out.println("Switch:"+switchId+" por el puerto:"+puertoSw);
        }*/
    }
         
    public void relajacion(long actual, long adyacente, double peso, double velocidad, long retardo, short puerto_host_desde, long fin, short puertoFin){
        //System.out.println("                distancia actual ("+distancia.get((long)actual)+") + peso ("+peso+") < distancia adyacente ("+distancia.get((long)adyacente)+")");
        //Si la distancia del origen al vertice actual + peso de su arista es menor a la distancia del origen al vertice adyacente
        if( distancia.get((long)actual) + peso < distancia.get((long)adyacente) ){   //en caso de switches seria el ancho de banda,retardo o f(QoS)
            //System.out.println("                SI");
            //distancia[ adyacente ] = distancia[ actual ] + peso;  //relajamos el vertice actualizando la distancia
            double aux = distancia.get((long)actual)+peso;
            distancia.put((long)adyacente, aux);
 
            //previo[0][ adyacente ] = actual;                         //a su vez actualizamos el vertice previo
            //previo[1][ adyacente ] = puerto_host_desde;
            if(adyacente == fin){
                previo.add(new ParSwitchPuerto(actual, puerto_host_desde));//agregado por LEO************************
                previo.add(new ParSwitchPuerto(adyacente, puertoFin));
            //  previo[2][ adyacente ] = puertoFin;
            }
            else{
                previo.add(new ParSwitchPuerto(actual, puerto_host_desde));
            }
            colaPrioridad.add( new VerticePrioridad( adyacente , velocidad)); //agregamos adyacente a la cola de prioridad
        }
        else System.out.println("               NO");
    }
 
    public Map<Long, Set<LinkGass>> getTopologia(){return topologia;}
 
    public Map<Long,Boolean> getVertVisitado() {return vertVisitado;}
 
    public Map<Long,Double> getDistancia() {return distancia;}
 
    public PriorityQueue<VerticePrioridad> getColaPrioridad() {return colaPrioridad;}
 
    public int getNumV() {return numV;}
 
 
    public void setTopologia(Map<Long, Set<LinkGass>> topologia){ this.topologia=topologia;}
     
    public void setVertVisitado(Map<Long,Boolean> vertVisitado) {this.vertVisitado = vertVisitado;}
 
    public void setDistancia(Map<Long,Double> distancia) {this.distancia = distancia;}
 
    public void setColaPrioridad(PriorityQueue<VerticePrioridad> colaPrioridad) {this.colaPrioridad = colaPrioridad;}
 
    public void setNumV(int numV) {this.numV = numV;}
         
}