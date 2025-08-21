package com.example.chat_api.magic;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

record MagicRequest(String email) {}

@Controller
public class MagicController {

    private final TokenService tokens;
    private final EmailService emailService;

    public MagicController(TokenService tokens, EmailService emailService) {
        this.tokens = tokens;
        this.emailService = emailService;
    }

    // 1) Request a magic link via POST
    @PostMapping("/magic/request")
    @ResponseBody
    public ResponseEntity<?> requestLink(@RequestBody MagicRequest req) {
        // TODO: look up/create user by email, rate-limit, etc.
        String token = tokens.createToken(req.email());
        emailService.sendMagicLink(req.email(), token);
        return ResponseEntity.ok().body("{\"status\":\"sent\"}");
    }

    // 2) Verify the token; if good, "log in" and show a basic page
    @GetMapping("/magic/verify")
    public String verify(@RequestParam("token") String token, HttpSession session, Model model) {
        try {
            String email = tokens.validateAndGetEmail(token);
            // Minimal "session login" for demo:
            session.setAttribute("userEmail", email);
            model.addAttribute("email", email);
            return "welcome";
        } catch (Exception e) {
            model.addAttribute("error", "Invalid or expired link.");
            return "invalid";
        }
    }
}
