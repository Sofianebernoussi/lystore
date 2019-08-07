package fr.openent.lystore.export.equipmentRapp;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.Collections;

public class RecapTab extends TabHelper {
    private JsonArray programs;
    String type = "";
    final protected int yTab = 0;
    final protected int xTab = 0;
    protected int operationsRowNumber = 2;
    JsonArray operations;
    private String actionStr = "actions";
    private JsonObject programLabel = new JsonObject();


    public RecapTab(Workbook workbook, JsonObject instruction, String type) {
        super(workbook, instruction, "RECAP - " + type);
        this.type = type;
        excel.setDefaultFont();
    }


    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        getDatas(event -> {
            if (event.isLeft()) {
                log.error("Failed to retrieve programs");
                handler.handle(new Either.Left<>("Failed to retrieve programs"));
            } else {

                JsonArray programs = event.right().getValue();
                //Delete tab if empty
                setLabels();
                setArray(programs);

                if (programs.size() == 0) {
                    wb.removeSheetAt(wb.getSheetIndex(sheet));

                }
                handler.handle(new Either.Right<>(true));
            }
        });
    }

    //    /**
//     * Set labels of the tabs
//     */
    @Override
    protected void setLabels() {
        int cellLabelColumn = 0;
        int programRowNumber = 0;
        String previousProgram = "";
        int initProgramX = 0;
        int endProgramX = 0;
        cellColumn = 2;
        ArrayList<String> programsActionList = new ArrayList<>();
        if (programs.isEmpty()) {
            return;
        }
        for (int i = 0; i < programs.size(); i++) {

            JsonObject operation = programs.getJsonObject(i);
            String actionsStrToArray = operation.getString(actionStr);

            Row operationRow = sheet.createRow(this.operationsRowNumber);
            excel.insertLabel(operationRow, cellLabelColumn, operation.getLong("id").toString());
            excel.insertLabel(operationRow, cellLabelColumn + 1, operation.getString("label"));


            JsonArray actions = new JsonArray(actionsStrToArray);
            if (actions.isEmpty()) continue;
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                String program = action.getString("name");
                String code = action.getString("code");
                String key = program + " - " + code;
                if (!programsActionList.contains(key))
                    programsActionList.add(key);

            }
            Collections.sort(programsActionList);
            for (int j = 0; j < programsActionList.size(); j++) {
                String key = programsActionList.get(j);
                //getting program and code separated
                String segments[] = key.split(" - ");
                String program = segments[0];
                String code = segments[1];
                if (!programLabel.containsKey(key)) {
                    programLabel.put(key, programLabel.size());


                    if (previousProgram.equals(program)) {
                        endProgramX = cellColumn;
                    } else {
                        previousProgram = program;
                        if (initProgramX < endProgramX) {
                            CellRangeAddress merge = new CellRangeAddress(programRowNumber, programRowNumber, initProgramX, endProgramX);
                            sheet.addMergedRegion(merge);
                            excel.setRegionHeader(merge, sheet);
                        }
                        initProgramX = cellColumn;
                        excel.insertHeader(programRowNumber, cellColumn, program);
                    }
                    excel.insertHeader(programRowNumber + 1, cellColumn, code);
                    cellColumn++;
                }
            }

            this.operationsRowNumber++;
        }
        if (initProgramX < endProgramX) {
            CellRangeAddress merge = new CellRangeAddress(programRowNumber, programRowNumber, initProgramX, endProgramX);
            sheet.addMergedRegion(merge);
            excel.setRegionHeader(merge, sheet);
        }
    }

    @Override
    protected void setArray(JsonArray datas) {


        for (int i = 0; i < datas.size(); i++) {
            JsonObject operation = programs.getJsonObject(i);

            String actionsStrToArray = operation.getString(actionStr);
            JsonArray actions = new JsonArray(actionsStrToArray);

            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                String key = action.getString("name") + " - " + action.getString("code");
                excel.insertCellTabFloat(programLabel.getInteger(key) + 2,
                        2 + i,
                        action.getFloat("total"));
            }
        }

        excel.fillTab(2, programLabel.size() + 2, 2, operationsRowNumber);
        excel.insertHeader(operationsRowNumber, 1, excel.totalLabel);

        for (int i = 0; i < programLabel.size(); i++) {
            excel.setTotalX(2, operationsRowNumber - 1, i + 2, operationsRowNumber);
        }

        excel.insertHeader(1, programLabel.size() + 2, excel.totalLabel);

        for (int i = 0; i <= datas.size(); i++) {
            excel.setTotalY(2, programLabel.size() + 1, 2 + i, programLabel.size() + 2);
        }

        excel.autoSize(programLabel.size() + 3);
    }
    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = " With values as ( " +
                "   with temps as(" +
                "(SELECT SUM( " +
                "  CASE WHEN oce.price_proposal is not null " +
                " THEN oce.price_proposal *  oce.amount " +
                "  ELSE (oce.price * oce.amount) + ((oce.price*oce.amount)*oce.tax_amount)/100 + " +
                " ( " +
                "           SELECT CASE WHEN  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)  IS NULL " +
                "           THEN 0  " +
                "           ELSE  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)  " +
                "           END " +
                "           from lystore.order_client_options oco  " +
                "          WHERE id_order_client_equipment = oce.id  " +
                " )  " +
                " END " +
                " " +
                " ) as Total  , contract_type.code as code,oce.id_operation , program_action.id_program ,contract_type.name as market ,program.name" +
                "        FROM " + Lystore.lystoreSchema + ".order_client_equipment oce" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id)" +
                "        WHERE instruction.id = ?  AND oce.override_region = false"
        ;
        if (type.equals(CMR))
            query += "    AND structure_program_action.structure_type =  '" + type + "'  " +
                    " AND oce.id_structure IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + type + "' ) ";

        else
            query += "    AND (structure_program_action.structure_type !=  '" + CMR + "'  " +
                    " OR oce.id_structure NOT IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + CMR + "' )) ";
        query +=
                " Group by  program.name,contract_type.code, contract_type.name , program_action.id, oce.id_operation " +
                " order by  program.name,id_program,code,oce.id_operation)" +
                "UNION" +
                " (SELECT SUM( ore.amount *ore.price ) as Total  , contract_type.code as code,ore.id_operation , program_action.id_program ,contract_type.name as market ,program.name" +
                "        FROM " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".operation ON (ore.id_operation = operation.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".contract ON (ore.id_contract = contract.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)" +
                "        INNER JOIN " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id)" +
                        "        WHERE instruction.id = ?  ";
        if (type.equals(CMR))
            query += "    AND structure_program_action.structure_type =  '" + type + "'  " +
                    " AND ore.id_structure IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + type + "' ) ";

        else
            query += "    AND( structure_program_action.structure_type !=  '" + CMR + "'  " +
                    " OR ore.id_structure NOT IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + CMR + "' ) )";
        query +=
                " Group by  program.name,contract_type.code, contract_type.name , program_action.id, ore.id_operation " +
                " order by  program.name,id_program,code,ore.id_operation)" +
                " ) " +
                "   select temps.* from temps " +
                "   order by  temps.name,temps.code,temps.id_operation" +
                ") " +
                        " SELECT label.label,operation.id , array_to_json(array_agg(values)) as actions " +
                        " from " + Lystore.lystoreSchema + ".operation as operation    " +
                        " INNER JOIN " + Lystore.lystoreSchema + ".label_operation as label on (operation.id_label = label.id)  " +
                        " INNER JOIN values  on (operation.id = values.id_operation)  " +
                        " Group by operation.id, label.label ";
        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")).add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                programs = event.right().getValue();
                handler.handle(new Either.Right<>(programs));
            }

        }));
    }


}

