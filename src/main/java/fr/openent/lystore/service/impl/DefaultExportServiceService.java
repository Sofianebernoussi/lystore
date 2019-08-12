package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.ExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;

public class DefaultExportServiceService implements ExportService {
    Storage storage;

    public DefaultExportServiceService(String lystoreSchema, String instruction, Storage storage) {
        this.storage = storage;
    }

    @Override
    public void getExports(Handler<Either<String, JsonArray>> handler, UserInfos user) {
        String query = "SELECT filename,fileid,created " +
                "FROM " + Lystore.lystoreSchema + ".export " +
                "WHERE ownerid = ?" +
                "order by created  DESC";
        Sql.getInstance().prepared(query, new JsonArray().add(user.getUserId()), SqlResult.validResultHandler(handler));

    }


    @Override
    public void getXlsx(String fileId, Handler<Buffer> handler) {
        storage.readFile(fileId, handler);

    }

    @Override
    public void getXlsxName(String fileId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT filename " +
                "FROM " + Lystore.lystoreSchema + ".export " +
                "WHERE fileId = ?";
        Sql.getInstance().prepared(query, new JsonArray().add(fileId), SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteExport(String fileId, Handler<JsonObject> handler) {
        deleteExportSql(fileId, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isLeft()) {
                    handler.handle(new JsonObject().put("error", "Error when deleting in SQL"));
                } else {
                    storage.removeFile(fileId, handler);
                }
            }
        });
    }

    private void deleteExportSql(String fileId, Handler<Either<String, JsonArray>> handler) {
        String query = "DELETE " +
                "FROM " + Lystore.lystoreSchema + ".export " +
                "WHERE fileId = ?";
        Sql.getInstance().prepared(query, new JsonArray().add(fileId), SqlResult.validResultHandler(handler));

    }
}
