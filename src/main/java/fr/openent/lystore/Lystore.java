package fr.openent.lystore;

import fr.openent.lystore.controllers.*;
import org.entcore.common.http.BaseServer;

public class Lystore extends BaseServer {

    public static String LYSTORE_SCHEMA;

    public static final String ADMINISTRATOR_RIGHT = "lystore.administrator";
    public static final String MANAGER_RIGHT = "lystore.manager";

	@Override
	public void start() {
        LYSTORE_SCHEMA = container.config().getString("db-schema");
        super.start();
        addController(new LystoreController());
        addController(new AgentController());
        addController(new SupplierController());
        addController(new ProgramController());
        addController(new ContractTypeController());
        addController(new ContractController());
        addController(new TagController());
        addController(new EquipmentController());
        addController(new TaxController());
        addController(new LogController());
        addController(new CampaignController());
	}

}
