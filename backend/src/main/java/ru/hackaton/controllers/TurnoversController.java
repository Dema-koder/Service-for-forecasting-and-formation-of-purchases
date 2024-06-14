package ru.hackaton.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.hackaton.parsers.FromMultipartToFile;
import ru.hackaton.parsers.TurnoversParser;

import java.io.File;

@RestController
@RequestMapping("/upload")
@Slf4j
public class TurnoversController {

    private static final String SUCCESS_MESSAGE = "Success Upload";
    private static final String FAILED_MESSAGE = "Fail To Upload";

    @Autowired
    FromMultipartToFile fromMultipartToFile;

    @Autowired
    TurnoversParser parser;

    @PostMapping("/turnovers")
    @Operation(summary = "Upload an XLSX file", description = "Upload an XLSX file and save it with the original filename",
            requestBody = @RequestBody(content = @Content(mediaType = "multipart/form-data",
                    schema = @Schema(implementation = TurnoversController.UploadXLSXRequest.class))))
    public String turnoversUpload(@RequestParam("file") MultipartFile file) {
        try {
            File excelFile = fromMultipartToFile.convertMultipartFileToFile(file);
            parser.processFile(excelFile);
            excelFile.delete();
        } catch (Exception e) {
            log.error("Exception occurred: {}", e.getMessage());
            return FAILED_MESSAGE;
        }
        return SUCCESS_MESSAGE;
    }

    public static class UploadXLSXRequest {
        @Schema(type = "string", format = "binary", description = "XLSX file to upload")
        public MultipartFile file;
    }
}
