package net.floodlightcontroller.gass_qos_leo;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.core.web.SwitchRoleResource;
import net.floodlightcontroller.restserver.RestletRoutable;

public class QoSLeoWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		// TODO Auto-generated method stub
		Router router = new Router(context);
		router.attach("/qosleo/json", QoSLeoResource.class);
		router.attach("/qosleo/{valueHost}/json", QoSLeoSendHostValueResource.class);
		return router;
		//return null;
	}

	@Override
	public String basePath() {
		// TODO Auto-generated method stub
		return "/wm/pktqosleo";
		//return null;
	}

}
