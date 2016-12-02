package net.floodlightcontroller.gass_qos_leo;

//import java.util.ArrayList;
//import java.util.List;
import java.util.HashMap;
import java.util.Map;
//import java.util.Set;
//import net.floodlightcontroller.mon_gass_leo.MonLeoService;

//import net.floodlightcontroller.core.types.SwitchMessagePair;

import net.floodlightcontroller.gass_mon_leo.MonLeoService;

//import org.openflow.protocol.statistics.OFStatisticsType;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class QoSLeoSendHostValueResource extends ServerResource{
	
	@Get("json")
	public Map<String, Object> retrieve() {
        HashMap<String,Object> result = new HashMap<String,Object>();
        Object values = null;
        
        QoSLeoService pihr = (QoSLeoService)getContext().getAttributes().get(QoSLeoService.class.getCanonicalName());
        
        String hostValue = (String) getRequestAttributes().get("valueHost"); 
        
        pihr.setHostValue(hostValue);
        values=hostValue;
        
        result.put("received FROM CLIENT", values);
        return result;
    }
}
