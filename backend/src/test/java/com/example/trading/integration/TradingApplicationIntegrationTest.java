package com.example.trading.integration;

import com.example.trading.model.Stock;
import com.example.trading.model.User;
import com.example.trading.repository.UserRepository;
import com.example.trading.repository.StockRepository;
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
class TradingApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        stockRepository.deleteAll();
    }

    @Test
    void authRegisterAndLoginFlowWorks() throws Exception {
        String username = "auth_user";
        String password = "Pass123!";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void watchlistCrudAndSymbolManagementFlowWorks() throws Exception {
        String token = registerAndLogin("watch_user", "Pass123!");

        String createResponse = mockMvc.perform(post("/api/watchlists")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Growth\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Growth"))
            .andReturn().getResponse().getContentAsString();

        long watchlistId = objectMapper.readTree(createResponse).path("id").asLong();

        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/symbols")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"MSFT\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbols", hasSize(1)));

        mockMvc.perform(put("/api/watchlists/" + watchlistId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Growth 2\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Growth 2"));

        mockMvc.perform(delete("/api/watchlists/" + watchlistId + "/symbols/MSFT")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbols", hasSize(0)));

        mockMvc.perform(delete("/api/watchlists/" + watchlistId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void marketAndMetricsEndpointsWork() throws Exception {
        mockMvc.perform(get("/api/stocks/MSFT/price"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("MSFT"));

        mockMvc.perform(get("/api/stocks/MSFT/history").param("interval", "daily"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("MSFT"))
            .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/metrics/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.providers").exists());
    }

    @Test
    void ordersAndPortfolioFlowWorks() throws Exception {
        String token = registerAndLogin("order_user", "Pass123!");

        stockRepository.save(new Stock("MSFT", "Microsoft", new BigDecimal("210.00")));

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"MSFT\",\"quantity\":2,\"side\":\"BUY\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("MSFT"));

        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/portfolio")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void billingEntitlementEndpointReturnsDefaultPlanForAuthenticatedUser() throws Exception {
        String token = registerAndLogin("billing_user", "Pass123!");

        mockMvc.perform(get("/api/billing/me")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("billing_user"))
            .andExpect(jsonPath("$.planTier").value("FREE"))
            .andExpect(jsonPath("$.billingStatus").value("TRIAL"))
            .andExpect(jsonPath("$.trialEndsAt").isNotEmpty())
            .andExpect(jsonPath("$.trialActive").isBoolean());
    }

    @Test
    void checkoutSessionEndpointReturnsPlaceholderResponseForAuthenticatedUser() throws Exception {
        String token = registerAndLogin("checkout_user", "Pass123!");

        mockMvc.perform(post("/api/billing/checkout-session")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planTier\":\"PRO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("checkout_user"))
            .andExpect(jsonPath("$.requestedPlan").value("PRO"))
            .andExpect(jsonPath("$.status").value("NOT_CONFIGURED"))
            .andExpect(jsonPath("$.sessionId").isNotEmpty())
            .andExpect(jsonPath("$.checkoutUrl").isNotEmpty());
    }

            @Test
            void checkoutSessionEndpointReturnsBadRequestForInvalidPlanTier() throws Exception {
            String token = registerAndLogin("checkout_invalid_user", "Pass123!");

            mockMvc.perform(post("/api/billing/checkout-session")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planTier\":\"GOLD\"}"))
                .andExpect(status().isBadRequest());
            }

    @Test
    void webhookEndpointAppliesEntitlementUpdateWithoutAuthentication() throws Exception {
        registerAndLogin("webhook_user", "Pass123!");

        mockMvc.perform(post("/api/billing/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"webhook_user\",\"planTier\":\"PREMIUM\",\"billingStatus\":\"active\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.accepted").value(true));

        User user = userRepository.findByUsername("webhook_user").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("PREMIUM", user.getPlanTier().name());
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", user.getBillingStatus());
    }

    @Test
    void alertsCrudFlowWorksForAuthenticatedUser() throws Exception {
        String token = registerAndLogin("alerts_user", "Pass123!");

        String createResponse = mockMvc.perform(post("/api/alerts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"MSFT\",\"conditionType\":\"ABOVE\",\"targetPrice\":300.5}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("MSFT"))
            .andExpect(jsonPath("$.conditionType").value("ABOVE"))
            .andExpect(jsonPath("$.targetPrice").value(300.5))
            .andReturn().getResponse().getContentAsString();

        long alertId = objectMapper.readTree(createResponse).path("id").asLong();

        mockMvc.perform(get("/api/alerts").param("symbol", "MSFT")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/alerts/" + alertId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deleted").value(true));

        mockMvc.perform(get("/api/alerts").param("symbol", "MSFT")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
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
