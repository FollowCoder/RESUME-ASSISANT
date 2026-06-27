package com.resume.config;

import com.resume.websocket.InterviewWebSocketHandler;
import com.resume.websocket.SpeechWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final InterviewWebSocketHandler interviewWebSocketHandler;
    private final SpeechWebSocketHandler speechWebSocketHandler;

    public WebSocketConfig(InterviewWebSocketHandler interviewWebSocketHandler,
                           SpeechWebSocketHandler speechWebSocketHandler) {
        this.interviewWebSocketHandler = interviewWebSocketHandler;
        this.speechWebSocketHandler = speechWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(interviewWebSocketHandler, "/ws/interview/*")
                .setAllowedOrigins("*");
        registry.addHandler(speechWebSocketHandler, "/ws/interview/*/voice")
                .setAllowedOrigins("*");
    }
}
