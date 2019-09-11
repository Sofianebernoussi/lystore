package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.MongoHelper;
import fr.openent.lystore.service.ExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
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
    MongoHelper mongo;

    public DefaultExportServiceService(String lystoreSchema, String instruction, Storage storage) {
        this.storage = storage;
        mongo = new MongoHelper(Lystore.LYSTORE_COLLECTION);

    }

    @Override
    public void getExports(Handler<Either<String, JsonArray>> handler, UserInfos user) {
      mongo.getExports(handler,user.getUserId());
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
        mongo.getExport(new JsonObject().put("fileId",fileId),handler);
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
        try {

            String nameQuery= "SELECT object from "+Lystore.lystoreSchema+".instruction where id= ?";
            Sql.getInstance().prepared(nameQuery,new JsonArray().add(instruction_id), SqlResult.validResultHandler(new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if(event.isRight()){

                        JsonArray results = event.right().getValue();
                        JsonObject params = new JsonObject()
                                .put("filename",nameFile)
                                .put("userId",userId)
                                .put("instruction_id",instruction_id)
                                .put("instruction_name",results.getJsonObject(0).getString("object"))
                                .put("status","WAITING");

                        mongo.addExport(params, new Handler<String>() {
                            @Override
                            public void handle(String event) {
                                if(event.equals("mongoinsertfailed"))
                                    handler.handle(new Either.Left<>("Error when inserting mongo"));
                                else{
                                    handler.handle(new Either.Right<>(new JsonObject().put("id",event)));
                                }
                            }
                        });
                    }else{
                        handler.handle(new Either.Left<>("Error when init export excel in Mongo"));
                        logger.error("Error when init export excel in Mongo");
                    }
                }
            }));
        } catch (Exception error){
            logger.error("error when create export" + error);
        }
    }

    public void updateWhenError (String idExport, Handler<Either<String, Boolean>> handler){
        try{
            mongo.updateExport(idExport,"ERROR", "NO FILE", new Handler<String>() {
                @Override
                public void handle(String event) {
                    if(event.equals("mongoinsertfailed"))
                        handler.handle(new Either.Left<>("Error when inserting mongo"));
                    else{
                        handler.handle(new Either.Right<>(true));
                    }

                }
            });
        } catch (Exception error){
            logger.error("error when update ERROR in export" + error);
        }
    }

    public void updateWhenSuccess (String fileId, String idExport, Handler<Either<String, Boolean>> handler) {
        try {
            mongo.updateExport(idExport,"SUCCESS",fileId,  new Handler<String>() {
                @Override
                public void handle(String event) {
                    if (event.equals("mongoinsertfailed"))
                        handler.handle(new Either.Left<>("Error when inserting mongo"));
                    else {
                        handler.handle(new Either.Right<>(true));
                    }
                }
            });
        } catch (Exception error) {
            logger.error("error when update ERROR in export" + error);

        }
    }
}
