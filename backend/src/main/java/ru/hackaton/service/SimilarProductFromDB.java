package ru.hackaton.service;

import com.mongodb.client.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;

@Slf4j
@Data
@Component
public class SimilarProductFromDB {
    private static final Integer TOP_K = 5;
    private PriorityQueue<Product>products = new PriorityQueue<>(TOP_K);
    private static MongoClient client = MongoClients.create("mongodb://localhost:27017");
    private static MongoDatabase db = client.getDatabase("stock_remainings");
    private static MongoCollection<Document> collection = db.getCollection("Складские остатки");

    private static double similar(String s1, String s2) {
        return FuzzySearch.ratio(s1, s2) / 100.0;
    }

    public static boolean computePrefixFunction(String cur, String t) {
        String s = cur + "≈" + t;
        int n = s.length();
        int[] pi = new int[n];
        int j = 0;

        for (int i = 1; i < n; i++) {
            while (j > 0 && s.charAt(i) != s.charAt(j)) {
                j = pi[j - 1];
            }
            if (s.charAt(i) == s.charAt(j)) {
                j++;
            }
            pi[i] = j;
            if (pi[i] == cur.length())
                return true;
        }
        return false;
    }

    public List<String> mostSimilar(String product) {

        FindIterable<Document> documents = collection.find();
        List<String> ans = new ArrayList<>();
        for (var document: documents) {
            String name = document.getString("Название");
            if (computePrefixFunction(product.toLowerCase(new Locale("ru")), name.toLowerCase(new Locale("ru")))) {
                Product product1 = new Product(name, similar(product, name));

                products.add(product1);
            }

        }
        System.out.println();
        int k = ans.size();
        while (k < 100 && !products.isEmpty()) {
            ans.add(products.peek().name);
            products.poll();
            k++;
        }
        return ans;
    }

    @Data
    @AllArgsConstructor
    static class Product implements Comparable<Product>{
        String name;
        double coeff;

        @Override
        public int compareTo(Product other) {
            return Double.compare(other.coeff, this.coeff);
        }
    }

    public static void main(String[] args) {
        SimilarProductFromDB similarProductFromDB = new SimilarProductFromDB();

        var list = similarProductFromDB.mostSimilar("Бумага");

        for (var item: list) {
            System.out.println(item);
        }
    }
}
