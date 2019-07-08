package fr.openent.lystore.export.investissement;

import fr.openent.lystore.Lystore;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class RecapImputationBud extends TabHelper {
    private JsonArray programs;
    private final int xTab = 0;
    private final int yTab = 7;

    public RecapImputationBud(Workbook workbook, JsonObject instruction) {

        super(workbook, instruction, TabName.IMPUTATION_BUDG.toString());
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        getPrograms(event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>("Failed to retrieve programs"));
                return;
            }
            setArray(programs);
            handler.handle(new Either.Right<>(true));
        });
    }

    @Override
    protected void setArray(JsonArray programs) {
        JsonObject program;
        for (int i = 0; i < programs.size(); i++) {
            program = programs.getJsonObject(i);
            excel.insertCellTab(xTab, yTab + i, program.getString("section"));
            excel.insertCellTab(xTab + 1, yTab + i, program.getInteger("chapter").toString());
            excel.insertCellTab(xTab + 2, yTab + i, program.getInteger("functional_code").toString());
            excel.insertCellTab(xTab + 3, yTab + i, program.getString("program_name"));
            excel.insertCellTab(xTab + 4, yTab + i, program.getString("program_label"));
            excel.insertCellTab(xTab + 5, yTab + i, program.getString("action_code"));
            excel.insertCellTab(xTab + 6, yTab + i, program.getString("action_name"));
            excel.insertCellTabFloatWithPrice(xTab + 7, yTab + i, Float.parseFloat(program.getString("total")));


        }
    }

    @Override
    public void getPrograms(Handler<Either<String, JsonArray>> handler) {
        query = "SELECT  program_action.action as action_code, program.section, program_action.description as action_name,program_action.id as action_id , program.name as program_name,program.id as program_id,  " +
                "program.label as program_label, program.functional_code, program.chapter, " +
                " SUM((oce.price * oce.amount) + ((oce.price*oce.amount)*oce.tax_amount)/100 ) as Total  " +
                "FROM lystore.order_client_equipment oce " +
                "INNER JOIN " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id)   " +
                "INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)    " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id)   " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)   " +
                "INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)  " +
                "INNER JOIN " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id)  " +
                "WHERE instruction.id = ? " +
                "group by program_action.id,program.id " +
                "order by section desc,program_id,action_id;";

        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                programs = event.right().getValue();
                handler.handle(new Either.Right<>(programs));
            }
        }));
    }

}
