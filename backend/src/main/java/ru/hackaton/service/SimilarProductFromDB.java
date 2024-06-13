package ru.hackaton.service;

import com.mongodb.client.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.concurrent.*;

@Slf4j
@Data
@Component
public class SimilarProductFromDB {
    private static final int THREAD_POOL_SIZE = 5;
    private static MongoClient client = MongoClients.create("mongodb://localhost:27017");
    private static MongoDatabase db = client.getDatabase("stock_remainings");
    private static MongoCollection<Document> collection = db.getCollection("Нормализированные имена");
    @Autowired
    private ChatGPTService chatGPTService;

    public String mostSimilarProduct(String product) {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Callable<String>> tasks = new ArrayList<>();

        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            tasks.add(() -> chatGPTService.sendMessage("Ответь на вопрос", "Как зовут президента России и сколько ему лет?"));
        }

        List<Future<String>> results;

        try {
            results = executorService.invokeAll(tasks);

            executorService.shutdown();

            StringBuilder answer = new StringBuilder();

            for (Future<String> result : results) {
                answer.append(result.get()).append("\n");
            }

            return answer.toString();
        } catch (InterruptedException e) {
            log.error("Error while executing tasks", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        FindIterable<Document> documents = collection.find();

        StringBuilder requestBuilder = new StringBuilder();
//        for (var document: documents) {
//            String productName = document.getString("name");
//            requestBuilder.append(productName);
//            if (requestBuilder.)
//        }


        return "";
    }

}
