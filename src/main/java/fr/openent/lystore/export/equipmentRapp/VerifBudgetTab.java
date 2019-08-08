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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VerifBudgetTab extends TabHelper {
    JsonArray operations;
    private int currentY = 0, programY = 0;
    private JsonArray datas;
    private Double programTotal = 0.d;
    JsonObject programMarket;
    private StructureService structureService;
    private String type;

    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     * @param type
     */
    public VerifBudgetTab(Workbook wb, JsonObject instruction, String type) {
        super(wb, instruction, "Vérification ligne budgétaire");
        structureService = new DefaultStructureService(Lystore.lystoreSchema);
        this.type = type;
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
            JsonArray actions = new JsonArray(data.getString("actions"));
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                structuresId.add(structuresId.size(), action.getString("id_structure"));

            }
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
        String previousProgramCode = "";
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            String actualProgramCode = data.getString("program") + "/" + data.getString("code");
            if (actualProgramCode.equals(previousProgramCode)) {
                insertNewMarket(data);
            } else {
                if (i != 0) {
                    insertProgramPrice();
                }

                insertNewProgramCode(actualProgramCode, data);
                previousProgramCode = actualProgramCode;

            }
        }
        insertProgramPrice();
        excel.autoSize(7);
    }

    private void insertProgramPrice() {


        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(2);

        excel.insertBlackTitleHeader(0, programY, "TOTAL ENVELOPPE / NATURE : " + (df.format(programTotal)) + " EUROS");
        CellRangeAddress merge = new CellRangeAddress(programY, programY, 0, 4);
        sheet.addMergedRegion(merge);
        excel.setRegionHeader(merge, sheet);
    }

    private void insertNewProgramCode(String actualProgramCode, JsonObject data) {
        currentY += 2;
        programY = currentY + 1;
        programTotal = 0.d;
        excel.insertBlackTitleHeader(0, currentY, "ENVELOPPE/NATURE :" + actualProgramCode);
        CellRangeAddress merge = new CellRangeAddress(currentY, currentY, 0, 4);
        sheet.addMergedRegion(merge);
        excel.setRegionHeader(merge, sheet);
        currentY += 2;
        insertNewMarket(data);

    }

    private void insertNewMarket(JsonObject data) {
        currentY += 1;

        String market = data.getString("market");
        String totalMarket;
        try {
            totalMarket = String.format("%.2f", Float.parseFloat(data.getString("totalmarket")));
        } catch (ClassCastException e) {
            totalMarket = data.getInteger("totalmarket").toString();
        }

        programTotal += Double.parseDouble(totalMarket);
        excel.insertBlueTitleHeader(0, currentY, market);
        CellRangeAddress merge = new CellRangeAddress(currentY, currentY, 0, 4);
        sheet.addMergedRegion(merge);
        excel.setRegionHeader(merge, sheet);
        currentY++;
        excel.insertBlueTitleHeader(0, currentY, "Pour un sous total de : " + totalMarket + " EUROS");
        merge = new CellRangeAddress(currentY, currentY, 0, 4);
        sheet.addMergedRegion(merge);
        excel.setRegionHeader(merge, sheet);
        currentY += 2;

        insertArrays(data);
    }

    private void insertArrays(JsonObject data) {
        JsonArray values = data.getJsonArray("actionsJO");
        values = sortByCity(values);
        String previousZip = "", previousCity = "";
        CellRangeAddress merge;
        for (int i = 0; i < values.size(); i++) {
            String currentCity = "";
            String currentZip = "";
            JsonObject value = values.getJsonObject(i);
            if (value.containsKey("zipCode"))
                currentZip = value.getString("zipCode").substring(0, 2);//get number of departement
            if (value.containsKey("city")) {
                currentCity = value.getString("city");
            }
            if (currentZip.equals(previousZip)) {
            } else {
                previousCity = "";
                currentY += 2;
                excel.insertBlackTitleHeader(0, currentY, currentZip);
                merge = new CellRangeAddress(currentY, currentY, 0, 4);
                sheet.addMergedRegion(merge);
                excel.setRegionHeader(merge, sheet);

                previousZip = currentZip;
                currentY += 2;
            }
            if (previousCity.equals(currentCity)) {
            } else {
                currentY += 2;
                excel.insertBlackTitleHeader(0, currentY, currentCity);
                merge = new CellRangeAddress(currentY, currentY, 0, 4);
                sheet.addMergedRegion(merge);
                excel.setRegionHeader(merge, sheet);

                previousCity = currentCity;
                currentY += 2;
            }
            if (value.containsKey("uai")) {
                excel.insertLabel(currentY, 0, value.getString("uai"));
            } else
                excel.insertLabel(currentY, 0, "");
            excel.insertLabel(currentY, 1, "R");
            try {
                if (!value.getString("old_name").equals("null")) {
                    excel.insertLabel(currentY, 2, "REG : " + value.getString("old_name"));
                } else {
                    excel.insertLabel(currentY, 2, "REG : " + value.getString("name_equipment"));
                }
            } catch (NullPointerException e) {
                excel.insertLabel(currentY, 2, "REG : " + value.getString("name_equipment"));
            }
            excel.insertCellTabStringRight(2, currentY, value.getInteger("amount").toString());
            try {
                excel.insertLabel(currentY, 4, "M : " + value.getString("total"));
            } catch (ClassCastException e) {
                excel.insertLabel(currentY, 4, "M : " + value.getInteger("total").toString());
            }
            currentY++;

            excel.insertLabel(currentY, 0, "OPE  : " + value.getInteger("id_operation").toString());
            excel.insertLabel(currentY, 1, " ");
            excel.insertLabel(currentY, 2, "LYC : " + value.getString("name_equipment"));
            excel.insertLabel(currentY, 3, "Ref : " + value.getInteger("key").toString());
            if (value.getBoolean("region")) {
                excel.insertLabel(currentY, 4, "N° dem : " + " - " + value.getInteger("id").toString());

            } else {
                excel.insertLabel(currentY, 4, "N° dem : " + value.getInteger("id").toString());

            }
            currentY++;

        }
    }

    private JsonArray sortByCity(JsonArray values) {
        JsonArray sortedJsonArray = new JsonArray();

        List<JsonObject> jsonValues = new ArrayList<JsonObject>();
        for (int i = 0; i < values.size(); i++) {
            jsonValues.add(values.getJsonObject(i));
        }

        Collections.sort(jsonValues, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "zipCode";

            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                String cityA = "";
                String cityB = "";
                String nameA = "";
                String nameB = "";
                try {
                    if (a.containsKey(KEY_NAME)) {
                        valA = a.getString(KEY_NAME);
                    }
                    if (b.containsKey(KEY_NAME)) {
                        valB = b.getString(KEY_NAME);
                    }
                } catch (NullPointerException e) {
                    log.error("error when sorting structures during export");
                }
                if (valA.compareTo(valB) == 0) {
                    if (a.containsKey("city")) {
                        cityA = a.getString("city");
                    }
                    if (b.containsKey("city")) {
                        cityB = b.getString("city");
                    }
                    if (cityA.compareTo(cityB) == 0) {
                        if (a.containsKey("nameEtab")) {
                            nameA = a.getString("nameEtab");
                        }
                        if (b.containsKey("nameEtab")) {
                            nameB = b.getString("nameEtab");
                        }
                        return nameA.compareTo(nameB);
                    }
                    return cityA.compareTo(cityB);
                }
                return valA.compareTo(valB);
            }
        });

        for (int i = 0; i < values.size(); i++) {
            sortedJsonArray.add(jsonValues.get(i));
        }
        return sortedJsonArray;
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
                        action.put("zipCode", structure.getString("zipCode"));
                    }
                }
            }
            data.put("actionsJO", actions);
        }
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "    With values as (        With unionValues as (       ( " +
                "         SELECT DISTINCT ore.id,SUM( ore.price* ore.amount) as Total," +
                "         (SELECT CASE WHEN ore.id_order_client_equipment is not null " +
                "        THEN  oce.name " +
                "        ELSE '' " +
                "        END " +
                "        FROM lystore.order_client_equipment oce " +
                "        where oce.id = ore.id_order_client_equipment " +
                "        ) as old_name, " +
                "           ore.id_structure,ore.id_operation as id_operation,label.label as operation , " +
                "           ore.equipment_key as key,ore.name as name_equipment, true as region, " +
                "           program_action.id_program, ore.amount ,contract.id as market_id " +
                "           FROM  " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore      " +
                "           INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (ore.id_operation = operation.id)       " +
                "           INNER JOIN  " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)       " +
                "           INNER JOIN  " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)            " +
                "           INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (ore.id_contract = contract.id)        " +
                "           INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)       " +
                "           INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)     " +
                "           INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)         " +
                "           WHERE instruction.id = ?  ";
        if (type.equals(CMR))
            query += "    AND structure_program_action.structure_type =  '" + type + "'  " +
                    " AND ore.id_structure IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + type + "' ) ";

        else
            query += "    AND( structure_program_action.structure_type !=  '" + CMR + "'  " +
                    " OR ore.id_structure NOT IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + CMR + "' )) ";
        query +=
                "           Group by  ore.id_operation,ore.id_structure  ,ore.id, contract.id ,label.label  ,program_action.id_program    " +
                        "           order by  ore.id_operation )           " +
                        "            UNION " +
                        "            (  SELECT DISTINCT oce.id, SUM(  CASE WHEN oce.price_proposal is not null        " +
                        "                                       THEN oce.price_proposal *  oce.amount        " +
                        "                                       ELSE (oce.price * oce.amount) + ((oce.price*oce.amount)*oce.tax_amount +  " +
                        "                   (SELECT CASE WHEN  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)  IS NULL         " +
                        "                   THEN 0          " +
                        "                   ELSE  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)      " +
                        "                   END       " +
                        "                   from  " + Lystore.lystoreSchema + ".order_client_options oco       " +
                        "                   WHERE id_order_client_equipment = oce.id )    " +
                        "                    )/100 END                " +
                        "  ) as Total,     oce.name as old_name ,    " +
                        "            oce.id_structure,oce.id_operation as id_operation,label.label as operation , " +
                        "           oce.equipment_key as key,oce.name as name_equipment, false as region, " +
                        "           program_action.id_program, oce.amount ,contract.id as market_id " +
                        "           FROM  " + Lystore.lystoreSchema + ".order_client_equipment oce           " +
                        "           INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id)        " +
                        "           INNER JOIN  " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)      " +
                        "           INNER JOIN  " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)              " +
                        "           INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id)          " +
                        "           INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)        " +
                        "           INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)    " +
                        "           INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)         " +
                        "           INNER JOIN  " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id)            " +
                        "           WHERE instruction.id = ? AND oce.override_region=false ";
        if (type.equals(CMR))
            query += "    AND structure_program_action.structure_type =  '" + type + "'  " +
                    " AND oce.id_structure IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + type + "' ) ";

        else
            query += "    AND( structure_program_action.structure_type !=  '" + CMR + "'  " +
                    " OR oce.id_structure NOT IN (    SELECT id    FROM lystore.specific_structures    WHERE type='" + CMR + "' )) ";
        query +=
                "           Group by  oce.id_operation,oce.id_structure,oce.id,contract.id,label.label  ,program_action.id_program    " +
                        "            order by  oce.id_operation )    " +
                        "            )  SELECT * from unionValues  order by   id_operation,id_structure " +
                        "    ) " +
                        "SELECT contract.name as market, contract_type.code,program.name as program  , array_to_json(array_agg(values))as actions " +
                        ", SUM (values.total) as totalMarket     " +
                        "from  " + Lystore.lystoreSchema + ".contract " +
                        "INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)        " +
                        "INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)    " +
                        "INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)         " +
                        "INNER JOIN  " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id)            " +
                        "INNER JOIN values  on (contract.id = values.market_id)   " +
                        " Group by contract.name , contract_type.code,program.name" +
                        " Order by program.name,contract_type.code ;";
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
