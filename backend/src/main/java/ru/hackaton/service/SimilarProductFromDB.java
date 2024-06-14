package ru.hackaton.service;

import com.mongodb.client.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Data
@Component
public class SimilarProductFromDB {
    private static final int THREAD_POOL_SIZE = 10;
    private static final int MAX_PRODUCTS_IN_MESSAGE = 300;
    private static final String PROMPT = "У вас есть список товаров со склада ниже, необходимо найти наиболее похожие товары на основе поискового запроса \"огурцы\" и вывести только эти товары, строго название каждого товара в отдельной строчке, и строго без лишнего текста и форматирование. Если поиск не дает похожих результатов, следует вывести строго \"ничего не найдено\".\n" +
            "Список товаров, каждый товар в отдельной строке:";
    private static MongoClient client = MongoClients.create("mongodb://localhost:27017");
    private static MongoDatabase db = client.getDatabase("stock_remainings");
    private static MongoCollection<Document> collection = db.getCollection("Нормализированные имена");
    @Autowired
    private ChatGPTService chatGPTService;

    public String mostSimilarProduct(String product) {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Callable<String>> tasks = new ArrayList<>();

        FindIterable<Document> documents = collection.find();

        StringBuilder requestBuilder = new StringBuilder();
        int k = 0;
        for (var document: documents) {
            String productName = document.getString("name");
            requestBuilder.append(productName);
            k += 1;
            if (k == MAX_PRODUCTS_IN_MESSAGE) {
                tasks.add(() -> chatGPTService.sendMessage(PROMPT, requestBuilder.toString()));
                requestBuilder.setLength(0);
                k = 0;
            }
        }
        if (k != 0) {
            tasks.add(() -> chatGPTService.sendMessage(PROMPT, requestBuilder.toString()));
        }

        List<Future<String>> results;

        try {
            results = executorService.invokeAll(tasks);

            executorService.shutdown();

            StringBuilder answer = new StringBuilder();

            for (Future<String> result : results) {
                answer.append(result.get()).append("\n");
            }

            return chatGPTService.sendMessage(PROMPT, answer.toString());
        } catch (InterruptedException e) {
            log.error("Error while executing tasks", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        return "";
    }

}
