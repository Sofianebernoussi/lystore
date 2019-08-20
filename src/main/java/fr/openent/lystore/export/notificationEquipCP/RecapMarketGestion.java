package fr.openent.lystore.export.notificationEquipCP;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.service.impl.DefaultStructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class RecapMarketGestion extends NotifcationCpHelper {
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
            log.error("Incorrect date format");
        }

        for (int i = 0; i < datas.size(); i++) {
            lineNumber++;

            JsonObject market = datas.getJsonObject(i);
            setLabel(market.getString("market"));
            JsonArray orders = market.getJsonArray("actionsJO");


            orders = sortByCity(orders);
            String previousCampaign = "";
            String previousZip = orders.getJsonObject(0).getString("zipCode").substring(0, 2);//check if orders > 0
            String previousCode = "";
            String zip = "";
            int startLine = lineNumber;
            for (int j = 0; j < orders.size(); j++) {
                String address;
                JsonObject order = orders.getJsonObject(j);
                try {
                    address = order.getString("address").replace(",", ",\n");
                } catch (NullPointerException e) {
                    address = order.getString("address");
                }

                zip = order.getString("zipCode").substring(0, 2);
                String code = order.getString("code");
                String campaign = order.getString("campaign");


                if (!previousCampaign.equals(campaign)) {
                    if (j != 0) {
                        excel.insertHeader(lineNumber, 0, previousZip);
                        previousZip = zip;
                        excel.setTotalX(startLine, lineNumber - 1, 8, lineNumber, 1);
                        lineNumber++;
                        startLine = lineNumber;

                    }
                    lineNumber++;
                    excel.insertUnderscoreHeader(0, lineNumber, campaign);
                    mergeCurrentLine(true);
                    previousCampaign = campaign;
                    previousCode = "";

                    lineNumber += 2;
                }

                if (!previousCode.equals(code)) {
                    lineNumber++;
                    previousCode = code;
                    excel.insertHeader(lineNumber, 0, code);
                    mergeCurrentLine(false);
                    lineNumber++;
                    insertHeaders();
                    startLine = lineNumber;

                }

                if (!previousZip.equals(zip)) {
                    excel.insertHeader(lineNumber, 0, previousZip);
                    previousZip = zip;
                    excel.setTotalX(startLine, lineNumber - 1, 8, lineNumber, 1);
                    lineNumber++;
                    startLine = lineNumber;


                }

                excel.insertCellTabCenter(0, lineNumber, zip);
                excel.insertCellTabCenter(1, lineNumber, order.getString("uai") + "\n" + order.getString("nameEtab") + "\n" + order.getString("city"));

                excel.insertCellTabCenter(2, lineNumber, CIVILITY + "\n" + address);
                excel.insertCellTabCenter(3, lineNumber, formatterDateExcel.format(orderDate));
                excel.insertCellTabCenter(4, lineNumber, order.getString("market") + " \nCP " + instruction.getString("cp_number"));
                excel.insertCellTabCenter(5, lineNumber,
                        "OPE : " + order.getString("operation") + "\nDDE : -" + order.getInteger("id").toString());
                excel.insertCellTabCenter(6, lineNumber, formatStrToCell(campaign));
                excel.insertCellTabCenter(7, lineNumber, formatStrToCell(order.getInteger("amount").toString()));
                excel.insertCellTabFloat(8, lineNumber, order.getFloat("total"));
                excel.insertCellTabCenter(9, lineNumber, formatStrToCell(order.getString("name_equipment")));
                excel.insertCellTabCenter(10, lineNumber, order.getString("cite_mixte"));
                excel.insertCellTabCenter(11, lineNumber, formatStrToCell(order.getString("market")));
                excel.insertCellTabCenter(12, lineNumber, formatStrToCell(order.getString("comment")));
                lineNumber++;
            }
            excel.insertHeader(lineNumber, 0, zip);
            excel.setTotalX(startLine, lineNumber - 1, 8, lineNumber, 1);
            lineNumber++;

        }
        excel.autoSize(13);
        handler.handle(new Either.Right<>(true));

    }


    private void mergeCurrentLine(boolean underscore) {
        if (underscore) {
            CellRangeAddress merge = new CellRangeAddress(lineNumber, lineNumber, 0, 1);
            sheet.addMergedRegion(merge);
            excel.setRegionUnderscoreHeader(merge, sheet);
        } else {
            CellRangeAddress merge = new CellRangeAddress(lineNumber, lineNumber, 0, 1);
            sheet.addMergedRegion(merge);
            excel.setRegionHeader(merge, sheet);
        }

    }

    // doing \n when the str is too long
    private String formatStrToCell(String str) {
        try {
            String[] words = str.split(" ");
            String resultStr = "";
            if (words.length <= 5) {
                return str;
            } else {
                for (int i = 0; i < words.length; i++) {
                    resultStr += words[i] + " ";
                    if (i % 5 == 0 && i != 0) {
                        resultStr += "\n";
                    }
                }
            }
            return resultStr;
        } catch (NullPointerException e) {
            return str;
        }
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
        excel.insertBlackOnGreenHeader(lineNumber, 0, market);
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
                "             order by campaign,code,market_id, id_structure,program,code  " +
                "  )    SELECT  values.market as market,    array_to_json(array_agg(values))as actions, SUM (values.total) as totalMarket       " +
                "  from  values      " +
                "  Group by values.market   " +
                "  Order by values.market   ;";

        sqlHandler(handler);
    }
}
