package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface TitleService {
    /**
     * List all the titles
     *
     * @param handler
     */
    void getTitles(Handler<Either<String, JsonArray>> handler);


}
