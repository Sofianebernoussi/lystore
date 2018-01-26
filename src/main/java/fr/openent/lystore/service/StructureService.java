package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public interface StructureService {

    public void getStructures(Handler<Either<String,JsonArray>> handler);

    void getStructureByUAI(JsonArray uais, Handler<Either<String, JsonArray>> handler);

    void getStructureById(JsonArray ids, Handler<Either<String, JsonArray>> handler);
}
