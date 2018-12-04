package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ProjectService {
    /**
     * List all the projects
     *
     * @param handler
     */
    void getProjects(Handler<Either<String, JsonArray>> handler);

    void createProject(JsonObject projet, Handler<Either<String, JsonObject>> eitherHandler);


    void getProject(Integer id, Handler<Either<String, JsonObject>> handler);
}
