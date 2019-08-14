package fr.openent.lystore.helpers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.RegionUtil;

public class ExcelHelper {
    private Workbook wb;
    private Sheet sheet;
    public final CellStyle headCellStyle;
    public final CellStyle labelStyle;
    public final CellStyle tabNumeralStyle;
    public final CellStyle tabStringStyleCenter;
    public final CellStyle tabStringStyle;
    public final CellStyle totalStyle;
    public final CellStyle labelHeadStyle;
    public final CellStyle currencyStyle;
    public final CellStyle tabCurrencyStyle;
    public final CellStyle titleHeaderStyle;
    public final CellStyle yellowHeader;
    public final CellStyle underscoreHeader;
    public final CellStyle yellowLabel;
    public final CellStyle blackTitleHeaderStyle;
    public final CellStyle blueTitleHeaderStyle;
    public final CellStyle tabStringStyleRight;
    public final CellStyle floatOnYellowStyle;
    public final CellStyle whiteOnBlueLabel;

    private DataFormat format;
    public static final String totalLabel = "Total";
    public static final String sumLabel = "Somme";

    public ExcelHelper(Workbook wb, Sheet sheet) {
        this.wb = wb;
        this.sheet = sheet;
        this.headCellStyle = wb.createCellStyle();
        this.labelStyle = wb.createCellStyle();
        this.tabNumeralStyle = wb.createCellStyle();
        this.tabStringStyle = wb.createCellStyle();
        this.tabCurrencyStyle = wb.createCellStyle();
        this.currencyStyle = wb.createCellStyle();
        this.tabStringStyleCenter = wb.createCellStyle();
        this.totalStyle = wb.createCellStyle();
        this.yellowHeader = wb.createCellStyle();
        this.yellowLabel = wb.createCellStyle();
        this.underscoreHeader = wb.createCellStyle();
        this.blackTitleHeaderStyle = wb.createCellStyle();
        this.labelHeadStyle = wb.createCellStyle();
        this.titleHeaderStyle = wb.createCellStyle();
        this.blueTitleHeaderStyle = wb.createCellStyle();
        this.tabStringStyleRight = wb.createCellStyle();
        this.floatOnYellowStyle = wb.createCellStyle();
        this.whiteOnBlueLabel = wb.createCellStyle();

        format = wb.createDataFormat();
        format.getFormat("#.#");

        this.initStyles();

    }

    
    /**
     * Init all the stles of the sheet
     */
    private void initStyles() {
        //INIT HEader style
        Font headerFont = this.wb.createFont();
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setFontName("Calibri");
        headerFont.setBold(true);
        this.headCellStyle.setBorderLeft(BorderStyle.THIN);
        this.headCellStyle.setBorderRight(BorderStyle.THIN);
        this.headCellStyle.setBorderTop(BorderStyle.THIN);
        this.headCellStyle.setBorderBottom(BorderStyle.THIN);
        this.headCellStyle.setWrapText(true);
        this.headCellStyle.setAlignment(HorizontalAlignment.CENTER);
        this.headCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.headCellStyle.setFont(headerFont);

        //init LabelStyle
        Font labelFont = this.wb.createFont();
        labelFont.setFontHeightInPoints((short) 11);
        labelFont.setFontName("Calibri");
        labelFont.setBold(false);
        this.labelStyle.setBorderLeft(BorderStyle.THIN);
        this.labelStyle.setBorderRight(BorderStyle.THIN);
        this.labelStyle.setBorderTop(BorderStyle.THIN);
        this.labelStyle.setBorderBottom(BorderStyle.THIN);
        this.labelStyle.setWrapText(true);
        this.labelStyle.setAlignment(HorizontalAlignment.LEFT);
        this.labelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.labelStyle.setFont(labelFont);

        //TotalStyle
        Font totalFont = this.wb.createFont();
        totalFont.setFontHeightInPoints((short) 11);
        totalFont.setFontName("Calibri");
        totalFont.setBold(true);

        this.totalStyle.setBorderLeft(BorderStyle.THIN);
        this.totalStyle.setBorderRight(BorderStyle.THIN);
        this.totalStyle.setBorderTop(BorderStyle.THIN);
        this.totalStyle.setBorderBottom(BorderStyle.THIN);
        this.totalStyle.setWrapText(true);
        this.totalStyle.setAlignment(HorizontalAlignment.RIGHT);
        this.totalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.totalStyle.setFont(totalFont);
        this.totalStyle.setDataFormat(format.getFormat("#,##0.00"));

        //TabStyle
        Font tabFont = this.wb.createFont();
        tabFont.setFontHeightInPoints((short) 11);
        tabFont.setFontName("Calibri");
        tabFont.setBold(false);
        this.tabNumeralStyle.setBorderLeft(BorderStyle.THIN);
        this.tabNumeralStyle.setBorderRight(BorderStyle.THIN);
        this.tabNumeralStyle.setBorderTop(BorderStyle.THIN);
        this.tabNumeralStyle.setBorderBottom(BorderStyle.THIN);
        this.tabNumeralStyle.setWrapText(true);
        this.tabNumeralStyle.setAlignment(HorizontalAlignment.RIGHT);
        this.tabNumeralStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabNumeralStyle.setFont(tabFont);
        this.tabNumeralStyle.setDataFormat(format.getFormat("#,##0.00"));
        //TabStyle

        this.tabStringStyle.setBorderLeft(BorderStyle.THIN);
        this.tabStringStyle.setBorderRight(BorderStyle.THIN);
        this.tabStringStyle.setBorderTop(BorderStyle.THIN);
        this.tabStringStyle.setBorderBottom(BorderStyle.THIN);
        this.tabStringStyle.setWrapText(true);
        this.tabStringStyle.setAlignment(HorizontalAlignment.LEFT);
        this.tabStringStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabStringStyle.setFont(tabFont);
        this.tabStringStyle.setDataFormat(format.getFormat("#,##0.00"));

        this.tabStringStyleCenter.setBorderLeft(BorderStyle.THIN);
        this.tabStringStyleCenter.setBorderRight(BorderStyle.THIN);
        this.tabStringStyleCenter.setBorderTop(BorderStyle.THIN);
        this.tabStringStyleCenter.setBorderBottom(BorderStyle.THIN);
        this.tabStringStyleCenter.setWrapText(true);
        this.tabStringStyleCenter.setAlignment(HorizontalAlignment.CENTER);
        this.tabStringStyleCenter.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabStringStyleCenter.setFont(tabFont);
        this.tabStringStyleCenter.setDataFormat(format.getFormat("#,##0.00"));


        this.tabStringStyleRight.setBorderLeft(BorderStyle.THIN);
        this.tabStringStyleRight.setBorderRight(BorderStyle.THIN);
        this.tabStringStyleRight.setBorderTop(BorderStyle.THIN);
        this.tabStringStyleRight.setBorderBottom(BorderStyle.THIN);
        this.tabStringStyleRight.setWrapText(true);
        this.tabStringStyleRight.setAlignment(HorizontalAlignment.RIGHT);
        this.tabStringStyleRight.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabStringStyleRight.setFont(tabFont);
        this.tabStringStyleRight.setDataFormat(format.getFormat("#,##0.00"));

        //LabelHeadStyle
        Font labelHeadFont = this.wb.createFont();
        labelHeadFont.setFontHeightInPoints((short) 12);
        labelHeadFont.setFontName("Calibri");
        labelHeadFont.setBold(true);
        this.labelHeadStyle.setWrapText(true);
        this.labelHeadStyle.setAlignment(HorizontalAlignment.LEFT);
        this.labelHeadStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.labelHeadStyle.setFont(labelHeadFont);

        this.currencyStyle.setWrapText(true);
        this.currencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        this.currencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.currencyStyle.setFont(labelHeadFont);
        this.currencyStyle.setDataFormat(format.getFormat("#,##0.00 €"));

        this.tabCurrencyStyle.setWrapText(true);
        this.tabCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        this.tabCurrencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabCurrencyStyle.setBorderLeft(BorderStyle.THIN);
        this.tabCurrencyStyle.setBorderRight(BorderStyle.THIN);
        this.tabCurrencyStyle.setBorderTop(BorderStyle.THIN);
        this.tabCurrencyStyle.setBorderBottom(BorderStyle.THIN);
        this.tabCurrencyStyle.setFont(totalFont);
        this.tabCurrencyStyle.setDataFormat(format.getFormat("#,##0.00 €"));

        //init LabelStyle
        Font titleHeadFont = this.wb.createFont();
        titleHeadFont.setFontHeightInPoints((short) 14);
        titleHeadFont.setFontName("Calibri");
        titleHeadFont.setBold(true);
        this.titleHeaderStyle.setWrapText(true);
        this.titleHeaderStyle.setFillForegroundColor(IndexedColors.TURQUOISE.getIndex());
        this.titleHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.titleHeaderStyle.setBorderLeft(BorderStyle.THIN);
        this.titleHeaderStyle.setBorderRight(BorderStyle.THIN);
        this.titleHeaderStyle.setBorderTop(BorderStyle.THIN);
        this.titleHeaderStyle.setBorderBottom(BorderStyle.THIN);
        this.titleHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
        this.titleHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.titleHeaderStyle.setFont(titleHeadFont);

        this.blackTitleHeaderStyle.setWrapText(true);
        this.blackTitleHeaderStyle.setBorderLeft(BorderStyle.THIN);
        this.blackTitleHeaderStyle.setBorderRight(BorderStyle.THIN);
        this.blackTitleHeaderStyle.setBorderTop(BorderStyle.THIN);
        this.blackTitleHeaderStyle.setBorderBottom(BorderStyle.THIN);
        this.blackTitleHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        this.blackTitleHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.blackTitleHeaderStyle.setFont(titleHeadFont);

        Font blueTitleHeadFont = this.wb.createFont();
        blueTitleHeadFont.setFontHeightInPoints((short) 14);
        blueTitleHeadFont.setFontName("Calibri");
        blueTitleHeadFont.setBold(true);
        blueTitleHeadFont.setColor(IndexedColors.BLUE.getIndex());
        this.blueTitleHeaderStyle.setWrapText(true);
        this.blueTitleHeaderStyle.setBorderLeft(BorderStyle.THIN);
        this.blueTitleHeaderStyle.setBorderRight(BorderStyle.THIN);
        this.blueTitleHeaderStyle.setBorderTop(BorderStyle.THIN);
        this.blueTitleHeaderStyle.setBorderBottom(BorderStyle.THIN);
        this.blueTitleHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        this.blueTitleHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.blueTitleHeaderStyle.setFont(blueTitleHeadFont);


        //init LabelStyle
        this.yellowHeader.setWrapText(true);
        this.yellowHeader.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        this.yellowHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.yellowHeader.setBorderLeft(BorderStyle.THIN);
        this.yellowHeader.setBorderRight(BorderStyle.THIN);
        this.yellowHeader.setBorderTop(BorderStyle.THIN);
        this.yellowHeader.setBorderBottom(BorderStyle.THIN);
        this.yellowHeader.setAlignment(HorizontalAlignment.LEFT);
        this.yellowHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        this.yellowHeader.setFont(titleHeadFont);

        this.underscoreHeader.setWrapText(true);
        this.underscoreHeader.setBorderLeft(BorderStyle.NONE);
        this.underscoreHeader.setBorderTop(BorderStyle.NONE);
        this.underscoreHeader.setBorderRight(BorderStyle.NONE);
        this.underscoreHeader.setBorderBottom(BorderStyle.THIN);
        this.underscoreHeader.setAlignment(HorizontalAlignment.LEFT);
        this.underscoreHeader.setVerticalAlignment(VerticalAlignment.CENTER);
        this.underscoreHeader.setFont(titleHeadFont);

        this.yellowLabel.setWrapText(true);
        this.yellowLabel.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        this.yellowLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.yellowLabel.setBorderLeft(BorderStyle.THIN);
        this.yellowLabel.setBorderRight(BorderStyle.THIN);
        this.yellowLabel.setBorderTop(BorderStyle.THIN);
        this.yellowLabel.setBorderBottom(BorderStyle.THIN);
        this.yellowLabel.setAlignment(HorizontalAlignment.CENTER);
        this.yellowLabel.setVerticalAlignment(VerticalAlignment.CENTER);
        this.yellowLabel.setFont(labelHeadFont);

        this.floatOnYellowStyle.setWrapText(true);
        this.floatOnYellowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        this.floatOnYellowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.floatOnYellowStyle.setBorderLeft(BorderStyle.THIN);
        this.floatOnYellowStyle.setBorderRight(BorderStyle.THIN);
        this.floatOnYellowStyle.setBorderTop(BorderStyle.THIN);
        this.floatOnYellowStyle.setBorderBottom(BorderStyle.THIN);
        this.floatOnYellowStyle.setAlignment(HorizontalAlignment.RIGHT);
        this.floatOnYellowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.floatOnYellowStyle.setFont(tabFont);
        this.floatOnYellowStyle.setDataFormat(format.getFormat("#,##0.00"));


        Font whiteTabFont = this.wb.createFont();
        whiteTabFont.setFontHeightInPoints((short) 12);
        whiteTabFont.setFontName("Calibri");
        whiteTabFont.setBold(false);
        whiteTabFont.setColor(IndexedColors.WHITE.getIndex());
        this.whiteOnBlueLabel.setWrapText(true);
        this.whiteOnBlueLabel.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        this.whiteOnBlueLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.whiteOnBlueLabel.setBorderLeft(BorderStyle.THIN);
        this.whiteOnBlueLabel.setBorderRight(BorderStyle.THIN);
        this.whiteOnBlueLabel.setBorderTop(BorderStyle.THIN);
        this.whiteOnBlueLabel.setBorderBottom(BorderStyle.THIN);
        this.whiteOnBlueLabel.setAlignment(HorizontalAlignment.LEFT);
        this.whiteOnBlueLabel.setVerticalAlignment(VerticalAlignment.CENTER);
        this.whiteOnBlueLabel.setFont(whiteTabFont);

    }
    public void setBold(Cell cell) {
        Font font = wb.createFont();
        font.setBold(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        cell.setCellStyle(style);
    }

    public void setDefaultFont() {
        Font defaultFont = wb.createFont();
        defaultFont.setColor(IndexedColors.BLACK.getIndex());
        defaultFont.setBold(false);
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        CellStyle style = cell.getCellStyle();
        style.setFont(defaultFont);
        cell.setCellStyle(style);
    }

    public void setTitle(String title) {
        Row row = sheet.createRow(2);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        this.setBold(cell);
    }

    public void setSubTitle(String subtitle) {
        Row row = sheet.createRow(4);
        Cell cell = row.createCell(0);
        cell.setCellValue(subtitle);
        this.setBold(cell);
    }

    public void setCPNumber(String number) {
        Row row = sheet.createRow(1);
        Cell cell = row.createCell(0);
        cell.setCellValue("N° CP " + number);
        this.setBold(cell);
    }

    /**
     * set at a particular line and column
     *
     * @param number
     * @param line
     * @param column
     */
    public void setCPNumber(String number, int line, int column) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(column);
            cell.setCellValue("N° CP " + number);
            this.setBold(cell);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(column);
            cell.setCellValue("N° CP " + number);
            this.setBold(cell);
        }
    }

    /**
     * Adding borders to a merged region
     *
     * @param merge
     * @param sheet
     */
    public void setRegionHeader(CellRangeAddress merge, Sheet sheet) {
        RegionUtil.setBorderTop(BorderStyle.THIN, merge, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, merge, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, merge, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, merge, sheet);

    }

    public void setRegionUnderscoreHeader(CellRangeAddress merge, Sheet sheet) {
        RegionUtil.setBorderBottom(BorderStyle.THIN, merge, sheet);
    }
    /**
     * insert Header
     *
     * @param row
     * @param cellColumn
     * @param data
     */
    public void insertHeader(Row row, int cellColumn, String data) {
        Cell cell = row.createCell(cellColumn);
        cell.setCellValue(data);
        cell.setCellStyle(this.headCellStyle);
        row.setHeight((short) -1);

    }

    /**
     * insert a cell with label style
     *
     * @param row
     * @param cellColumn
     * @param data       data to insert
     */
    public void insertLabel(Row row, int cellColumn, String data) {
        Cell cell = row.createCell(cellColumn);
        cell.setCellValue(data);
        cell.setCellStyle(this.labelStyle);
    }




    public void insertFormula(Row row, int cellColumn, String data) {
        Cell cell = row.createCell(cellColumn);
        cell.setCellFormula(data);
        cell.setCellStyle(this.currencyStyle);
    }

    /**
     * insert a cell with float in the tab
     *
     * @param cellColumn
     * @param line
     * @param data       data to insert
     */
    public void insertCellTabFloat(int cellColumn, int line, float data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.tabNumeralStyle);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.tabNumeralStyle);
        }
    }

    public void insertCellTabStringRight(int cellColumn, int line, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.tabStringStyleRight);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.tabStringStyleRight);
        }
    }

    /**
     * insert a label in a tab at line,column
     *
     * @param line
     * @param cellColumn
     * @param data
     */
    public void insertLabel(int line, int cellColumn, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.labelStyle);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.labelStyle);
        }
    }

    public void insertLabelHead(int line, int cellColumn, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.labelHeadStyle);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.labelHeadStyle);
        }
    }

    public void insertHeader(int line, int cellColumn, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.headCellStyle);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.headCellStyle);
        }


    }

    public void insertFloatYellow(int line, int cellColumn, Float data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.floatOnYellowStyle);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.floatOnYellowStyle);
        }


    }

    /**
     * Insert a price in a array
     *
     * @param cellColumn
     * @param line
     * @param data
     */
    public void insertCellTabFloatWithPrice(int cellColumn, int line, float data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.tabCurrencyStyle);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.tabCurrencyStyle);
        }
    }
    /**
     * insert a cell in the tab
     *
     * @param cellColumn
     * @param line
     * @param data
     */
    public void insertCellTab(int cellColumn, int line, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.tabStringStyle);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.tabStringStyle);
        }

    }

    /**
     * insert a cell with an int in the tab
     *
     * @param cellColumn
     * @param line
     * @param data
     */
    public void insertCellTabInt(int cellColumn, int line, int data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.tabNumeralStyle);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.tabNumeralStyle);
        }

    }

    /**
     * insert a header with yellow background
     *
     * @param line
     * @param cellColumn
     * @param data
     */
    public void insertYellowHeader(int line, int cellColumn, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.yellowHeader);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.yellowHeader);
        }

    }

    /**
     * insert a label with yellow background
     *
     * @param line
     * @param cellColumn
     * @param data
     */
    public void insertYellowLabel(int line, int cellColumn, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.yellowLabel);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.yellowLabel);
        }
    }


    /**
     * insert a cell in a tab ith blue background and white font
     *
     * @param line
     * @param cellColumn
     * @param data
     */
    public void insertWhiteOnBlueTab(int line, int cellColumn, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.whiteOnBlueLabel);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.whiteOnBlueLabel);
        }
    }

    /**
     * insert a header with blue background
     *
     * @param line
     * @param cellColumn
     * @param data
     */
    public void insertTitleHeader(int cellColumn, int line, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.titleHeaderStyle);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.titleHeaderStyle);
        }

    }

    /**
     * insert a header with black police
     *
     * @param line
     * @param cellColumn
     * @param data
     */
    public void insertBlackTitleHeader(int cellColumn, int line, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.blackTitleHeaderStyle);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.blackTitleHeaderStyle);
        }

    }

    /**
     * insert a header with blue police
     *
     * @param line
     * @param cellColumn
     * @param data
     */
    public void insertBlueTitleHeader(int cellColumn, int line, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.blueTitleHeaderStyle);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.blueTitleHeaderStyle);
        }

    }

    /**
     * insert a data in an array wich will be centered in the cell
     *
     * @param cellColumn
     * @param line
     * @param data
     */
    public void insertCellTabCenter(int cellColumn, int line, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.tabStringStyleCenter);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.tabStringStyleCenter);
        }

    }

    /**
     * insert an header with an underscore
     *
     * @param cellColumn
     * @param cellColumn
     * @param data
     */
    public void insertUnderscoreHeader(int cellColumn, int line, String data) {
        Row tab;
        try {
            tab = sheet.getRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.underscoreHeader);
        } catch (NullPointerException e) {
            tab = sheet.createRow(line);
            Cell cell = tab.createCell(cellColumn);
            cell.setCellValue(data);
            cell.setCellStyle(this.underscoreHeader);
        }
    }

    /**
     * Set default style for a tab and init all non init cells of the tab
     *
     * @param columnStart
     * @param columnEnd
     * @param lineStart
     * @param lineEnd
     */
    public void fillTab(int columnStart, int columnEnd, int lineStart, int lineEnd) {
        Row tab;
        Cell cell;
        for (int line = lineStart; line < lineEnd; line++) {
            try {
                tab = sheet.getRow(line);

                for (int column = columnStart; column < columnEnd; column++) {
                    try {
                        cell = tab.getCell(column);
//                        cell.setCellStyle(this.tabNumeralStyle);
                    } catch (NullPointerException e) {
                        cell = tab.createCell(column);
                        cell.setCellStyle(this.tabNumeralStyle);
                    }
                }
            } catch (NullPointerException e) {
                tab = sheet.createRow(line);
                for (int column = columnStart; column < columnEnd; column++) {
                    try {
                        cell = tab.getCell(column);
                        cell.setCellStyle(this.tabNumeralStyle);
                    } catch (NullPointerException ee) {
                        cell = tab.createCell(column);
                        cell.setCellStyle(this.tabNumeralStyle);
                    }
                }
            }
        }
    }


    /**
     * Set specific style for a tab and init all non init cells of the tab
     *
     * @param columnStart
     * @param columnEnd
     * @param lineStart
     * @param lineEnd
     * @param style       style to insert
     */
    public void fillTabWithStyle(int columnStart, int columnEnd, int lineStart, int lineEnd, CellStyle style) {
        Row tab;
        Cell cell;
        for (int line = lineStart; line < lineEnd; line++) {
            try {
                tab = sheet.getRow(line);

                for (int column = columnStart; column < columnEnd; column++) {
                    try {
                        cell = tab.getCell(column);
                        cell.setCellStyle(style);
                    } catch (NullPointerException e) {
                        cell = tab.createCell(column);
                        cell.setCellStyle(style);
                    }
                }
            } catch (NullPointerException e) {
                tab = sheet.createRow(line);
                for (int column = columnStart; column < columnEnd; column++) {
                    try {
                        cell = tab.getCell(column);
                        cell.setCellStyle(style);
                    } catch (NullPointerException ee) {
                        cell = tab.createCell(column);
                        cell.setCellStyle(style);
                    }
                }
            }
        }
    }

    /**
     * set total of a column
     *
     * @param lineStart  start of the column
     * @param lineEnd    end of the column
     * @param column     number of the column
     * @param lineInsert line where the total is insert
     */
    public void setTotalX(int lineStart, int lineEnd, int column, int lineInsert) {
        Row tab, tabStart, tabEnd;

        tabStart = sheet.getRow(lineStart);
        tabEnd = sheet.getRow(lineEnd);
        Cell cell, cellStartSum, cellEndSum;
        tab = sheet.getRow(lineInsert);
        cell = tab.createCell(column);
        cell.setCellStyle(this.tabCurrencyStyle);
        cell.setCellValue("total");
        cellStartSum = tabStart.getCell(column);
        cellEndSum = tabEnd.getCell(column);
        cell.setCellStyle(this.tabCurrencyStyle);
        cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");

    }

    /**
     * Set total of a line
     *
     * @param columnStart  start of the line
     * @param columnEnd    end of the line
     * @param line         number of the line to make total
     * @param columnInsert column where to insert the total
     */
    public void setTotalY(int columnStart, int columnEnd, int line, int columnInsert) {
        Row tab, tabStart, tabEnd;
        tab = sheet.getRow(line);
        Cell cell, cellStartSum, cellEndSum;
        cell = tab.createCell(columnInsert);
        cellStartSum = tab.getCell(columnStart);
        cellEndSum = tab.getCell(columnEnd);
        cell.setCellStyle(this.tabCurrencyStyle);
        cell.setCellValue("total");
        cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");

    }


    /**
     * Set total column and  line of a tab
     *
     * @param columnEnd
     * @param lineEnd
     * @param columnStart
     * @param lineStart
     */
    public void setTotal(int columnEnd, int lineEnd, int columnStart, int lineStart) {
        Row tab, tabStart, tabEnd;
        Cell cell, cellStartSum, cellEndSum;
        // totalY
        tabStart = sheet.getRow(lineStart);
        tabEnd = sheet.getRow(lineEnd - 1);

        for (int i = lineStart; i < lineEnd; i++) {
            tab = sheet.getRow(i);
            cell = tab.createCell(columnEnd);
            cellStartSum = tab.getCell(columnStart);
            cellEndSum = tab.getCell(columnEnd - 1);
            cell.setCellStyle(this.tabCurrencyStyle);
            cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");
        }
        //totalX
        tab = sheet.getRow(lineEnd);

        for (int i = columnStart; i < columnEnd; i++) {
            cell = tab.createCell(i);
            cell.setCellStyle(this.tabCurrencyStyle);
            cell.setCellValue("total");

            cellStartSum = tabStart.getCell(i);
            cellEndSum = tabEnd.getCell(i);


            cell.setCellStyle(this.tabCurrencyStyle);
            cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");
        }
        cellStartSum = tabStart.getCell(columnEnd);
        cellEndSum = tabEnd.getCell(columnEnd);


        cell = tab.createCell(columnEnd);
        cell.setCellStyle(this.tabCurrencyStyle);
        cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");

    }


    /**
     * Get an excel address of a cell
     *
     * @param line
     * @param column
     * @return
     */
    public String getCellReference(int line, int column) {
        Row tab;
        Cell cell;
        tab = sheet.getRow(line);
        cell = tab.getCell(column);
        return new CellReference(cell).formatAsString();
    }


    /**
     * autosize the number of  columns of the page
     *
     * @param arrayLength number of columns to autosize
     */
    public void autoSize(int arrayLength) {
        for (int i = 0; i < arrayLength; i++) {
            sheet.autoSizeColumn(i);
        }
    }

}
