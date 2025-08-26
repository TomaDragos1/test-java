package com.example.hr.service;

import com.example.hr.ai.OllamaClient;
import com.example.hr.api.dto.ChatTurn;
import com.example.hr.repo.CandidateAnswerRepository;
import com.example.hr.model.CandidateAnswer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final CandidateAnswerRepository answerRepo;
    private final ObjectMapper objectMapper;
    private final OllamaClient ollama;

    private static final TypeReference<List<ChatTurn>> CHAT_TURNS = new TypeReference<>() {};

    /** Generate a short feedback summary for a user's whole round. */
    public FeedbackResult generateRoundFeedback(Long roundId, Long userId) {
        List<CandidateAnswer> answers = answerRepo.findAllForRoundAndUser(roundId, userId);
        if (answers.isEmpty()) {
            return new FeedbackResult(null, null, "No answers found for this round/user.");
        }

        // 1) Stitch transcript from all questions
        String transcript = answers.stream()
                .map(this::formatOneAnswerBlock)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 2) Average score (if any scores present)
        OptionalDouble avgOpt = answers.stream()
                .map(CandidateAnswer::getScore)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average();

        BigDecimal avgScore = avgOpt.isPresent()
                ? BigDecimal.valueOf(avgOpt.getAsDouble()).setScale(2, RoundingMode.HALF_UP)
                : null;

        // 3) Build chat messages for Ollama (short feedback, strengths/weaknesses + actionable tip)
        List<Map<String,String>> messages = new ArrayList<>();
        messages.add(Map.of("role","system","content",
                """
                You are a technical interviewer. Based on the transcript of a candidate's interview round,
                write a concise, helpful feedback summary (3–5 sentences) with:
                - strengths,
                - weaknesses/gaps,
                - one actionable next step.
                Do NOT reveal the correct solutions verbatim. Keep it professional and specific.
                """));

        if (avgScore != null) {
            messages.add(Map.of("role","system","content","Average numeric score (if present): " + avgScore));
        }

        messages.add(Map.of("role","user","content",
                "TRANSCRIPT (role-tagged lines across all questions):\n\n" + transcript +
                        "\n\nReturn only the feedback paragraph, no headings."));

        // 4) Call Ollama (/api/chat) with options
        String feedback = ollama.chat(messages, Map.of(
                "seed", 101,
                "temperature", 0.2  // keep it concise/stable
        )).block();

        return new FeedbackResult(
                feedback == null ? "" : feedback.trim(),
                avgScore,
                null
        );
    }

    /** Pretty block per question: include question text if available and the conversation turns. */
    private String formatOneAnswerBlock(CandidateAnswer ans) {
        StringBuilder sb = new StringBuilder();

        if (ans.getQuestion() != null && ans.getQuestion().getQuestionText() != null) {
            sb.append("Question: ").append(ans.getQuestion().getQuestionText()).append("\n");
        }

        List<ChatTurn> turns = parseConversation(ans.getConversationJson());
        for (ChatTurn t : turns) {
            String role = t.role() == null ? "user" : t.role();
            sb.append(role.toUpperCase()).append(": ").append(t.content()).append("\n");
        }

        if (ans.getScore() != null) {
            sb.append("SCORE: ").append(ans.getScore()).append("\n");
        }
        return sb.toString().trim();
    }

    private List<ChatTurn> parseConversation(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, CHAT_TURNS);
        } catch (Exception e) {
            // fallback: treat as a single user message
            return List.of(new ChatTurn("user", json));
        }
    }

    // small DTO to return
    public record FeedbackResult(String feedback, BigDecimal averageScore, String warning) {}
}
