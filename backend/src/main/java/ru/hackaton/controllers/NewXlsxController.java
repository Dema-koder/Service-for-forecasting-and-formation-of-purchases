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
import ru.hackaton.parsers.XlsxParser;

@Slf4j
@RestController
@RequestMapping("/xlsx")
public class NewXlsxController {

    private static final String FAILED_MESSAGE = "Upload Fail";
    private static final String SUCCESS_MESSAGE = "Success Upload";

    @Autowired
    private XlsxParser parser;

    @PostMapping("/upload")
    @Operation(summary = "Upload an XLSX file", description = "Upload an XLSX file and save it with the original filename",
            requestBody = @RequestBody(content = @Content(mediaType = "multipart/form-data",
                    schema = @Schema(implementation = NewXlsxController.UploadXlsxRequest.class))))
    public String uploadXlsx(@RequestParam("file") MultipartFile file) {
        try {
            parser.processFile(file);
        } catch (Exception e) {
            log.error("Exception occured: {}", e.getMessage());
            return FAILED_MESSAGE;
        }
        return SUCCESS_MESSAGE;
    }

    public static class UploadXlsxRequest {
        @Schema(type = "string", format = "binary", description = "XLSX file to upload")
        public MultipartFile file;
    }
}
