package fr.openent.lystore.export;

import fr.openent.lystore.helpers.ExcelHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;

public abstract class TabHelper {
    protected static final String CMD = "CMD";
    protected static final String CMR = "CMR";
    protected static final String Lycee = "LYC";
    protected static final String Investissement = "Investissement";
    protected static final String Fonctionnement = "Fonctionnement";
    protected Workbook wb;
    protected Sheet sheet;
    protected JsonObject instruction;
    protected ExcelHelper excel;
    protected int operationsRowNumber = 9;
    final protected int yTab = 9;
    final protected int xTab = 1;
    protected int cellColumn = 1;
    protected boolean isEmpty = false;
    /**
     * Format : H-code
     */
    protected JsonObject tabx;
    protected JsonArray taby;
    protected ArrayList<ArrayList<Float>> priceTab;

    public TabHelper(Workbook wb, JsonObject instruction, String TabName) {
        this.wb = wb;
        this.tabx = new JsonObject();
        this.taby = new JsonArray();
        this.instruction = instruction;
        this.sheet = wb.getSheet(TabName);
        this.excel = new ExcelHelper(wb, sheet);
        priceTab = new ArrayList<ArrayList<Float>>();
    }


    public void create(Handler<Either<String, Boolean>> handler) {
    }

    public void getPrograms(Handler<Either<String, JsonArray>> handler) {
    }

    public void getPrices(Handler<Either<String, JsonArray>> handler) {
    }

    /**
     * Init all the tab
     *
     * @param i                   xInit
     * @param cellColumn          xMax
     * @param j                   yInit
     * @param operationsRowNumber yMax
     */
    protected void initTabValue(int i, int cellColumn, int j, int operationsRowNumber) {
    }

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

    /**
     * Insert prices into the tab
     */
    protected void setPrices() {
    }


}
