package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface OrderRegionService {
    void createOrderRegion(JsonObject order, UserInfos user, Handler<Either<String, JsonObject>> handler);
}
