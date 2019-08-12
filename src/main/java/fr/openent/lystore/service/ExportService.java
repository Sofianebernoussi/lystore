package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface ExportService {
    void getExports(Handler<Either<String, JsonArray>> handler, UserInfos user);

    void getXlsx(String fileId, Handler<Buffer> handler);

    void getXlsxName(String fileId, Handler<Either<String, JsonArray>> handler);

    void deleteExport(String fileId, Handler<JsonObject> handler);
}
