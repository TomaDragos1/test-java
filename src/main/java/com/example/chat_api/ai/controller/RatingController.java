package com.example.chat_api.ai.controller;

import com.example.chat_api.ai.dto.RateRequest;
import com.example.chat_api.ai.dto.RateResult;
import com.example.chat_api.ai.service.GeminiRatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class RatingController {

    private final GeminiRatingService service;

    public RatingController(GeminiRatingService service) {
        this.service = service;
    }

    /** Full payload (Postman): POST /ai/rate */
    @PostMapping("/rate")
    public ResponseEntity<RateResult> rate(@RequestBody RateRequest req) {
        return ResponseEntity.ok(service.rate(req));
    }

    /** Quick browser test (no POST): GET /ai/rate?question=...&answer=... */
    @GetMapping("/rate")
    public ResponseEntity<RateResult> rateQuick(
            @RequestParam String question,
            @RequestParam String answer,
            @RequestParam(required = false) String rubric
    ) {
        var req = new RateRequest(
                question,
                answer,
                /* model_answer_bullets */ List.of(), // you can pass via POST when needed
                rubric
        );
        return ResponseEntity.ok(service.rate(req));
    }
}
