package fr.openent.lystore.export.iris;

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

public class IrisTab extends TabHelper {
    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     */

    private final String IDDOS = "iddos";//
    private final String LBDDOS = "lbdoss";//
    private final String OBJDDOS = "objdos";//
    private final String IDTIERS = "idtiers";//
    private final String NATURE = "nature";//
    private final String PROGRAMME = "programme";//
    private final String ACTION = "action";//
    private final String ENVILIG = "envilig";//
    private final String MTPROP = "mtprop";//
    private final String IDCOM = "idcom";//
    private final String IDLOCAL = "idlocal";//
    private final String IDCPER = "idcper";
    private final String IDCPRD = "idcprd";
    private final String QPVIDENT = "qpvident";
    private final String CPVIDENT = "cvident";
    private StructureService structureService;

    public IrisTab(Workbook wb, JsonObject instruction) {
        super(wb, instruction, "IRIS");
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        getDatas(event -> handleDatasDefault(event, handler));
        structureService= new DefaultStructureService(Lystore.lystoreSchema);
    }


    private void setStructures(JsonArray structures) {
        JsonObject program, structure;
        JsonArray actions;
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            actions = new JsonArray(data.getString("actions"));

                for (int j = 0; j < structures.size(); j++) {
                    structure = structures.getJsonObject(j);
                    data.put("nameEtab", NULL_DATA);
                    data.put("uai",NULL_DATA);
                    data.put("city", NULL_DATA);
                    data.put("type", NULL_DATA);
                    data.put("zipCode","??");

                    if (data.getString("id_structure").equals(structure.getString("id"))) {
                        data.put("nameEtab", structure.getString("name"));
                        data.put("uai", structure.getString("uai"));
                        data.put("city", structure.getString("city"));
                        data.put("type", structure.getString("type"));
                        data.put("zipCode", structure.getString("zipCode"));
                    }
                }
//            data.put("actionsJO", actions);
        }
    }

    @Override
    public void initDatas(Handler<Either<String, Boolean>> handler) {

        ArrayList structuresId = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
                structuresId.add(structuresId.size(), data.getString("id_structure"));
        }
        structureService.getStructureById(new JsonArray(structuresId), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> repStructures) {
                if (repStructures.isRight()) {
                    JsonArray structures = repStructures.right().getValue();
                    setStructures(structures);
                    setArray(datas);
                    handler.handle(new Either.Right<>(true));
                } else {
                    handler.handle(new Either.Left<>("Error when casting neo"));

                }
            }
        });

    }

    @Override
    protected void setArray(JsonArray datas){
        excel.insertStandarText(0, 0, IDDOS);
        excel.insertStandarText(1, 0, LBDDOS);
        excel.insertStandarText(2, 0, OBJDDOS);
        excel.insertStandarText(3, 0, IDTIERS);
        excel.insertStandarText(4, 0, NATURE);
        excel.insertStandarText(5, 0, PROGRAMME);
        excel.insertStandarText(6, 0, ACTION);
        excel.insertStandarText(7, 0, ENVILIG);
        excel.insertStandarText(8, 0, MTPROP);
        excel.insertStandarText(9, 0, IDCOM);
        excel.insertStandarText(10, 0, IDLOCAL);
        excel.insertStandarText(11, 0, IDCPER);
        excel.insertStandarText(12, 0, IDCPRD);
        excel.insertStandarText(13, 0, QPVIDENT);
        excel.insertStandarText(14, 0, CPVIDENT);
        for(int i=0;i<datas.size();i++){
            JsonObject data =datas.getJsonObject(i);
            excel.insertStandarText(0,0,"");
            excel.insertStandarText(1,0,"ETAB."+data.getString("uai"));
            excel.insertStandarText(2,0,"");
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
                " AND   contract_type.code = '236')      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".campaign ON orders.id_campaign = campaign.id  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".project ON orders.id_project = project.id  " +
                "             LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id)         ";
        query += "   AND ((spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "') " +
                "     OR  (spa.structure_type = '" + CMR + "' AND specific_structures.type ='" + CMR + "') " +
                "      OR (spa.structure_type = '" + LYCEE + "' AND specific_structures.type is null ))    ";
        query += "     INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id)    " +
                "     INNER JOIN " + Lystore.lystoreSchema + ".program on program_action.id_program = program.id           " +
                "             Group by program.name,code,specific_structures.type , orders.amount , orders.name, orders.equipment_key , " +
                "             orders.id_operation,orders.id_structure  ,orders.id, contract.id ,label.label  ,program_action.id_program ,  " +
                "             orders.id_order_client_equipment,orders.\"price TTC\",orders.price_proposal,orders.override_region , orders.comment,campaign.name , orders.id," +
                "               orders.isregion, " +
                "              project.room,project.stair, project.building " +
                "             order by campaign,code,market_id, id_structure,program,code " +
                "  )    SELECT  values.* " +
                "  from  values      " ;


        sqlHandler(handler);
    }
}
