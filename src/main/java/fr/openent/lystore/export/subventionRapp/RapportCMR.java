package fr.openent.lystore.export.subventionRapp;

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

import java.util.ArrayList;

/**
 * Possibilité de rajouter un string en param pour changer query et nom onglet(appelé 3 fois dans instruciton)
 */
public class RapportCMR extends TabHelper {
    private int yProgramLabel = 2;
    private int initY = 2;
    private String orderLabel = "Libellé Demande";
    private String orderComment = "Demande Commentaire";
    private String orderAmount = "Quantité Accordée";
    private String orderTotal = "Somme Montant Accordé";
    private String id_structureStr = "id_structure";

    private int program_id = -1, action_id = -1, contract_id = -1;
    private int xlabel = 0;
    private int xTotalLabel = 3;

    private ArrayList<String> structuresId;
    private StructureService structureService;
    private int nbLine = 0;

    public RapportCMR(Workbook workbook, JsonObject instruction) {
        super(workbook, instruction, "ANNEXE RAPPORT - CMR");
        structuresId = new ArrayList<>();
        structureService = new DefaultStructureService(Lystore.lystoreSchema);
        excel.setDefaultFont();
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        getDatas(event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>("Failed to retrieve programs"));
                return;
            }
            getAndSetDatas(handler);
        });
    }

    private void getAndSetDatas(Handler<Either<String, Boolean>> handler) {
        JsonObject program;
        for (int i = 0; i < datas.size(); i++) {
            program = datas.getJsonObject(i);
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
        for (int i = 0; i < datas.size(); i++) {
            program = datas.getJsonObject(i);
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

        for (int i = 0; i < datas.size(); i++) {
            program = datas.getJsonObject(i);

            if (!(id_structure.equals(program.getString(id_structureStr)))) {
                if (!id_structure.equals("")) {
                    settingSumLabel();
                }
                id_structure = newTab(program, false);
                initY = yProgramLabel;
            }
            sheet.createRow(yProgramLabel + 3);
            excel.insertCellTab(0, yProgramLabel + 3, program.getString("label"));
            excel.insertCellTab(1, yProgramLabel + 3, program.getString("comment"));
            excel.insertCellTabInt(2, yProgramLabel + 3, Integer.parseInt(program.getInteger("amount").toString()));
            excel.insertCellTabFloat(3, yProgramLabel + 3, Float.parseFloat(program.getString("total")));
            yProgramLabel++;
            nbLine++;

        }
        if (datas.size() > 0) {
            settingSumLabel();
        }
    }

    private String newTab(JsonObject program, boolean isLast) {


        String id_structure = "", zipCode, city, uai, nameEtab;
        CellRangeAddress merge;

        yProgramLabel += 3;
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
        return id_structure;

    }


    private void settingSumLabel() {
        excel.insertLabel(yProgramLabel + 3, xTotalLabel - 1, excel.totalLabel);
        excel.setTotalX(initY + 2, yProgramLabel + 2, xTotalLabel, yProgramLabel + 3);
    }


    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "WITH values as ( " +
                " SELECT DISTINCT oce.id,oce.id_structure, " +
                " oce.amount as amount,oce.name as label,oce.comment as comment, " +
                "SUM( " +
                " CASE WHEN oce.price_proposal is not null " +
                " THEN oce.price_proposal *  oce.amount  " +
                " ELSE (oce.price * oce.amount) + ((oce.price*oce.amount)*oce.tax_amount)/100  " +
                " END " +
                ") as total_equipment,  " +
                "SUM( (oco.price * oco.amount) + ((oco.price*oco.amount)*oco.tax_amount)/100 ) as total_options " +
                "FROM lystore.order_client_equipment oce    " +
                "LEFT JOIN lystore.order_client_options oco ON (oco.id_order_client_equipment = oce.id) " +
                "INNER JOIN lystore.operation ON (oce.id_operation = operation.id) " +
                "INNER JOIN lystore.instruction ON (operation.id_instruction = instruction.id)  " +
                "INNER JOIN lystore.contract ON (oce.id_contract = contract.id)    " +
                "INNER JOIN lystore.contract_type ON (contract.id_contract_type = contract_type.id)     " +
                "INNER JOIN lystore.structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)    " +
                "INNER JOIN lystore.program_action ON (structure_program_action.program_action_id = program_action.id) " +
                "INNER JOIN lystore.program ON (program_action.id_program = program.id)   " +
                "WHERE instruction.id = ? AND structure_program_action.structure_type =  'CMR' " +
                "group by oce.id,oce.id_structure,oce.name,oce.comment,oce.amount  " +
                "order by label " +
                ") SELECT values.*, SUM( " +
                " CASE WHEN values.total_options is not null " +
                " THEN values.total_options + values.total_equipment " +
                " ELSE values.total_equipment " +
                " END " +
                ") as Total " +
                "from values " +
                "group by values.id_structure,values.id,values.label,values.comment,values.amount ,values.total_equipment,total_options " +
                "order by label";
        sqlHandler(handler);
    }


}
