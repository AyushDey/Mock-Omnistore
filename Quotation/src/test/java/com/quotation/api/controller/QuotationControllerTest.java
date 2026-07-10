package com.quotation.api.controller;

import com.quotation.api.model.QuotationItem;
import com.quotation.api.service.QuotationService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuotationController.class)
@SuppressWarnings("removal")
public class QuotationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuotationService quotationService;

    private QuotationItem item1;
    private QuotationItem item2;

    @BeforeEach
    void setUp() {
        item1 = QuotationItem.builder()
                .id(UUID.randomUUID())
                .name("CLE MEULEUSE WOLFCRAFT")
                .barcode("4006885245808")
                .price(new BigDecimal("13.00"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("http://example.com/item1.jpg")
                .description("Meuleuse wrench")
                .build();

        item2 = QuotationItem.builder()
                .id(UUID.randomUUID())
                .name("PETITES BARQUETTES ALU")
                .barcode("65529254")
                .price(new BigDecimal("5.99"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("http://example.com/item2.jpg")
                .description("Aluminium plates")
                .build();
    }

    @Test
    void testGetAll() throws Exception {
        when(quotationService.getAll()).thenReturn(Arrays.asList(item1, item2));

        mockMvc.perform(get("/api/quotation")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("CLE MEULEUSE WOLFCRAFT")))
                .andExpect(jsonPath("$[1].name", is("PETITES BARQUETTES ALU")));

        verify(quotationService, times(1)).getAll();
    }

    @Test
    void testGetByBarcode_Found_NoPriceParam() throws Exception {
        when(quotationService.getByBarcode("4006885245808")).thenReturn(Optional.of(item1));

        mockMvc.perform(get("/api/quotation/barcode/4006885245808")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("CLE MEULEUSE WOLFCRAFT")))
                .andExpect(jsonPath("$.price", is(13.00)))
                .andExpect(jsonPath("$.priceChanged", is(false)));

        verify(quotationService, times(1)).getByBarcode("4006885245808");
    }

    @Test
    void testGetByBarcode_Found_PriceParam_SamePrice() throws Exception {
        when(quotationService.getByBarcode("4006885245808")).thenReturn(Optional.of(item1));

        mockMvc.perform(get("/api/quotation/barcode/4006885245808")
                        .param("price", "13.00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("CLE MEULEUSE WOLFCRAFT")))
                .andExpect(jsonPath("$.priceChanged", is(false)));

        verify(quotationService, times(1)).getByBarcode("4006885245808");
    }

    @Test
    void testGetByBarcode_Found_PriceParam_DifferentPrice() throws Exception {
        when(quotationService.getByBarcode("4006885245808")).thenReturn(Optional.of(item1));

        mockMvc.perform(get("/api/quotation/barcode/4006885245808")
                        .param("price", "15.50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("CLE MEULEUSE WOLFCRAFT")))
                .andExpect(jsonPath("$.priceChanged", is(true)));

        verify(quotationService, times(1)).getByBarcode("4006885245808");
    }

    @Test
    void testGetByBarcode_NotFound() throws Exception {
        when(quotationService.getByBarcode("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/quotation/barcode/unknown")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(quotationService, times(1)).getByBarcode("unknown");
    }

    @Test
    void testSearch() throws Exception {
        when(quotationService.search("WOLFCRAFT")).thenReturn(Collections.singletonList(item1));

        mockMvc.perform(get("/api/quotation/search")
                        .param("query", "WOLFCRAFT")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("CLE MEULEUSE WOLFCRAFT")));

        verify(quotationService, times(1)).search("WOLFCRAFT");
    }
}
