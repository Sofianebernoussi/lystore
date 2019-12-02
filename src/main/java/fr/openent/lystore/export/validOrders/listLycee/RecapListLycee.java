package fr.openent.lystore.export.validOrders.listLycee;

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
    private ArrayList<Integer> totalsXQty = new ArrayList<>();
    public RecapListLycee(Workbook workbook, String numberValidation) {
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
                }
            }
        });

    }

    private void writeArray(Handler<Either<String, Boolean>> handler) {
        setTitle();

        insertHeader();
        String oldIdStruct = "";
        int currentI = 2,initx = 2 ;
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
            excel.insertWithStyle(6,currentI, Integer.parseInt(data.getString("amount")),excel.tabStringStyleRight);
            typeEquipment = data.getString("typeequipment");
            Double priceAmount = Double.parseDouble(data.getString("price")) * Double.parseDouble(data.getString("amount"));
            if(typeEquipment.equals("EQUIPEMENT")) {
                excel.insertWithStyle(7,currentI,priceAmount ,excel.tabStringStyleRight);
            }else{
                excel.insertWithStyle(8,currentI,priceAmount,excel.tabStringStyleRight);
            }
            excel.insertWithStyle(10,currentI,priceAmount + (priceAmount * Double.parseDouble(data.getString("tax_amount"))/100),excel.tabStringStyleRight);
            oldUai = data.getString("uai");
            currentI ++;
        }
        //handle last struct
        excel.insertWithStyle(0,currentI ,"Total " + oldUai,excel.labelHeadStyle);
        inserTotal(initx,currentI);
        insertFinalTotal(currentI+2);
        excel.autoSize(20);
    }

    private void insertHeader() {
        String title ="MARCHE N°";
        excel.insertWithStyle(2,0,title,excel.blackOnBlueHeader);
        sizeMergeRegionWithStyle(0,2,5,excel.blackOnBlueHeader);
    }


    private void insertFinalTotal(int line) {
        String formulaQty = "";
        String formulaEquipmentHT = "";
        String formulaPrestaHT = "";
        String formulaTotalHT = "";
        String formulaTotalTTC = "";



        excel.insertWithStyle(0,line ,"Total général  ",excel.labelHeadStyle);

        for(int i = 0; i< totalsXQty.size(); i++){
            int x = totalsXQty.get(i);
            String qtyRef =  excel.getCellReference(x,6);
            String priceEquipmentHTRef =  excel.getCellReference(x,7);
            String pricePrestaHTRef =  excel.getCellReference(x,8);
            String priceHTCellRef =  excel.getCellReference(x,9);
            String priceTTCCellRef =  excel.getCellReference(x,10);

            qtyRef = qtyRef.replace("'Liste Commandes avec Prix'!", "");
            priceEquipmentHTRef = priceEquipmentHTRef.replace("'Liste Commandes avec Prix'!", "");
            pricePrestaHTRef = pricePrestaHTRef.replace("'Liste Commandes avec Prix'!", "");
            priceHTCellRef = priceHTCellRef.replace("'Liste Commandes avec Prix'!", "");
            priceTTCCellRef = priceTTCCellRef.replace("'Liste Commandes avec Prix'!", "");

            if (formulaTotalTTC.length() + priceTTCCellRef.length() < LIMIT_FORMULA_SIZE) {
                formulaQty += qtyRef + "+";
                formulaPrestaHT += pricePrestaHTRef + "+";
                formulaEquipmentHT += priceEquipmentHTRef + "+";
                formulaTotalHT += priceHTCellRef + "+";
                formulaTotalTTC += priceTTCCellRef + "+";

            } else { //Substring
                formulaQty = formulaQty.substring(0, formulaQty.length() - 1);
                excel.insertFormula( 40,1589 + i, formulaQty);
                formulaQty = excel.getCellReference(40,1589 + i ) + " +" + qtyRef +"+";

                formulaEquipmentHT = formulaEquipmentHT.substring(0, formulaEquipmentHT.length() - 1);
                excel.insertFormula( 41,1589 + i, formulaEquipmentHT);
                formulaEquipmentHT = excel.getCellReference(41,1589 + i ) + " +" + priceEquipmentHTRef +"+";

                formulaPrestaHT = formulaPrestaHT.substring(0, formulaPrestaHT.length() - 1);
                excel.insertFormula( 42,1589 + i, formulaPrestaHT);
                formulaPrestaHT = excel.getCellReference(42,1589 + i ) + " +" + pricePrestaHTRef +"+";

                formulaTotalHT = formulaTotalHT.substring(0, formulaTotalHT.length() - 1);
                excel.insertFormula( 43,1589 + i, formulaTotalHT);
                formulaTotalHT = excel.getCellReference(43,1589 + i ) + " +" + priceHTCellRef +"+";

                formulaTotalTTC = formulaTotalTTC.substring(0, formulaTotalTTC.length() - 1);
                excel.insertFormula( 44,1589 + i, formulaTotalTTC);
                formulaTotalTTC = excel.getCellReference(44,1589 + i ) + " +" + priceTTCCellRef +"+";
            }
        }
        formulaQty = formulaQty.substring(0, formulaQty.length() - 1);
        formulaEquipmentHT = formulaEquipmentHT.substring(0, formulaEquipmentHT.length() - 1);
        formulaPrestaHT = formulaPrestaHT.substring(0, formulaPrestaHT.length() - 1);
        formulaTotalHT = formulaTotalHT.substring(0, formulaTotalHT.length() - 1);
        formulaTotalTTC = formulaTotalTTC.substring(0, formulaTotalTTC.length() - 1);

        excel.insertFormulaWithStyle(line,6, formulaQty,excel.labelHeadStyle);
        excel.insertFormula(line,7, formulaEquipmentHT);
        excel.insertFormula(line,8, formulaPrestaHT);
        excel.insertFormula(line,9, formulaTotalHT);
        excel.insertFormula(line,10, formulaTotalTTC);

    }

    private int inserTotal(int initx, int currentI) {
        //faire le TTC
        excel.fillTab(6,10, initx,currentI );
        excel.setTotalXWithStyle(initx,currentI - 1,6,currentI,excel.totalStyle);
        excel.setTotal(initx, currentI , 7, 9,currentI,9,excel.tabCurrencyStyle);
        excel.setTotalX(initx,currentI-1,10,currentI);
        totalsXQty.add(currentI);
        return currentI + 1;
    }


    private void setTitle() {
        excel.insertWithStyle(1,1,"UAI",excel.yellowLabel);
        excel.insertWithStyle(2,1,"Nom de l'établissement",excel.yellowLabel);
        excel.insertWithStyle(3,1,"Commune",excel.yellowLabel);
        excel.insertWithStyle(4,1,"Tel",excel.yellowLabel);
        excel.insertWithStyle(5,1,"Equipement",excel.yellowLabel);
        excel.insertWithStyle(6,1,"Qté",excel.yellowLabel);
        // PRIX TOTAL EQUIPEMENT1T 	 PRIX TOTAL PRESTATION HT 	 PRIX TOTAL HT 	 PRIX TOTAL TTC 	DATE ARC	DATE LIMITE -LIVRAISON	DATE CSF	FACTURE	DATE FACTURE	FACTURE / RECAP	MONTANT FACTURE
        excel.insertWithStyle(7,1,"PRIX TOTAL FOURNITURE HT",excel.yellowLabel);
        excel.insertWithStyle(8,1,"PRIX TOTAL MISE EN SERVICE HT",excel.yellowLabel);
        excel.insertWithStyle(9,1,"PRIX TOTAL HT ",excel.yellowLabel);
        excel.insertWithStyle(10,1,"PRIX TOTAL TTC",excel.yellowLabel);
        excel.insertWithStyle(11,1,"DATE ARC",excel.yellowLabel);
        excel.insertWithStyle(12,1,"DATE LIMITE -LIVRAISON",excel.yellowLabel);
        excel.insertWithStyle(13,1,"DATE CSF",excel.yellowLabel);
        excel.insertWithStyle(14,1,"NUMERO FACTURE ",excel.yellowLabel);
        excel.insertWithStyle(15,1,"DATE FACTURE",excel.yellowLabel);
        excel.insertWithStyle(16,1,"MONTANT FACTURE",excel.yellowLabel);
        excel.insertWithStyle(17,1,"FACTURE / RECAP ",excel.yellowLabel);
        excel.insertWithStyle(18,1,"RETARD DE LIVRAISON ",excel.yellowLabel);
        excel.insertWithStyle(19,1,"PENALITE DE RETARD ",excel.yellowLabel);
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
                "       INNER JOIN "+ Lystore.lystoreSchema +".equipment_type et  " +
                "               ON oce.id_type = et.id  " +
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

