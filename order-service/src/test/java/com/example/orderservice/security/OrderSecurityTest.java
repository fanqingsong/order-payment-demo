package com.example.orderservice.security;

import com.example.orderservice.config.SecurityConfig;
import com.example.orderservice.controller.AdminOrderController;
import com.example.orderservice.controller.OrderController;
import com.example.orderservice.entity.Role;
import com.example.orderservice.service.OrderService;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OrderController.class, AdminOrderController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class OrderSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsService userDetailsService;

    @Test
    void rejectsMissingJwtForAuthenticatedEndpoint() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidJwtForAuthenticatedEndpoint() throws Exception {
        when(jwtService.parseToken(anyString())).thenThrow(new MalformedJwtException("Invalid token"));

        mockMvc.perform(get("/api/orders").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forbidsUserFromAdminEndpoint() throws Exception {
        AppUserPrincipal principal = principal(Role.USER);

        mockMvc.perform(get("/api/admin/orders").with(user(principal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminToAccessAdminEndpoint() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/orders").with(user(principal(Role.ADMIN))))
                .andExpect(status().isOk());

        verify(orderService).getAllOrders();
    }

    @Test
    void createsOrderForAuthenticatedPrincipal() throws Exception {
        AppUserPrincipal principal = principal(Role.USER);

        mockMvc.perform(post("/api/orders")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productName": "Mechanical Keyboard",
                                  "quantity": 2,
                                  "unitPrice": 49.95
                                }
                                """))
                .andExpect(status().isCreated());

        verify(orderService).createOrder(
                org.mockito.ArgumentMatchers.eq(principal.id()),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private AppUserPrincipal principal(Role role) {
        return new AppUserPrincipal(UUID.randomUUID(), "learner@example.com", "encoded-password", role);
    }
}
