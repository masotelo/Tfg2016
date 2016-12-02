package net.floodlightcontroller.gass_mon_leo;

public class EstadisticasSwitch {

	private long idSwitch;
	private short idPuerto;
	private long paquetesRecibidos; 
	private long paquetesTransmitidos; 
	private long colisiones;
	private long bytesRecibidos;
	private long bytesTransmitidos;
	private long paquetesPerdidosIN;
	private long paquetesPerdidosOUT;
	
	public EstadisticasSwitch(long idSwitch, short idPuerto, long paquetesRecibidos, long paquetesTransmitidos, long colisiones, long bytesRecibidos,
			long bytesTransmitidos, long paquetesPerdidosIN,long paquetesPerdidosOUT) {
		this.idSwitch = idSwitch;
		this.idPuerto = idPuerto;
		this.paquetesRecibidos = paquetesRecibidos;
		this.paquetesTransmitidos = paquetesTransmitidos;
		this.colisiones = colisiones;
		this.bytesRecibidos = bytesRecibidos;
		this.bytesTransmitidos = bytesTransmitidos;
		this.paquetesPerdidosIN = paquetesPerdidosIN;
		this.paquetesPerdidosOUT = paquetesPerdidosOUT;
	}

	public long getIdSwitch() {return idSwitch;}
	public short getIdPuerto() {return idPuerto;}

	public long getPaquetesRecibidos() {return paquetesRecibidos;}
	public long getPaquetesTransmitidos() {return paquetesTransmitidos;}
	
	public long getColisiones() {return colisiones;}
	
	public long getBytesRecibidos() {return bytesRecibidos;}
	public long getBytesTransmitidos() {return bytesTransmitidos;}
	
	public long getPaquetesPerdidosIN() {return paquetesPerdidosIN;}
	public long getPaquetesPerdidosOUT() {return paquetesPerdidosOUT;}

	
	public void setIdSwitch(long idSwitch) {this.idSwitch = idSwitch;}
	public void setIdPuerto(short idPuerto) {this.idPuerto = idPuerto;}

	public void setPaquetesRecibidos(long paquetesRecibidos) {this.paquetesRecibidos = paquetesRecibidos;}
	public void setPaquetesTransmitidos(long paquetesTransmitidos) {this.paquetesTransmitidos = paquetesTransmitidos;}
	
	public void setColisiones(long colisiones) {this.colisiones = colisiones;}
	
	public void setBytesRecibidos(long bytesRecibidos) {this.bytesRecibidos = bytesRecibidos;}
	public void setBytesTransmitidos(long bytesTransmitidos) {this.bytesTransmitidos = bytesTransmitidos;}
	
	public void setPaquetesPerdidosIN(long paquetesPerdidosIN) {this.paquetesPerdidosIN = paquetesPerdidosIN;}
	public void setPaquetesPerdidosOUT(long paquetesPerdidosOUT) {this.paquetesPerdidosOUT = paquetesPerdidosOUT;}
	
	
}
