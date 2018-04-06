package fr.openent.lystore;

import fr.openent.lystore.controllers.*;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.entcore.common.storage.impl.MongoDBApplicationStorage;
import org.vertx.java.core.json.JsonObject;

public class Lystore extends BaseServer {

    public static String lystoreSchema;

    public static final String ADMINISTRATOR_RIGHT = "lystore.administrator";
    public static final String MANAGER_RIGHT = "lystore.manager";

    @Override
    public void start() {
        lystoreSchema = container.config().getString("db-schema");
        super.start();

        Storage storage = new StorageFactory(vertx, config).getStorage();

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
        addController(new PurseController());
        addController(new StructureGroupController());
        addController(new StructureController());
        addController(new BasketController(vertx, container.config().getObject("slack", new JsonObject()) ));
        addController(new OrderController(storage));
    }
}
