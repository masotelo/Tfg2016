package net.floodlightcontroller.gass_qos_leo;



public class VerticeSwitch implements Comparable<VerticeSwitch>{

	
	private long id_switch=0; //identificador para nuestro calculo
	private double prioridad;//prioridad del switch
	private long id_dir_mac;//identificador con la direccion del switch
	
	private long dataRate_enlace;//medido en mbps
	private long errorRate_enlace;//medido en ms
	private long delay_enlace;

	
	private int puerto_desde; 
	//puerto de salida del switch que se conecta con este (NO CONFUNDIR con el puerto entrada de este switch-> inicializacion = 0
	private int puerto_host;
	//puerto de salida de ESTE switch que se conecta con host
	
	public VerticeSwitch(long id, double pri, long id_m, long dataRate, long erroRate, long delay, int p_desde, int p_host ){							
		this.id_switch = id;
		this.prioridad = pri;
		this.id_dir_mac = id_m;
		this.dataRate_enlace = dataRate;
		this.errorRate_enlace = erroRate;
		this.delay_enlace = delay;
		this.puerto_desde = p_desde;
		this.puerto_host = p_host;
	}
	
	//comparadores para ordenar correctamente la cola de prioridad
	
	//comparador de velocidad
	public int compareTo( VerticeSwitch v){				
		if( this.prioridad < v.prioridad ) return 1;
		if( this.prioridad == v.prioridad ) return 0;
		return -1;
	}
	
	//comparador de velocidad y retraso con un 0.6*velocidad mas 0.4*retardo
//	public int compareTo( VerticeSwitch v){				
//		if( (this.velocidad_enlace*0.6+this.retardo_enlace*0.4) > (v.velocidad_enlace*0.6+v.retardo_enlace*0.4) ) return 1;
//		if( (this.velocidad_enlace*0.6+this.retardo_enlace*0.4) == (v.velocidad_enlace*0.6+v.retardo_enlace*0.4) ) return 0;
//		return -1;
//	}
	
	//comparador de f(QoS)//TODO
//	public int compareTo( VerticeSwitch v){				
//		if( this.prioridad > v.prioridad ) return 1;
//		if( this.prioridad == v.prioridad ) return 0;
//		return -1;
//	}
	

	public long getIdSwitch() {return id_switch;}

	public double getPrioridad() {return prioridad;}

	public long getIdDirMac() {return id_dir_mac;}
	
	public long getDataRateEnlace() {return dataRate_enlace;}

	public long getErrorRateEnlace() {return errorRate_enlace;}
	
	public long getDelayEnlace() {return delay_enlace;}
	
	public int getPuertoDesde() {return puerto_desde;}

	public int getPuertoHost() {return puerto_host;}

	

	public void setIdSwitch(int id_switch) {this.id_switch = id_switch;}

	public void setPrioridad(double prioridad) {this.prioridad = prioridad;}

	public void setIdDirMac(long id_dir_mac) {this.id_dir_mac = id_dir_mac;}

	public void setDataRateEnlace(long dataRate_enlace) {this.dataRate_enlace = dataRate_enlace;}

	public void setErrorRateEnlace(long errorRate_enlace) {this.errorRate_enlace = errorRate_enlace;}

	public void setDelayEnlace(long delay_enlace) {this.delay_enlace = delay_enlace;}
	
	public void setPuertoDesde(int puerto_desde) {this.puerto_desde = puerto_desde;}
	
	public void setPuertoHost(int puerto_host) {this.puerto_host = puerto_host;}
	
	
	
}
