package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public interface TaxService {

    /**
     * List all taxes in database
     * @param handler function handler returning data
     */
    public void list (Handler<Either<String, JsonArray>> handler);
}
