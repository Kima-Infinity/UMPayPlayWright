package com.umpay.utility;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ExcelDataProvider {

    XSSFWorkbook wb;

    public ExcelDataProvider(String fileName, String sheetName) {
        this(fileName);
    }

    public ExcelDataProvider(String fileName) {

        if (!fileName.endsWith(".xlsx")) {
            fileName = fileName + ".xlsx";
        }
        File src = new File("./TestData/" + fileName);

        try {
            FileInputStream fis = new FileInputStream(src);

            wb = new XSSFWorkbook(fis);
        } catch (IOException e) {
            System.out.println("Error in reading excel file" +e.getMessage());
        }
    }

    public String getStringData(String sheetName, int row, int col) {
        org.apache.poi.ss.usermodel.Cell cell = wb.getSheet(sheetName).getRow(row).getCell(col);
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {

            double value = cell.getNumericCellValue();

            /*
             * Account and phone numbers run past the range of an int, and casting
             * one clamps it to 2147483647 without any error. Whole numbers go
             * through long, and anything with a fraction is printed in plain
             * notation so an amount never arrives as 6.5E10.
             */
            if (value == Math.rint(value) && !Double.isInfinite(value)) {
                return String.valueOf((long) value);
            }
            return new java.math.BigDecimal(Double.toString(value)).stripTrailingZeros().toPlainString();
        }
        return cell.getStringCellValue();
    }

    public double getNumericData(String sheetName, int row, int col) {
        return wb.getSheet(sheetName).getRow(row).getCell(col).getNumericCellValue();
    }
}
