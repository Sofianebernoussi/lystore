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

public class ComptaTab extends TabHelper {
    private JsonArray datas;
    private String type;
    private int yProgramLabel = 0;
    private StructureService structureService;

    public ComptaTab(Workbook workbook, JsonObject instruction, String type) {
        super(workbook, instruction, "COMPTA du rapport  " + type);
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
            JsonObject operation = datas.getJsonObject(i);
            int currentY = yProgramLabel;
            yProgramLabel += 2;
            setTitle(currentY, operation);
            JsonArray actions = operation.getJsonArray("actionsJO");
            JsonObject idPassed = new JsonObject();
            initYProgramLabel = yProgramLabel;
            yProgramLabel += 2;
            String campaign = "", key = "", oldkey = "";
            Float oldTotal = 0.f;

//            //Insert datas
//
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                if (!action.getString("campaign").equals(campaign)) {
                    if (j != 0) {
                        setTotal(programLabel.size() + 4, initYProgramLabel);
                    }
                    campaign = action.getString("campaign");
                    yProgramLabel += 2;
                    setCampaign(campaign, yProgramLabel);
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
                key = action.getString("program") + " - " + action.getString("code");
                if (!programLabel.containsKey(key)) {
                    programLabel.put(key, programLabel.size());
                    excel.insertHeader(initYProgramLabel, 4 + programLabel.getInteger(key)
                            , action.getString("program"));
                    excel.insertHeader(initYProgramLabel + 1, 4 + programLabel.getInteger(key)
                            , action.getString("code"));
                }


                if (!checkIdPassed(idPassed, action.getString("id_structure"))) {
                    columnTotal = 4;
                    idPassed.put(action.getString("id_structure"), true);

                    try {
                        excel.insertLabel(yProgramLabel, 0, action.getString("zipCode").substring(0, 2));

                    } catch (NullPointerException e) {
                        excel.insertLabel(yProgramLabel, 0, action.getString("zipCode"));
                    }
                    excel.insertLabel(yProgramLabel, 1, action.getString("city"));
                    excel.insertLabel(yProgramLabel, 2, action.getString("nameEtab"));
                    excel.insertLabel(yProgramLabel, 3, action.getString("uai"));

                    oldTotal = 0.f;
                    oldkey = key;
                    oldTotal += action.getFloat("total");
                    excel.insertCellTabFloat(4 + programLabel.getInteger(key),
                            yProgramLabel, oldTotal);
                } else {
                    yProgramLabel--;
                    if (!oldkey.equals(key)) {
                        oldTotal = 0.f;
                    }
                    oldkey = key;
                    oldTotal += action.getFloat("total");
                    excel.insertCellTabFloat(4 + programLabel.getInteger(action.getString("program") + " - " + action.getString("code")), yProgramLabel
                            , oldTotal);
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

    private void setCampaign(String campaign, int y) {
        CellRangeAddress merge = new CellRangeAddress(y, y, 0, 6);
        sheet.addMergedRegion(merge);
        excel.setRegionHeader(merge, sheet);
        excel.insertYellowHeader(y, 0, campaign);
    }

    private void setTitle(int currentY, JsonObject operation) {
        CellRangeAddress merge = new CellRangeAddress(currentY, currentY, 0, 6);
        sheet.addMergedRegion(merge);
        excel.setRegionHeader(merge, sheet);
        excel.insertTitleHeader(0, currentY, "Lycées concernés par: " + operation.getString("label"));
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
        query = "       With values as  (             " +
                "     SELECT  orders.id ,orders.\"price TTC\",  " +
                "             ROUND((( SELECT CASE          " +
                "            WHEN orders.price_proposal IS NOT NULL THEN 0     " +
                "            WHEN orders.override_region IS NULL THEN 0 " +
                "            WHEN SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) IS NULL THEN 0         " +
                "            ELSE SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount)         " +
                "            END           " +
                "             FROM   " + Lystore.lystoreSchema + ".order_client_options oco  " +
                "              where oco.id_order_client_equipment = orders.id " +
                "             ) + orders.\"price TTC\" " +
                "              ) * orders.amount   ,2 ) " +
                "             as Total, contract.name as market, contract_type.code as code, campaign.name as campaign,   " +
                "             program.name as program,         CASE WHEN orders.id_order_client_equipment is not null  " +
                "             THEN  (select oce.name FROM " + Lystore.lystoreSchema + ".order_client_equipment oce    " +
                "              where oce.id = orders.id_order_client_equipment limit 1)     " +
                "             ELSE ''      " +
                "             END as old_name,     " +
                "             orders.id_structure,orders.id_operation as id_operation, label.label as operation ,     " +
                "             orders.equipment_key as key, orders.name as name_equipment, true as region,    " +
                "             program_action.id_program, orders.amount ,contract.id as market_id,       " +
                "             case when specific_structures.type is null      " +
                "             then '" + LYCEE + "'          " +
                "             ELSE specific_structures.type     " +
                "             END as cite_mixte     " +
                "             FROM (      " +
                "             (select ore.id,  ore.price as \"price TTC\",  ore.amount,  ore.creation_date,  ore.modification_date,  ore.name,  ore.summary, " +
                "             ore.description,  ore.image,    ore.status,  ore.id_contract,  ore.equipment_key,  ore.id_campaign,  ore.id_structure, " +
                "             ore.cause_status,  ore.number_validation,  ore.id_order,  ore.comment,  ore.rank as \"prio\", null as price_proposal,  " +
                "             ore.id_project,  ore.id_order_client_equipment, null as program, null as action,  ore.id_operation , " +
                "             null as override_region          from " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore )      " +
                "             union      " +
                "             (select oce.id," +
                "             CASE WHEN price_proposal is null then  price + (price*tax_amount/100)  else price_proposal end as \"price TTC\", " +
                "             amount, creation_date, null as modification_date, name,  " +
                "             summary, description, image,  status, id_contract, equipment_key, id_campaign, id_structure, cause_status, number_validation, " +
                "             id_order, comment, rank as \"prio\", price_proposal, id_project, null as id_order_client_equipment,  program, action,  " +
                "             id_operation, override_region           from " + Lystore.lystoreSchema + ".order_client_equipment  oce) " +
                "             ) as orders       " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id)               " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id)                  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)      " +
                "             INNER JOIN " + Lystore.lystoreSchema + ".campaign ON orders.id_campaign = campaign.id  " +
                "             LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id)         ";
        if (type.equals(CMR))
            query +=
                    "   AND (spa.structure_type = '" + CMR + "' AND specific_structures.type ='" + CMR + "')  ";
        else {
            query +=
                    "   AND ((spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "')  " +
                            "     OR                     (spa.structure_type = '" + LYCEE + "' AND specific_structures.type is null ))    ";
        }

        query +=
                "     INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id)    " +
                        "     INNER JOIN " + Lystore.lystoreSchema + ".program on program_action.id_program = program.id           " +
                        "     WHERE instruction.id = ?   ";


        if (type.equals(CMR))
            query += "  AND specific_structures.type =  '" + CMR + "'   ";
        else {
            query += "  AND specific_structures.type !=  '" + CMR + "'   " +
                    "  OR specific_structures.type is null   ";
        }
        query +=
                "             Group by program.name,code,specific_structures.type , orders.amount , orders.name, orders.equipment_key , " +
                        "             orders.id_operation,orders.id_structure  ,orders.id, contract.id ,label.label  ,program_action.id_program ,  " +
                        "             orders.id_order_client_equipment,orders.\"price TTC\",orders.price_proposal,orders.override_region ,campaign" +
                        "             order by  orders.id_operation,program,code ,orders.id_structure    )        " +
                        " SELECT values.operation as label , array_to_json(array_agg(values)) as actions   " +
                        " from values  " +
                        " Group by label ; ";


        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                datas = event.right().getValue();
                handler.handle(new Either.Right<>(datas));
            }

        }));
    }
}

