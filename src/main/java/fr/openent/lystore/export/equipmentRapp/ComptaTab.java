package fr.openent.lystore.export.equipmentRapp;

import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class ComptaTab extends TabHelper {
    private JsonArray programs;

    public ComptaTab(Workbook workbook, JsonObject instruction) {
        super(workbook, instruction, "COMPTA DU RAPPORT ");
        excel.setDefaultFont();
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        setLabels();
        getDatas(event -> {
            if (event.isLeft()) {
                log.error("Failed to retrieve programs");
                handler.handle(new Either.Left<>("Failed to retrieve programs"));
            } else {

                JsonArray programs = event.right().getValue();
                //Delete tab if empty

                if (programs.size() == 0) {
                    wb.removeSheetAt(wb.getSheetIndex(sheet));
                }
                handler.handle(new Either.Right<>(true));
            }
        });
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "SELECT distinct contract_type.code as contract_code,  contract_type.name as contract_name, contract_type.id as contract_type_id," +
                "oce.id_structure, " +
                "program_action.action as action_code, program_action.description as action_name,program_action.id as action_id , " +
                "program.name as program_name,program.id as program_id, program.label as program_label," +
                "oce.id_structure,oce.amount as amount,oce.name as label,oce.comment as comment,oce.id,  " +
                "SUM(CASE WHEN oce.price_proposal is not null THEN oce.price_proposal *  oce.amount ELSE (oce.price * oce.amount) + ((oce.price*oce.amount)*oce.tax_amount)/100 END) as Total  " +
                "FROM lystore.order_client_equipment oce    " +
                "INNER JOIN lystore.operation ON (oce.id_operation = operation.id)   " +
                " INNER JOIN lystore.instruction ON (operation.id_instruction = instruction.id)  " +
                " INNER JOIN lystore.contract ON (oce.id_contract = contract.id)  " +
                " INNER JOIN lystore.contract_type ON (contract.id_contract_type = contract_type.id)   " +
                " INNER JOIN lystore.structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)   " +
                " INNER JOIN lystore.program_action ON (structure_program_action.program_action_id = program_action.id)  " +
                " INNER JOIN lystore.program ON (program_action.id_program = program.id) " +
                " WHERE instruction.id = ? " +
                "group by oce.id,contract_type.id,contract_code, contract_name, program_action.id,program.id,oce.id_structure,oce.name,oce.comment,oce.amount " +
                "order by program_name";


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
