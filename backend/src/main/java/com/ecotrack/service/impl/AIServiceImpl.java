package com.ecotrack.service.impl;

import com.ecotrack.dto.RecommendationResponse;
import com.ecotrack.entity.CarbonEmission;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.AIService;
import com.ecotrack.utils.PromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private static final String OPENAI_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4o-mini";

    private final CarbonEmissionRepository carbonEmissionRepository;
    private final UserRepository userRepository;
    private final PromptBuilder promptBuilder;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse recommend(String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<CarbonEmission> emissions = carbonEmissionRepository.findByUserId(user.getId());
        String prompt = promptBuilder.buildPrompt(user, emissions);

        String responseBody = callOpenAi(prompt);
        List<String> recommendations = parseRecommendations(responseBody);

        return RecommendationResponse.builder()
                .recommendations(recommendations)
                .build();
    }

    private String callOpenAi(String prompt) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        OpenAiChatRequest requestPayload = new OpenAiChatRequest(
                OPENAI_MODEL,
                List.of(
                        new ChatMessage("system", "You are a sustainability assistant that returns only valid JSON."),
                        new ChatMessage("user", prompt)
                ),
                new ResponseFormat("json_object")
        );

        try {
            String requestJson = buildRequestJson(requestPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_ENDPOINT))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI request failed with status code: " + response.statusCode());
            }

            String content = extractAssistantContent(response.body());
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("OpenAI returned an empty recommendation payload");
            }

            return content;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to generate AI recommendations", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate AI recommendations", ex);
        }
    }

    private List<String> parseRecommendations(String responseBody) {
        Pattern arrayPattern = Pattern.compile("\\\"recommendations\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher arrayMatcher = arrayPattern.matcher(responseBody);

        if (!arrayMatcher.find()) {
            throw new IllegalStateException("Unable to parse AI recommendations");
        }

        String arrayContent = arrayMatcher.group(1);
        Pattern itemPattern = Pattern.compile("\\\"((?:\\\\.|[^\\\"])*)\\\"");
        Matcher itemMatcher = itemPattern.matcher(arrayContent);

        List<String> recommendations = new java.util.ArrayList<>();
        while (itemMatcher.find()) {
            recommendations.add(unescapeJsonString(itemMatcher.group(1)));
        }

        if (recommendations.isEmpty()) {
            throw new IllegalStateException("OpenAI returned no recommendations");
        }

        return recommendations.stream().limit(5).toList();
    }

    private String buildRequestJson(OpenAiChatRequest requestPayload) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        builder.append("\"model\":\"").append(escapeJson(requestPayload.model())).append("\",");
        builder.append("\"messages\":[");
        for (int index = 0; index < requestPayload.messages().size(); index++) {
            ChatMessage message = requestPayload.messages().get(index);
            builder.append('{')
                    .append("\"role\":\"").append(escapeJson(message.role())).append("\",")
                    .append("\"content\":\"").append(escapeJson(message.content())).append("\"")
                    .append('}');
            if (index < requestPayload.messages().size() - 1) {
                builder.append(',');
            }
        }
        builder.append("],");
        builder.append("\"response_format\":{\"type\":\"")
                .append(escapeJson(requestPayload.responseFormat().type()))
                .append("\"}");
        builder.append('}');
        return builder.toString();
    }

    private String extractAssistantContent(String responseBody) {
        Pattern contentPattern = Pattern.compile("\\\"content\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"", Pattern.DOTALL);
        Matcher contentMatcher = contentPattern.matcher(responseBody);

        if (!contentMatcher.find()) {
            throw new IllegalStateException("OpenAI returned no recommendations");
        }

        return unescapeJsonString(contentMatcher.group(1));
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJsonString(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);

            if (escaping) {
                switch (current) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    default -> builder.append(current);
                }
                escaping = false;
                continue;
            }

            if (current == '\\') {
                escaping = true;
                continue;
            }

            builder.append(current);
        }

        return builder.toString();
    }

    private record OpenAiChatRequest(String model, List<ChatMessage> messages, ResponseFormat responseFormat) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ResponseFormat(String type) {
    }
}

