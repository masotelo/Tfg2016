package net.floodlightcontroller.gass_qos_leo;

import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.module.IFloodlightService;
//import net.floodlightcontroller.core.types.SwitchMessagePair;
//import java.util.concurrent.atomic.AtomicInteger;
import net.floodlightcontroller.gass_mon_leo.LinkGass;

public interface QoSLeoService extends IFloodlightService {
	public int getUsenetValue();
	
	public void setHostValue(String hostValue);
	
}
