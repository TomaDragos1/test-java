package com.example.chat_api.ai.dto;

import java.util.List;

public record RateRequest(
        String original_question,
        String candidate_answer,
        List<String> model_answer_bullets,
        String rubric
) {}
