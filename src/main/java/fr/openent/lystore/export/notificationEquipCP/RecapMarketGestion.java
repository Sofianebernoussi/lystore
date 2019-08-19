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
import org.apache.poi.ss.util.CellRangeAddress;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class RecapMarketGestion extends TabHelper {
    private static final String DPT = "DPT";
    private static final String RNE_lYC = "RNE + NOM DU LYCEE ET COMMUNE";
    private static final String ADDR = "ADRESSE DU LYCEE";
    private static final String CPDATE = "DATE DE CP";
    private static final String MARKET_CP = "NOM DU MARCHE ET N° DE CP";
    private static final String OP_DDE_NUMBER = "N°DE L'OPERATION + N° DE LA DDE ";
    private static final String CAMPAIGN = "CAMPAGNE";
    private static final String AMOUNT = "QUANTITE";
    private static final String TOTAL_PRICE = "PRIX TOTAL TTC";
    private static final String EQUIPMENT_NAME = "NOM DE L'EQUIPEMENT";
    private static final String TYPE = "TYPE";
    private static final String MARKET_NUMBER = "N° DE MARCHE";
    private static final String REGION_COMMENT = "COMMENTAIRE DE LA DEMANDE REGION";
    private static final String CIVILITY = "M. Mme Le Proviseur-e";
    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     */

    private int lineNumber = 0;

    public RecapMarketGestion(Workbook wb, JsonObject instruction) {
        super(wb, instruction, "RECAP MARCHES GESTIONNAIRE");
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
//                    setLabels();
//                    setArray(datas);
                }
            }
        });
    }

    private void writeArray(Handler<Either<String, Boolean>> handler) {
        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat formatterDateExcel = new SimpleDateFormat("dd/MM/yyyy");
        Date orderDate = null;
        try {
            orderDate = formatterDate.parse(instruction.getString("date_cp"));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < datas.size(); i++) {
            lineNumber++;

            JsonObject market = datas.getJsonObject(i);
            setLabel(market.getString("market"));
            JsonArray orders = market.getJsonArray("actionsJO");
            insertHeaders();
            for (int j = 0; j < orders.size(); j++) {
                JsonObject order = orders.getJsonObject(j);

                excel.insertLabel(lineNumber, 0, order.getString("zipCode").substring(0, 2));
                excel.insertLabel(lineNumber, 1, order.getString("uai") + "\n" + order.getString("nameEtab") + "\n" + order.getString("city"));

                excel.insertLabel(lineNumber, 2, CIVILITY + "\n" + order.getString("address"));
                excel.insertLabel(lineNumber, 3, formatterDateExcel.format(orderDate));
                excel.insertLabel(lineNumber, 4, order.getString("market") + " \nCP " + instruction.getString("cp_number"));
                excel.insertLabel(lineNumber, 5,
                        "OPE : " + order.getString("operation") + "\n DDE : " + order.getInteger("id").toString());
                excel.insertLabel(lineNumber, 6, order.getString("campaign"));
                excel.insertLabel(lineNumber, 7, order.getInteger("amount").toString());
                excel.insertLabel(lineNumber, 8, order.getDouble("total").toString());
                excel.insertLabel(lineNumber, 9, order.getString("name_equipment"));
                excel.insertLabel(lineNumber, 10, order.getString("cite_mixte"));
                excel.insertLabel(lineNumber, 11, order.getString("market"));
                excel.insertLabel(lineNumber, 12, order.getString("comment"));
                lineNumber++;
            }

        }
        excel.autoSize(13);
        handler.handle(new Either.Right<>(true));

    }

    private void insertHeaders() {


        excel.insertHeader(lineNumber, 0, DPT);
        excel.insertHeader(lineNumber, 1, RNE_lYC);

        excel.insertHeader(lineNumber, 2, ADDR);
        excel.insertHeader(lineNumber, 3, CPDATE);
        excel.insertHeader(lineNumber, 4, MARKET_CP);
        excel.insertHeader(lineNumber, 5, OP_DDE_NUMBER);
        excel.insertHeader(lineNumber, 6, CAMPAIGN);
        excel.insertHeader(lineNumber, 7, AMOUNT);
        excel.insertHeader(lineNumber, 8, TOTAL_PRICE);
        excel.insertHeader(lineNumber, 9, EQUIPMENT_NAME);
        excel.insertHeader(lineNumber, 10, TYPE);
        excel.insertHeader(lineNumber, 11, MARKET_NUMBER);
        excel.insertHeader(lineNumber, 12, REGION_COMMENT);
        lineNumber++;
    }

    private void setLabel(String market) {
        excel.insertLabel(lineNumber, 0, market);
        CellRangeAddress merge = new CellRangeAddress(lineNumber, lineNumber, 0, 12);
        sheet.addMergedRegion(merge);
        excel.setRegionHeader(merge, sheet);
        lineNumber += 2;

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
                        action.put("address", structure.getString("address"));
                        action.put("zipCode", structure.getString("zipCode"));
                    }
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
                "             order by campaign,market_id, id_structure,program,code  " +
                "  )    SELECT  values.market as market,    array_to_json(array_agg(values))as actions, SUM (values.total) as totalMarket       " +
                "  from  values      " +
                "  Group by values.market   " +
                "  Order by values.market   ;";

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
