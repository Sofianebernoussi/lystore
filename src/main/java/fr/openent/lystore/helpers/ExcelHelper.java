package fr.openent.lystore.helpers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.RegionUtil;

public class ExcelHelper {
    private Workbook wb;
    private Sheet sheet;
    public final CellStyle headCellStyle;
    private final CellStyle labelStyle;
    private final CellStyle tabStyle;
    private final CellStyle totalStyle;
    private DataFormat format;
    public static final String totalLabel = "Totaux";
    public ExcelHelper(Workbook wb, Sheet sheet) {
        this.wb = wb;
        this.sheet = sheet;
        this.headCellStyle = wb.createCellStyle();
        this.labelStyle = wb.createCellStyle();
        this.tabStyle = wb.createCellStyle();
        this.totalStyle = wb.createCellStyle();
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
        this.tabStyle.setBorderLeft(BorderStyle.THIN);
        this.tabStyle.setBorderRight(BorderStyle.THIN);
        this.tabStyle.setBorderTop(BorderStyle.THIN);
        this.tabStyle.setBorderBottom(BorderStyle.THIN);
        this.tabStyle.setWrapText(true);
        this.tabStyle.setAlignment(HorizontalAlignment.RIGHT);
        this.tabStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.tabStyle.setFont(tabFont);
        this.tabStyle.setDataFormat(format.getFormat("#,##0.00"));
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

    public void setRegionHeader(CellRangeAddress merge, Sheet sheet) {
        RegionUtil.setBorderTop(BorderStyle.THIN, merge, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, merge, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, merge, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, merge, sheet);

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

    /**
     * insert a cell in the tab
     *
     * @param cellColumn
     * @param line
     * @param data       data to insert
     */
    public void insertCellTabFloat(int cellColumn, int line, float data) {
        Row tab;
        tab = sheet.getRow(line);
        Cell cell = tab.createCell(cellColumn);
        cell.setCellValue(data);
        cell.setCellStyle(this.tabStyle);
    }

    /**
     * Set style for a tab
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
            tab = sheet.getRow(line);
            for (int column = columnStart; column < columnEnd; column++) {
                cell = tab.createCell(column);
                cell.setCellStyle(this.tabStyle);
            }
        }
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
            cell.setCellStyle(this.totalStyle);
            cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");


        }
        //totalX
        tab = sheet.getRow(lineEnd);

        for (int i = columnStart; i < columnEnd; i++) {
            cell = tab.createCell(i);
            cell.setCellStyle(this.totalStyle);
            cell.setCellValue("total");

            cellStartSum = tabStart.getCell(i);
            cellEndSum = tabEnd.getCell(i);


            cell.setCellStyle(this.totalStyle);
            cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");
        }
        cellStartSum = tabStart.getCell(columnEnd);
        cellEndSum = tabEnd.getCell(columnEnd);


        cell = tab.createCell(columnEnd);
        cell.setCellStyle(this.totalStyle);
        cell.setCellFormula("SUM(" + (new CellReference(cellStartSum)).formatAsString() + ":" + (new CellReference(cellEndSum)).formatAsString() + ")");

    }
}
