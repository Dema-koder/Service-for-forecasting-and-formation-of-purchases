package ru.hackaton.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.hackaton.service.ChatGPTService;

/**
 * REST контроллер для обработки действий пользователей.
 *
 * Этот контроллер предоставляет эндпоинт для обработки сообщений пользователей и получения специфических ответов от ChatGPT.
 *
 * Аннотация {@link RestController} указывает, что этот класс является контроллером Spring.
 * Аннотация {@link RequestMapping} определяет базовый URL для всех эндпоинтов в этом контроллере.
 * Аннотация {@link Tag} добавляет метаданные OpenAPI для этого контроллера.
 */
@RestController
@RequestMapping("/user-action")
@Slf4j
@Tag(name = "User Action Controller", description = "Endpoint for user actions")
public class UserActionController {

    @Autowired
    ChatGPTService chatGPTService;

    /**
     * Эндпоинт для обработки сообщений пользователей.
     *
     * Этот метод принимает сообщение пользователя, отправляет его в ChatGPT для обработки и возвращает ответ.
     *
     * @param message Сообщение пользователя, которое необходимо обработать.
     * @return Объект {@link ResponseEntity} с ответом от ChatGPT или сообщением об ошибке.
     */
    @GetMapping
    @Operation(summary = "Process user message", description = "Processes user message and provides a specific response")
    @ApiResponse(responseCode = "200", description = "Successful response with processed message", content = @Content(mediaType = "text/plain"))
    @ApiResponse(responseCode = "404", description = "Request to ChatGPT failed")
    public ResponseEntity<String> userAction(@RequestParam("message") String message) {
        String PROMPT = "Определи по сообщению \"" + message + "\", что хочет сделать пользователь, с каким товаром он хочет это сделать, какой временной промежуток указал пользователь в сообщении в днях. Выведи строго опцию, строго товар указанный пользователем строгое или -, и строго временной промежуток в днях строгое или -. Ответ должен иметь одну строку в формате цифра, название товара, число без лишних знаков препинания для времени в днях. В случае если не одна из опций не подходит под запрос пользователя, выведи 3.  Если ты не понял запрос и выводишь \"3\" дополнительно выведи надпись \"Немного не понял вас, не могли бы вы задать вопрос по другому. Пример: \"Сколько \"товара\" осталось на складе\".\". Вместо названия товара должен стоять товар указанный пользователем в сообщении, вместо времееного промежутка в днях должен стоят временной промежуток в днях указанный пользователем. Популярный единицы измерения времени квартал - 90 дней, год - 365 дней, месяц - 30 дней, неделя - 7 дней.\n" +
                "\n" +
                "Сколько товаров/чего-либо осталось на складе, или сколько чего-либо лежит на складе.\n" +
                "Сделать запрос на прогноз, формирование прогноза, прогнозирование количества купить чего-либо на какой-либо срок для дальнейшей закупки или покупки\n" +
                "Вот пример сообщений и ответов в формате <\"Сообщение пользователя\" -> ответ> строго для твоего обучения, не печатай их, если в примерах стоит * то это любое наименование товара твоя задача напечатать его наименование в ответе  в единственном числе Именительном падеже.\n" +
                "\"сколько *лежит на складе\" -> 1, *, -\n" +
                "\"сколько * на складе\" -> 1, *, -\n" +
                "\"как много * осталось\" -> 1, *, -\n" +
                "\"сколько нужно купить *\" -> 2, *, -\n" +
                "\"сколько * есть\" -> 1, *, -\n" +
                "\"сколько купить картриджей на 4 квартала\" -> 2, картридж, 360\n" +
                "\"построй прогноз для клея на 1 месяц\" -> 2, клей, 30\n" +
                "\"спрогнозируй количество клея нужное купить на год\" -> 2, клей, 365\n" +
                "\"много ли у нас *\" -> 1, *, -\n" +
                "\"сколько *\" -> 1, *, -\n" +
                "\"сколько есть * на складе\" -> 1, *, -\n" +
                "\"сколько осталось *\" -> 1, *, -\n" +
                "\"сколько осталось *\" -> 1, *, -\n" +
                "\"cколько мне надо закупить туалетной бумаги на 1 год\" -> 2, туалетная бумага, 365\n" +
                "\"cколько мне закупить туалетной бумаги на 6 месяцев\" -> 2, туалетная бумага, 180\n" +
                "\"cколько закупить туалетной бумаги на месяц\" -> 2, туалетная бумага, 30\n" +
                "\"сколько * осталось на складе\" -> 1, *, -\n" +
                "\"сколько * осталось на складе\" -> 1, *, -\n" +
                "\"ск *\" -> 1,*,-\n" +
                "\"сделай прогноз\" -> 2, -, -\n" +
                "\"сколько нужно закупить товара\" ->2, -, -\n" +
                "\"сделать мне прогноз\" -> 2, -, -\n" +
                "\"сделай прогноз для бумаги\" -> 2, бумага, -\n" +
                "\"прогноз *\" -> 2, *, -\n" +
                "\"остатки\" -> 1, -, -\n" +
                "\"скажи остатки\" -> 1, -, -\n" +
                "\"что лежит на складе\" -> 1, -, -\n" +
                "\"хочу узнать остатки\" -> 1, -, -\n" +
                "\"узнать складские остатки\" -> 1, -, -\n" +
                "\"остаток на складе\" -> 1, -, -\n" +
                "\"остаток\" -> 1, -, -\n" +
                "Выведи строго только ответ строго в формате \"число, название, число\" или \"число, -, число\" или \"число, -, -\" или \"число, название, -\" без изначального запроса и состоящий из 3 элементов только. Еще раз обращаю внимание, что символ * означает любое слово и в ответе тебе надо написать это слово. Пример \"сколько бумаги осталось\" такой же вопрос что \"сколько * осталось\" тебе необходимо дать ответ \"1,бумага,-\". В запросе могут быть орфографические ошибки игнорируй их если слова в который допущены ошибки похожи на слова: \"сколько\",\"осталось\",\"на\",\"складе\",\"лежит\",\"количество\",\"купить\",\"нужно\". Товар может быть указан не одним словом пример: \"бумага а4 белая\" ответ должен быть \"1,бумаги а4 белая\".\n" +
                "Выводи название товара в единственном числе Именительном падеже";
        try {
            log.info("Сообщение пользователя {}", message);
            String answer = chatGPTService.sendMessage(PROMPT, message);
            log.info("Ответ чатки: {}", answer);
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            log.error("Exception occured: {}", e.getMessage());
            return ResponseEntity.status(404).body("Request to ChatGPT failed");
        }
    }
}