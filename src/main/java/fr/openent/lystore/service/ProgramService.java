package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public interface ProgramService {

    /**
     * List all programs in database
     * @param handler Function handler returning data
     */
    void listPrograms (Handler<Either<String, JsonArray>> handler);
}
