package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface OperationService {

    void getLabels (Handler<Either<String, JsonArray>> handler);

    void getOperations(Handler<Either<String, JsonArray>> handler);

    void create(JsonObject operation,  Handler<Either<String, JsonObject>> handler);

    void updateOperation(Integer id, JsonObject operation, Handler<Either<String, JsonObject>> handler);
}
