package net.floodlightcontroller.gass_qos_leo;

public class VerticePrioridad implements Comparable<VerticePrioridad> {

	private long id_switch=0; //identificador para nuestro calculo
	private double prioridad;//prioridad del switch
	
	public VerticePrioridad(){
		this.id_switch=0L;
		this.prioridad=0.0;
	}
	
	public VerticePrioridad(long id, double pri){
		this.id_switch=id;
		this.prioridad=pri;
	}

	public long getId_switch() {
		return id_switch;
	}

	public void setId_switch(long id_switch) {
		this.id_switch = id_switch;
	}

	public double getPrioridad() {
		return prioridad;
	}

	public void setPrioridad(double prioridad) {
		this.prioridad = prioridad;
	}
	
	//comparador de velocidad
	public int compareTo( VerticePrioridad v){				
		if( this.prioridad < v.getPrioridad() ) return 1;
		if( this.prioridad == v.getPrioridad() ) return 0;
		return -1;
	}


	
}
