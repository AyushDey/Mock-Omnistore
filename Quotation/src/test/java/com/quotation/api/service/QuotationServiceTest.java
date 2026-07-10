package com.quotation.api.service;

import com.quotation.api.model.QuotationItem;
import com.quotation.api.repository.QuotationItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
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
    void testGetAll() {
        when(quotationItemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

        List<QuotationItem> result = quotationService.getAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("CLE MEULEUSE WOLFCRAFT", result.get(0).getName());
        assertEquals("PETITES BARQUETTES ALU", result.get(1).getName());
        verify(quotationItemRepository, times(1)).findAll();
    }

    @Test
    void testGetByBarcode_Found() {
        when(quotationItemRepository.findByBarcode("4006885245808")).thenReturn(Optional.of(item1));

        Optional<QuotationItem> result = quotationService.getByBarcode("4006885245808");

        assertTrue(result.isPresent());
        assertEquals("CLE MEULEUSE WOLFCRAFT", result.get().getName());
        verify(quotationItemRepository, times(1)).findByBarcode("4006885245808");
    }

    @Test
    void testGetByBarcode_NotFound() {
        when(quotationItemRepository.findByBarcode("unknown")).thenReturn(Optional.empty());

        Optional<QuotationItem> result = quotationService.getByBarcode("unknown");

        assertFalse(result.isPresent());
        verify(quotationItemRepository, times(1)).findByBarcode("unknown");
    }

    @Test
    void testGetById_Found() {
        UUID id = item1.getId();
        when(quotationItemRepository.findById(id)).thenReturn(Optional.of(item1));

        Optional<QuotationItem> result = quotationService.getById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        verify(quotationItemRepository, times(1)).findById(id);
    }

    @Test
    void testGetById_NotFound() {
        UUID randomId = UUID.randomUUID();
        when(quotationItemRepository.findById(randomId)).thenReturn(Optional.empty());

        Optional<QuotationItem> result = quotationService.getById(randomId);

        assertFalse(result.isPresent());
        verify(quotationItemRepository, times(1)).findById(randomId);
    }

    @Test
    void testSearch_EmptyQuery() {
        when(quotationItemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

        List<QuotationItem> result = quotationService.search("");

        assertEquals(2, result.size());
        verify(quotationItemRepository, times(1)).findAll();
        verify(quotationItemRepository, never()).findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase(anyString(), anyString());
    }

    @Test
    void testSearch_NullQuery() {
        when(quotationItemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

        List<QuotationItem> result = quotationService.search(null);

        assertEquals(2, result.size());
        verify(quotationItemRepository, times(1)).findAll();
        verify(quotationItemRepository, never()).findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase(anyString(), anyString());
    }

    @Test
    void testSearch_WithQuery() {
        when(quotationItemRepository.findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase("WOLFCRAFT", "WOLFCRAFT"))
                .thenReturn(Collections.singletonList(item1));

        List<QuotationItem> result = quotationService.search("WOLFCRAFT");

        assertEquals(1, result.size());
        assertEquals("CLE MEULEUSE WOLFCRAFT", result.get(0).getName());
        verify(quotationItemRepository, times(1)).findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase("WOLFCRAFT", "WOLFCRAFT");
        verify(quotationItemRepository, never()).findAll();
    }
}
