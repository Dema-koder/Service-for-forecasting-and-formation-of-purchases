package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ProcessStockRemainings {

    // MongoDB connection parameters
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

    // Method to process an Excel file
    public static void processFile(File excelFile) {
        try {
            FileInputStream fis = new FileInputStream(excelFile);
            Workbook workbook = new XSSFWorkbook(fis); // Load Excel workbook
            Sheet sheet = workbook.getSheetAt(0); // Assuming there's only one sheet

            String filename = excelFile.getName();
            if (filename.endsWith("(сч. 21).xlsx")) {
                addIntoDb21(sheet, filename);
            } else if (filename.endsWith("(сч. 105).xlsx")) {
                addIntoDb105(sheet, filename);
            } else if (filename.endsWith("(сч. 101).xlsx")) {
                addIntoDb101(sheet, filename);
            }

            workbook.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void insertDataToDb(Map<String, Object> data) {

        Document queryFilter = new Document("Название", data.get("Название"))
                .append("Дата", data.get("Дата"));
        Document doc = collection.findOneAndDelete(queryFilter);
        List<String> to_sum = new ArrayList<>();
        to_sum.add("Остаток");
        if (doc!=null){
            for(String field : to_sum){
                data.put(field, Double.parseDouble(data.get(field).toString() )+ Double.parseDouble(doc.get(field).toString()));
            }
        }

        collection.insertOne(new Document(data));
    }

    public static void addIntoDb21(Sheet sheet, String filename) throws IOException {
        try {
            String[] parts = filename.split("\\\\");

            // Get the last part of the split filename
            String lastPart = parts[parts.length - 1];

            // Extract the substring from the 22nd to the 32nd character
            String date = lastPart.substring(22, 32);

            String subgroup = "";
            for (int i = 8; i <= sheet.getLastRowNum(); ++i) {
                Row row = sheet.getRow(i);
                Cell cell0 = row.getCell(0);
                if (cell0 == null) {
                    continue;
                }
                try {
                    double numeric_value = Integer.parseInt(cell0.getStringCellValue().replaceAll(" ", ""));
                    Cell cell2 = row.getCell(2);
                    Cell cell20 = row.getCell(20);
                    String val2 = cell2.getStringCellValue();
                    double val20 = cell20.getNumericCellValue();
                    if (val2 != null) {
                        Document document = new Document("Название", normalizeName(val2))
                                .append("Остаток", val20)
                                .append("Подгруппа", subgroup)
                                .append("Дата", date)
                                .append("сч", 21)
                                .append("полное название", val2);
                        insertDataToDb(document);
                    }
                } catch (NumberFormatException e) {
                    String string = cell0.getStringCellValue();

                    if (string.contains("21.")) {
                        Pattern pattern = Pattern.compile("21\\.\\d+");
                        Matcher matcher = pattern.matcher(string);
                        if (matcher.find()) {
                            subgroup = matcher.group();

                        }
                        i += 4;
                    } else {
                        if (string.equals("Итого")) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void addIntoDb105(Sheet sheet, String filename) throws IOException {
        try {
            // Split the filename by the backslash character
            String[] parts = filename.split("\\\\");

            // Get the last part of the split filename
            String lastPart = parts[parts.length - 1];

            // Extract the substring from the 22nd to the 32nd character
            String date = lastPart.substring(22, 32);
            int i = 0;
            boolean isNewSubgroup = false;
            boolean seenOne = false;
            double subgroupId = 0;

            // Process each row in the sheet
            for (Row row : sheet) {
                if (i < 6) {
                    i++;
                    continue;
                } else {
                    Cell cell = row.getCell(0); // Adjust this based on your actual column index or header name
                    // Check cell type and handle accordingly
                    if (cell != null) {
                        try {
                            double numericValue = Double.parseDouble(cell.toString());
                            if (!isNewSubgroup) {
                                isNewSubgroup = true;
                                subgroupId = numericValue;
                            } else {
                                if (numericValue == 1.0) {
                                    seenOne = true;
                                } else if (isNewSubgroup && seenOne && numericValue != 1.0) {
                                    subgroupId = numericValue;
                                } else {
                                    isNewSubgroup = false;
                                    seenOne = false;
                                }
                            }
                        } catch (NumberFormatException e) {
                            switch (cell.getCellType()) {
                                case STRING:
                                    String stringValue = cell.getStringCellValue();

                                    // Handle the row containing a string and add it to MongoDB
                                    Cell cell2 = row.getCell(2);
                                    Cell cell0 = row.getCell(0);

                                    if (cell0 != null) {
                                        try {
                                            String value0 = cell0.getStringCellValue();
                                            if (value0.equals("Итого")) {
                                                break;
                                            }
                                            String value2 = Double.toString(cell2.getNumericCellValue());

                                            if (!value0.isEmpty()) {

                                                Document document = new Document("Название", normalizeName(value0.substring(0, value0.lastIndexOf(','))))
                                                        .append("Остаток", value2)
                                                        .append("Подгруппа", subgroupId)
                                                        .append("Дата", date)
                                                        .append("сч", 105)
                                                        .append("полное название", value0.substring(0, value0.lastIndexOf(',')));

                                                insertDataToDb(document);
                                            }
                                        } catch (IllegalStateException ex) {
                                            System.out.println("Error processing row for MongoDB insertion: " + ex.getMessage());
                                        }
                                    }
                            }
                        }
                    }
                    i++;
                }
            }
        } finally {

        }
    }

    // Method to add data into MongoDB for сч. 21
    public static void addIntoDb101(Sheet sheet, String filename) throws IOException {
        try {
            String[] parts = filename.split("\\\\");

            // Get the last part of the split filename
            String lastPart = parts[parts.length - 1];

            // Extract the substring from the 22nd to the 32nd character
            String date = lastPart.substring(22, 32);

            String subgroup = "";
            for (int i = 9; i <= sheet.getLastRowNum(); ++i) {
                Row row = sheet.getRow(i);
                Cell cell0 = row.getCell(0);
                try {
                    double numeric_value = Integer.parseInt(cell0.getStringCellValue().replaceAll(" ", ""));
                    Cell cell2 = row.getCell(2);
                    Cell cell20 = row.getCell(20);
                    String val2 = cell2.getStringCellValue();
                    double val20 = cell20.getNumericCellValue();

                    if (val2 != null) {
                        Document document = new Document("Название", normalizeName(val2))
                                .append("Остаток", val20)
                                .append("Подгруппа", subgroup)
                                .append("Дата", date)
                                .append("сч", 101)
                                .append("полное название", val2);
                        insertDataToDb(document);
                    }
                } catch (NumberFormatException e) {
                    String string = cell0.getStringCellValue();
                    if (string.contains("101.")) {
                        Pattern pattern = Pattern.compile("101\\.\\d+");
                        Matcher matcher = pattern.matcher(string);
                        if (matcher.find()) {
                            subgroup = matcher.group();

                        }
                        i += 4;
                    } else {
                        if (string.equals("Итого")) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    // Method to normalize name
    public static String normalizeName(String name) {
        // Implement your normalization logic here if needed
        return name.replaceAll("\\s", "").toLowerCase();
    }
}
