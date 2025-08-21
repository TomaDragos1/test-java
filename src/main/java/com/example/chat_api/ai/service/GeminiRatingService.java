package com.example.chat_api.ai.service;

import com.example.chat_api.ai.dto.RateRequest;
import com.example.chat_api.ai.dto.RateResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GeminiRatingService {

    private final Client client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String modelId;

    public GeminiRatingService(Client client,
                               @Value("${gemini.model-id:gemini-2.5-flash}") String modelId) {
        this.client = client;
        this.modelId = modelId;
    }

    public RateResult rate(RateRequest req) {
        // --- Enforce { "score": integer [0..4] } via JSON Schema as a Map ---
        Map<String, Object> responseSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "score", Map.of(
                                "type", "integer",
                                "minimum", 0,
                                "maximum", 4
                        ),
                        "explanation", Map.of(            // NEW
                                "type", "string",
                                "minLength", 1,
                                "maxLength", 400             // keep it concise
                        )
                ),
                "required", List.of("score", "explanation"),
                "additionalProperties", false
        );


        // --- System instruction + strict JSON output ---
        GenerateContentConfig cfg = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(
                        """
                        You are a strict technical interviewer.
                        Return ONLY a JSON object with exactly these fields:
                        {"score": <integer 0..4>, "explanation": "<<=400 chars>"}.
                        Score the candidate's answer against model_answer_bullets and (if present) rubric.
                        0 = incorrect/none, 2 = mostly right with gaps, 4 = expert.
                        No extra fields, no prose outside the JSON.
                        """
                )))
                .responseMimeType("application/json")
                .responseJsonSchema(responseSchema)
                .temperature(0.0f)
                .build();

        // --- Build payload (the actual user content) as JSON string ---
        var payload = Map.of(
                "original_question",    nz(req.original_question()),
                "candidate_answer",     nz(req.candidate_answer()),
                "model_answer_bullets", req.model_answer_bullets() == null ? List.of() : req.model_answer_bullets(),
                "rubric",               req.rubric() // may be null
        );

        final String payloadJson;
        try {
            payloadJson = mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build payload JSON", e);
        }

        // --- Call Gemini: pass prompt as plain String (simplest) ---
        GenerateContentResponse res =
                client.models.generateContent(modelId, payloadJson, cfg); // String input is valid

        String text = cleanFences(res.text()); // safety: strip ```json fences if any

        try {
            return mapper.readValue(text, RateResult.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON from model: " + text, e);
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }

    /** Removes possible ```json ... ``` code fences, if any. */
    private static String cleanFences(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            int first = t.indexOf('\n');
            int last = t.lastIndexOf("```");
            if (first > 0 && last > first) t = t.substring(first + 1, last).trim();
        }
        return t;
    }
}
