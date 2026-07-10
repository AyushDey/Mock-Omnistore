package com.omnistore.mockui.controller;

import com.omnistore.mockui.model.PaymentMethod;
import com.omnistore.mockui.model.Transaction;
import com.omnistore.mockui.model.TransactionStatus;
import com.omnistore.mockui.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@SuppressWarnings("removal")
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    private Transaction transaction;
    private UUID transactionId;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        transactionId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.ACTIVE)
                .totalAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .invoiceRequired(false)
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();
    }

    @Test
    void testGetActive() throws Exception {
        when(transactionService.getActiveTransaction()).thenReturn(transaction);

        mockMvc.perform(get("/api/transactions/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(transactionId.toString())))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        verify(transactionService, times(1)).getActiveTransaction();
    }

    @Test
    void testAddItem() throws Exception {
        when(transactionService.addItemToActiveTransaction("4006885245808", 2)).thenReturn(transaction);

        mockMvc.perform(post("/api/transactions/items")
                        .param("barcode", "4006885245808")
                        .param("quantity", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(transactionId.toString())));

        verify(transactionService, times(1)).addItemToActiveTransaction("4006885245808", 2);
    }

    @Test
    void testUpdateItemQuantity() throws Exception {
        when(transactionService.updateItemQuantity(itemId, 5)).thenReturn(transaction);

        mockMvc.perform(put("/api/transactions/items/" + itemId)
                        .param("quantity", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(transactionId.toString())));

        verify(transactionService, times(1)).updateItemQuantity(itemId, 5);
    }

    @Test
    void testRemoveItem() throws Exception {
        when(transactionService.removeItem(itemId)).thenReturn(transaction);

        mockMvc.perform(delete("/api/transactions/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(transactionId.toString())));

        verify(transactionService, times(1)).removeItem(itemId);
    }

    @Test
    void testSuspend() throws Exception {
        transaction.setStatus(TransactionStatus.SUSPENDED);
        when(transactionService.suspendActiveTransaction()).thenReturn(transaction);

        mockMvc.perform(post("/api/transactions/suspend")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUSPENDED")));

        verify(transactionService, times(1)).suspendActiveTransaction();
    }

    @Test
    void testGetSuspended() throws Exception {
        transaction.setStatus(TransactionStatus.SUSPENDED);
        when(transactionService.listSuspendedTransactions()).thenReturn(Collections.singletonList(transaction));

        mockMvc.perform(get("/api/transactions/suspended")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("SUSPENDED")));

        verify(transactionService, times(1)).listSuspendedTransactions();
    }

    @Test
    void testGetAll() throws Exception {
        when(transactionService.listAllTransactions()).thenReturn(Collections.singletonList(transaction));

        mockMvc.perform(get("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(transactionService, times(1)).listAllTransactions();
    }

    @Test
    void testResume() throws Exception {
        when(transactionService.resumeTransaction(transactionId)).thenReturn(transaction);

        mockMvc.perform(post("/api/transactions/resume/" + transactionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(transactionId.toString())));

        verify(transactionService, times(1)).resumeTransaction(transactionId);
    }

    @Test
    void testAbandon() throws Exception {
        transaction.setStatus(TransactionStatus.ABANDONED);
        when(transactionService.abandonActiveTransaction()).thenReturn(transaction);

        mockMvc.perform(post("/api/transactions/abandon")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ABANDONED")));

        verify(transactionService, times(1)).abandonActiveTransaction();
    }

    @Test
    void testPay() throws Exception {
        transaction.setStatus(TransactionStatus.PAID);
        when(transactionService.addPaymentToActiveTransaction(PaymentMethod.CARD, new BigDecimal("10.00")))
                .thenReturn(transaction);

        mockMvc.perform(post("/api/transactions/pay")
                        .param("method", "CARD")
                        .param("amount", "10.00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")));

        verify(transactionService, times(1)).addPaymentToActiveTransaction(PaymentMethod.CARD, new BigDecimal("10.00"));
    }

    @Test
    void testToggleInvoice() throws Exception {
        transaction.setInvoiceRequired(true);
        when(transactionService.setInvoiceRequired(true)).thenReturn(transaction);

        mockMvc.perform(post("/api/transactions/invoice")
                        .param("required", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceRequired", is(true)));

        verify(transactionService, times(1)).setInvoiceRequired(true);
    }
}
