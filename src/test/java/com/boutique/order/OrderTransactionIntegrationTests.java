package com.boutique.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.boutique.customer.Customer;
import com.boutique.customer.CustomerRepository;
import com.boutique.product.Product;
import com.boutique.product.ProductRepository;
import com.boutique.security.JwtService;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class OrderTransactionIntegrationTests {

    private static final int SEEDED_STOCK = 20;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private Customer alice;
    private Product product;
    private String aliceToken;

    @BeforeEach
    void prepareIsolatedFixtures() {
        resetOrdersAndStock();
        alice = customerRepository.findByEmail("alice@example.com").orElseThrow();
        aliceToken = jwtService.generateToken(alice);
        product = findSeedProduct();
    }

    @AfterEach
    void cleanUpFixtures() {
        resetOrdersAndStock();
    }

    @Test
    void creatingOrderPersistsOrderAndDecrementsStock() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest("{\"productId\":" + product.getId() + ",\"quantity\":3}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total").value(269.70))
                .andExpect(jsonPath("$.items[0].quantity").value(3));

        assertEquals(SEEDED_STOCK - 3, currentStock());
        assertEquals(1, orderRepository.count());
    }

    @Test
    void failureAfterFirstReservationRollsBackStockAndOrder() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest(
                                "{\"productId\":" + product.getId() + ",\"quantity\":2},"
                                        + "{\"productId\":" + product.getId() + ",\"quantity\":100}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertEquals(SEEDED_STOCK, currentStock());
        assertEquals(0, orderRepository.count());
    }

    @Test
    void cancellingOrderRestoresStockOnlyOnce() throws Exception {
        MvcResult creation = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderRequest("{\"productId\":" + product.getId() + ",\"quantity\":4}")))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(creation.getResponse().getContentAsString())
                .get("id").asLong();
        assertEquals(SEEDED_STOCK - 4, currentStock());

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(SEEDED_STOCK, currentStock());

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isConflict());
        assertEquals(SEEDED_STOCK, currentStock());
        assertEquals(1, orderRepository.count());
    }

    private String orderRequest(String items) {
        return "{\"customerId\":" + alice.getId() + ",\"items\":[" + items + "]}";
    }

    private int currentStock() {
        return productRepository.findById(product.getId()).orElseThrow().getStock();
    }

    private Product findSeedProduct() {
        return productRepository.findAll().stream()
                .filter(candidate -> "Clavier mécanique".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
    }

    private void resetOrdersAndStock() {
        orderRepository.deleteAll();
        orderRepository.flush();
        productRepository.findAll().stream()
                .filter(candidate -> "Clavier mécanique".equals(candidate.getName()))
                .findFirst()
                .ifPresent(candidate -> {
                    candidate.setStock(SEEDED_STOCK);
                    productRepository.saveAndFlush(candidate);
                });
    }
}