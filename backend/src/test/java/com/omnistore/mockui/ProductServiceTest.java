package com.omnistore.mockui;

import com.omnistore.mockui.model.Product;
import com.omnistore.mockui.repository.ProductRepository;
import com.omnistore.mockui.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
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
                .build();

        product2 = Product.builder()
                .id(UUID.randomUUID())
                .name("PETITES BARQUETTES ALU")
                .barcode("65529254")
                .price(new BigDecimal("5.99"))
                .taxRate(new BigDecimal("20.00"))
                .build();
    }

    @Test
    void testGetProductByBarcode_Found() {
        when(productRepository.findByBarcode("4006885245808")).thenReturn(Optional.of(product1));

        Optional<Product> result = productService.getProductByBarcode("4006885245808");

        assertTrue(result.isPresent());
        assertEquals("CLE MEULEUSE WOLFCRAFT", result.get().getName());
        assertEquals(new BigDecimal("13.00"), result.get().getPrice());
    }

    @Test
    void testGetProductByBarcode_NotFound() {
        when(productRepository.findByBarcode("invalid")).thenReturn(Optional.empty());

        Optional<Product> result = productService.getProductByBarcode("invalid");

        assertFalse(result.isPresent());
    }

    @Test
    void testSearchProducts_EmptyQuery() {
        when(productRepository.findAll()).thenReturn(Arrays.asList(product1, product2));

        List<Product> result = productService.searchProducts("");

        assertEquals(2, result.size());
        verify(productRepository, times(1)).findAll();
        verify(productRepository, never()).findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase(anyString(), anyString());
    }

    @Test
    void testSearchProducts_WithQuery() {
        when(productRepository.findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase("WOLFCRAFT", "WOLFCRAFT")).thenReturn(Arrays.asList(product1));

        List<Product> result = productService.searchProducts("WOLFCRAFT");

        assertEquals(1, result.size());
        assertEquals("CLE MEULEUSE WOLFCRAFT", result.get(0).getName());
        verify(productRepository, times(1)).findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase("WOLFCRAFT", "WOLFCRAFT");
        verify(productRepository, never()).findAll();
    }
}
