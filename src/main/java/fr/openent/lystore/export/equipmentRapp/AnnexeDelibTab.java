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
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;

public class AnnexeDelibTab extends TabHelper {
    private JsonArray datas;
    private String type;
    private int yProgramLabel = 0;
    private StructureService structureService;
    private JsonObject programMarket;

    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     */
    public AnnexeDelibTab(Workbook wb, JsonObject instruction, String type) {
        super(wb, instruction, "ANNEXE DELIB");
        structureService = new DefaultStructureService(Lystore.lystoreSchema);
        this.type = type;

        programMarket = new JsonObject();
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
//                initDatas(handler);
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
            structuresId.add(structuresId.size(), data.getString("id_structure"));

        }
        structureService.getStructureById(new JsonArray(structuresId), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> repStructures) {
                if (repStructures.isRight()) {
                    JsonArray structures = repStructures.right().getValue();
                    setStructures(structures);
                    handler.handle(new Either.Right<>(true));
                }
            }
        });
    }

    @Override
    protected void setArray(JsonArray datas) {

//        excel.autoSize(arrayLength + 1);
    }

    private boolean checkIdPassed(JsonObject idPassed, String id) {
        return idPassed.containsKey(id);
    }

    @Override
    protected void setLabels() {


    }

    private void setStructures(JsonArray structures) {
        JsonObject program, structure;
        JsonArray actions;
        for (int i = 0; i < datas.size(); i++) {
            JsonObject action = datas.getJsonObject(i);
            for (int j = 0; j < structures.size(); j++) {
                structure = structures.getJsonObject(j);
                if (action.getString("id_structure").equals(structure.getString("id"))) {
                    action.put("nameEtab", structure.getString("name"));
                    action.put("uai", structure.getString("uai"));
                    action.put("city", structure.getString("city"));
                    try {
                        action.put("zipCode", structure.getString("zipCode"));
                    } catch (ClassCastException e) {
                        action.put("zipCode", structure.getInteger("zipCode").toString());
                    }
                }
            }
        }
    }


    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "    With values as (  " +
                "      With unionValues as ( " +
                "      (SELECT DISTINCT ore.id,SUM( ore.price* ore.amount) as Total,  " +
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
                "       WHERE instruction.id = ? ";

        if (type.equals(CMR))
            query += "    AND structure_program_action.structure_type =  '" + type + "'  " +
                    " AND ore.id_structure IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + type + "' ) ";

        else
            query += "    AND structure_program_action.structure_type !=  '" + CMR + "'  " +
                    " AND ore.id_structure NOT IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + CMR + "' ) ";
        query +=
                "      Group by  ore.id_operation,label.label ,program.name,contract_type.code, contract_type.name , program_action.id, ore.id_operation,campaign_id,ore.id_structure  ,ore.id " +
                        "      order by  ore.id_operation,label.label ,program.name,id_program,code,ore.id_operation  )    " +
                        "      UNION (  SELECT DISTINCT oce.id, SUM(  CASE WHEN oce.price_proposal is not null      " +
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
                        "       WHERE instruction.id = ? ";
        if (type.equals(CMR))
            query += "    AND structure_program_action.structure_type =  '" + type + "'  " +
                    " AND oce.id_structure IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + type + "' ) ";

        else
            query += "    AND structure_program_action.structure_type !=  '" + CMR + "'  " +
                    " OR oce.id_structure  IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + CMR + "' ) ";
        query +=
                "       Group by  oce.id_operation,label.label,program.name,contract_type.code, contract_type.name , program_action.id, campaign_id,oce.id_structure,oce.id      " +
                        "       order by  oce.id_operation,label.label,program.name,id_program,code )   " +
                        "  )  SELECT * from unionValues " +
                        " order by  operation,program,id_program,code,id_operation " +
                        "      ) " +
                        "SELECT campaign.name as campaign , array_to_json(array_agg(values))as actions   " +
                        "from " + Lystore.lystoreSchema + ".campaign as campaign " +
                        "INNER JOIN values  on (campaign.id = values.campaign_id)  " +
                        "Group by campaign.name " +
                        "   ";

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
