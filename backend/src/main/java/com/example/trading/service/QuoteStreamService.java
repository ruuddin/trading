package com.example.trading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class QuoteStreamService extends TextWebSocketHandler {

    private final SimpleStockPriceService priceService;
    private final QuoteResolutionService quoteResolutionService;
    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public QuoteStreamService(SimpleStockPriceService priceService, QuoteResolutionService quoteResolutionService, ObjectMapper objectMapper) {
        this.priceService = priceService;
        this.quoteResolutionService = quoteResolutionService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void startBroadcastLoop() {
        scheduler.scheduleAtFixedRate(this::broadcastQuotes, 2, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stopBroadcastLoop() {
        scheduler.shutdownNow();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        subscriptions.put(session.getId(), ConcurrentHashMap.newKeySet());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = root.path("type").asText("");
        if (!"subscribe".equalsIgnoreCase(type)) {
            return;
        }

        Set<String> nextSymbols = ConcurrentHashMap.newKeySet();
        JsonNode symbolsNode = root.path("symbols");
        if (symbolsNode.isArray()) {
            for (JsonNode symbolNode : symbolsNode) {
                if (!symbolNode.isTextual()) continue;
                String normalized = symbolNode.asText("").trim().toUpperCase(Locale.ROOT);
                if (!normalized.isBlank() && priceService.isValidSymbol(normalized)) {
                    nextSymbols.add(normalized);
                }
            }
        }

        subscriptions.put(session.getId(), nextSymbols);
        sendSnapshot(session, nextSymbols);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        subscriptions.remove(session.getId());
    }

    private void sendSnapshot(WebSocketSession session, Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return;
        }
        try {
            session.sendMessage(buildQuotesMessage(symbols));
        } catch (IOException ex) {
            sessions.remove(session.getId());
            subscriptions.remove(session.getId());
        }
    }

    private void broadcastQuotes() {
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String sessionId = entry.getKey();
            WebSocketSession session = entry.getValue();
            if (!session.isOpen()) {
                sessions.remove(sessionId);
                subscriptions.remove(sessionId);
                continue;
            }

            Set<String> symbols = subscriptions.get(sessionId);
            if (symbols == null || symbols.isEmpty()) {
                continue;
            }

            try {
                session.sendMessage(buildQuotesMessage(symbols));
            } catch (Exception ex) {
                sessions.remove(sessionId);
                subscriptions.remove(sessionId);
            }
        }
    }

    private TextMessage buildQuotesMessage(Set<String> symbols) throws IOException {
        List<Map<String, Object>> quotes = new ArrayList<>();

        for (String symbol : symbols) {
            QuoteResolutionService.ResolvedQuote quote = quoteResolutionService.resolve(symbol);
            if (quote == null) {
                continue;
            }

            quotes.add(Map.of(
                "symbol", quote.symbol(),
                "price", quote.price(),
                "high", quote.high(),
                "low", quote.low(),
                "date", quote.date(),
                "source", quote.source(),
                "timestamp", Instant.now().toString()
            ));
        }

        String payload = objectMapper.writeValueAsString(Map.of(
            "type", "quotes",
            "quotes", quotes
        ));
        return new TextMessage(payload);
    }
}
