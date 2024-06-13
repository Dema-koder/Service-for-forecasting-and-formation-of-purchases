package ru.hackaton.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/predict")
public class PredictModelController {
    @PostMapping
    public MultipartFile predict(String message) {
        return null;
    }
}
