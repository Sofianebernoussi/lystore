package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.ExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import io.vertx.core.logging.Logger;

public class DefaultExportServiceService implements ExportService {
    Storage storage;
    private Logger logger = LoggerFactory.getLogger(DefaultProjectService.class);
    private EventBus eb;

    public DefaultExportServiceService(String lystoreSchema, String instruction, Storage storage) {
        this.storage = storage;
    }

    @Override
    public void getExports(Handler<Either<String, JsonArray>> handler, UserInfos user) {
        String query = "" +
                "SELECT " +
                "id, " +
                "status, " +
                "filename," +
                "fileid," +
                "created," +
                "instruction_name," +
                "instruction_id " +
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

    public void deleteExport(JsonArray filesIds, Handler<JsonObject> handler) {
        storage.removeFiles(filesIds, handler);
    }

    public void deleteExportSql(JsonArray idsExports, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray();
        for (int i = 0; i < idsExports.size(); i++) {
            values.add(idsExports.getValue(i));
        }

        String query = "DELETE " +
                "FROM " + Lystore.lystoreSchema + ".export " +
                "WHERE id IN " +
                Sql.listPrepared(idsExports.getList()) + " " +
                "RETURNING id ";
        Sql.getInstance().prepared(query, values, SqlResult.validRowsResultHandler(handler));

    }
    public void createWhenStart (Integer instruction_id,String nameFile, String userId, Handler<Either<String, JsonObject>> handler){
        try{
            String nameQuery= "SELECT object from "+Lystore.lystoreSchema+".instruction where id= ?";
            Sql.getInstance().prepared(nameQuery,new JsonArray().add(instruction_id), SqlResult.validResultHandler(new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if(event.isRight()){
                        JsonArray results = event.right().getValue();
                        String query = "" +
                                "INSERT INTO " +
                                Lystore.lystoreSchema + ".export(  " +
                                "filename," +
                                "ownerid," +
                                " instruction_id," +
                                " instruction_name) " +
                                "VALUES (" +
                                "?, " +
                                "?," +
                                "?," +
                                "?)" +
                                "RETURNING id ;";
                        JsonArray params = new JsonArray()
                                .add(nameFile)
                                .add(userId)
                                .add(instruction_id)
                                .add(results.getJsonObject(0).getString("object"));

                        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
                    }else{
                        handler.handle(new Either.Left<>("Error when init export excel in SQL"));
                        logger.error("Error when init export excel in SQL");
                    }
                }
            }));
        } catch (Exception error){
            logger.error("error when create export" + error);
        }
    }

    public void updateWhenError (Number idExport, Handler<Either<String, JsonObject>> handler){
        try{
            String query = "" +
                    "UPDATE " +
                    Lystore.lystoreSchema + ".export  " +
                    "SET " +
                    "status = 'ERROR'  " +
                    "WHERE id = ? ";
            Sql.getInstance().prepared(query, new JsonArray().add(idExport), SqlResult.validUniqueResultHandler(handler));
        } catch (Exception error){
            logger.error("error when update ERROR in export" + error);
        }
    }

    public void updateWhenSuccess (String fileId, Number idExport, Handler<Either<String, JsonObject>> handler){
        try{
            String query = "" +
                    "UPDATE " +
                    Lystore.lystoreSchema + ".export  " +
                    "SET " +
                    "fileid = ? ," +
                    "status = 'SUCCESS'  " +
                    "WHERE id = ? ";
            Sql.getInstance().prepared(query, new JsonArray().add(fileId).add(idExport), SqlResult.validUniqueResultHandler(handler));
        } catch (Exception error){
            logger.error("error when update SUCCESS in export" + error);
        }
    }
}
