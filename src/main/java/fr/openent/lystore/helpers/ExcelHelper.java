package fr.openent.lystore.helpers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;

public class ExcelHelper {
    private Workbook wb;
    private Sheet sheet;
    public final CellStyle headCellStyle;
    private final CellStyle labelStyle;
    private final CellStyle tabStyle;

    public ExcelHelper(Workbook wb, Sheet sheet) {
        this.wb = wb;
        this.sheet = sheet;
        this.headCellStyle = wb.createCellStyle();
        this.labelStyle = wb.createCellStyle();
        this.tabStyle = wb.createCellStyle();
        initStyles();

    }

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

    public void insertHeader(Row row, int cellColumn, String data) {
        Cell cell = row.createCell(cellColumn);
        cell.setCellValue(data);
        cell.setCellStyle(this.headCellStyle);

    }

    public void insertLabel(Row row, int cellColumn, String data) {
        Cell cell = row.createCell(cellColumn);
        cell.setCellValue(data);
        cell.setCellStyle(this.labelStyle);
    }

    public void fillTab(int columnStart, int columnEnd, int lineStart, int lineEnd, Sheet sheet) {
        Row tab;
        Cell cell;
        for (int line = lineStart; line < lineEnd; line++) {
            tab = sheet.createRow(line);
            for (int column = columnStart; column < columnEnd; column++) {
                cell = tab.createCell(column);
                cell.setCellStyle(this.tabStyle);
            }
        }
    }
}
