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

    void deleteExport(JsonArray filesIds, Handler<JsonObject> handler);

    void deleteExportMongo(JsonArray idsExports, Handler<Either<String, JsonObject>> handler);

    void createWhenStart(String typeObject, JsonObject infoFile, String instruction_id, String nameFile, String userId, String action, Handler<Either<String, JsonObject>> handler);

    void updateWhenError(String idExport, Handler<Either<String, Boolean>> handler);

    void updateWhenSuccess(String fileId, String idExport, Handler<Either<String, Boolean>> handler);


    void getWaitingExport(Handler<Either<String, JsonObject>> handler);
}
