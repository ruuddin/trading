package com.example.trading.integration;

import com.example.trading.model.Stock;
import com.example.trading.model.PlanTier;
import com.example.trading.model.User;
import com.example.trading.repository.ApiKeyRepository;
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

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    void setup() {
        stockRepository.deleteAll();
        apiKeyRepository.deleteAll();
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

    @Test
    void analyticsEndpointsReturnPortfolioSummaryAndPerformance() throws Exception {
        String token = registerAndLogin("analytics_user", "Pass123!");

        stockRepository.save(new Stock("MSFT", "Microsoft", new BigDecimal("210.00")));

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"MSFT\",\"quantity\":2,\"side\":\"BUY\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/analytics/portfolio-summary")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.marketValue").exists())
            .andExpect(jsonPath("$.costBasis").exists())
            .andExpect(jsonPath("$.positions").isNumber());

        mockMvc.perform(get("/api/analytics/performance")
                .param("range", "1M")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.range").value("1M"))
            .andExpect(jsonPath("$.series").isArray());
    }

    @Test
    void screenerEndpointAndSavedScansFlowWorks() throws Exception {
        String token = registerAndLogin("screener_user", "Pass123!");

        stockRepository.save(new Stock("NVDA", "NVIDIA", new BigDecimal("450.00")));
        stockRepository.save(new Stock("AMD", "AMD", new BigDecimal("120.00")));

        mockMvc.perform(get("/api/screener")
                .param("query", "NV")
                .param("minPrice", "100")
                .param("maxPrice", "700")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results").isArray())
            .andExpect(jsonPath("$.count").isNumber());

        mockMvc.perform(post("/api/screener/saved")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"AI Momentum\",\"query\":\"NV\",\"minPrice\":100,\"maxPrice\":700}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("AI Momentum"));

        mockMvc.perform(get("/api/screener/saved")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void developerApiKeyEndpointsRequirePremiumAndReturnUsage() throws Exception {
        String freeToken = registerAndLogin("dev_free_user", "Pass123!");

        mockMvc.perform(post("/api/dev/keys")
                .header("Authorization", "Bearer " + freeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Free Key\"}"))
            .andExpect(status().isForbidden());

        String premiumToken = registerAndLogin("dev_premium_user", "Pass123!");
        User premiumUser = userRepository.findByUsername("dev_premium_user").orElseThrow();
        premiumUser.setPlanTier(PlanTier.PREMIUM);
        premiumUser.setBillingStatus("ACTIVE");
        userRepository.save(premiumUser);

        mockMvc.perform(post("/api/dev/keys")
                .header("Authorization", "Bearer " + premiumToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Prod Key\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Prod Key"))
            .andExpect(jsonPath("$.apiKey").isNotEmpty())
            .andExpect(jsonPath("$.keyPrefix").isNotEmpty())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/dev/usage")
                .header("Authorization", "Bearer " + premiumToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.activeKeys").value(1))
            .andExpect(jsonPath("$.summary.totalRequests").value(0))
            .andExpect(jsonPath("$.keys", hasSize(1)))
            .andExpect(jsonPath("$.keys[0].name").value("Prod Key"));

        String premiumToken2 = registerAndLogin("dev_premium_user_2", "Pass123!");
        User premiumUser2 = userRepository.findByUsername("dev_premium_user_2").orElseThrow();
        premiumUser2.setPlanTier(PlanTier.PREMIUM);
        premiumUser2.setBillingStatus("ACTIVE");
        userRepository.save(premiumUser2);

        mockMvc.perform(get("/api/dev/usage")
                .header("Authorization", "Bearer " + premiumToken2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary.activeKeys").value(0))
            .andExpect(jsonPath("$.keys", hasSize(0)));
    }

    @Test
    void sharedWatchlistReadOnlyFlowWorks() throws Exception {
        String ownerToken = registerAndLogin("share_owner", "Pass123!");
        String viewerToken = registerAndLogin("share_viewer", "Pass123!");

        String createResponse = mockMvc.perform(post("/api/watchlists")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Shared Core\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        long watchlistId = objectMapper.readTree(createResponse).path("id").asLong();

        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/symbols")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"MSFT\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/share")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"share_viewer\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("READ_ONLY"));

        mockMvc.perform(get("/api/watchlists/shared")
                .header("Authorization", "Bearer " + viewerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("Shared Core"));

        mockMvc.perform(get("/api/watchlists/" + watchlistId)
                .header("Authorization", "Bearer " + viewerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Shared Core"));

        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/symbols")
                .header("Authorization", "Bearer " + viewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"AAPL\"}"))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/watchlists/" + watchlistId + "/share/share_viewer")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deleted").value(true));

        mockMvc.perform(get("/api/watchlists/shared")
                .header("Authorization", "Bearer " + viewerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void auditEndpointReturnsAuthenticatedUsersEventsOnly() throws Exception {
        String token = registerAndLogin("audit_user", "Pass123!");

        mockMvc.perform(post("/api/watchlists")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Audit List\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[0].actorUsername").value("audit_user"));

        String tokenOther = registerAndLogin("audit_other_user", "Pass123!");

        mockMvc.perform(get("/api/audit")
                .header("Authorization", "Bearer " + tokenOther))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].actorUsername").value("audit_other_user"));
    }

    @Test
    void revokeSessionsInvalidatesOldTokenAndRequiresFreshLogin() throws Exception {
        String username = "session_user";
        String password = "Pass123!";

        String token = registerAndLogin(username, password);

        mockMvc.perform(get("/api/watchlists")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/sessions/revoke")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revoked").value(true));

        mockMvc.perform(get("/api/watchlists")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String refreshedToken = objectMapper.readTree(loginResponse).path("token").asText();

        mockMvc.perform(get("/api/watchlists")
                .header("Authorization", "Bearer " + refreshedToken))
            .andExpect(status().isOk());
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
