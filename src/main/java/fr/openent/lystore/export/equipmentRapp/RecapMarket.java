package fr.openent.lystore.export.equipmentRapp;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.Collections;

public class RecapMarket extends TabHelper {
    JsonArray operations;
    private JsonArray datas;
    JsonObject programMarket;
    private String type;

    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     * @param type
     */
    public RecapMarket(Workbook wb, JsonObject instruction, String type) {
        super(wb, instruction, "Récapitulatif par marché ");
        this.type = type;
        this.programMarket = new JsonObject();
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
                setArray(programs);

                //DElete tab if empty
                if (programs.size() == 0) {
                    wb.removeSheetAt(wb.getSheetIndex(sheet));

                }
                handler.handle(new Either.Right<>(true));
            }
        });
    }


    @Override
    protected void setArray(JsonArray datas) {

        setLabels();

        for (int i = 0; i < datas.size(); i++) {//operations

            JsonObject operation = datas.getJsonObject(i);
            String actionsStrToArray = operation.getString("actions");

            excel.insertLabel(operationsRowNumber, 0, operation.getString("label"));

            this.operationsRowNumber++;


            JsonArray actions = new JsonArray(actionsStrToArray);
            if (actions.isEmpty()) continue;
            for (int j = 0; j < actions.size(); j++) { // datas of the array
                JsonObject action = actions.getJsonObject(j);
                //get the key to insert the data
                String key = action.getString("market") + " - " + action.getString("program") + " - " + action.getString("code");
                if (programMarket.containsKey(key)) {
                    excel.insertCellTabFloat(1 + programMarket.getInteger(key), i + 9, action.getFloat("total"));
                }
            }
        }
        //Setting total
        excel.insertYellowLabel(operationsRowNumber, 0, ExcelHelper.totalLabel);
        excel.fillTab(1, programMarket.size() + 1, 9, 9 + datas.size()); // init all empty tab cells

        for (int i = 1; i < programMarket.size() + 1; i++) {
            excel.setTotalX(9, operationsRowNumber - 1, i, operationsRowNumber);
        }

        excel.insertYellowLabel(8, programMarket.size() + 1, ExcelHelper.totalLabel);

        for (int i = 0; i <= datas.size(); i++) {
            excel.setTotalY(1, programMarket.size(), 9 + i, programMarket.size() + 1);
        }

        excel.autoSize(arrayLength + 1); // size all the colmuns wich contains datas to display

    }

    @Override
    protected void setLabels() {


        ArrayList<String> programsActionList = new ArrayList<>();
        String previousProgram = "";
        String previousMarket = "";
        CellRangeAddress merge;
        int initProgramX = 0;
        int endProgramX = 0;
        int initMarketX = 0;
        int endMarketX = 0;
        for (int i = 0; i < datas.size(); i++) {

            JsonObject operation = datas.getJsonObject(i);


            String actionStr = operation.getString("actions");
            JsonArray actions = new JsonArray(actionStr);
            if (actions.isEmpty()) continue;
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                //preparing keys
                String programMarketStr = action.getString("market") + " - " + action.getString("program") + " - " + action.getString("code");
                if (!programsActionList.contains(programMarketStr))//inserting the key filter
                    programsActionList.add(programMarketStr);
            }
            // Sorted array
            Collections.sort(programsActionList);


        }
        for (int i = 0; i < programsActionList.size(); i++) {
            String progM = programsActionList.get(i);
            //getting program and code separated
            String segments[] = progM.split(" - ");

            //merge region if same program
            if (previousMarket.equals(segments[0])) { //if same market megred region instead
                endMarketX = 1 + programMarket.size();
                if (previousProgram.equals(segments[1])) { // if same program adding merged instead
                    endMarketX = 1 + programMarket.size();

                } else {

                    previousProgram = segments[1];
                    if (initProgramX < endProgramX) {
                        merge = new CellRangeAddress(operationsRowNumber - 2, operationsRowNumber - 2, initProgramX, endProgramX);
                        sheet.addMergedRegion(merge);
                        excel.setRegionHeader(merge, sheet);
                    }
                    initProgramX = 3 + programMarket.size();
                    excel.insertYellowLabel(operationsRowNumber - 2, 1 + programMarket.size(), segments[1]);
                }
            } else {
                previousMarket = segments[0];
                previousProgram = segments[1];
                if (initMarketX < endMarketX) {
                    merge = new CellRangeAddress(operationsRowNumber - 3, operationsRowNumber - 3, initMarketX, endMarketX);
                    sheet.addMergedRegion(merge);
                    excel.setRegionHeader(merge, sheet);
                }
                initMarketX = 1 + programMarket.size();
                excel.insertYellowLabel(operationsRowNumber - 3, 1 + programMarket.size(), segments[0]);
                excel.insertYellowLabel(operationsRowNumber - 2, 1 + programMarket.size(), segments[1]);
            }
            //always insert contract_type code
            excel.insertYellowLabel(operationsRowNumber - 1, 1 + programMarket.size(), segments[2]);

            programMarket.put(progM, i);
        }

        //merge last region if there is one
        if (initProgramX < endProgramX) {
            merge = new CellRangeAddress(operationsRowNumber - 2, operationsRowNumber - 2, initProgramX, endProgramX);
            sheet.addMergedRegion(merge);
            excel.setRegionHeader(merge, sheet);
        }
        if (initMarketX < endMarketX) {
            merge = new CellRangeAddress(operationsRowNumber - 3, operationsRowNumber - 3, initMarketX, endMarketX);
            sheet.addMergedRegion(merge);
            excel.setRegionHeader(merge, sheet);
        }

        arrayLength += programMarket.size();
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "WITH values AS ( " +
                " With tempValues as( " +
                "   (SELECT SUM(CASE WHEN oce.price_proposal is not null  " +
                "    THEN oce.price_proposal *  oce.amount  " +
                "    ELSE (oce.price * oce.amount) + ((oce.price*oce.amount)*oce.tax_amount)/100 + " +
                "    (            SELECT CASE WHEN  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)  IS NULL    " +
                "    THEN 0            " +
                "    ELSE  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)   " +
                "    END            from  " + Lystore.lystoreSchema + ".order_client_options oco          " +
                "    WHERE id_order_client_equipment = oce.id   ) END    ) as Total  , " +
                "  contract_type.code as code, program_action.id_program as id_program,program.name as program ,oce.id_operation , contract_type.name , " +
                "  contract.name as market " +
                "  FROM   " + Lystore.lystoreSchema + ".order_client_equipment oce    INNER JOIN   " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id)     " +
                "  INNER JOIN   " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)     " +
                "  INNER JOIN   " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id)   " +
                "  INNER JOIN   " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)   " +
                "  INNER JOIN   " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id) " +
                "  INNER JOIN   " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)     " +
                "  INNER JOIN " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id)  " +
                "  WHERE instruction.id = ?    AND oce.override_region = false ";
        if (type.equals(CMR))
            query += "    AND structure_program_action.structure_type =  '" + type + "'  " +
                    " AND oce.id_structure IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + type + "' ) ";

        else
            query += "    AND( structure_program_action.structure_type !=  '" + CMR + "'  " +
                    " OR oce.id_structure NOT IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + CMR + "' )) ";

        query += "  Group by  contract.name,contract_type.code, contract_type.name , program_action.id,program.name, oce.id_operation order by id_program,code,oce.id_operation)  " +
                " UNION " +
                " (SELECT SUM(ore.price *  ore.amount ) as Total  , contract_type.code as code, program_action.id_program as id_program ,program.name as program ,ore.id_operation , contract_type.name, " +
                "  contract.name as market " +
                "  FROM   " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore    " +
                "  INNER JOIN   " + Lystore.lystoreSchema + ".operation ON (ore.id_operation = operation.id)    " +
                "  INNER JOIN   " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)    " +
                "  INNER JOIN   " + Lystore.lystoreSchema + ".contract ON (ore.id_contract = contract.id)    " +
                "  INNER JOIN   " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)   " +
                "  INNER JOIN   " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)   " +
                "  INNER JOIN   " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)     " +
                "  INNER JOIN " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id)  " +
                "  WHERE instruction.id = ? ";
        if (type.equals(CMR))
            query += "    AND structure_program_action.structure_type =  '" + type + "'  " +
                    " AND ore.id_structure IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + type + "' ) ";

        else
            query += "    AND( structure_program_action.structure_type !=  '" + CMR + "'  " +
                    " OR ore.id_structure NOT IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + CMR + "' )) ";

        query +=
                "  Group by  contract.name,contract_type.code, contract_type.name , program_action.id,program.name, ore.id_operation  " +
                        "  order by id_program,code,ore.id_operation) " +
                        "           ) " +
                        " select * from tempValues  " +
                        " order by market,id_program,code,id_operation " +
                        ") " +
                        "SELECT  label.label, array_to_json(array_agg(values)) as actions  " +
                        " from " + Lystore.lystoreSchema + ".operation as operation    " +
                        " INNER JOIN " + Lystore.lystoreSchema + ".label_operation as label on (operation.id_label = label.id)  " +
                        " INNER JOIN values  on (operation.id = values.id_operation)  " +
                        " Group by label.label  ;";


        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")).add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                datas = event.right().getValue();
                handler.handle(new Either.Right<>(datas));
            }
        }));
    }
}
