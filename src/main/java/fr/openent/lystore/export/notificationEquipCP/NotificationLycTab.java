package fr.openent.lystore.export.notificationEquipCP;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.service.impl.DefaultStructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationLycTab extends NotifcationCpHelper {
    private int lineNumber = 0;
    private final String DESTINATION = "Destination";
    private final String MARKET_CODE = "Code marché Région";
    private final String REGION_LABEL = "Libellé Région";
    private final String DATE = "DATE N° RAPPORT";
    private final String NUMBER_ORDER = "N° de demande";
    private final String AMOUNT = "Qté";
    final String Subvention = "236";
    private final String SubventionLabel = "GESTION DIRECTE\n" +
            "Les matériels seront fournis au lycée par l'intermédiaire des marchés publics Région.";
    private final String NotSubventionLabel = "SUBVENTIONS\n" +
            "Ce document constitue une information et sert également de notification comptable.";

    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     */
    public NotificationLycTab(Workbook wb, JsonObject instruction) {
        super(wb, instruction, "NOTIFICATION POUR LES LYCEES");
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        getDatas(event -> {
            if (event.isLeft()) {
                log.error("Failed to retrieve datas");
                handler.handle(new Either.Left<>("Failed to retrieve datas"));
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
        StructureService structureService = new DefaultStructureService(Lystore.lystoreSchema);
        structureService.getStructureById(new JsonArray(structuresId), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> repStructures) {
                if (repStructures.isRight()) {
                    JsonArray structures = repStructures.right().getValue();
                    setStructures(structures);
                    writeArray(handler);
                }
            }
        });

        datas = sortByCity(datas);
    }

    private void writeArray(Handler<Either<String, Boolean>> handler) {
        for (int i = 0; i < datas.size(); i++) {
            lineNumber += 2;
            excel.insertLabel(lineNumber, 0, datas.getJsonObject(i).getString("city"));
            excel.insertLabel(lineNumber, 2, datas.getJsonObject(i).getString("nameEtab"));
            excel.insertLabel(lineNumber, 4, datas.getJsonObject(i).getString("uai"));
            lineNumber += 2;
            JsonObject structure = datas.getJsonObject(i);
            JsonArray orders = structure.getJsonArray("actionsJO");
            orders = sortByType(orders);
            String previousMarket = "";
            String previousCode = "";
            for (int j = 0; j < orders.size(); j++) {
                JsonObject order = orders.getJsonObject(j);
                String market = order.getString("market");
                String code = order.getString("code");

                if (code.equals(Subvention)) {
                    excel.insertLabel(lineNumber, 0, SubventionLabel);
                } else {
                    excel.insertLabel(lineNumber, 0, NotSubventionLabel);
                }
                lineNumber++;
                setLabels();

            }
        }

        excel.autoSize(8);
        handler.handle(new Either.Right<>(true));

    }


    private JsonArray sortByType(JsonArray orders) {
        JsonArray sortedJsonArray = new JsonArray();

        List<JsonObject> jsonValues = new ArrayList<JsonObject>();
        for (int i = 0; i < orders.size(); i++) {
            jsonValues.add(orders.getJsonObject(i));
        }

        Collections.sort(jsonValues, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "code";

            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                try {
                    if (a.containsKey(KEY_NAME)) {
                        valA = a.getString(KEY_NAME);
                    }
                    if (b.containsKey(KEY_NAME)) {
                        valB = b.getString(KEY_NAME);
                    }
                } catch (NullPointerException e) {
                    log.error("error when sorting orders NotificationLycTab during export");
                }

                if (valA.equals(Subvention) && !valB.equals(valA)) {
                    return -6000000;
                } else if (valB.equals(Subvention) && !valB.equals(valA)) {
                    return 6000000;
                } else {
                    return valA.compareTo(valB);
                }
            }
        });

        for (int i = 0; i < orders.size(); i++) {
            sortedJsonArray.add(jsonValues.get(i));
        }
        return sortedJsonArray;
    }

    @Override
    protected void setLabels() {
        excel.insertLabel(lineNumber, 0, DESTINATION);
        excel.insertLabel(lineNumber, 1, MARKET_CODE);
        excel.insertLabel(lineNumber, 2, REGION_LABEL);
        excel.insertLabel(lineNumber, 3, DATE);
        excel.insertLabel(lineNumber, 4, NUMBER_ORDER);
        excel.insertLabel(lineNumber, 5, AMOUNT);
        lineNumber++;
    }

    private void setStructures(JsonArray structures) {
        JsonObject program, structure;
        JsonArray actions;
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            actions = new JsonArray(data.getString("actions"));
            for (int j = 0; j < structures.size(); j++) {
                structure = structures.getJsonObject(j);
                if (data.getString("id_structure").equals(structure.getString("id"))) {
                    data.put("nameEtab", structure.getString("name"));
                    data.put("uai", structure.getString("uai"));
                    data.put("city", structure.getString("city"));
                    data.put("type", structure.getString("type"));
                    data.put("zipCode", structure.getString("zipCode"));
                }
            }
            data.put("actionsJO", actions);
        }
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
                "             orders.equipment_key as key, orders.name as name_equipment, true as region,  orders.id as id,  " +
                "             program_action.id_program, orders.amount ,contract.id as market_id,   campaign.name as campaign, orders.comment,    " +
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
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id)                  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id )      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".campaign ON orders.id_campaign = campaign.id  " +
                "             LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id)         " +
                "   AND ((spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "') " +
                "  OR (spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "') " +
                "     OR                     (spa.structure_type = '" + LYCEE + "' AND specific_structures.type is null ))    " +
                "     INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id)    " +
                "     INNER JOIN " + Lystore.lystoreSchema + ".program on program_action.id_program = program.id           " +


                "             Group by program.name,code,specific_structures.type , orders.amount , orders.name, orders.equipment_key , " +
                "             orders.id_operation,orders.id_structure  ,orders.id, contract.id ,label.label  ,program_action.id_program ,  " +
                "             orders.id_order_client_equipment,orders.\"price TTC\",orders.price_proposal,orders.override_region , orders.comment,campaign.name , orders.id" +
                "             order by code,campaign, id_structure,program,code  " +
                "  )    SELECT  values.id_structure as id_structure,    array_to_json(array_agg(values))as actions  " +
                "  from  values      " +
                "  Group by values.id_structure   " +
                "  Order by values.id_structure   ;";

        sqlHandler(handler);
    }
}
