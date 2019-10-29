package fr.openent.lystore.export.validOrders;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;

public class RecapListLycee extends TabHelper {
    private String numberValidation;
    private String formula = "";
    private ArrayList<Integer> totalsX = new ArrayList<>();
    RecapListLycee(Workbook workbook, String numberValidation) {
        super(workbook,"Liste Commandes avec Prix");
        this.numberValidation = numberValidation;

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
            if(!structuresId.contains(data.getString("id_structure")))
                structuresId.add(structuresId.size(), data.getString("id_structure"));

        }
        getStructures(new JsonArray(structuresId), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> repStructures) {
//
                boolean errorCatch= false;
                if (repStructures.isRight()) {
                    try {
                        JsonArray structures = repStructures.right().getValue();
                        setStructuresFromDatas(structures);
                        if (datas.isEmpty()) {
                            handler.handle(new Either.Left<>("No data in database"));
                        } else {
                            datas = sortByCity(datas);
                            writeArray(handler);
                        }
                    }catch (Exception e){
                        errorCatch = true;
                    }
                    if(errorCatch)
                        handler.handle(new Either.Left<>("Error when writting files"));
                    else
                        handler.handle(new Either.Right<>(true));
                } else {
                    handler.handle(new Either.Left<>("Error when casting neo"));
//
                }
            }
//
//
        });

    }

    private void writeArray(Handler<Either<String, Boolean>> handler) {
        setTitle();

        String oldIdStruct = "";
        int currentI = 1,initx = 1 ;
        String typeEquipment;
        String oldUai = "";
        for(int i=0;i<datas.size();i++){
            JsonObject data = datas.getJsonObject(i);
            String idStruct = data.getString("id_structure");
            if (!oldIdStruct.equals(idStruct)){
                oldIdStruct = idStruct;
                if(i!=0){
                    initx = inserTotal(initx,currentI);
                    excel.insertWithStyle(0,currentI,"Total " + oldUai,excel.labelHeadStyle);
                    currentI ++;
                }
            }
            excel.insertCellTab(1,currentI,makeCellWithoutNull(data.getString("uai")));
            excel.insertCellTab(2,currentI,makeCellWithoutNull(data.getString("nameEtab")));
            excel.insertCellTab(3,currentI,makeCellWithoutNull(data.getString("city")));
            excel.insertCellTab(4,currentI,makeCellWithoutNull(data.getString("phone")));
            excel.insertCellTab(5,currentI,makeCellWithoutNull(data.getString("name")));
            excel.insertWithStyle(6,currentI, Integer.parseInt(data.getString("amount")),excel.totalStyle);

            typeEquipment = data.getString("typeequipment");

            if(typeEquipment.equals("EQUIPEMENT")) {
                excel.insertWithStyle(7,currentI, Double.parseDouble(data.getString("price")),excel.tabStringStyleRight);
            }else{
                logger.info(typeEquipment);
                excel.insertWithStyle(8,currentI, Double.parseDouble(data.getString("price")),excel.tabStringStyleRight);
            }
            oldUai = data.getString("uai");
            currentI ++;
        }

        //handle last struct
        excel.insertWithStyle(0,currentI ,"Total " + oldUai,excel.labelHeadStyle);
        inserTotal(initx,currentI);
        insertFinalTotal(currentI+1);
        excel.autoSize(18);
    }



    private void insertFinalTotal(int line) {
        for(int i=0;i<totalsX.size();i++){
            int x = totalsX.get(i);
            String cellRef =  excel.getCellReference(x,6);
            cellRef = cellRef.replace("'Liste Commandes avec Prix'!", "");

            if (formula.length() + cellRef.length() < LIMIT_FORMULA_SIZE) {
                formula += cellRef + "+";

            } else {
                formula = formula.substring(0, formula.length() - 1);
                excel.insertFormula( 40,1589 + i, formula);
                formula = excel.getCellReference(40,1589 + i ) + " +" + cellRef +"+";
            }
        }
        formula = formula.substring(0, formula.length() - 1);
        excel.insertFormulaWithStyle(line,6,formula,excel.totalStyle);
    }

    private int inserTotal(int initx, int currentI) {
        excel.setTotalXWithStyle(initx,currentI - 1,6,currentI,excel.totalStyle);
        totalsX.add(currentI);
        return currentI;
    }


    private void setTitle() {
        excel.insertWithStyle(1,0,"UAI",excel.yellowLabel);
        excel.insertWithStyle(2,0,"Nom de l'établissement",excel.yellowLabel);
        excel.insertWithStyle(3,0,"Commune",excel.yellowLabel);
        excel.insertWithStyle(4,0,"Tel",excel.yellowLabel);
        excel.insertWithStyle(5,0,"Equipment",excel.yellowLabel);
        excel.insertWithStyle(6,0,"Qté",excel.yellowLabel);
        // PRIX TOTAL EQUIPEMENT HT 	 PRIX TOTAL PRESTATION HT 	 PRIX TOTAL HT 	 PRIX TOTAL TTC 	DATE ARC	DATE LIMITE -LIVRAISON	DATE CSF	FACTURE	DATE FACTURE	FACTURE / RECAP	MONTANT FACTURE
        excel.insertWithStyle(7,0,"PRIX TOTAL EQUIPEMENT HT",excel.yellowLabel);
        excel.insertWithStyle(8,0,"PRIX TOTAL PRESTATION HT",excel.yellowLabel);
        excel.insertWithStyle(9,0,"PRIX TOTAL HT ",excel.yellowLabel);
        excel.insertWithStyle(10,0,"PRIX TOTAL TTC",excel.yellowLabel);
        excel.insertWithStyle(11,0,"DATE ARC",excel.yellowLabel);
        excel.insertWithStyle(12,0,"DATE LIMITE -LIVRAISON",excel.yellowLabel);
        excel.insertWithStyle(13,0,"DATE CSF",excel.yellowLabel);
        excel.insertWithStyle(14,0,"FACTURE ",excel.yellowLabel);
        excel.insertWithStyle(15,0,"DATE FACTURE",excel.yellowLabel);
        excel.insertWithStyle(16,0,"FACTURE / RECAP ",excel.yellowLabel);
        excel.insertWithStyle(17,0,"MONTANT FACTURE",excel.yellowLabel);
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "SELECT oce.price,  " +
                "       oce.tax_amount,  " +
                "       oce.NAME,  " +
                "       oce.id_contract,  " +
                "       Sum(oce.amount) AS amount,  " +
                "       oce.id_structure,  " +
                "       et.NAME  as typeequipment" +
                "   FROM   "+ Lystore.lystoreSchema +".order_client_equipment oce  " +
                "       LEFT JOIN "+ Lystore.lystoreSchema +".equipment  " +
                "              ON oce.equipment_key = equipment.id  " +
                "       INNER JOIN "+ Lystore.lystoreSchema +".equipment_type et  " +
                "               ON equipment.id_type = et.id  " +
                "WHERE  number_validation = ?  " +
                "GROUP  BY oce.equipment_key,  " +
                "          oce.price,  " +
                "          oce.tax_amount,  " +
                "          oce.NAME,  " +
                "          oce.id_contract,  " +
                "          oce.id_structure,  " +
                "          et.NAME  " +
                "UNION  " +
                "SELECT opt.price,  " +
                "       opt.tax_amount,  " +
                "       opt.NAME,  " +
                "       opt.id_contract,  " +
                "       Sum(opt.amount) AS amount,  " +
                "       opt.id_structure,  " +
                "       opt.typeequipment  " +
                "FROM   (SELECT options.price,  " +
                "               options.tax_amount,  " +
                "               options.NAME,  " +
                "               oce.id_contract,  " +
                "               oce.amount,  " +
                "               options.id_order_client_equipment,  " +
                "               oce.id_structure,  " +
                "               et.NAME AS typeequipment  " +
                "        FROM   "+ Lystore.lystoreSchema +".order_client_options options  " +
                "               INNER JOIN "+ Lystore.lystoreSchema +".order_client_equipment oce  " +
                "                       ON ( options.id_order_client_equipment = oce.id )  " +
                "               INNER JOIN "+ Lystore.lystoreSchema +".equipment_type et  " +
                "                       ON options.id_type = et.id  " +
                "        WHERE  number_validation = ? ) AS opt  " +
                "       INNER JOIN "+ Lystore.lystoreSchema +".order_client_equipment equipment  " +
                "               ON ( opt.id_order_client_equipment = equipment.id )  " +
                "GROUP  BY opt.NAME,  " +
                "          opt.price,  " +
                "          opt.tax_amount,  " +
                "          opt.id_contract,  " +
                "          opt.id_structure,  " +
                "          opt.typeequipment ";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(this.numberValidation).add(this.numberValidation);
        sqlHandler(handler,params);
    }
}

