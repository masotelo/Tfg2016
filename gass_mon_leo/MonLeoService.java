package net.floodlightcontroller.gass_mon_leo;

import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.module.IFloodlightService;
//import net.floodlightcontroller.core.types.SwitchMessagePair;
//import java.util.concurrent.atomic.AtomicInteger;

public interface MonLeoService extends IFloodlightService {
	public Map<Long, Set<LinkGass>> getBuffer();
}
