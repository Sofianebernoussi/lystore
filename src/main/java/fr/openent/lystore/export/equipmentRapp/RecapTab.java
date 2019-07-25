package fr.openent.lystore.export.equipmentRapp;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

public class RecapTab extends TabHelper {
    private JsonArray programs;
    String type = "";
    final protected int yTab = 0;
    final protected int xTab = 0;
    protected int operationsRowNumber = 2;
    JsonArray operations;
    private String actionStr = "actions";

    public RecapTab(Workbook workbook, JsonObject instruction, String type) {
        super(workbook, instruction, "RECAP - " + type);
        this.type = type;
        excel.setDefaultFont();
    }


    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
//        setLabels();
        getDatas(event -> {
            if (event.isLeft()) {
                log.error("Failed to retrieve programs");
                handler.handle(new Either.Left<>("Failed to retrieve programs"));
            } else {

                JsonArray programs = event.right().getValue();
                //Delete tab if empty
                setArray(programs);

                if (programs.size() == 0) {
                    wb.removeSheetAt(wb.getSheetIndex(sheet));

                }
                handler.handle(new Either.Right<>(true));
            }
        });
    }

    /**
     * Set labels of the tabs
     */
    @Override
    protected void setLabels() {
        int cellLabelColumn = 0;

        if (this.instruction.getJsonArray("operations").isEmpty()) {
            return;
        }
        operations = this.instruction.getJsonArray("operations");
        for (int i = 0; i < operations.size(); i++) {
            JsonObject operation = operations.getJsonObject(i);
            taby.add(operation.getInteger("id"));
            Row operationRow = sheet.createRow(this.operationsRowNumber);
            excel.insertLabel(operationRow, cellLabelColumn, operation.getString("label"));
            this.operationsRowNumber++;
        }
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = " With values as ( " +
                " SELECT SUM( " +
                "  CASE WHEN oce.price_proposal is not null " +
                " THEN oce.price_proposal *  oce.amount " +
                "  ELSE (oce.price * oce.amount) + ((oce.price*oce.amount)*oce.tax_amount)/100 END\n" +
                " ) as Total  , contract_type.code as code,oce.id_operation , program_action.id_program ,contract_type.name ,program.name" +
                "        FROM lystore.order_client_equipment oce" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id)" +
                "        WHERE instruction.id = ?" +
                "     AND structure_program_action.structure_type =  '" + this.type + "'" +
                " Group by  program.name,contract_type.code, contract_type.name , program_action.id, oce.id_operation " +
                " order by  program.name,id_program,code,oce.id_operation) " +
                " SELECT label.label , array_to_json(array_agg(values)) as actions " +
                " from " + Lystore.lystoreSchema + ".label_operation as label " +
                " INNER JOIN values  on (label.id = values.id_operation) " +
                " Group by label.label ";

        sqlHandler(handler);
    }

    @Override
    protected void setArray(JsonArray datas) {
        int cellLabelColumn = 0;
        int programRowNumber = 0;
        if (datas.isEmpty()) {
            return;
        }
        Row programRow = sheet.createRow(programRowNumber);
        Row typeRow = sheet.createRow(programRowNumber + 1);

        for (int i = 0; i < datas.size(); i++) {

            JsonObject operation = datas.getJsonObject(i);
            String actionsStrToArray = operation.getString(actionStr);

            Row operationRow = sheet.createRow(this.operationsRowNumber);
            excel.insertLabel(operationRow, cellLabelColumn, operation.getString("label"));

            this.operationsRowNumber++;

            JsonArray actions = new JsonArray(actionsStrToArray);
            if (actions.isEmpty()) continue;
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                excel.insertHeader(programRow, cellColumn, action.getString("name"));
                excel.insertHeader(typeRow, cellColumn, action.getString("code"));
                excel.insertCellTabFloat(cellColumn, programRowNumber + 2 + i, action.getFloat("total"));
                this.cellColumn++;
            }

        }
        excel.insertHeader(typeRow, cellColumn, ExcelHelper.totalLabel);
        excel.fillTab(xTab + 1, this.cellColumn, yTab + 2, this.operationsRowNumber);

        for (int i = 0; i < datas.size(); i++) {
            excel.setTotalY(yTab + 1, cellColumn - 1, programRowNumber + 2 + i, cellColumn);
        }


//        excel.insertHeader(sheet.getRow(programRowNumber), cellColumn, excel.totalLabel);
    }
}

