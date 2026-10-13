package com.boutique.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.customer.Customer;
import com.boutique.customer.CustomerRepository;
import com.boutique.product.ProductRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiSecurityAndValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

        @Autowired
        private ProductRepository productRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private Customer alice;
    private Customer bob;
    private Customer admin;

    @BeforeEach
    void loadDemoCustomers() {
        alice = customerRepository.findByEmail("alice@example.com").orElseThrow();
        bob = customerRepository.findByEmail("bob@example.com").orElseThrow();
        admin = customerRepository.findByEmail("admin@boutique.com").orElseThrow();
    }

    @Test
    void catalogueEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRequestWithoutTokenUsesCommonUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/customers/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void rejectsMalformedAndExpiredJwt() throws Exception {
        mockMvc.perform(get("/api/customers/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/customers/me").header("Authorization", "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginIssuesUsableSignedCustomerJwt() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","password":"alice1234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        mockMvc.perform(get("/api/customers/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void customerCanAccessOnlyTheirOwnCustomerResource() throws Exception {
        String aliceToken = tokenFor(alice);
        mockMvc.perform(get("/api/customers/{id}", alice.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        mockMvc.perform(get("/api/customers/{id}", bob.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void customerCanAccessOnlyTheirOwnOrdersAndCannotCreateForAnotherCustomer() throws Exception {
        Long productId = productRepository.findAll().get(0).getId();
        String aliceToken = tokenFor(alice);
        MvcResult createdOrder = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + alice.getId()
                                + ",\"items\":[{\"productId\":" + productId + ",\"quantity\":1}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(createdOrder.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + tokenFor(bob)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + bob.getId()
                                + ",\"items\":[{\"productId\":" + productId + ",\"quantity\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerListingIsAdminOnly() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + tokenFor(alice)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").exists());
    }

    @Test
    void customerCannotMutateCatalogOrChangeOrderStatus() throws Exception {
        String token = tokenFor(alice);
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New category\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/orders/1/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidRegistrationReturnsCommonValidationResponse() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Données invalides"))
                .andExpect(jsonPath("$.details.firstName").exists())
                .andExpect(jsonPath("$.details.email").exists());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"" + "a".repeat(101)
                                + "\",\"lastName\":\"Test\",\"email\":\"not-an-email\","
                                + "\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.firstName").exists())
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    void invalidCatalogAndOrderRequestsReturnValidationDetails() throws Exception {
        String adminToken = tokenFor(admin);
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":0,\"stock\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.price").exists())
                .andExpect(jsonPath("$.details.stock").exists())
                .andExpect(jsonPath("$.details.categoryId").exists());

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + tokenFor(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":null,\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.customerId").exists())
                .andExpect(jsonPath("$.details.items").exists());

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + tokenFor(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":" + alice.getId()
                                + ",\"items\":[{\"productId\":1,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void missingResourceUsesCommonNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/products/9223372036854775807"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void duplicateCategoryUsesCommonConflictResponse() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Informatique\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.details").isMap());
    }

        @Test
        void deletingCategoryReferencedByProductReturnsCommonConflictResponse() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                                .delete("/api/categories/1")
                                                .header("Authorization", "Bearer " + tokenFor(admin)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.error").value("Conflict"));
        }

    private String tokenFor(Customer customer) {
        return jwtService.generateToken(customer);
    }

    private String expiredToken() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(60))
                .subject(alice.getEmail())
                .claim(JwtService.CLAIM_CUSTOMER_ID, alice.getId())
                .claim(JwtService.CLAIM_ROLES, List.of(alice.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}