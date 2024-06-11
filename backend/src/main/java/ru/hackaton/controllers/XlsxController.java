package ru.hackaton.controllers;

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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@RestController
@RequestMapping("/xls")
@Tag(name = "XLSX Management System", description = "Operations pertaining to XLSX file handling")
public class XlsxController {

    private static final String TOPIC_NAME = "file_upload_topic";
    private static final String WEBHOOK_URL = "http://127.0.0.1:5000/webhook";
    private static final String WEBHOOK_MESSAGE = "New stock remaining";

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @PostMapping("/upload")
    @Operation(summary = "Upload an XLSX file", description = "Upload an XLSX file and save it with the original filename",
            requestBody = @RequestBody(content = @Content(mediaType = "multipart/form-data",
                    schema = @Schema(implementation = UploadXlsxRequest.class))))
    public String uploadXlsx(@RequestParam("file") MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            log.info("File size: {} bytes", bytes.length);
            log.info("Отправляем в кафку");
            kafkaTemplate.send(TOPIC_NAME, file.getOriginalFilename(), bytes);
            log.info("Отправляем webhook");
            sendWebhookNotification();
            log.info("File uploaded and processed successfully.");
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

    private static void sendWebhookNotification() {
        try {
            URL url = new URL(WEBHOOK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(WEBHOOK_MESSAGE.getBytes());
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            log.info("Webhook response code: {}", responseCode);
        } catch (Exception e) {
            log.error("Exception occurred: {}", e.getMessage());
        }
    }
}
