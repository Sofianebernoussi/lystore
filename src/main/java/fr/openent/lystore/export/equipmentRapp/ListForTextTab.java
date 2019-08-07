package fr.openent.lystore.export.equipmentRapp;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.service.impl.DefaultStructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;

public class ListForTextTab extends TabHelper {
    private JsonArray datas;
    private String type;
    private int yProgramLabel = 1;
    private StructureService structureService;

    public ListForTextTab(Workbook workbook, JsonObject instruction, String type) {
        super(workbook, instruction, "liste pour texte du RAPPORT " + type);
        structureService = new DefaultStructureService(Lystore.lystoreSchema);
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

                initDatas(handler);
                //Delete tab if empty
                if (programs.size() == 0) {
                    wb.removeSheetAt(wb.getSheetIndex(sheet));
                }
            }
        });
    }

    private void initDatas(Handler<Either<String, Boolean>> handler) {
        ArrayList structuresId = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            JsonArray actions = new JsonArray(data.getString("actions"));
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                structuresId.add(structuresId.size(), action.getString("id_structure"));

            }
        }
        structureService.getStructureById(new JsonArray(structuresId), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> repStructures) {
                if (repStructures.isRight()) {
                    JsonArray structures = repStructures.right().getValue();
                    setStructures(structures);
                    setLabels();
                    handler.handle(new Either.Right<>(true));
                }
            }
        });
    }

    private void setStructures(JsonArray structures) {
        JsonObject program, structure;
        JsonArray actions;
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            actions = new JsonArray(data.getString("actions"));
            for (int k = 0; k < actions.size(); k++) {
                JsonObject action = actions.getJsonObject(k);
                for (int j = 0; j < structures.size(); j++) {
                    structure = structures.getJsonObject(j);
                    if (action.getString("id_structure").equals(structure.getString("id"))) {
                        action.put("nameEtab", structure.getString("name"));
                        action.put("uai", structure.getString("uai"));
                        action.put("city", structure.getString("city"));
                        action.put("zipCode", structure.getString("zipCode"));
                    }
                }
            }
            data.put("actionsJO", actions);
        }
    }


    @Override
    protected void setLabels() {
        int initYProgramLabel = 2;
        for (int i = 0; i < datas.size(); i++) {
            JsonObject programLabel = new JsonObject();
            //creating label
            int columnTotal = 4;
            JsonObject campaign = datas.getJsonObject(i);
            int currentY = yProgramLabel;
            yProgramLabel += 2;
            setTitle(currentY, campaign);
            JsonArray actions = campaign.getJsonArray("actionsJO");
            JsonObject idPassed = new JsonObject();
            initYProgramLabel = yProgramLabel;
            yProgramLabel += 2;
            String operation = "";
//            //Insert datas
//
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                if (!action.getString("operation").equals(operation)) {
                    if (j != 0) {
                        setTotal(programLabel.size() + 4, initYProgramLabel);
                    }
                    operation = action.getString("operation");
                    yProgramLabel += 2;
                    setOperation(operation, yProgramLabel);
                    yProgramLabel += 2;
                    initYProgramLabel = yProgramLabel;
                    yProgramLabel += 2;
                    if (arrayLength - 4 < columnTotal) {
                        arrayLength += columnTotal;
                    }
                    columnTotal = 4;
                    idPassed = new JsonObject();
                    programLabel = new JsonObject();
                }
                if (!programLabel.containsKey(action.getString("program") + " - " + action.getString("code"))) {
                    programLabel.put(action.getString("program") + " - " + action.getString("code"), programLabel.size());
                    excel.insertHeader(initYProgramLabel, 4 + programLabel.getInteger(action.getString("program") + " - " + action.getString("code"))
                            , action.getString("program"));
                    excel.insertHeader(initYProgramLabel + 1, 4 + programLabel.getInteger(action.getString("program") + " - " + action.getString("code"))
                            , action.getString("code"));
                }


                if (!checkIdPassed(idPassed, action.getString("id_structure"))) {
                    columnTotal = 4;
                    idPassed.put(action.getString("id_structure"), true);
                    excel.insertLabel(yProgramLabel, 0, action.getString("zipCode"));
                    excel.insertLabel(yProgramLabel, 1, action.getString("city"));
                    excel.insertLabel(yProgramLabel, 2, action.getString("nameEtab"));
                    excel.insertLabel(yProgramLabel, 3, action.getString("uai"));

                    excel.insertCellTabFloat(4 + programLabel.getInteger(action.getString("program") + " - " + action.getString("code")),
                            yProgramLabel, action.getFloat("total"));
                } else {
                    yProgramLabel--;
                    excel.insertCellTabFloat(4 + programLabel.getInteger(action.getString("program") + " - " + action.getString("code")), yProgramLabel
                            , action.getFloat("total"));
                }

                columnTotal++;

                yProgramLabel++;

            }
            if (arrayLength - 4 < columnTotal) {
                arrayLength += columnTotal;
            }
            setTotal(programLabel.size() + 4, initYProgramLabel);
            yProgramLabel += 2;
        }
        excel.autoSize(arrayLength);
    }

    private void setOperation(String operation, int y) {
        CellRangeAddress merge = new CellRangeAddress(y, y, 0, 6);
        sheet.addMergedRegion(merge);
        excel.setRegionHeader(merge, sheet);
        excel.insertTitleHeader(0, y, " Lycées concernés par: " + operation);
    }

    private void setTitle(int currentY, JsonObject campaign) {
        CellRangeAddress merge = new CellRangeAddress(currentY, currentY, 0, 6);
        sheet.addMergedRegion(merge);
        excel.setRegionUnderscoreHeader(merge, sheet);
        excel.insertUnderscoreHeader(0, currentY, campaign.getString("campaign"));
    }

    private void setTotal(int nbTotaux, int initYProgramLabel) {
        excel.fillTab(4, nbTotaux, initYProgramLabel + 2, yProgramLabel);
        excel.insertHeader(yProgramLabel, 3, excel.totalLabel);
        for (int nbTotal = 4; nbTotal < nbTotaux; nbTotal++) {
            excel.setTotalX(initYProgramLabel + 1, yProgramLabel - 1, nbTotal, yProgramLabel);
        }
        excel.insertHeader(initYProgramLabel + 1, nbTotaux, excel.totalLabel);
        for (int y = initYProgramLabel + 2; y <= yProgramLabel; y++) {
            excel.setTotalY(4, nbTotaux - 1, y, nbTotaux);
        }
    }

    private boolean checkIdPassed(JsonObject idPassed, String id) {
        return idPassed.containsKey(id);
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "    With values as (  " +
                "      With unionValues as ( " +
                "      (SELECT SUM( ore.price* ore.amount) as Total,  " +
                "      ore.id_structure,campaign.id as campaign_id,contract_type.code as code,ore.id_operation,label.label as operation , " +
                "      program_action.id_program ,contract_type.name ,program.name as program   " +
                "      FROM " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore     " +
                "      INNER JOIN " + Lystore.lystoreSchema + ".operation ON (ore.id_operation = operation.id)       " +
                "      INNER JOIN " + Lystore.lystoreSchema + ".campaign ON ore.id_campaign = campaign.id  " +
                "      INNER JOIN " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)   " +
                "      INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)          " +
                "      INNER JOIN " + Lystore.lystoreSchema + ".contract ON (ore.id_contract = contract.id)        " +
                "      INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)    " +
                "      INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)     " +
                "      INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)       " +
                "      INNER JOIN " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id)      " +
                "       WHERE instruction.id = ?  ";

        if (type.equals(CMR))
            query += "    AND structure_program_action.structure_type =  '" + type + "'  " +
                    " AND ore.id_structure IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + type + "' ) ";

        else
            query += "    AND structure_program_action.structure_type !=  '" + CMR + "'  " +
                    " OR ore.id_structure NOT IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + CMR + "' ) ";
        query +=
                "      Group by  ore.id_operation,label.label ,program.name,contract_type.code, contract_type.name , program_action.id, ore.id_operation,campaign_id,ore.id_structure   " +
                "      order by  ore.id_operation,label.label ,program.name,id_program,code,ore.id_operation  )    " +
                "      UNION (  SELECT SUM(  CASE WHEN oce.price_proposal is not null      " +
                "            THEN oce.price_proposal *  oce.amount   " +
                "            ELSE (oce.price * oce.amount) + ((oce.price*oce.amount)*oce.tax_amount +   " +
                "                    (SELECT CASE WHEN  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)  IS NULL   " +
                "                    THEN 0             ELSE  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)      " +
                "                    END            from " + Lystore.lystoreSchema + ".order_client_options oco             WHERE id_order_client_equipment = oce.id )    " +
                "                    )/100 END      " +
                "            ) as Total, " +
                "      oce.id_structure,campaign.id as campaign_id, contract_type.code as code,oce.id_operation,label.label as operation, program_action.id_program ,contract_type.name  " +
                "       ,program.name as program " +
                "      FROM " + Lystore.lystoreSchema + ".order_client_equipment oce      " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id)    " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".campaign ON oce.id_campaign = campaign.id    " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)    " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)       " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id)     " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)  " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)      " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)  " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id)       " +
                        "       WHERE instruction.id = ?  AND oce.override_region = false";
        if (type.equals(CMR))
            query += "    AND structure_program_action.structure_type =  '" + type + "'  " +
                    " AND oce.id_structure IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + type + "' ) ";

        else
            query += "    AND( structure_program_action.structure_type !=  '" + CMR + "'  " +
                    " OR oce.id_structure NOT IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + CMR + "') ) ";
        query +=
                "       Group by  oce.id_operation,label.label,program.name,contract_type.code, contract_type.name , program_action.id, campaign_id,oce.id_structure      " +
                "       order by  oce.id_operation,label.label,program.name,id_program,code )   " +
                "  )  SELECT * from unionValues " +
                " order by  operation,program,id_program,code,id_operation " +
                "      ) " +
                "SELECT campaign.name as campaign , array_to_json(array_agg(values))as actions   " +
                "from " + Lystore.lystoreSchema + ".campaign as campaign " +
                "INNER JOIN values  on (campaign.id = values.campaign_id)  " +
                "Group by campaign.name " +
                        "  ; ";


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

