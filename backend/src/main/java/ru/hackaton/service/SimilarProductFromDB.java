package ru.hackaton.service;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.hackaton.config.ApplicationConfig;

import java.util.*;
import java.util.concurrent.*;

/**
 * Класс для поиска наиболее похожих продуктов из базы данных складских остатков.
 * Использует MongoDB для доступа к данным и ChatGPT для генерации ответов на основе запросов.
 * Реализует два метода поиска: простой поиск по префиксной функции и поиск с использованием ChatGPT.
 */
@Slf4j
@Data
@Component
public class SimilarProductFromDB {
    final ApplicationConfig config;
    /**
     * Размер пула потоков для обработки данных.
     */
    private static final int THREAD_POOL_SIZE = 5;
    /**
     * Максимальное количество продуктов в одном сообщении.
     */
    private static final int MAX_PRODUCTS_IN_MESSAGE = 350;
    /**
     * Клиент MongoDB для доступа к базе данных "stock_remainings".
     */
    private static MongoClient client;
    /**
     * База данных MongoDB для складских остатков.
     */
    private static MongoDatabase db;
    /**
     * Коллекция MongoDB для хранения складских остатков.
     */
    private static MongoCollection<Document> collection;
    /**
     * Сервис ChatGPT для отправки запросов и получения ответов.
     */
    @Autowired
    private ChatGPTService chatGPTService;

    public SimilarProductFromDB(ApplicationConfig config) {
        this.config = config;
        client = MongoClients.create(config.getMongoUrl());
        db = client.getDatabase("stock_remainings");
        collection = db.getCollection("Складские остатки");
    }

    /**
     * Метод для поиска наиболее похожих продуктов на основе запроса.
     *
     * @param product запрос пользователя для поиска похожих продуктов
     * @return список наиболее похожих продуктов
     */
    public List<String> mostSimilarProduct(String product) {
        FindIterable<Document> stocksss = collection.find();

        Set<String>stocks = new HashSet<>();
        for (var doc: stocksss) {
            stocks.add(doc.getString("полное название"));
        }

        var st = searchViaPrefixFunction(stocks, product);
        if (st.isEmpty()) {
            return new ArrayList<>();
        }
        if (st.size() < 5) {
            List<String> answer = new ArrayList<>();
            answer.addAll(st);
            return answer;
        }
        return simpleSearchViaChatGPT(st, product);
    }

    /**
     * Метод для поиска продуктов с использованием префиксной функции.
     *
     * @param stocks  множество продуктов для поиска
     * @param product запрос пользователя для поиска похожих продуктов
     * @return множество найденных похожих продуктов
     */
    private Set<String> searchViaPrefixFunction(Set<String>stocks, String product) {
        String[] words = product.split(" ");
        Set<String>answer = new HashSet<>();
        Set<String>normStrs = new HashSet<>();
        for (var word: words) {
            for (String doc : stocks) {
                String norm = normalize(doc);
                if (containsSubstring(norm, normalize(word))) {
                    if (normStrs.contains(norm))
                        continue;
                    normStrs.add(norm);
                    answer.add(doc);
                }
            }
        }
        return answer;
    }

    /**
     * Метод для поиска продуктов с использованием ChatGPT.
     *
     * @param st      множество продуктов для поиска
     * @param product запрос пользователя для поиска похожих продуктов
     * @return список наиболее похожих продуктов, полученный с помощью ChatGPT
     */
    private List<String> simpleSearchViaChatGPT(Set<String>st, String product) {
        String PROMPT = "У вас есть список товаров со склада ниже, необходимо найти ровно 5 наиболее похожих товаров на основе поискового запроса \"" + product + "\" и вывести только эти товары, строго название каждого товара в отдельной строчке, и строго без лишнего текста и форматирования.\n" +
                "Список товаров, каждый товар в отдельной строке:";
        StringBuilder builder = new StringBuilder();
        for (String str: st)
            builder.append(str).append("\n");
        List<String> answer = List.of(chatGPTService.sendMessage(PROMPT, builder.toString()).split("\n"));
        return answer;
    }

    /**
     * Метод для нормализации строки (приведение к нижнему регистру).
     *
     * @param str строка для нормализации
     * @return нормализованная строка
     */
    private String normalize(String str) {
        str = str.toLowerCase();
        return str;
    }

    /**
     * Метод для вычисления префиксной функции строки.
     *
     * @param pattern строка для вычисления префиксной функции
     * @return массив значений префиксной функции
     */
    private int[] computePrefixFunction(String pattern) {
        int m = pattern.length();
        int[] pi = new int[m];
        int k = 0;
        for (int i = 1; i < m; i++) {
            while (k > 0 && pattern.charAt(k) != pattern.charAt(i)) {
                k = pi[k - 1];
            }
            if (pattern.charAt(k) == pattern.charAt(i)) {
                k++;
            }
            pi[i] = k;
        }
        return pi;
    }

    /**
     * Метод для проверки наличия подстроки в тексте с использованием префиксной функции.
     *
     * @param text    текст, в котором производится поиск
     * @param pattern подстрока, которую необходимо найти
     * @return true, если подстрока найдена в тексте, иначе false
     */
    private boolean containsSubstring(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) {
            return true;
        }
        int[] pi = computePrefixFunction(pattern);
        int q = 0;
        for (int i = 0; i < n; i++) {
            while (q > 0 && pattern.charAt(q) != text.charAt(i)) {
                q = pi[q - 1];
            }
            if (pattern.charAt(q) == text.charAt(i)) {
                q++;
            }
            if (q == m) {
                return true;
            }
        }
        return false;
    }
}
