package fr.openent.lystore.export;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class LyceeTab {
    private Workbook wb;
    private Sheet sheet;
    private JsonObject instruction;
    private ExcelHelper excel;
    private int operationsRowNumber = 9;
    private int cellColumn = 1;
    private String TITLE = "Récapitulatif des mesures engagées";
    private String SUBTITLE = "Récapitulatif investissement";

    public LyceeTab(Workbook wb, JsonObject instruction) {
        this.wb = wb;
        this.instruction = instruction;
        this.sheet = wb.getSheet("Investissement-LYCEES");
        this.excel = new ExcelHelper(wb, sheet);


    }


    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        excel.setCPNumber(instruction.getString("cp_number"));
        setLabels();
        getPrograms(event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>("Failed to retrieve programs"));
                return;
            }

            JsonArray programs = event.right().getValue();
            setPrograms(programs);
//            excel.fillTab(1, this.cellColumn, 9, this.operationsRowNumber, this.sheet);
            handler.handle(new Either.Right<>(true));
        });
    }

    private void setLabels() {
        int cellLabelColumn = 0;

        if (this.instruction.getJsonArray("operations").isEmpty()) {
            return;
        }
        JsonArray operations = this.instruction.getJsonArray("operations");
        for (int i = 0; i < operations.size(); i++) {
            JsonObject operation = operations.getJsonObject(i);

            Row operationRow = sheet.createRow(this.operationsRowNumber);
            excel.insertLabel(operationRow, cellLabelColumn, operation.getString("label"));
            this.operationsRowNumber++;
        }
    }

    private void setPrograms(JsonArray programs) {

        int programRowNumber = 6;
        if (programs.isEmpty()) {
            return;
        }
        Row programRow = sheet.createRow(programRowNumber);
        Row actionDescRow = sheet.getRow(programRowNumber + 1);
        Row actionNumRow = sheet.getRow(programRowNumber + 2);
        for (int i = 0; i < programs.size(); i++) {
            JsonObject program = programs.getJsonObject(i);

            JsonArray actions = program.getJsonArray("actions", new JsonArray());
            if (actions.isEmpty()) continue;
            excel.insertHeader(programRow, cellColumn, program.getString("name"));
            if (actions.size() != 1) {
                CellRangeAddress merge = new CellRangeAddress(programRowNumber, programRowNumber, cellColumn, cellColumn + actions.size() - 1);
                sheet.addMergedRegion(merge);
                excel.setRegionHeader(merge, sheet);
            }
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                excel.insertHeader(actionDescRow, cellColumn, action.getString("description"));
                excel.insertHeader(actionNumRow, cellColumn, action.getString("code"));
                this.cellColumn++;

            }
        }
        System.out.println(this.cellColumn);
    }

    private void getPrograms(Handler<Either<String, JsonArray>> handler) {
        String query = "WITH values AS (" +
                "   SELECT distinct contract_type.code, program_action.description, program_action.id_program " +
                "   FROM " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id) " +
                "   WHERE instruction.id = ? " +
                "   AND structure_program_action.structure_type = 'LYC' " +
                "   AND oce.id_structure NOT IN ( " +
                "   SELECT id " +
                "   FROM " + Lystore.lystoreSchema + ".specific_structures " +
                "   )" +
                ") " +
                "SELECT program.*, array_to_json(array_agg(values)) as actions " +
                "FROM " + Lystore.lystoreSchema + ".program " +
                "INNER JOIN values ON (values.id_program = program.id) " +
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
}
