package ru.hackaton.parsers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.bson.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@Data
public class XlsxParser {
    private static MongoClient client = MongoClients.create("mongodb://localhost:27017");
    private static MongoDatabase db = client.getDatabase("stock_remainings");
    private static MongoCollection<Document> collection = db.getCollection("Складские остатки");

    private void addIntoDb21(Sheet sheet, String filename) throws ParseException {
        log.info("21 формат");
        String date = filename.split("\\\\")[(filename.split("\\\\")).length - 1].substring(22, 32);
        String subgroup = "";
        Pattern pattern = Pattern.compile("21.{3}");

        for (Row row : sheet) {
            Cell cell0 = row.getCell(0);
            Cell cell2 = row.getCell(2);
            Cell cell20 = row.getCell(20);

            if (cell0 != null && cell2 != null && cell20 != null) {
                String cell0Value = cell0.toString();
                String cell2Value = cell2.toString();
                String cell20Value = cell20.toString();

                if (cell0Value.contains("21.")) {
                    Matcher matcher = pattern.matcher(cell0Value);
                    if (matcher.find()) {
                        subgroup = matcher.group(0);
                    }
                }

                if (!cell2Value.equals("nan") && !cell20Value.equals("nan")) {
                    Document doc = new Document("Название", cell2Value)
                            .append("Остаток", cell20Value)
                            .append("Подгруппа", subgroup)
                            .append("Дата", date)
                            .append("сч", 21);
                    collection.insertOne(doc);
                }
            }
        }
    }

    private void addIntoDb105(Sheet sheet, String filename) throws ParseException {
        log.info("105 формат");
        String date = filename.split("\\\\")[(filename.split("\\\\")).length - 1].substring(22, 32);
        boolean isNewSubgroup = false;
        boolean seenOne = false;
        Double subgroupId = null;

        for (Row row : sheet) {
            log.info(row.toString());
            Cell molCell = row.getCell(0); // Assuming "МОЛ" is in the first cell
            Cell quantityCell = row.getCell(1); // Assuming "Количество" is in the second cell

            System.out.println(molCell.getRichStringCellValue().getString());

            if (molCell != null) {
                try {
                    double molValue = molCell.getNumericCellValue();
                    if (!Double.isNaN(molValue) && !isNewSubgroup) {
                        isNewSubgroup = true;
                        subgroupId = molValue;
                    } else if (!Double.isNaN(molValue)) {
                        if (molValue == 1.0) {
                            seenOne = true;
                        } else if (isNewSubgroup && seenOne && molValue != 1.0) {
                            subgroupId = molValue;
                        } else {
                            isNewSubgroup = false;
                            seenOne = false;
                        }
                    }
                } catch (IllegalStateException e) {
                    if (isNewSubgroup && seenOne && quantityCell != null) {
                        String mol = molCell.toString();
                        String quantity = quantityCell.toString();
                        Document doc = new Document("Название", mol)
                                .append("Остаток", quantity)
                                .append("Подгруппа", subgroupId)
                                .append("Дата", date)
                                .append("сч", 105);
                        collection.insertOne(doc);
                    } else {
                        isNewSubgroup = false;
                        seenOne = false;
                    }
                }
            }
        }
    }

    private void addIntoDb101(Sheet sheet, String filename) throws ParseException {
        log.info("101 формат");
        String date = extractDate(filename);
        String subgroup = "";
        for (Row row : sheet) {
            Cell cell0 = row.getCell(0);
            Cell cell2 = row.getCell(2);
            Cell cell20 = row.getCell(20);
            if (cell0 != null && cell2 != null && cell20 != null) {
                String cell0Value = cell0.toString();
                String cell2Value = cell2.toString();
                String cell20Value = cell20.toString();

                if (cell0Value.contains("101.")) {
                    Pattern pattern = Pattern.compile("101.{3}");
                    Matcher matcher = pattern.matcher(cell0Value);
                    if (matcher.find()) {
                        subgroup = matcher.group(0);
                    }
                }

                if (!cell2Value.equals("nan") && !cell20Value.equals("nan")) {
                    Document doc = new Document("Название", cell2Value)
                            .append("Остаток", cell20Value)
                            .append("Подгруппа", subgroup)
                            .append("Дата", date)
                            .append("сч", 101);
                    collection.insertOne(doc);
                }
            }
        }
    }

    public void processFile(MultipartFile multipartFile) throws IOException, ParseException {
        InputStream is = multipartFile.getInputStream();
        Workbook workbook = new XSSFWorkbook(is);
        Sheet sheet = workbook.getSheetAt(0);
        String filename = multipartFile.getOriginalFilename();

        log.info("Пришел файл с именем: {}", filename);

        if (filename != null) {
            if (filename.endsWith("(сч. 21).xlsx")) {
                addIntoDb21(sheet, filename);
            } else if (filename.endsWith("(сч. 105).xlsx")) {
                addIntoDb105(sheet, filename);
            } else if (filename.endsWith("(сч. 101).xlsx")) {
                addIntoDb101(sheet, filename);
            }
        }

        workbook.close();
        is.close();
    }

    private static String extractDate(String filename) throws ParseException {
        Pattern pattern = Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4}");
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            String dateString = matcher.group(0);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            Date date = sdf.parse(dateString);
            return sdf.format(date);
        } else {
            throw new ParseException("Date not found in the filename", 0);
        }
    }
}
