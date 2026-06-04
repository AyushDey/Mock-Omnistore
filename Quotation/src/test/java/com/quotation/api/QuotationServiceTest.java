package com.quotation.api;

import com.quotation.api.model.QuotationItem;
import com.quotation.api.repository.QuotationItemRepository;
import com.quotation.api.service.QuotationService;
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
public class QuotationServiceTest {

    @Mock
    private QuotationItemRepository quotationItemRepository;

    @InjectMocks
    private QuotationService quotationService;

    private QuotationItem item1;
    private QuotationItem item2;

    @BeforeEach
    void setUp() {
        item1 = QuotationItem.builder()
                .id(UUID.randomUUID())
                .name("CLE MEULEUSE WOLFCRAFT 34/5MM")
                .barcode("4006885245808")
                .price(new BigDecimal("14.50"))
                .taxRate(new BigDecimal("20.00"))
                .description("Clé pour meuleuse Wolfcraft")
                .build();

        item2 = QuotationItem.builder()
                .id(UUID.randomUUID())
                .name("PETITES BARQUETTES ALU WEBER")
                .barcode("65529254")
                .price(new BigDecimal("5.99"))
                .taxRate(new BigDecimal("20.00"))
                .description("Petites barquettes aluminium Weber")
                .build();
    }

    @Test
    void testGetByBarcode_Found() {
        when(quotationItemRepository.findByBarcode("4006885245808")).thenReturn(Optional.of(item1));

        Optional<QuotationItem> result = quotationService.getByBarcode("4006885245808");

        assertTrue(result.isPresent());
        assertEquals("CLE MEULEUSE WOLFCRAFT 34/5MM", result.get().getName());
        assertEquals(new BigDecimal("14.50"), result.get().getPrice());
        assertEquals(new BigDecimal("20.00"), result.get().getTaxRate());
        verify(quotationItemRepository, times(1)).findByBarcode("4006885245808");
    }

    @Test
    void testGetByBarcode_NotFound() {
        when(quotationItemRepository.findByBarcode("INVALID_BARCODE")).thenReturn(Optional.empty());

        Optional<QuotationItem> result = quotationService.getByBarcode("INVALID_BARCODE");

        assertFalse(result.isPresent());
        verify(quotationItemRepository, times(1)).findByBarcode("INVALID_BARCODE");
    }

    @Test
    void testGetAll() {
        when(quotationItemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

        List<QuotationItem> result = quotationService.getAll();

        assertEquals(2, result.size());
        assertEquals("CLE MEULEUSE WOLFCRAFT 34/5MM", result.get(0).getName());
        assertEquals("PETITES BARQUETTES ALU WEBER", result.get(1).getName());
        verify(quotationItemRepository, times(1)).findAll();
    }

    @Test
    void testSearch_WithQuery() {
        when(quotationItemRepository.findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase("WOLFCRAFT", "WOLFCRAFT"))
                .thenReturn(Arrays.asList(item1));

        List<QuotationItem> result = quotationService.search("WOLFCRAFT");

        assertEquals(1, result.size());
        assertEquals("CLE MEULEUSE WOLFCRAFT 34/5MM", result.get(0).getName());
        verify(quotationItemRepository, times(1))
                .findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase("WOLFCRAFT", "WOLFCRAFT");
        verify(quotationItemRepository, never()).findAll();
    }

    @Test
    void testSearch_EmptyQuery() {
        when(quotationItemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

        List<QuotationItem> result = quotationService.search("");

        assertEquals(2, result.size());
        verify(quotationItemRepository, times(1)).findAll();
        verify(quotationItemRepository, never())
                .findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase(anyString(), anyString());
    }
}
