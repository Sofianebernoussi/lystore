package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface GradeService {

    /**
     * List all grades in database
     *
     * @param handler Function handler returning data
     */
    void listGrades(Handler<Either<String, JsonArray>> handler);
}
