package com.alexlego19.ainpcs.backend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.CompletableFuture;

public class LLMService {

    /**
     * Simulates an asynchronous call to an LLM backend API.
     *
     * @param input The input message from the player.
     * @return A CompletableFuture that resolves with a parsed JSON structured response.
     */
    public static CompletableFuture<JsonObject> requestDialogueAsync(String input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate network latency (2 seconds)
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // A mock structured response matching dialogue_response.schema.json
            String mockJsonResponse = """
                {
                  "spoken_text": "Hello traveler! I am processing your words thoughtfully.",
                  "tone_evaluation": "friendly",
                  "engine_function_calls": []
                }
                """;

            // Parse and return as a Gson JsonObject
            return JsonParser.parseString(mockJsonResponse).getAsJsonObject();
        });
    }
}
