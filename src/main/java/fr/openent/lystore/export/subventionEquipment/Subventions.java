package fr.openent.lystore.export.subventionEquipment;

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

public class Subventions extends TabHelper {
    private StructureService structureService;
    private ArrayList<Integer> codes = new ArrayList<>();
    private int arraylength = 5;
    private int lineNumber = 0;
    private final String ORDER_LABEL = "Libellé demande";
    private final String ORDER_COMMENT = "Commentaire demande";
    private final String AMOUNT = "Quantité accordé";
    private final String TOTAL = "Somme Montant Accordé";
    private final String ANNEXE_TEXT = "ANNEXE au rapport";
    private final String TOTAL_TITLE = "Dotation sur marchés régionaux au titre du présent rapport";
    private boolean isCMR;
    Float totalSubv = 0.f;


    public Subventions(Workbook workbook, JsonObject instruction, boolean isCMR) {
        super(workbook, instruction, (isCMR) ? "ANN. 1RAPPORT CMR Subventions" : "ANN. 1 RAPPORT PUB. subventions");
        structureService = new DefaultStructureService(Lystore.lystoreSchema);
        this.isCMR = isCMR;
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
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
        StructureService structureService = new DefaultStructureService(Lystore.lystoreSchema);
        structureService.getStructureById(new JsonArray(structuresId), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> repStructures) {
                if (repStructures.isRight()) {
                    JsonArray structures = repStructures.right().getValue();
                    setStructures(structures);
                    if (datas.isEmpty()) {
                        handler.handle(new Either.Left<>("No data in database"));
                    } else {
                        setTitle();
                        writeArray(handler);
                    }
                } else {
                    handler.handle(new Either.Left<>("Error when casting neo"));

                }
            }


        });

        datas = sortByCity(datas);
    }

    private void setTitle() {
        excel.insertBlackTitleHeaderBorderlessCenter(0, lineNumber, ANNEXE_TEXT);
        sizeMergeRegionWithStyle(lineNumber, 0, 2, excel.blackTitleHeaderBorderlessCenteredStyle);
        lineNumber++;
        excel.insertBlackTitleHeaderBorderlessCenter(0, lineNumber, TOTAL_TITLE);
        sizeMergeRegionWithStyle(lineNumber, 0, 2, excel.blackTitleHeaderBorderlessCenteredStyle);
        lineNumber++;
        excel.insertBlueTitleHeaderBorderlessCenterFloatCurrency(0, lineNumber, totalSubv);
        sizeMergeRegionWithStyle(lineNumber, 0, 2, excel.blackTitleHeaderBorderlessCenteredStyle);
        lineNumber += 2;

    }

    private void writeArray(Handler<Either<String, Boolean>> handler) {
        for (int i = 0; i < datas.size(); i++) {
            JsonObject structureDatas = datas.getJsonObject(i);
            JsonArray orders = structureDatas.getJsonArray("ordersJO");
            String zip = structureDatas.getString("zipCode").substring(0, 2);

            String structString = zip + " - " +
                    structureDatas.getString("city") + " - " + structureDatas.getString("nameEtab") + "(" + structureDatas.getString("uai") + ")";
            excel.insertHeader(lineNumber, 0, structString);
            sizeMergeRegion(lineNumber, 0, 3);
            lineNumber++;

            setLabels();
            for (int j = 0; j < orders.size(); j++) {
                JsonObject order = orders.getJsonObject(j);
                excel.insertCellTab(0, lineNumber, order.getString("name_equipment"));
                excel.insertCellTab(1, lineNumber, order.getString("comment"));
                excel.insertCellTabCenter(2, lineNumber, order.getInteger("amount").toString());
                excel.insertCellTabFloatWithPrice(3, lineNumber, order.getFloat("total"));
                lineNumber++;
            }

            excel.insertCellTabFloatWithPrice(3, lineNumber, Float.parseFloat(structureDatas.getString("totalprice")));
            lineNumber += 2;


        }

        excel.autoSize(4);
        handler.handle(new Either.Right<>(true));

    }


    @Override
    protected void setLabels() {
        excel.insertHeader(lineNumber, 0, ORDER_LABEL);
        excel.insertHeader(lineNumber, 1, ORDER_COMMENT);
        excel.insertHeader(lineNumber, 2, AMOUNT);
        excel.insertHeader(lineNumber, 3, TOTAL);
        lineNumber++;
    }


    private void setStructures(JsonArray structures) {
        JsonObject program, structure;
        JsonArray actions;
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            totalSubv += Float.parseFloat(data.getString("totalprice"));
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
            data.put("ordersJO", actions);

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
                "             program_action.id_program, orders.amount ,contract.id as market_id,   campaign.name as campaign, orders.comment, project.room, orders.isregion, " +
                "             project.stair,project.building,    " +
                "             case when specific_structures.type is null      " +
                "             then '" + LYCEE + "'          " +
                "             ELSE specific_structures.type     " +
                "             END as cite_mixte     " +
                "             FROM (      " +
                "             (select ore.id,  true as isregion, ore.price as \"price TTC\",  ore.amount,  ore.creation_date,  ore.modification_date,  ore.name,  ore.summary, " +
                "             ore.description,  ore.image,    ore.status,  ore.id_contract,  ore.equipment_key,  ore.id_campaign,  ore.id_structure, " +
                "             ore.cause_status,  ore.number_validation,  ore.id_order,  ore.comment,  ore.rank as \"prio\", null as price_proposal,  " +
                "             ore.id_project,  ore.id_order_client_equipment, null as program, null as action,  ore.id_operation ," +
                "             null as override_region          from " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore )      " +
                "             UNION      " +
                "             (select oce.id ,  false as isregion," +
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
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id" +
                " AND   contract_type.code = '236')      " + // a modifier pour non subventions
                "             INNER JOIN  " + Lystore.lystoreSchema + ".campaign ON orders.id_campaign = campaign.id  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".project ON orders.id_project = project.id  " +
                "             LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id)         ";
        if (isCMR)
            query += "   AND (spa.structure_type = '" + CMR + "' AND specific_structures.type ='" + CMR + "')  ";
        else
            query += "   AND ((spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "') " +
                    "     OR                     (spa.structure_type = '" + LYCEE + "' AND specific_structures.type is null ))    ";
        query += "     INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id)    " +
                "     INNER JOIN " + Lystore.lystoreSchema + ".program on program_action.id_program = program.id           " +
                "             Group by program.name,code,specific_structures.type , orders.amount , orders.name, orders.equipment_key , " +
                "             orders.id_operation,orders.id_structure  ,orders.id, contract.id ,label.label  ,program_action.id_program ,  " +
                "             orders.id_order_client_equipment,orders.\"price TTC\",orders.price_proposal,orders.override_region , orders.comment,campaign.name , orders.id," +
                "               orders.isregion, " +
                "              project.room,project.stair, project.building " +
                "             order by campaign,code,market_id, id_structure,program,code " +
                "  )    SELECT  values.id_structure as id_structure,    array_to_json(array_agg(values))as actions ,SUM(values.total) as totalPrice " +
                "  from  values      " +
                "  Group by values.id_structure   " +
                "  Order by values.id_structure   ;";


        sqlHandler(handler);

    }
}
