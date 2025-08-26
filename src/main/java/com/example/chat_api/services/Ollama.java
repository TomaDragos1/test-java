package com.example.chat_api.services;

public class Ollama {

    // OllamaClient.java
    public Mono<String> chat(List<Map<String, String>> messages, Map<String, Object> options) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", props.getModel());
        payload.put("messages", messages);
        payload.put("stream", false);
        if (options != null && !options.isEmpty()) payload.put("options", options);

        return client().post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(body -> Mono.<String>error(
                                        new RuntimeException("Ollama " + resp.statusCode() + ": " + body))))
                .bodyToMono(new ParameterizedTypeReference<Map<String,Object>>() {})
                .map(m -> {
                    Object msgObj = m.get("message");
                    if (msgObj instanceof Map<?,?> msg) {
                        Object content = msg.get("content");
                        return content == null ? "" : content.toString();
                    }
                    return "";
                });
    }

}
