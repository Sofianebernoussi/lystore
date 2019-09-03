package fr.openent.lystore.export.notificationEquipCP;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.service.impl.DefaultStructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;
import java.util.Collections;


public class LinesBudget extends TabHelper {
    private StructureService structureService;
    private ArrayList<Integer> codes = new ArrayList<>();
    private int arraylength = 5;
    private int lineNumber = 1;
    public LinesBudget(Workbook workbook, JsonObject instruction) {
        super(workbook, instruction, "Lignes Budgetaires");
        structureService = new DefaultStructureService(Lystore.lystoreSchema);
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        excel.setCPNumber(instruction.getString("cp_number"), 1, 1);
        getDatas(event -> handleDatasDefault(event, handler));
    }

    @Override
    protected void initDatas(Handler<Either<String, Boolean>> handler) {
        ArrayList structuresId = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            JsonArray actions = new JsonArray(data.getString("actions"));
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                if(!structuresId.contains(action.getString("id_structure")))
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
                    setArray(datas);
                    handler.handle(new Either.Right<>(true));
                } else {
                    handler.handle(new Either.Left<>("Error when casting neo"));

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
                        action.put("nameEtab", NULL_DATA);
                        action.put("uai",NULL_DATA);
                        action.put("city", NULL_DATA);
                        action.put("type", NULL_DATA);
                        action.put("zipCode","??");

                    if (action.getString("id_structure").equals(structure.getString("id"))) {
                        action.put("nameEtab", structure.getString("name"));
                        action.put("uai", structure.getString("uai"));
                        action.put("city", structure.getString("city"));
                        action.put("type", structure.getString("type"));
                        action.put("zipCode", structure.getString("zipCode"));
                    }
                }
            }
            data.put("actionsJO", actions);
        }
    }


    @Override
    protected void setArray(JsonArray datas) {
        int initLineNumber;

        for (int i = 0; i < datas.size(); i++) {
            Float totalToInsert = 0.f;
            String previousStructure = "";
            JsonObject operationData = datas.getJsonObject(i);
            JsonArray orders = operationData.getJsonArray("actionsJO");
            String labelOperation = operationData.getString("label");
            boolean operationAdded = false;
            initLineNumber = lineNumber;
            int nbTotaux = 0;
            String previousCode = "";


            for (int j = 0; j < orders.size(); j++) {

                JsonObject order = orders.getJsonObject(j);
                String currentStructure = order.getString("id_structure");
                String code = order.getString("code");
                if (!previousStructure.equals(currentStructure)) {
                    nbTotaux++;
                    lineNumber++;
                    if (!operationAdded) {
                        excel.insertWhiteOnBlueTab(lineNumber, 1, labelOperation);
                        operationAdded = true;
                    }
                    totalToInsert = 0.f;
                    previousCode = "";
                    previousStructure = currentStructure;
                    excel.insertWhiteOnBlueTab(lineNumber, 2, order.getString("uai"));
                    excel.insertWhiteOnBlueTab(lineNumber, 3, order.getString("type"));
                    excel.insertWhiteOnBlueTab(lineNumber, 4, order.getString("nameEtab"));

                }
                if (!previousCode.equals(code)) {
                    previousCode = code;
                    totalToInsert = 0.f;
                }
                totalToInsert += safeGetFloat(order,"total", "LinesBudget");
                excel.insertFloatYellow(lineNumber,
                        5 + codes.indexOf(Integer.parseInt(code)), totalToInsert);
            }
            //insert Total
            excel.fillTabWithStyle(1, 4, initLineNumber + 1, lineNumber + 1, excel.whiteOnBlueLabel);
            excel.fillTabWithStyle(5, arraylength, initLineNumber + 1, lineNumber + 1, excel.floatOnYellowStyle);

            lineNumber++;
            excel.insertHeader(lineNumber, 1, excel.totalLabel + " : " + labelOperation);
            for (int nbTotal = 0; nbTotal < codes.size(); nbTotal++) {
                excel.setTotalX(initLineNumber + 1, lineNumber - 1, 5 + nbTotal, lineNumber);
            }
            for (int nbTotal = 0; nbTotal <= nbTotaux; nbTotal++) {
                excel.setTotalY(5, arraylength - 1, initLineNumber + 1 + nbTotal, arraylength);
            }
            lineNumber++;

        }
        excel.autoSize(arrayLength + 1);
    }

    protected void setLabels() {
        for (int i = 0; i < datas.size(); i++) {
            JsonObject operationData = datas.getJsonObject(i);
            JsonArray orders = operationData.getJsonArray("actionsJO");

            for (int j = 0; j < orders.size(); j++) {
                JsonObject order = orders.getJsonObject(j);
                Integer code = Integer.parseInt(order.getString("code"));
                if (!codes.contains(code)) {
                    codes.add(code);
                }
            }
        }
        Collections.sort(codes);
        for (int i = 0; i < codes.size(); i++) {
            excel.insertLabel(1, 5 + i, codes.get(i).toString());
        }
        arraylength += codes.size();
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
                "             as Total, contract.name as market, contract_type.code as code,    " +
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
                "             INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id   and (orders.override_region != true OR orders.override_region is NULL))               " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id  AND instruction.id = ?)    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id )                  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)      " +
                "             LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id)         ";
        query +=
                "   AND ((spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "') " +
                        "  OR (spa.structure_type = '" + CMR + "' AND specific_structures.type ='" + CMR + "') " +
                        "     OR                     (spa.structure_type = '" + LYCEE + "' AND specific_structures.type is null ))    ";
        query +=
                "     INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id)    " +
                        "     INNER JOIN " + Lystore.lystoreSchema + ".program on program_action.id_program = program.id           ";


        query +=
                "             Group by program.name,code,specific_structures.type , orders.amount , orders.name, orders.equipment_key , " +
                        "             orders.id_operation,orders.id_structure  ,orders.id, contract.id ,label.label  ,program_action.id_program ,  " +
                        "             orders.id_order_client_equipment,orders.\"price TTC\",orders.price_proposal,orders.override_region " +
                        "             order by id_operation, id_structure,program,code   " +
                        "  )    SELECT values.id_operation as id, values.operation as label,    array_to_json(array_agg(values))as actions, SUM (values.total) as totalMarket       " +
                        "  from  values      " +
                        "  Group by values.id_operation, values.operation   " +
                        "  Order by values.operation ;";


        sqlHandler(handler);
    }
}
