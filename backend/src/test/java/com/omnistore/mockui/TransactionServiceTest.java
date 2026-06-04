package com.omnistore.mockui;

import com.omnistore.mockui.model.*;
import com.omnistore.mockui.repository.ProductRepository;
import com.omnistore.mockui.repository.TransactionItemRepository;
import com.omnistore.mockui.repository.TransactionRepository;
import com.omnistore.mockui.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "rawtypes", "unused"})
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TransactionItemRepository transactionItemRepository;

    @Mock
    private WebClient quotationWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private TransactionService transactionService;

    private Product product1;
    private Product product2;
    private Transaction activeTransaction;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository, productRepository,
                transactionItemRepository, quotationWebClient);

        product1 = Product.builder()
                .id(UUID.randomUUID())
                .name("CLE MEULEUSE WOLFCRAFT")
                .barcode("4006885245808")
                .price(new BigDecimal("13.00"))
                .taxRate(new BigDecimal("20.00")) // 20%
                .build();

        product2 = Product.builder()
                .id(UUID.randomUUID())
                .name("PETITES BARQUETTES ALU")
                .barcode("65529254")
                .price(new BigDecimal("5.99"))
                .taxRate(new BigDecimal("20.00")) // 20%
                .build();

        activeTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .status(TransactionStatus.ACTIVE)
                .totalAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();
    }

    /**
     * Helper to set up WebClient mock chain for a successful Quotation API response.
     */
    @SuppressWarnings("unchecked")
    private void mockQuotationApiResponse(QuotationResponse response) {
        when(quotationWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(QuotationResponse.class)).thenReturn(Mono.just(response));
    }

    /**
     * Helper to set up WebClient mock chain for a Quotation API failure.
     */
    @SuppressWarnings("unchecked")
    private void mockQuotationApiFailure() {
        when(quotationWebClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(QuotationResponse.class)).thenReturn(Mono.error(new RuntimeException("Connection refused")));
    }

    // ==========================================
    // Existing Tests (unchanged behavior)
    // ==========================================

    @Test
    void testGetActiveTransaction_Existing() {
        when(transactionRepository.findFirstByStatusOrderByCreatedAtDesc(TransactionStatus.ACTIVE))
                .thenReturn(Optional.of(activeTransaction));

        Transaction result = transactionService.getActiveTransaction();

        assertNotNull(result);
        assertEquals(TransactionStatus.ACTIVE, result.getStatus());
        assertEquals(activeTransaction.getId(), result.getId());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testGetActiveTransaction_New() {
        when(transactionRepository.findFirstByStatusOrderByCreatedAtDesc(TransactionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = transactionService.getActiveTransaction();

        assertNotNull(result);
        assertEquals(TransactionStatus.ACTIVE, result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getTotalAmount());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testAddItemToActiveTransaction_NewItem() {
        // Mock quotation API returning same price
        mockQuotationApiResponse(QuotationResponse.builder()
                .barcode("4006885245808").price(new BigDecimal("13.00")).taxRate(new BigDecimal("20.00")).build());

        when(transactionRepository.findFirstByStatusOrderByCreatedAtDesc(TransactionStatus.ACTIVE))
                .thenReturn(Optional.of(activeTransaction));
        when(productRepository.findByBarcode("4006885245808")).thenReturn(Optional.of(product1));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = transactionService.addItemToActiveTransaction("4006885245808", 1);

        assertEquals(1, result.getItems().size());
        assertEquals(new BigDecimal("13.00"), result.getTotalAmount());
        // tax-inclusive 20% tax calculation: 13.00 - (13.00 / 1.2) = 13.00 - 10.8333 = 2.17
        assertEquals(new BigDecimal("2.17"), result.getTaxAmount());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testAddItemToActiveTransaction_ExistingItem() {
        // Mock quotation API returning same price
        mockQuotationApiResponse(QuotationResponse.builder()
                .barcode("4006885245808").price(new BigDecimal("13.00")).taxRate(new BigDecimal("20.00")).build());

        // Prepare active transaction with 1 wolfcraft wrench already in it
        TransactionItem item = TransactionItem.builder()
                .transaction(activeTransaction)
                .product(product1)
                .quantity(1)
                .price(product1.getPrice())
                .build();
        activeTransaction.getItems().add(item);

        when(transactionRepository.findFirstByStatusOrderByCreatedAtDesc(TransactionStatus.ACTIVE))
                .thenReturn(Optional.of(activeTransaction));
        when(productRepository.findByBarcode("4006885245808")).thenReturn(Optional.of(product1));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = transactionService.addItemToActiveTransaction("4006885245808", 1);

        assertEquals(1, result.getItems().size());
        assertEquals(2, result.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("26.00"), result.getTotalAmount());
        // tax-inclusive 20% tax: 26.00 - (26.00 / 1.20) = 4.33
        assertEquals(new BigDecimal("4.33"), result.getTaxAmount());
    }

    @Test
    void testSuspendActiveTransaction() {
        // Add item so suspension is allowed
        TransactionItem item = TransactionItem.builder()
                .transaction(activeTransaction)
                .product(product1)
                .quantity(1)
                .price(product1.getPrice())
                .build();
        activeTransaction.getItems().add(item);

        when(transactionRepository.findFirstByStatusOrderByCreatedAtDesc(TransactionStatus.ACTIVE))
                .thenReturn(Optional.of(activeTransaction))
                .thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = transactionService.suspendActiveTransaction();

        assertEquals(TransactionStatus.SUSPENDED, result.getStatus());
        verify(transactionRepository, times(2)).save(any(Transaction.class)); // 1 for active suspend, 1 for next active init
    }

    @Test
    void testAddPaymentToActiveTransaction_PaidInFull() {
        TransactionItem item = TransactionItem.builder()
                .transaction(activeTransaction)
                .product(product1)
                .quantity(1)
                .price(product1.getPrice())
                .build();
        activeTransaction.getItems().add(item);
        activeTransaction.setTotalAmount(new BigDecimal("13.00"));

        when(transactionRepository.findFirstByStatusOrderByCreatedAtDesc(TransactionStatus.ACTIVE))
                .thenReturn(Optional.of(activeTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = transactionService.addPaymentToActiveTransaction(PaymentMethod.CASH, new BigDecimal("15.00"));

        assertEquals(1, result.getPayments().size());
        assertEquals(TransactionStatus.PAID, result.getStatus());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    // ==========================================
    // NEW: Quotation API Integration Tests
    // ==========================================

    @Test
    void testAddItem_PriceSyncFromQuotationAPI() {
        // Quotation API returns a DIFFERENT price (14.50 instead of 13.00)
        mockQuotationApiResponse(QuotationResponse.builder()
                .barcode("4006885245808")
                .price(new BigDecimal("14.50"))
                .taxRate(new BigDecimal("20.00"))
                .name("CLE MEULEUSE WOLFCRAFT")
                .build());

        when(transactionRepository.findFirstByStatusOrderByCreatedAtDesc(TransactionStatus.ACTIVE))
                .thenReturn(Optional.of(activeTransaction));
        when(productRepository.findByBarcode("4006885245808")).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = transactionService.addItemToActiveTransaction("4006885245808", 1);

        // Verify the cart item uses the quotation price
        assertEquals(1, result.getItems().size());
        assertEquals(new BigDecimal("14.50"), result.getItems().get(0).getPrice());

        // Verify the product entity was updated in the DB
        assertEquals(new BigDecimal("14.50"), product1.getPrice());
        verify(productRepository, times(1)).save(product1);

        // Verify totals are recalculated with the new price
        assertEquals(new BigDecimal("14.50"), result.getTotalAmount());
        // tax-inclusive 20% tax: 14.50 - (14.50 / 1.2) = 14.50 - 12.0833 = 2.42
        assertEquals(new BigDecimal("2.42"), result.getTaxAmount());
    }

    @Test
    void testAddItem_QuotationAPI_SamePrice() {
        // Quotation API returns the SAME price (13.00)
        mockQuotationApiResponse(QuotationResponse.builder()
                .barcode("4006885245808")
                .price(new BigDecimal("13.00"))
                .taxRate(new BigDecimal("20.00"))
                .name("CLE MEULEUSE WOLFCRAFT")
                .build());

        when(transactionRepository.findFirstByStatusOrderByCreatedAtDesc(TransactionStatus.ACTIVE))
                .thenReturn(Optional.of(activeTransaction));
        when(productRepository.findByBarcode("4006885245808")).thenReturn(Optional.of(product1));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = transactionService.addItemToActiveTransaction("4006885245808", 1);

        // Verify price unchanged
        assertEquals(new BigDecimal("13.00"), result.getItems().get(0).getPrice());
        assertEquals(new BigDecimal("13.00"), product1.getPrice());

        // Verify product was NOT saved again (no update needed)
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testAddItem_QuotationAPI_Unavailable() {
        // Quotation API is down
        mockQuotationApiFailure();

        when(transactionRepository.findFirstByStatusOrderByCreatedAtDesc(TransactionStatus.ACTIVE))
                .thenReturn(Optional.of(activeTransaction));
        when(productRepository.findByBarcode("4006885245808")).thenReturn(Optional.of(product1));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Should NOT throw — graceful fallback
        Transaction result = transactionService.addItemToActiveTransaction("4006885245808", 1);

        // Verify item was still added with existing Omnistore price
        assertEquals(1, result.getItems().size());
        assertEquals(new BigDecimal("13.00"), result.getItems().get(0).getPrice());
        assertEquals(new BigDecimal("13.00"), product1.getPrice());

        // Product should not be updated when API is down
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void testAddItem_ExistingItemPriceUpdate() {
        // Quotation API returns a DIFFERENT price (14.50)
        mockQuotationApiResponse(QuotationResponse.builder()
                .barcode("4006885245808")
                .price(new BigDecimal("14.50"))
                .taxRate(new BigDecimal("20.00"))
                .name("CLE MEULEUSE WOLFCRAFT")
                .build());

        // Existing cart item with old price
        TransactionItem existingItem = TransactionItem.builder()
                .id(UUID.randomUUID())
                .transaction(activeTransaction)
                .product(product1)
                .quantity(1)
                .price(new BigDecimal("13.00")) // old price in cart
                .build();
        activeTransaction.getItems().add(existingItem);

        when(transactionRepository.findFirstByStatusOrderByCreatedAtDesc(TransactionStatus.ACTIVE))
                .thenReturn(Optional.of(activeTransaction));
        when(productRepository.findByBarcode("4006885245808")).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Add 1 more of the same item
        Transaction result = transactionService.addItemToActiveTransaction("4006885245808", 1);

        // Verify item quantity is increased
        assertEquals(1, result.getItems().size());
        assertEquals(2, result.getItems().get(0).getQuantity());

        // Verify the cart item price was updated to the quotation price
        assertEquals(new BigDecimal("14.50"), result.getItems().get(0).getPrice());

        // Verify product was updated
        verify(productRepository, times(1)).save(product1);
        assertEquals(new BigDecimal("14.50"), product1.getPrice());

        // Verify totals: 14.50 * 2 = 29.00
        assertEquals(new BigDecimal("29.00"), result.getTotalAmount());
    }
}
