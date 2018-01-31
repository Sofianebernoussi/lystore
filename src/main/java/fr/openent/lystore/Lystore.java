package fr.openent.lystore;

import fr.openent.lystore.controllers.AgentController;
import fr.openent.lystore.controllers.CampaignController;
import fr.openent.lystore.controllers.ContractController;
import fr.openent.lystore.controllers.ContractTypeController;
import fr.openent.lystore.controllers.EquipmentController;
import fr.openent.lystore.controllers.LogController;
import fr.openent.lystore.controllers.LystoreController;
import fr.openent.lystore.controllers.ProgramController;
import fr.openent.lystore.controllers.PurseController;
import fr.openent.lystore.controllers.StructureController;
import fr.openent.lystore.controllers.StructureGroupController;
import fr.openent.lystore.controllers.SupplierController;
import fr.openent.lystore.controllers.TagController;
import fr.openent.lystore.controllers.TaxController;
import org.entcore.common.http.BaseServer;

public class Lystore extends BaseServer {

    public static String lystoreSchema;

    public static final String ADMINISTRATOR_RIGHT = "lystore.administrator";
    public static final String MANAGER_RIGHT = "lystore.manager";

    @Override
    public void start() {
        lystoreSchema = container.config().getString("db-schema");
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
        addController(new PurseController());
        addController(new StructureGroupController());
        addController(new StructureController());
    }
}
