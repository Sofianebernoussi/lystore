package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public interface ContractTypeService {

    /**
     * List all contract types in database
     * @param handler Function handler returning data
     */
    public void listContractTypes(Handler<Either<String, JsonArray>> handler);
}
