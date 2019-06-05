package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.InstructionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;

import java.util.List;

public class DefaultInstructionService  extends SqlCrudService implements InstructionService {

    public DefaultInstructionService(String schema, String table) {
        super(schema, table);
    }

    public void getExercises (Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Lystore.lystoreSchema +".exercise";
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler) );
    }


    private String getTextFilter(List<String> filters) {
        String filter = "";
        if (filters.size() > 0) {
            filter = "WHERE ";
            for (int i = 0; i < filters.size(); i++) {
                if (i > 0) {
                    filter += "AND ";
                }

                filter += "(LOWER(label.label) ~ LOWER(?) OR LOWER(o.order_number) ~ LOWER(?)) ";
            }
        }

        return filter;
    }

    public void getInstructions(List<String> filters, Handler<Either<String, JsonArray>> handler){
        JsonArray params = new JsonArray();
        if (!filters.isEmpty()) {
            for (String filter : filters) {
                params.add(filter).add(filter);
            }
        }
        String query =  "SELECT  instruction.* , to_json(exercise_years.*) AS exercise_years " +
                "FROM  " + Lystore.lystoreSchema +".instruction " +
                "INNER JOIN " + Lystore.lystoreSchema +".exercise exercise_years ON exercise_years.id = instruction.id_exercise " +
                "GROUP BY (instruction.id, exercise_years.*)";
                //getTextFilter(filters);
        sql.prepared(query, params, SqlResult.validResultHandler(handler) );
    }

/*    public void create(JsonObject operation, Handler<Either<String, JsonObject>> handler){
        String query = "INSERT INTO " + Lystore.lystoreSchema + ".operation(id_label, status) " +
                "VALUES (?, ?) RETURNING id;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(operation.getInteger("id_label"))
                .add(operation.getBoolean("status"));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
 /*   public  void updateOperation(Integer id, JsonObject operation, Handler<Either<String, JsonObject>> handler){
        String query = " UPDATE " + Lystore.lystoreSchema + ".operation " +
                "SET id_label = ?, " +
                "status = ? " +
                " WHERE id = ?;";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
                .add(operation.getInteger("id_label"))
                .add(operation.getBoolean("status"))
                .add(id);
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

/*    public  void deleteOperation(JsonArray operationIds, Handler<Either<String, JsonObject>> handler){
        JsonArray values = new JsonArray();
        for (int i = 0; i < operationIds.size(); i++) {
            values.add(operationIds.getValue(i));
        }
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".operation" + " WHERE id IN " + Sql.listPrepared(operationIds.getList());
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }
 */
}
