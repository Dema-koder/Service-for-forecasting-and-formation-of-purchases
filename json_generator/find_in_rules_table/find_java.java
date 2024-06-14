package org.example;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class CodeTranslator {
    public static void main(String[] args) throws IOException {
        // List of Excel file paths
        String[] filePaths = {
                "138_Применение ограничений допуска отдельных видов пищевых продуктов__     происходящих из иностранн....xls",
                "139_Применение ограничений допуска отдельных видов медицинских изделий_ происходящих из иностранных ....xls",
                "141_Применение запрета для товаров_ входящих в Перечень сельскохозяйственной продукции_ сырья и прод....xls",
                "142_Применение запрета на допуск промышленных товаров_ происходящих из иностранных государств_ за ис....xls",
                "143_Товары_ происходящие из иностранного государства или группы иностранных государств_ допускаемые ....xls",
                "144_Применение ограничений допуска отдельных видов промышленных товаров_ происходящих из иностранных....xls",
                "1_Преимущества организациям инвалидов (ст. 29 44-ФЗ_ РП РФ от 08.12.2021 N 3500-р).xls",
                "2_Перечень аукционной продукции (Распоряжение Правительства РФ от 21.03.2016 г. N 471-р).xls",
                "3_Преимущества учреждениям и предприятиям УИС (ст. 28 44-ФЗ_ РП РФ от 08.12.2021 № 3500-р).xls"
        };

        ArrayList<org.apache.poi.ss.usermodel.Sheet> dfs = new ArrayList<>();
        // Initialize an empty list to store the dataframes
        for (String file : filePaths) {
            try {
                Workbook workbook = WorkbookFactory.create(new File(file));
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    if (sheet.getPhysicalNumberOfRows() > 0) {
                        dfs.add(sheet);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        org.apache.poi.ss.usermodel.Workbook fullWorkbook = WorkbookFactory.create(true);
        org.apache.poi.ss.usermodel.Sheet fullSheet = fullWorkbook.createSheet("Output");
        int rowIndex = 0;
        for (Sheet sheet : dfs) {
            for (Row row : sheet) {
                Row newRow = fullSheet.createRow(rowIndex++);
                for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null) {
                        newRow.createCell(i).setCellValue(cell.getStringCellValue());
                    }
                }
            }
        }

        try {
            fullWorkbook.write(new java.io.FileOutputStream("output.xlsx"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String name = "Папка-уголок";
        String OKPD = findOKPD(name);
//        System.out.println(OKPD);
        boolean found = findInRules(OKPD, dfs);
        System.out.println(found);
    }

    public static String findOKPD(String name) {
        try {
            CSVParser parser = CSVFormat.DEFAULT.parse(new java.io.FileReader("СТЕ_ОКПД-2"));
            for (CSVRecord record : parser) {
//                System.out.println(record.get(2));
                if (record.get(1).equals(name)) {
//                  System.out.println(record.get(2));
                  return record.get(2);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "0";
    }

    public static boolean findInRules(String OKPD, ArrayList<Sheet> dfs) {
        boolean flag = false;
//        System.out.println(OKPD);
        for (Sheet sheet : dfs) {
            for (Row row : sheet) {
                Cell cell = row.getCell(1);
                if (cell != null && String.valueOf(OKPD).contains(cell.getStringCellValue())&& cell.getStringCellValue().length()>=5) {
//                    System.out.println(cell.getStringCellValue());
                    flag = true;
                    break;
                }
            }
        }
        return flag;
    }
}

