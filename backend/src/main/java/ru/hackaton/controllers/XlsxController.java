package ru.hackaton.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@RestController
@RequestMapping("/xlsx")
@Tag(name = "XLSX Management System", description = "Operations pertaining to XLSX file handling")
public class XlsxController {

    @PostMapping("/upload")
    @Operation(summary = "Upload an XLSX file", description = "Upload an XLSX file and save it with the original filename",
            requestBody = @RequestBody(content = @Content(mediaType = "multipart/form-data",
                    schema = @Schema(implementation = UploadXlsxRequest.class))))
    public String uploadXlsx(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        log.info("Received file with original filename: {}", file.getOriginalFilename());
        log.info("Target filename: {}", filename);

        try {
            byte[] bytes = file.getBytes();
            log.info("File size: {} bytes", bytes.length);

            try (InputStream inputStream = new ByteArrayInputStream(bytes);
                 Workbook workbook = new XSSFWorkbook(inputStream);
                 FileOutputStream fileOut = new FileOutputStream(filename)) {
                workbook.write(fileOut);
                log.info("Workbook written to file successfully");
            }
            return "File uploaded and processed successfully.";
        } catch (IOException e) {
            log.error("Failed to process file {}", e.getMessage());
            return "Failed to process file: " + e.getMessage();
        } catch (Exception e) {
            log.error("An unexpected error occurred: {}", e.getMessage());
            return "An unexpected error occurred: " + e.getMessage();
        }
    }

    public static class UploadXlsxRequest {
        @Schema(type = "string", format = "binary", description = "XLSX file to upload")
        public MultipartFile file;
    }
}
