package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

abstract class Parser {
    protected MongoClient client;
    protected MongoDatabase db;
    private static final String HOST = "localhost";
    private static final int PORT = 27017;
    private static final String DATABASE_NAME = "stock_remainings";
    private static final String COLLECTION_NAME = "Складские остатки";
    public Parser() {
        this.client = MongoClients.create("mongodb://" + HOST + ":" + PORT);
        this.db = client.getDatabase("stock_remainings");
    }

    public abstract void parse(String directoryPath);
}

class OborotyParser extends Parser {
    private MongoCollection<Document> collection;

    public OborotyParser() {
        super();
        this.collection = db.getCollection("Оборотная ведомость");
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


    @Override
    public void parse(String directoryPath) {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directoryPath), "*.xlsx")) {
            for (Path path : directoryStream) {
                processFile(path.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] extractQuarterYear(String filename) {
        Pattern quarterPattern = Pattern.compile("(\\d+) кв\\.");
        Pattern yearPattern = Pattern.compile("кв\\. (\\d+)");
        Matcher quarterMatcher = quarterPattern.matcher(filename);
        Matcher yearMatcher = yearPattern.matcher(filename);
        String quarter = quarterMatcher.find() ? quarterMatcher.group(1) : null;
        String year = yearMatcher.find() ? yearMatcher.group(1) : null;
        return new String[]{quarter, year};
    }

    private void insertDataToDb(Map<String, Object> data) {

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

    private void processCommonLogic(Sheet sheet, String quarter, String year, int group, int rowIndex, int nameIndex) {
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

    private void processFile(String filepath) {
        if (filepath.contains("сч. 21")) {
            process21(filepath);
        } else if (filepath.contains("сч. 105")) {
            process105(filepath);
        } else if (filepath.contains("сч. 101")) {
            process101(filepath);
        } else {
            System.out.println("Skipping " + filepath + ", no matching function found");
        }
    }

    private void process21(String filepath) {
        try (FileInputStream fis = new FileInputStream(filepath); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            String[] quarterYear = extractQuarterYear(filepath);
            processCommonLogic(sheet, quarterYear[0], quarterYear[1], 21, 9, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void process105(String filepath) {
        try (FileInputStream fis = new FileInputStream(filepath)) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            String[] quarterYear = extractQuarterYear(filepath);
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void process101(String filepath) {
        try (FileInputStream fis = new FileInputStream(filepath); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            String[] quarterYear = extractQuarterYear(filepath);
            processCommonLogic(sheet, quarterYear[0], quarterYear[1], 101, 9, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String normalizeName(String name) {
        // Implement normalization logic
        return name.replaceAll("\\s", "").toLowerCase();
    }
}

class ReferenceBookParser extends Parser {
    private MongoCollection<Document> collection;

    public ReferenceBookParser() {
        super();
        this.collection = db.getCollection("Справочники");
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

    @Override
    public void parse(String filepath) {
        try (FileInputStream fis = new FileInputStream(filepath); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            List<Map<String, String>> dataList = new ArrayList<>();

            // Extract header
            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            headerRow.forEach(cell -> headers.add(cell.getStringCellValue()));

            headers.set(4, "Конечный код КПГЗ");

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getCell(headers.indexOf("Название СТЕ")) == null) {
                    continue;
                }


                Cell registryNumber = row.getCell(headers.indexOf("Реестровый номер в РК"));
                if (!isNumericCode(registryNumber)) {
                    continue;
                }

                Map<String, String> data = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i);
                    data.put(headers.get(i), cell.toString());
                }



                String nameCharacteristics = row.getCell(headers.indexOf("наименование характеристик")).toString();
                String[] splitResult = splitAndAggregate(data.get("Название СТЕ"));
                data.put("Название СТЕ", splitResult[0].toLowerCase().replaceAll(" ", ""));
                data.put("характеристики", splitResult[1]);

                // Placeholder for predict function
//                boolean isResursny = predict((String) data.get("Конечный код КПГЗ"), registryNumber);
                boolean isResursny = false;
                data.put("ресурсный", Boolean.toString(isResursny));

                dataList.add(data);
            }
            collection.insertMany(dataList.stream().map(Document::new).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isNumericCode(Cell cell) {
        return cell.getCellType() == CellType.NUMERIC;
    }

    private String[] splitAndAggregate(String nameCharacteristics) {
        int position = nameCharacteristics.indexOf(';');
        if (position == -1) {
            return new String[]{nameCharacteristics, ""};
        } else {
            String simpleName = nameCharacteristics.substring(0, position);
            String characteristics = nameCharacteristics.substring(position + 1);
            return new String[]{simpleName, characteristics};
        }
    }
}

class StockBalanceParser extends Parser {
    private MongoCollection<Document> collection;

    public StockBalanceParser() {
        super();
        this.collection = db.getCollection("Складские остатки");
    }

    @Override
    public void parse(String directoryPath) {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directoryPath), "*.xlsx")) {
            for (Path path : directoryStream) {
                if (path.toString().contains("(сч. 21)")) {
                    addIntoDb21(path.toString());
                } else if (path.toString().contains("(сч. 105)")) {
                    addIntoDb105(path.toString());
                } else if (path.toString().contains("(сч. 101)")) {
                    addIntoDb101(path.toString());
                } else{
                    System.out.println("oops");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void insertDataToDb(Map<String, Object> data) {

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

    public void addIntoDb21(String filepath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filepath); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            String[] parts = filepath.split("\\\\");

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
                                .append("полное название", cell0.getStringCellValue());
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
        }
    }

    public void addIntoDb105(String filepath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filepath); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            // Split the filename by the backslash character
            String[] parts = filepath.split("\\\\");

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
        }
    }

    // Method to add data into MongoDB for сч. 21
    public void addIntoDb101(String filepath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filepath); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            String[] parts = filepath.split("\\\\");

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
        }
    }

    private String normalizeName(String name) {
        // Implement normalization logic
        return name.replaceAll("\\s", "").toLowerCase();
    }
}

public class Parsers {
    public static void main(String[] args) {
        StockBalanceParser stockParser = new StockBalanceParser();
        OborotyParser oborotyParser = new OborotyParser();
        ReferenceBookParser referenceBookParser = new ReferenceBookParser();
        stockParser.parse("C:\\Users\\danil\\Desktop\\hackaton\\Service-for-forecasting-and-formation-of-purchases\\dataset\\Складские остатки");
        oborotyParser.parse("C:\\Users\\danil\\Desktop\\hackaton\\Service-for-forecasting-and-formation-of-purchases\\dataset\\Обороты по счету");
        referenceBookParser.parse("C:\\Users\\danil\\Desktop\\hackaton\\Service-for-forecasting-and-formation-of-purchases\\dataset\\КПГЗ ,СПГЗ, СТЕ.xlsx");

    }
}
