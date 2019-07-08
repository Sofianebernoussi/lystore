package fr.openent.lystore;

import fr.openent.lystore.controllers.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

public class Lystore extends BaseServer {

    public static String lystoreSchema;

    public static Integer PAGE_SIZE = 50;

    public static final String ADMINISTRATOR_RIGHT = "lystore.administrator";
    public static final String MANAGER_RIGHT = "lystore.manager";

    @Override
    public void start() throws Exception {
        super.start();
        lystoreSchema = config.getString("db-schema");
        EventBus eb = getEventBus(vertx);
        Storage storage = new StorageFactory(vertx, config).getStorage();
        JsonObject mail = config.getJsonObject("mail", new JsonObject());

        addController(new LystoreController());
        addController(new AgentController());
        addController(new SupplierController());
        addController(new ProgramController());
        addController(new ContractTypeController());
        addController(new ContractController());
        addController(new TagController());
        addController(new EquipmentController(vertx));
        addController(new TaxController());
        addController(new LogController());
        addController(new CampaignController());
        addController(new PurseController(vertx));
        addController(new StructureGroupController(vertx));
        addController(new StructureController());
        addController(new BasketController(vertx, storage, config.getJsonObject("slack", new JsonObject()), mail));
        addController(new OrderController(storage, vertx, config, eb));
        addController(new UserController());
        addController(new EquipmentTypeController());
        addController(new TitleController(vertx, eb));
        addController(new GradeController());
        addController(new ProjectController());
        addController(new OperationController());
        addController(new InstructionController(storage));
    }
}
