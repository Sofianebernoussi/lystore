package fr.openent.lystore.export.investissement;

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


public class RecapEPLETab extends TabHelper {

    private int yProgramLabel = 0;
    private final int xProgramLabel = 0;
    private String programLabel = "Programme : ";
    private String totalLabel = "Montant : ";
    private String contractType = "Nature comptable : ";
    private String actionLabel = "Action : ";
    private String orderLabel = "Libellé Demande";
    private String orderComment = "Demande Commentaire";
    private String orderAmount = "Quantité Accordée";
    private String orderTotal = "Somme Montant Accordé";
    private String id_structureStr = "id_structure";
    private boolean notFirstTab = false;
    private boolean notFirstPart = false;

    private int program_id = -1, action_id = -1, contract_id = -1;
    private int xlabel = 0;
    private int yTotalLabel = 3;

    private JsonArray programs;
    private ArrayList<String> structuresId;
    private StructureService structureService;
    private int nbLine = 0;
    private String totalLabelInt = "";

    public RecapEPLETab(Workbook workbook, JsonObject instruction) {
        super(workbook, instruction, TabName.EPLE.toString());
        structuresId = new ArrayList<>();
        structureService = new DefaultStructureService(Lystore.lystoreSchema);
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        excel.setCPNumber(instruction.getString("cp_number"));
        getDatas(event -> {
            try {
                if (event.isLeft()) {
                    handler.handle(new Either.Left<>("Failed to retrieve programs"));
                    return;
                }
                getAndSetDatas(handler);
            }catch(Exception e){
                logger.error(e.getMessage());
                logger.error(e.getStackTrace());
                handler.handle(new Either.Left<>("error when creating excel"));
            }
        });
    }

    private void getAndSetDatas(Handler<Either<String, Boolean>> handler) {
        JsonObject program;
        for (int i = 0; i < programs.size(); i++) {
            program = programs.getJsonObject(i);
            program.getString("comment");
            if (!structuresId.contains(program.getString(id_structureStr)))
                structuresId.add(structuresId.size(), program.getString(id_structureStr));
        }
        structureService.getStructureById(new JsonArray(structuresId), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> repStructures) {
                if (repStructures.isRight()) {
                    JsonArray structures = repStructures.right().getValue();
                    setStructures(structures);
                    setTab();
                    handler.handle(new Either.Right<>(true));
                }
            }
        });


    }

    private void setStructures(JsonArray structures) {
        JsonObject program, structure;
        for (int i = 0; i < programs.size(); i++) {
            program = programs.getJsonObject(i);
            for (int j = 0; j < structures.size(); j++) {
                structure = structures.getJsonObject(j);
                if (program.getString(id_structureStr).equals(structure.getString("id"))) {
                    program.put("nameEtab", structure.getString("name"));
                    program.put("uai", structure.getString("uai"));
                    program.put("city", structure.getString("city"));
                    program.put("zipCode", structure.getString("zipCode"));
                }
            }

        }

    }

    private void setTab() {
        JsonObject program;
        String id_structure = "";

        for (int i = 0; i < programs.size(); i++) {
            program = programs.getJsonObject(i);

            if (!(id_structure.equals(program.getString(id_structureStr)))) {
                id_structure = newTab(program, false);
            }
            sheet.createRow(yProgramLabel + 3);
            excel.insertCellTab(0, yProgramLabel + 3, program.getString("label"));
            excel.insertCellTab(1, yProgramLabel + 3, program.getString("comment"));
            excel.insertCellTabInt(2, yProgramLabel + 3, Integer.parseInt(program.getInteger("amount").toString()));
            excel.insertCellTabDouble(3, yProgramLabel + 3, Double.parseDouble(program.getString("total")));
            yProgramLabel++;
            nbLine++;

        }
        if (programs.size() > 0) {
            newTab(programs.getJsonObject(programs.size() - 1), true);
//            settingSumLabel();
        }
    }

    private String newTab(JsonObject program, boolean isLast) {


        String id_structure = "", zipCode, city, uai, nameEtab;
        CellRangeAddress merge;
        if (notFirstTab) {
            merge = new CellRangeAddress(yProgramLabel + 3, yProgramLabel + 5, 0, 1);
            sheet.addMergedRegion(merge);
            merge = new CellRangeAddress(yProgramLabel + 4, yProgramLabel + 5, 2, 3);
            sheet.addMergedRegion(merge);
            excel.insertHeader(yProgramLabel + 3, 2, excel.sumLabel);
            excel.setTotalX(yProgramLabel + 2 - nbLine, yProgramLabel + 2, 3, yProgramLabel + 3);
            addTotalLabelInt(excel.getCellReference(yProgramLabel + 3, 3) + " +");
            nbLine = 0;


        } else {
            notFirstTab = true;
        }
        if (!isLast) {
            yProgramLabel += 3;

            if (program_id != program.getInteger("program_id") || action_id != program.getInteger("action_id") || contract_id != program.getInteger("contract_type_id")) {
                program_id = program.getInteger("program_id");
                action_id = program.getInteger("action_id");
                contract_id = program.getInteger("contract_type_id");
                if (notFirstPart) { //adding sum
                    settingSumLabel();
                }
                setLabelHead(program);
                notFirstPart = true;
            }
//            //adding  new label
            id_structure = program.getString(id_structureStr);
            nameEtab = program.getString("nameEtab");
            uai = program.getString("uai");
            zipCode = program.getString("zipCode");
            city = program.getString("city");
            excel.insertHeader(sheet.createRow(yProgramLabel + 3), 0, zipCode + " - " + city + " - " + nameEtab + " (" + uai + ")");
            merge = new CellRangeAddress(yProgramLabel + 3, yProgramLabel + 3, 0, 3);
            sheet.addMergedRegion(merge);
            excel.setRegionHeader(merge, sheet);
            excel.insertHeader(sheet.createRow(yProgramLabel + 4), 0, orderLabel);
            excel.insertHeader(sheet.getRow(yProgramLabel + 4), 1, orderComment);
            excel.insertHeader(sheet.getRow(yProgramLabel + 4), 2, orderAmount);
            excel.insertHeader(sheet.getRow(yProgramLabel + 4), 3, orderTotal);
            yProgramLabel += 2;
        }
        return id_structure;

    }

    private void addTotalLabelInt(String s) {
        int limitFormulaSize = 8000;
        String cellToAdd = excel.getCellReference(yProgramLabel + 3, 3) + " +";
        cellToAdd = cellToAdd.replace("'Récap. EPLE'!", "");
        if (totalLabelInt.length() + cellToAdd.length() < limitFormulaSize) {
            totalLabelInt += cellToAdd;
        } else {
            totalLabelInt = totalLabelInt.substring(0, totalLabelInt.length() - 1);
            excel.insertFormula(sheet.getRow(yProgramLabel + 3), 1589, totalLabelInt);
            totalLabelInt = excel.getCellReference(yProgramLabel + 3, 1589) + " +" + cellToAdd;
        }
    }

    private void settingSumLabel() {
        totalLabelInt = totalLabelInt.substring(0, totalLabelInt.length() - 1);
        excel.insertFormula(sheet.getRow(xlabel), yTotalLabel, totalLabelInt);
        totalLabelInt = "";
    }

    public void setLabelHead(JsonObject program) {
        yProgramLabel += 4;

        excel.insertLabelHead(yProgramLabel, xProgramLabel,
                programLabel + program.getString("program_name") + " " + program.getString("program_label"));
        excel.insertLabelHead(yProgramLabel + 2, xProgramLabel,
                actionLabel + program.getString("action_code") + " - " + program.getString("action_name"));
        excel.insertLabelHead(yProgramLabel, xProgramLabel + 1,
                contractType + program.getString("contract_code") + " - " + program.getString("contract_name"));
        excel.insertLabelHead(yProgramLabel, xProgramLabel + 2, totalLabel);
        xlabel = yProgramLabel;
        yProgramLabel += 2;

    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "With values as ((SELECT distinct contract_type.code as contract_code, contract_type.name as contract_name, contract_type.id as contract_type_id, " +
                "program_action.action as action_code, program_action.description as action_name,program_action.id as action_id , program.name as program_name,program.id as program_id, " +
                "program.label as program_label,oce.id_structure,oce.amount as amount,oce.name as label,oce.comment as comment, " +
                "SUM(CASE WHEN oce.price_proposal is not null " +
                " THEN oce.price_proposal *  oce.amount " +
                " ELSE (oce.price * oce.amount) + ((oce.price*oce.amount)*oce.tax_amount)/100 +  " +
                "  " +
                " ( " +
                "           SELECT CASE WHEN  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)  IS NULL " +
                "           THEN 0  " +
                "           ELSE  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)  " +
                "           END " +
                "           from lystore.order_client_options oco  " +
                "           WHERE id_order_client_equipment = oce.id  " +
                " ) " +
                " END ) as Total  " +
                "FROM lystore.order_client_equipment oce   " +
                "INNER JOIN lystore.operation ON (oce.id_operation = operation.id)    " +
                "INNER JOIN lystore.instruction ON (operation.id_instruction = instruction.id)  " +
                "INNER JOIN lystore.contract ON (oce.id_contract = contract.id)  " +
                "INNER JOIN lystore.contract_type ON (contract.id_contract_type = contract_type.id) " +
                "INNER JOIN lystore.structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)   " +
                "INNER JOIN lystore.program_action ON (structure_program_action.program_action_id = program_action.id)   " +
                "INNER JOIN lystore.program ON (program_action.id_program = program.id)  " +
                "WHERE instruction.id = ? AND " +
                "oce.override_region = false " +
                "group by oce.id,contract_type.id,contract_code, contract_name, program_action.id,program.id,oce.id_structure,oce.name,oce.comment,oce.amount " +
                "order by program_name) " +
                "UNION " +
                "(SELECT distinct contract_type.code as contract_code, contract_type.name as contract_name, contract_type.id as contract_type_id, " +
                "program_action.action as action_code, program_action.description as action_name,program_action.id as action_id , program.name as program_name,program.id as program_id, " +
                "program.label as program_label,ore.id_structure,ore.amount as amount,ore.name as label,ore.comment as comment, " +
                "SUM(ore.price * ore.amount) as Total  " +
                "FROM lystore.\"order-region-equipment\" ore " +
                "INNER JOIN lystore.operation ON (ore.id_operation = operation.id)    " +
                "INNER JOIN lystore.instruction ON (operation.id_instruction = instruction.id)  " +
                "INNER JOIN lystore.contract ON (ore.id_contract = contract.id)  " +
                "INNER JOIN lystore.contract_type ON (contract.id_contract_type = contract_type.id) " +
                "INNER JOIN lystore.structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)   " +
                "INNER JOIN lystore.program_action ON (structure_program_action.program_action_id = program_action.id)   " +
                "INNER JOIN lystore.program ON (program_action.id_program = program.id)  " +
                "WHERE instruction.id = ?  " +
                "group by ore.id,contract_type.id,contract_code, contract_name, program_action.id,program.id,ore.id_structure,ore.name,ore.comment,ore.amount " +
                "order by program_name) " +
                ") " +
                "SELECT * from values  " +
                "order by program_name,id_structure ";


        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")).add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                programs = event.right().getValue();
                handler.handle(new Either.Right<>(programs));
            }
        }));
    }

}

