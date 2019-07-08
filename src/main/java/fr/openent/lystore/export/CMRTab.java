package fr.openent.lystore.export;

import fr.openent.lystore.Lystore;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class CMRTab extends Investissement {

    /**
     * Format : H-code
     */

    public CMRTab(Workbook wb, JsonObject instruction) {
        super(wb, instruction, TabName.CMR.toString());
    }


    @Override
    public void getPrograms(Handler<Either<String, JsonArray>> handler) {
        String query = "WITH values AS (" +
                "   SELECT distinct contract_type.code,  contract_type.name, program_action.id_program  " +
                "   FROM " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id) " +
                "   WHERE instruction.id = ? " +
                "   AND structure_program_action.structure_type = '" + CMR + "'" +
                "   AND oce.id_structure IN ( " +
                "   SELECT id " +
                "   FROM " + Lystore.lystoreSchema + ".specific_structures " +
                "   )" +
                ") " +
                "SELECT program.*, array_to_json(array_agg(values)) as actions " +
                "FROM " + Lystore.lystoreSchema + ".program " +
                "INNER JOIN values ON (values.id_program = program.id) " +
                "WHERE program.section ='" + Investissement + "' " +
                "GROUP BY program.id";

        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                JsonArray programs = event.right().getValue();
                for (int i = 0; i < programs.size(); i++) {
                    JsonObject program = programs.getJsonObject(i);
                    program.put("actions", new JsonArray(program.getString("actions")));

                }

                handler.handle(new Either.Right<>(programs));
            }
        }));
    }

    /**
     * Get all the prices of equipments
     *
     * @param handler
     */
    @Override
    public void getPrices(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT SUM((oce.price * oce.amount) + ((oce.price*oce.amount)*oce.tax_amount)/100 ) as Total ," +
                "contract_type.code as code, program_action.id_program as id_program ,oce.id_operation " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment oce  " +
                "INNER JOIN " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)   " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id " +
                "AND structure_program_action.structure_type =  '" + CMR + "') " +
                "INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)  " +
                "INNER JOIN " + Lystore.lystoreSchema + ".program ON (program.id = program_action.id_program and program.section =  '" + Investissement + "')" +
                "WHERE instruction.id = ?   AND structure_program_action.structure_type = '" + CMR + "'   " +
                "AND oce.id_structure  IN (    SELECT id    FROM " + Lystore.lystoreSchema + ".specific_structures WHERE specific_structures.type = 'CMR') " +
                "Group by  contract_type.code, program_action.id, oce.id_operation " +
                "order by id_program,code,oce.id_operation";
        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                JsonArray commands = event.right().getValue();
                for (int i = 0; i < commands.size(); i++) {
                    JsonObject command = commands.getJsonObject(i);
                    float priceCommand = (Float.parseFloat(command.getString("total")));
                    for (int y = 0; y < taby.size(); y++) {
                        if (command.getInteger("id_operation") == taby.getInteger(y)) {
                            priceTab.get(tabx.getInteger(command.getInteger("id_program").toString() + "-" + command.getString("code"))).set(y, priceTab.get(0).get(y) + priceCommand);
                        }
                    }
                }

                setPrices();
                handler.handle(new Either.Right<>(commands));
            }
        }));

    }
}
