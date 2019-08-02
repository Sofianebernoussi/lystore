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
import java.util.Collections;

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
    public AnnexeDelibTab(Workbook wb, JsonObject instruction) {
        super(wb, instruction, "ANNEXE DELIB");
        structureService = new DefaultStructureService(Lystore.lystoreSchema);
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
                }
            }
        });
    }

    @Override
    protected void setArray(JsonArray datas) {
        setLabels();
        if (datas.isEmpty()) {
            return;
        }
        int lineToInsert = 5;
        JsonObject idPassed = new JsonObject();

        for (int i = 0; i < datas.size(); i++) {
            JsonObject action = datas.getJsonObject(i);
            int columnToInsert = programMarket.getInteger(action.getString("program") + " - " + action.getString("code"));
            if (!checkIdPassed(idPassed, action.getString("id_structure"))) {
                idPassed.put(action.getString("id_structure"), 1);
                lineToInsert++;
                excel.insertCellTab(0, lineToInsert, action.getString("zipCode"));
                excel.insertCellTab(1, lineToInsert, action.getString("city"));
                excel.insertCellTab(2, lineToInsert, action.getString("nameEtab"));
                excel.insertCellTab(3, lineToInsert, action.getString("uai"));
                excel.insertCellTabFloat(columnToInsert + 4, lineToInsert, Float.parseFloat(action.getString("total")));
            } else {
                excel.insertCellTabFloat(columnToInsert + 4, lineToInsert, Float.parseFloat(action.getString("total")));
            }

        }

        excel.fillTab(0, arrayLength, 6, lineToInsert + 1);
        for (int i = 6; i <= lineToInsert; i++) {
            excel.setTotalY(4, 4 + programMarket.size() - 1, i, 4 + programMarket.size());
        }

        for (int i = 0; i < programMarket.size(); i++) {
            excel.insertHeader(lineToInsert + 1, 3, excel.totalLabel);
            excel.setTotalX(6, lineToInsert, 4 + i, lineToInsert + 1);

        }
        excel.autoSize(arrayLength + 1);
    }

    private boolean checkIdPassed(JsonObject idPassed, String id) {
        return idPassed.containsKey(id);
    }

    @Override
    protected void setLabels() {
        ArrayList<String> programsActionList = new ArrayList<>();
        String previousProgram = "";
        excel.insertHeader(4, 1, "COMMUNE");
        excel.insertHeader(5, 1, "");

        excel.insertHeader(4, 2, "LYCEE");
        excel.insertHeader(5, 2, "");
        excel.insertHeader(4, 3, "UAI");
        excel.insertHeader(5, 3, "");

        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            String programMarketStr = data.getString("program") + " - " + data.getString("code");
            if (!programsActionList.contains(programMarketStr))
                programsActionList.add(programMarketStr);
        }
        Collections.sort(programsActionList);
        // Getting merged region
        for (int i = 0; i < programsActionList.size(); i++) {
            String progM = programsActionList.get(i);
            //getting program and code separated
            String segments[] = progM.split(" - ");

            //merge region if same program
            if (previousProgram.equals(segments[0])) {
                CellRangeAddress merge = new CellRangeAddress(4, 4, 4 + programMarket.size() - 1, 4 + programMarket.size());
                sheet.addMergedRegion(merge);
                excel.setRegionHeader(merge, sheet);
            } else {
                previousProgram = segments[0];
                excel.insertHeader(4, 4 + programMarket.size(), segments[0]);
            }
            excel.insertHeader(5, 4 + programMarket.size(), segments[1]);

            programMarket.put(progM, i);

        }

        arrayLength += programMarket.size();
        excel.insertHeader(4, 4 + programMarket.size(), excel.totalLabel);
        excel.insertHeader(5, 4 + programMarket.size(), "");

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
                    action.put("zipCode", structure.getString("zipCode"));
                }
            }
        }
    }


    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "   With values as (" +
                "(SELECT SUM( ore.price* ore.amount) as Total," +
                "ore.id_structure,contract_type.code as code,ore.id_operation , program_action.id_program ,contract_type.name as market ,program.name as program     " +
                "FROM " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore        " +
                "INNER JOIN " + Lystore.lystoreSchema + ".operation ON (ore.id_operation = operation.id)     " +
                "INNER JOIN " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)        " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (ore.id_contract = contract.id)      " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)    " +
                "INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)    " +
                "INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)     " +
                "INNER JOIN " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id)" +
                "WHERE instruction.id = ?  " +
                "Group by  program.name,contract_type.code, contract_type.name , program_action.id, ore.id_operation,ore.id_structure     " +
                "order by  program.name,id_program,code,ore.id_operation  )  " +
                "UNION " +
                "(    SELECT    SUM(  CASE WHEN oce.price_proposal is not null    " +
                "THEN oce.price_proposal *  oce.amount    " +
                "ELSE (oce.price * oce.amount) + ((oce.price*oce.amount)*oce.tax_amount +       " +
                "(SELECT CASE WHEN  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)  IS NULL        " +
                "THEN 0             ELSE  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)       " +
                "END            from " + Lystore.lystoreSchema + ".order_client_options oco             WHERE id_order_client_equipment = oce.id )     " +
                ")/100 END       ) " +
                "as Total,    oce.id_structure,contract_type.code as code,oce.id_operation , program_action.id_program ,contract_type.name as market,program.name   as program       " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment oce      " +
                "INNER JOIN " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id)    " +
                "INNER JOIN " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)   " +
                "INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)   " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id)       " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)  " +
                "INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)  " +
                "INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)    " +
                "INNER JOIN " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id)        " +
                "WHERE instruction.id = ?    " +
                "Group by  program.name,contract_type.code, contract_type.name , program_action.id, oce.id_operation,oce.id_structure  " +
                "order by  program.name,id_program,code,oce.id_operation    )" +
                "  )" +
                "  SELECT values.*    " +
                " from values  " +
                " order by id_structure,program  " +
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
