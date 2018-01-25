package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public interface StructureService {

    void getStructureByUAI(JsonArray uais, Handler<Either<String, JsonArray>> handler);
}
