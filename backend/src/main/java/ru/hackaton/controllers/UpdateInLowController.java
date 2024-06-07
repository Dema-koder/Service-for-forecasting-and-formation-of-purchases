package ru.hackaton.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hackaton.parser.FZ44Parser;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/update")
public class UpdateInLowController {

    private LocalDate lastCheckedData = LocalDate.of(2024, 6, 7);
    @Autowired
    private FZ44Parser fz44Parser;

    @GetMapping
    public String getLowUpdate() {
        String info = fz44Parser.isUpdate();
        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Matcher matcher = datePattern.matcher(info);

        if (matcher.find()) {
            String dateString = matcher.group(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            try {
                LocalDate date = LocalDate.parse(dateString, formatter);
                log.info("Извлеченная дата: {}", date);
                if (date.isEqual(lastCheckedData)) {
                    lastCheckedData = lastCheckedData.plusDays(1);
                    return info;
                }
            } catch (DateTimeParseException e) {
                log.error("Ошибка парсинга даты: {}", e.getMessage());
            }
        } else {
            log.error("Дата не найдена в строке.");
        }
        return "No";
    }
}
