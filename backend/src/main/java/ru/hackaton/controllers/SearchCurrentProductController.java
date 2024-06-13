package ru.hackaton.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hackaton.service.SimilarProductFromDB;

@RestController
@RequestMapping("/message")
public class SearchCurrentProductController {

    @Autowired
    private SimilarProductFromDB similarProductFromDB;

    @PostMapping
    public String currentProduct(@RequestParam("product") String product) {
        return similarProductFromDB.mostSimilarProduct(product);
    }
}
