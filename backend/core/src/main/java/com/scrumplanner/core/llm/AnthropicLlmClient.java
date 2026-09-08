package com.scrumplanner.core.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AnthropicLlmClient (AISC-19): single-provider {@link LlmClient} implementation that
 * calls the Anthropic Messages API directly via the JDK's built-in {@link HttpClient} —
 * no SDK dependency needed for this minimal, single-provider use.
 */
@Service
public class AnthropicLlmClient implements LlmClient {

    private static final URI MESSAGES_ENDPOINT = URI.create("https://api.anthropic.com/v1/messages");
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 2048;

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnthropicLlmClient(
            @Value("${app.llm.api-key:}") String apiKey,
            @Value("${app.llm.model}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmClientException(
                    "No LLM API key configured (app.llm.api-key / APP_LLM_API_KEY) — cannot call the assistant.");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "max_tokens", MAX_TOKENS,
                    "system", systemPrompt,
                    "messages", List.of(Map.of("role", "user", "content", userPrompt))
            ));

            HttpRequest request = HttpRequest.newBuilder(MESSAGES_ENDPOINT)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new LlmClientException(
                        "LLM request failed with status " + response.statusCode() + ": " + response.body());
            }

            JsonNode content = objectMapper.readTree(response.body()).path("content");
            if (!content.isArray() || content.isEmpty()) {
                throw new LlmClientException("LLM response had no content: " + response.body());
            }
            return content.get(0).path("text").asText();
        } catch (IOException e) {
            throw new LlmClientException("Failed to call the LLM provider: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmClientException("Interrupted while calling the LLM provider", e);
        }
    }
}
