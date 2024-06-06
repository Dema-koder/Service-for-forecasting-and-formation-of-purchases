package ru.hackaton.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/xlsx")
@Tag(name = "XLSX Management System", description = "Operations pertaining to XLSX file handling")
public class XlsxController {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @PostMapping("/upload")
    @Operation(summary = "Upload an XLSX file", description = "Upload an XLSX file and save it with the original filename",
            requestBody = @RequestBody(content = @Content(mediaType = "multipart/form-data",
                    schema = @Schema(implementation = UploadXlsxRequest.class))))
    public String uploadXlsx(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        log.info("Received file with original filename: {}", filename);
        log.info("Target filename: {}", filename);

        try {
            byte[] bytes = file.getBytes();
            log.info("File size: {} bytes", bytes.length);
            Map<String, Object> message = new HashMap<>();
            message.put("filename", filename);
            message.put("file", bytes);
            kafkaTemplate.send("file_upload_topic", new ObjectMapper().writeValueAsString(message));
            return "File uploaded and processed successfully.";
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
