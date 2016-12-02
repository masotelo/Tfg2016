package net.floodlightcontroller.gass_mon_leo;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class MonLeoWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		// TODO Auto-generated method stub
		Router router = new Router(context);
		router.attach("/monleo/json", MonLeoResource.class);
		return router;
		//return null;
	}

	@Override
	public String basePath() {
		// TODO Auto-generated method stub
		return "/wm/pktmonleo";
		//return null;
	}

}
