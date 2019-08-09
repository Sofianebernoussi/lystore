package fr.openent.lystore.export.equipmentRapp;

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
            String labelOperation = operation.getString("label");

            Row operationRow = sheet.createRow(this.operationsRowNumber);
            excel.insertLabel(operationRow, cellLabelColumn, operation.getLong("id").toString());
            excel.insertLabel(operationRow, cellLabelColumn + 1, labelOperation);


            JsonArray actions = new JsonArray(actionsStrToArray);
            if (actions.isEmpty()) continue;
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                String program = action.getString("program");
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
            operationsRowNumber++;


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
                String key = action.getString("program") + " - " + action.getString("code");
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
        query = "     With values as " +
                " (        " +
                "              SELECT  orders.id , SUM( orders.\"price TTC\"* orders.amount) as Total, contract.name as market, contract_type.code as code, " +
                "          program.name as program, " +
                "        CASE WHEN orders.id_order_client_equipment is not null  " +
                "        THEN  (select oce.name FROM lystore.order_client_equipment oce " +
                "        where oce.id = orders.id_order_client_equipment limit 1) " +
                "        ELSE '' " +
                "        END as old_name, " +
                "        orders.id_structure,orders.id_operation as id_operation, label.label as operation ,     " +
                "        orders.equipment_key as key, orders.name as name_equipment, true as region,   " +
                "        program_action.id_program, orders.amount ,contract.id as market_id, " +
                "        case when specific_structures.type is null " +
                "        then 'LYC' " +
                "        ELSE specific_structures.type  " +
                "        END as cite_mixte " +
                "        FROM ( " +
                "        select ore.id, ore.price as \"price TTC\",  ore.amount,  ore.creation_date,  ore.modification_date,  ore.name,  ore.summary,  ore.description,  ore.image,    ore.status,  ore.id_contract,  ore.equipment_key,  ore.id_campaign,  ore.id_structure,  ore.cause_status,  ore.number_validation,  ore.id_order,  ore.comment,  ore.rank as \"prio\", null as price_proposal,  ore.id_project,  ore.id_order_client_equipment, null as program, null as action,  ore.id_operation , null as override_region  " +
                "        from lystore.\"order-region-equipment\" ore " +
                "        union  " +
                "        select id, price + price*tax_amount as \"price TTC\", amount, creation_date, null as modification_date, name, summary, description, image,  status, id_contract, equipment_key, id_campaign, id_structure, cause_status, number_validation, id_order, comment, rank as \"prio\", price_proposal, id_project, null as id_order_client_equipment,  program, action, id_operation, override_region " +
                " " +
                " " +
                "        from lystore.order_client_equipment  oce ) as orders " +
                "         " +
                "        INNER JOIN  lystore.operation ON (orders.id_operation = operation.id)          " +
                "        INNER JOIN  lystore.label_operation as label ON (operation.id_label = label.id)            " +
                "        INNER JOIN  lystore.instruction ON (operation.id_instruction = instruction.id)             " +
                "        INNER JOIN  lystore.contract ON (orders.id_contract = contract.id)               " +
                "        INNER JOIN  lystore.contract_type ON (contract.id_contract_type = contract_type.id)     " +
                "        LEFT JOIN lystore.specific_structures ON orders.id_structure = specific_structures.id " +
                "        INNER JOIN  lystore.structure_program_action spa ON (spa.contract_type_id = contract_type.id)  " +
                "                       AND ((spa.structure_type = 'CMD' AND specific_structures.type ='CMD') OR " +
                "                    (spa.structure_type = 'LYC' AND specific_structures.type is null ))     " +
                "        INNER JOIN  lystore.program_action ON (spa.program_action_id = program_action.id)  " +
                "             INNER JOIN lystore.program on program_action.id_program = program.id " +
                " " +
                " " +
                "         " +
                "        WHERE instruction.id = ?       " +
                "        AND specific_structures.type !=  'CMR'   OR specific_structures.type is null " +
                "         " +
                "        Group by program.name,code,specific_structures.type , orders.amount , orders.name, orders.equipment_key , orders.id_operation,orders.id_structure  ,orders.id, contract.id ,label.label  ,program_action.id_program , orders.id_order_client_equipment   " +
                "        order by  orders.id_operation  " +
                "           )  " +
                "  SELECT values.id_operation as id, values.operation as label,  " +
                "  array_to_json(array_agg(values))as actions, SUM (values.total) as totalMarket     " +
                "  from  values " +
                "   " +
                "  Group by values.id_operation, values.operation " +
                "   Order by values.operation ;";
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

