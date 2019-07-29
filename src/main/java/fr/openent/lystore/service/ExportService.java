package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.user.UserInfos;

public interface ExportService {
    void getExports(Handler<Either<String, JsonArray>> handler, UserInfos user);
}
