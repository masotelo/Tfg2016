package net.floodlightcontroller.gass_mon_leo;

//import java.util.ArrayList;
//import java.util.List;
import java.util.HashMap;
import java.util.Map;
//import java.util.Set;
//import net.floodlightcontroller.mon_gass_leo.MonLeoService;

//import net.floodlightcontroller.core.types.SwitchMessagePair;

//import org.openflow.protocol.statistics.OFStatisticsType;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class MonLeoResource extends ServerResource{
	
	@Get("json")
	public Map<String, Object> retrieve() {
        HashMap<String,Object> result = new HashMap<String,Object>();
        Object values = null;
        
        
        MonLeoService pihr = (MonLeoService)getContext().getAttributes().get(MonLeoService.class.getCanonicalName());

        values=pihr.getBuffer();
            
        result.put("testmonleo", values);
        return result;
    }
}
