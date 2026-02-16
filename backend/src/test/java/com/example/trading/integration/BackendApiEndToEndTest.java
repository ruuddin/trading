package com.example.trading.integration;

import com.example.trading.model.Stock;
import com.example.trading.repository.OrderRepository;
import com.example.trading.repository.PortfolioRepository;
import com.example.trading.repository.StockRepository;
import com.example.trading.repository.UserRepository;
import com.example.trading.repository.WatchlistRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class BackendApiEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @BeforeEach
    void resetState() {
        orderRepository.deleteAll();
        portfolioRepository.deleteAll();
        watchlistRepository.deleteAll();
        stockRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void protectedEndpointsRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/watchlists"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"MSFT\",\"quantity\":1,\"side\":\"BUY\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/portfolio"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/dev/keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Key\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/dev/usage"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/watchlists/1/share")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"someone\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/watchlists/shared"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit"))
            .andExpect(status().isForbidden());
    }

    @Test
    void watchlistValidationAndCrudFlowWorks() throws Exception {
        String token = registerAndLogin("api_watch_user", "Pass123!");

        String createResponse = mockMvc.perform(post("/api/watchlists")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Core\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Core"))
            .andReturn().getResponse().getContentAsString();

        long watchlistId = objectMapper.readTree(createResponse).path("id").asLong();

        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/symbols")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"bad!\"}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/symbols")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"MSFT\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbols", hasSize(1)));

        mockMvc.perform(put("/api/watchlists/" + watchlistId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Core Renamed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Core Renamed"));

        mockMvc.perform(get("/api/watchlists")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/watchlists/" + watchlistId + "/symbols/MSFT")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbols", hasSize(0)));

        mockMvc.perform(delete("/api/watchlists/" + watchlistId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/watchlists")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void buyAndSellOrdersUpdatePortfolioQuantity() throws Exception {
        String token = registerAndLogin("api_order_user", "Pass123!");
        stockRepository.save(new Stock("MSFT", "Microsoft", new BigDecimal("210.00")));

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"MSFT\",\"quantity\":5,\"side\":\"BUY\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("MSFT"));

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"MSFT\",\"quantity\":2,\"side\":\"SELL\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.side").value("SELL"));

        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/portfolio")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].symbol").value("MSFT"))
            .andExpect(jsonPath("$[0].quantity").value(3));
    }

    @Test
    void publicMarketAndMetricsEndpointsRemainAccessible() throws Exception {
        stockRepository.save(new Stock("MSFT", "Microsoft", new BigDecimal("210.00")));

        mockMvc.perform(get("/api/stocks/MSFT/price"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("MSFT"))
            .andExpect(jsonPath("$.source").isNotEmpty());

        mockMvc.perform(get("/api/stocks/MSFT/history").param("interval", "daily"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("MSFT"))
            .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/metrics/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.providers").exists());
    }

    private String registerAndLogin(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk());

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(loginResponse);
        return jsonNode.path("token").asText();
    }
}
