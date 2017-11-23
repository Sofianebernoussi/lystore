package fr.openent.lystore;

import org.entcore.common.http.BaseServer;

import fr.openent.lystore.controllers.LystoreController;

public class Lystore extends BaseServer {

	@Override
	public void start() {
	    super.start();
		addController(new LystoreController());
	}

}
