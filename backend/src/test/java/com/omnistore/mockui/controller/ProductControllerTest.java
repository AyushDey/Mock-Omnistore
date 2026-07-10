package com.omnistore.mockui.controller;

import com.omnistore.mockui.model.Product;
import com.omnistore.mockui.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@SuppressWarnings("removal")
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        product1 = Product.builder()
                .id(UUID.randomUUID())
                .name("CLE MEULEUSE WOLFCRAFT")
                .barcode("4006885245808")
                .price(new BigDecimal("13.00"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("http://example.com/p1.jpg")
                .build();

        product2 = Product.builder()
                .id(UUID.randomUUID())
                .name("PETITES BARQUETTES ALU")
                .barcode("65529254")
                .price(new BigDecimal("5.99"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("http://example.com/p2.jpg")
                .build();
    }

    @Test
    void testListAll() throws Exception {
        when(productService.listAllProducts()).thenReturn(Arrays.asList(product1, product2));

        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("CLE MEULEUSE WOLFCRAFT")))
                .andExpect(jsonPath("$[1].name", is("PETITES BARQUETTES ALU")));

        verify(productService, times(1)).listAllProducts();
    }

    @Test
    void testSearch() throws Exception {
        when(productService.searchProducts("WOLFCRAFT")).thenReturn(Collections.singletonList(product1));

        mockMvc.perform(get("/api/products/search")
                        .param("query", "WOLFCRAFT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("CLE MEULEUSE WOLFCRAFT")));

        verify(productService, times(1)).searchProducts("WOLFCRAFT");
    }

    @Test
    void testGetByBarcode_Found() throws Exception {
        when(productService.getProductByBarcode("4006885245808")).thenReturn(Optional.of(product1));

        mockMvc.perform(get("/api/products/barcode/4006885245808")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("CLE MEULEUSE WOLFCRAFT")))
                .andExpect(jsonPath("$.barcode", is("4006885245808")));

        verify(productService, times(1)).getProductByBarcode("4006885245808");
    }

    @Test
    void testGetByBarcode_NotFound() throws Exception {
        when(productService.getProductByBarcode("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/barcode/unknown")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(productService, times(1)).getProductByBarcode("unknown");
    }

    @Test
    void testCreate() throws Exception {
        Product newProduct = Product.builder()
                .name("NEW PRODUCT")
                .barcode("12345678")
                .price(new BigDecimal("9.99"))
                .taxRate(new BigDecimal("20.00"))
                .build();

        Product savedProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("NEW PRODUCT")
                .barcode("12345678")
                .price(new BigDecimal("9.99"))
                .taxRate(new BigDecimal("20.00"))
                .build();

        when(productService.saveProduct(any(Product.class))).thenReturn(savedProduct);

        String jsonBody = "{\"name\":\"NEW PRODUCT\",\"barcode\":\"12345678\",\"price\":9.99,\"taxRate\":20.00}";

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("NEW PRODUCT")))
                .andExpect(jsonPath("$.barcode", is("12345678")));

        verify(productService, times(1)).saveProduct(any(Product.class));
    }
}
