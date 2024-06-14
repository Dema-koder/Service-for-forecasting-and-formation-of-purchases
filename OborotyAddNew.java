package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



class OborotyAddNew {
    private static final String HOST = "localhost";
    private static final int PORT = 27017;
    private static final String DATABASE_NAME = "stock_remainings";
    private static final String COLLECTION_NAME = "Складские остатки";

    // MongoDB client and collection objects
    private static MongoClient mongoClient;
    private static MongoCollection<Document> collection;

    public static void main(String[] args) {
        // Connect to MongoDB
        mongoClient = MongoClients.create("mongodb://" + HOST + ":" + PORT);
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        collection = database.getCollection(COLLECTION_NAME);

        // Example usage: process a file
        String filePath = "C:\\Users\\danil\\Desktop\\hackaton\\Service-for-forecasting-and-formation-of-purchases\\dataset\\Складские остатки\\Ведомость остатков на 30.06.2022г. (сч. 101).xlsx";
        File excelFile = new File(filePath);
        processFile(excelFile);
    }

    private static double getCellValue(Cell cell) {
        if (cell == null) {
            return Double.NaN;
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return Double.NaN;
                }
            default:
                return Double.NaN;
        }
    }

    private static boolean isNaN(double value) {
        return Double.isNaN(value);
    }


    private static String[] extractQuarterYear(String filename) {
        Pattern quarterPattern = Pattern.compile("(\\d+) кв\\.");
        Pattern yearPattern = Pattern.compile("кв\\. (\\d+)");
        Matcher quarterMatcher = quarterPattern.matcher(filename);
        Matcher yearMatcher = yearPattern.matcher(filename);
        String quarter = quarterMatcher.find() ? quarterMatcher.group(1) : null;
        String year = yearMatcher.find() ? yearMatcher.group(1) : null;
        return new String[]{quarter, year};
    }

    private static void insertDataToDb(Map<String, Object> data) {
        collection.insertOne(new Document(data));
    }

    private static void processCommonLogic(Sheet sheet, String quarter, String year, int group, int rowIndex, int nameIndex) {
        String subgroup = null;
        int index = rowIndex;
        while (index < sheet.getPhysicalNumberOfRows()) {
            Row row = sheet.getRow(index);
            Row nextRow = sheet.getRow(index + 1);
            if (row == null || row.getCell(0) == null) {
                break;
            }

            String cellValue = row.getCell(0).toString();
            if (cellValue.matches(group + "\\.\\d+")) {
                subgroup = cellValue;
            } else if ("Итого".equals(cellValue)) {
                break;
            } else if (subgroup != null && !cellValue.isEmpty()) {
                String name = row.getCell(nameIndex).toString();
                // Processing logic here.
                double countBeforeDebet = getCellValue(nextRow.getCell(10));
                double priceBeforeDebet = getCellValue(row.getCell(10));
                priceBeforeDebet = isNaN(countBeforeDebet) ? priceBeforeDebet : priceBeforeDebet / countBeforeDebet;

                double priceInDebet = getCellValue(row.getCell(12));
                double countInDebet = getCellValue(nextRow.getCell(12));
                priceInDebet = isNaN(countInDebet) ? priceInDebet : priceInDebet / countInDebet;

                double priceInKredit = getCellValue(row.getCell(13));
                double countInKredit = getCellValue(nextRow.getCell(13));
                priceInKredit = isNaN(countInKredit) ? priceInKredit : priceInKredit / countInKredit;

                double priceAfterDebet = getCellValue(row.getCell(14));
                double countAfterDebet = getCellValue(nextRow.getCell(14));
                priceAfterDebet = isNaN(countAfterDebet) ? priceAfterDebet : priceAfterDebet / countAfterDebet;

                // Insert data to DB
                Map<String, Object> data = new HashMap<>();
                // Populate data map with required fields
                data.put("name", normalizeName(name));
                data.put("группа", group);
                data.put("подгруппа", subgroup);
                data.put("квартал", quarter);
                data.put("год", year);
                // Insert to DB
                insertDataToDb(data);
                index += 3;
            }
            index++;
        }
    }

    private static void processFile(File file) {
        if (file.getName().contains("сч. 21")) {
            process21(file);
        } else if (file.getName().contains("сч. 105")) {
            process105(file);
        } else if (file.getName().contains("сч. 101")) {
            process101(file);
        } else {
            System.out.println("Skipping " + file.getName() + ", no matching function found");
        }
    }

    private static void process21(File file) {
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            String[] quarterYear = extractQuarterYear(file.getName());
            processCommonLogic(sheet, quarterYear[0], quarterYear[1], 21, 9, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void process105(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            String[] quarterYear = extractQuarterYear(file.getName());
            String quarter = quarterYear[0];
            String year = quarterYear[1];
            String subgroup = null;

            int i = 3;
            while (i <= sheet.getLastRowNum()) {
                Row row = sheet.getRow(i);

                if (row.getCell(0) == null) {
                    if(row.getCell(1) != null){
                        subgroup = row.getCell(1).getStringCellValue().split(" ")[0];
                    } else{
                        i++;
                        continue;
                    }

                } else if (row.getCell(3) != null && row.getCell(3).getStringCellValue().equals("Итого")) {
                    break;
                } else {
                    String name = row.getCell(3).getStringCellValue();
                    if(name.isEmpty()){
                        i++;
                        continue;
                    }
                    double countBeforeDebet = getCellValue(row.getCell(5));
                    double priceBeforeDebet = getCellValue(row.getCell(6));
                    priceBeforeDebet = Double.isNaN(countBeforeDebet) ? priceBeforeDebet : priceBeforeDebet / countBeforeDebet;

                    double priceInDebet = getCellValue(row.getCell(8));
                    double countInDebet = getCellValue(row.getCell(7));
                    priceInDebet = Double.isNaN(countInDebet) ? priceInDebet : priceInDebet / countInDebet;

                    double priceInKredit = getCellValue(row.getCell(10));
                    double countInKredit = getCellValue(row.getCell(9));
                    priceInKredit = Double.isNaN(countInKredit) ? priceInKredit : priceInKredit / countInKredit;

                    double priceAfterDebet = getCellValue(row.getCell(12));
                    double countAfterDebet = getCellValue(row.getCell(11));
                    priceAfterDebet = Double.isNaN(countAfterDebet) ? priceAfterDebet : priceAfterDebet / countAfterDebet;

                    Map<String, Object> data = new HashMap<>();
                    data.put("name", normalizeName(name));
                    data.put("цена до", priceBeforeDebet);
                    data.put("единицы до", countBeforeDebet);
                    data.put("цена во деб", priceInDebet);
                    data.put("единицы во деб", countInDebet);
                    data.put("цена во кред", priceInKredit);
                    data.put("единицы во кред", countInKredit);
                    data.put("цена после", priceAfterDebet);
                    data.put("единицы после", countAfterDebet);
                    data.put("группа", 105);
                    data.put("подгруппа", subgroup);
                    data.put("квартал", quarter);
                    data.put("год", year);

                    insertDataToDb(data);
                }
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void process101(File file) {
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            String[] quarterYear = extractQuarterYear(file.getName());
            processCommonLogic(sheet, quarterYear[0], quarterYear[1], 101, 9, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String normalizeName(String name) {
        // Implement normalization logic
        return name.replaceAll("\\s", "").toLowerCase();
    }
}