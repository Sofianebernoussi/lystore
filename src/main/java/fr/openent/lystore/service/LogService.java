package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public interface LogService {

    /**
     * List logs with pagination
     * @param page page log
     * @param handler function handler returning data
     */
    void list(Integer page, Handler<Either<String, JsonArray>> handler);
}
