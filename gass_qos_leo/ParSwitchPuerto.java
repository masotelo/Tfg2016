package net.floodlightcontroller.gass_qos_leo;

public class ParSwitchPuerto {

	private Long swID;
	private Short puerto;

	
	public ParSwitchPuerto(Long swID,Short puerto){
		this.swID = swID;
		this.puerto = puerto;

	}

	public Long getSwID() {
		return swID;
	}

	public void setSwID(Long swID) {
		this.swID = swID;
	}


	public Short getPuerto() {
		return puerto;
	}

	public void setPuerto(Short puerto) {
		this.puerto = puerto;
	}


}
