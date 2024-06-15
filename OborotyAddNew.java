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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public static void main(String[] args) throws IOException {
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

        Document queryFilter = new Document("name", data.get("name"))
                .append("год", data.get("год"))
                .append("квартал", data.get("квартал"));
        Document doc = collection.findOneAndDelete(queryFilter);
        List<String> to_sum = new ArrayList<>();
        to_sum.add("единиц после");
        to_sum.add("единиц кредит во");
        to_sum.add("единиц дебет во");
        to_sum.add("единиц до");
        List<String> to_substitude = new ArrayList<>();
        to_substitude.add("цена после");
        to_substitude.add("цена кредит во");
        to_substitude.add("цена дебет во");
        to_substitude.add("цена до");
        if (doc!=null){
            for(String field : to_sum){
                data.put(field, Double.parseDouble(data.get(field).toString() )+ Double.parseDouble(doc.get(field).toString()));
            }
            for(String field: to_substitude){
                if(data.get(field) == null){
                    data.put(field, doc.get(field));
                }
            }
        }

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
                data.put("единиц до", countBeforeDebet);
                data.put("цена до", priceBeforeDebet);
                data.put("цена дебет во", priceInDebet);
                data.put("единиц дебет во", countInDebet);
                data.put("цена кредит во", priceInKredit);
                data.put("единиц кредит во", countInKredit);
                data.put("цена после", priceAfterDebet);
                data.put("единиц после", countAfterDebet);

                // Insert to DB
                insertDataToDb(data);
                index += 3;
            }
            index++;
        }
    }

    private static void processFile(File excelFile) throws IOException {
        FileInputStream fis = new FileInputStream(excelFile);
        Workbook workbook = new XSSFWorkbook(fis); // Load Excel workbook
        Sheet sheet = workbook.getSheetAt(0); // Assuming there's only one sheet

        String filename = excelFile.getName();
        if (filename.contains("сч. 21")) {
            process21(sheet, filename);
        } else if (filename.contains("сч. 105")) {
            process105(sheet, filename);
        } else if (filename.contains("сч. 101")) {
            process101(sheet, filename);
        } else {
            System.out.println("Skipping " + filename + ", no matching function found");
        }
    }

    private static void process21(Sheet sheet, String filename) {
        String[] quarterYear = extractQuarterYear(filename);
        processCommonLogic(sheet, quarterYear[0], quarterYear[1], 21, 9, 0);
    }

    private static void process105(Sheet sheet, String filename) {
        String[] quarterYear = extractQuarterYear(filename);
        String quarter = quarterYear[0];
        String year = quarterYear[1];
        String subgroup = null;

        int i = 2;
        while (i <= sheet.getLastRowNum()) {
            Row row = sheet.getRow(i);
            if (row.getCell(0) == null || row.getCell(0).toString().isEmpty()) {

                if(row.getCell(1) != null && !row.getCell(1).getStringCellValue().isEmpty()){
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
                data.put("единиц до", countBeforeDebet);
                data.put("цена до", priceBeforeDebet);
                data.put("цена дебет во", priceInDebet);
                data.put("единиц дебет во", countInDebet);
                data.put("цена кредит во", priceInKredit);
                data.put("единиц кредит во", countInKredit);
                data.put("цена после", priceAfterDebet);
                data.put("единиц после", countAfterDebet);
                data.put("группа", 105);
                data.put("подгруппа", subgroup);
                data.put("квартал", quarter);
                data.put("год", year);

                insertDataToDb(data);
            }
            i++;
        }
    }

    private static void process101(Sheet sheet, String filename) {
        String[] quarterYear = extractQuarterYear(filename);
        processCommonLogic(sheet, quarterYear[0], quarterYear[1], 101, 9, 0);
    }


    private static String normalizeName(String name) {
        // Implement normalization logic
        return name.replaceAll("\\s", "").toLowerCase();
    }
}