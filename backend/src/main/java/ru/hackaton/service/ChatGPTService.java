package ru.hackaton.service;

import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.hackaton.config.ApplicationConfig;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;

@Slf4j
@Component
public class ChatGPTService {
    private ChatGPT chatGPT;
    final ApplicationConfig config;
    private ArrayList<Message> messageHistory = new ArrayList<>();

    public ChatGPTService(ApplicationConfig config) {
        this.config = config;
        // Setting up a proxy for the ChatGPT client
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("18.199.183.77", 49232));

        // Initializing ChatGPT instance with API key and proxy
        this.chatGPT = ChatGPT.builder()
                .apiKey(config.getGptToken()) // API key from application configuration
                .apiHost("https://api.openai.com/") // Host URL for OpenAI's API
                .proxy(proxy) // Proxy setup for requests
                .build()
                .init();
    }

    /**
     * Method to send a message to ChatGPT and receive a response.
     *
     * @param prompt   The prompt to set the context of the conversation (system message)
     * @param question The actual question or message from the user
     * @return The response generated by ChatGPT
     */
    public String sendMessage(String prompt, String question) {
        log.info("Пришел вопрос: {}", question);
        // Creating messages for the history
        Message system = Message.ofSystem(prompt);
        Message message = Message.of(question);
        messageHistory = new ArrayList<>(Arrays.asList(system, message));

        return sendMessagesToChatGPT();
    }

    /**
     * Method to send messages to ChatGPT and retrieve the response.
     *
     * @return The response generated by ChatGPT
     */
    private String sendMessagesToChatGPT(){
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT4Turbo.getName()) // GPT4Turbo or GPT_3_5_TURBO
                .messages(messageHistory)
                .maxTokens(3000)
                .temperature(0.9)
                .build();

        ChatCompletionResponse response = chatGPT.chatCompletion(chatCompletion);
        Message res = response.getChoices().get(0).getMessage();
        messageHistory.add(res);

        return res.getContent();
    }
}
