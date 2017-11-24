package fr.openent.lystore;

import fr.openent.lystore.controllers.AgentController;
import org.entcore.common.http.BaseServer;

import fr.openent.lystore.controllers.LystoreController;

public class Lystore extends BaseServer {

    public static String LYSTORE_SCHEMA;

	@Override
	public void start() {
        LYSTORE_SCHEMA = container.config().getString("db-schema");
        super.start();
        addController(new LystoreController());
        addController(new AgentController());
	}

}
