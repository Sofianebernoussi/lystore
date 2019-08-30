package fr.openent.lystore.export;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.investissement.TabName;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.openent.lystore.service.impl.DefaultProjectService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class TabHelper {
    protected Logger logger = LoggerFactory.getLogger(DefaultProjectService.class);
    protected static final String CMD = "CMD";
    protected static final String CMR = "CMR";
    protected static final String LYCEE = "LYC";
    protected static final String NULL_DATA="Pas de données sur l'établissement";
    protected static final String INVESTISSEMENT = "Investissement";
    protected static final String FONCTIONNEMENT = "Fonctionnement";
    protected Workbook wb;
    protected String query;
    protected Sheet sheet;
    protected JsonObject instruction;
    protected ExcelHelper excel;
    protected int operationsRowNumber = 9;
    final protected int yTab = 9;
    final protected int xTab = 1;
    protected int cellColumn = 1;
    protected boolean isEmpty = false;
    protected Logger log = LoggerFactory.getLogger(DefaultProjectService.class);
    protected int arrayLength = 4;
    protected long timeout = 999999999;
    protected JsonArray datas;


    /**
     * Format : H-code
     */
    protected JsonObject tabx;
    protected JsonArray taby;
    protected ArrayList<ArrayList<Float>> priceTab;

    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     * @param TabName
     */
    public TabHelper(Workbook wb, JsonObject instruction, String TabName) {
        this.wb = wb;
        this.tabx = new JsonObject();
        this.taby = new JsonArray();
        this.instruction = instruction;
        this.sheet = wb.getSheet(TabName);
        if (wb.getSheetIndex(this.sheet) == -1) {
            this.sheet = wb.createSheet(TabName);
        }
        this.excel = new ExcelHelper(wb, sheet);
        priceTab = new ArrayList<ArrayList<Float>>();
        log.info("Initialize tab : " + TabName);
    }


    public abstract void create(Handler<Either<String, Boolean>> handler);

    /**
     * retrieve datas to insert into the page
     *
     * @param handler
     */
    public abstract void getDatas(Handler<Either<String, JsonArray>> handler);


    /**
     * Set labels of the tabs
     */
    protected void setLabels() {
    }

    /**
     * Set the headers of tab for investissement
     *
     * @param programs
     */
    protected void setArray(JsonArray programs) {
    }

    protected JsonArray sortByCity(JsonArray values) {
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

    protected void sizeMergeRegion(int line, int columnStart, int columnEnd) {
        CellRangeAddress merge = new CellRangeAddress(line, line, columnStart, columnEnd);
        sheet.addMergedRegion(merge);
        excel.setRegionHeader(merge, sheet);
        short height = 1000;
        Row row = sheet.getRow(line);
        row.setHeight(height);

    }

    protected void sizeMergeRegionWithStyle(int line, int columnStart, int columnEnd, CellStyle style) {
        CellRangeAddress merge = new CellRangeAddress(line, line, columnStart, columnEnd);
        sheet.addMergedRegion(merge);
        excel.setRegionHeaderStyle(merge, sheet, style);
        short height = 1000;
        Row row = sheet.getRow(line);
        row.setHeight(height);

    }

    // doing \n when the str is too long
    protected String formatStrToCell(String str, int nbWords) {
        try {
            String[] words = str.split(" ");
            String resultStr = "";
            if (words.length <= nbWords) {
                return str;
            } else {
                for (int i = 0; i < words.length; i++) {
                    resultStr += words[i] + " ";
                    if (i % nbWords == 0 && i != 0) {
                        resultStr += "\n";
                    }
                }
            }
            return resultStr;
        } catch (NullPointerException e) {
            return str;
        }
    }

    public boolean checkEmpty() {
        if (datas.isEmpty()) {
            excel.insertBlackOnGreenHeader(0, 0, "Cet onglet ne possède pas de données à afficher");
            excel.autoSize(1);
        }
        return datas.isEmpty();
    }

    public void sqlHandler(Handler<Either<String, JsonArray>> handler) {
        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")), new DeliveryOptions().setSendTimeout(Lystore.timeout * 1000000000L),SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                datas = event.right().getValue();
                handler.handle(new Either.Right<>(datas));
            }
        }));

    }

    public void handleDatasDefault(Either<String, JsonArray> event, Handler<Either<String, Boolean>> handler) {
        if (event.isLeft()) {
            log.error("Failed to retrieve datas");
            handler.handle(new Either.Left<>("Failed to retrieve datas"));
        } else {
            if (checkEmpty()) {
                handler.handle(new Either.Right<>(true));
            } else {
                initDatas(handler);
            }
        }
    }

    protected void initDatas(Handler<Either<String, Boolean>> handler) {

    }

    protected void getStructures(JsonArray ids, Handler<Either<String, JsonArray>> handler)  {
        String query = "" +
                "MATCH (s:Structure) " +
                "WHERE s.id IN {ids} " +
                "RETURN " +
                "s.id as id," +
                " s.UAI as uai," +
                " s.name as name," +
                " s.phone as phone," +
                " s.address + ' ,' + s.zipCode +' ' + s.city as address,  " +
                "s.zipCode as zipCode," +
                " s.city as city";
        Neo4j.getInstance().execute(query, new JsonObject().put("ids", ids), Neo4jResult.validResultHandler(handler));
    }

}
