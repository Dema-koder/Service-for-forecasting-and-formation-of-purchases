package ru.hackaton.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hackaton.service.ChatGPTService;

@Slf4j
@RestController
@RequestMapping("/translate")
public class NaturalToCommandController {

    @Autowired
    ChatGPTService chatGPTService;

    @PostMapping
    public String translateFromNatural(@RequestParam("message") String message) {
        log.info("Пришел вопрос: {}", message);
        String answer = chatGPTService.sendMessage("", message); // Сюда надо че то написать вместо промпта
        log.info("Ответ чатки: {}", answer);
        return answer;
    }
}
