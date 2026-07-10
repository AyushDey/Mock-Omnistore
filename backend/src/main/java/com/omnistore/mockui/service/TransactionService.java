package com.omnistore.mockui.service;

import com.omnistore.mockui.model.*;
import com.omnistore.mockui.repository.ProductRepository;
import com.omnistore.mockui.repository.TransactionItemRepository;
import com.omnistore.mockui.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final WebClient quotationWebClient;

    public TransactionService(TransactionRepository transactionRepository,
                              ProductRepository productRepository,
                              TransactionItemRepository transactionItemRepository,
                              WebClient quotationWebClient) {
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
        this.transactionItemRepository = transactionItemRepository;
        this.quotationWebClient = quotationWebClient;
    }

    public Transaction getActiveTransaction() {
        Optional<Transaction> activeOpt = transactionRepository.findFirstByStatusOrderByCreatedAtDesc(TransactionStatus.ACTIVE);
        if (activeOpt.isPresent()) {
            return activeOpt.get();
        }
        
        // Create new active transaction if none exists
        Transaction newTransaction = Transaction.builder()
                .status(TransactionStatus.ACTIVE)
                .totalAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .invoiceRequired(false)
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();
        return transactionRepository.save(newTransaction);
    }

    public Transaction addItemToActiveTransaction(String barcode, int quantity) {
        Transaction active = getActiveTransaction();
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with barcode: " + barcode));

        // Cache the current price before synchronization
        BigDecimal oldPrice = product.getPrice();
        // --- Quotation API Price Sync ---
        boolean priceUpdated = syncPriceFromQuotationApi(product);

        Optional<TransactionItem> existingItemOpt = active.getItems().stream()
                .filter(item -> item.getProduct().getBarcode().equals(barcode))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            TransactionItem item = existingItemOpt.get();
            int newQty = item.getQuantity() + quantity;
            if (newQty <= 0) {
                active.getItems().remove(item);
                transactionItemRepository.delete(item);
            } else {
                item.setQuantity(newQty);
                // If price was updated from Quotation API, update existing cart item price too
                if (priceUpdated) {
                    item.setPrice(product.getPrice());
                    item.setPriceChanged(true);
                    item.setOldPrice(oldPrice);
                    log.info("Updated existing cart item price for {} to {} (old price was {}), priceChanged flagged",
                            barcode, product.getPrice(), oldPrice);
                }
            }
        } else {
            if (quantity > 0) {
                TransactionItem newItem = TransactionItem.builder()
                        .transaction(active)
                        .product(product)
                        .quantity(quantity)
                        .price(product.getPrice())
                        .priceChanged(priceUpdated)
                        .oldPrice(priceUpdated ? oldPrice : null)
                        .build();
                active.getItems().add(newItem);
            }
        }

        recalculateTotals(active);
        Transaction saved = transactionRepository.save(active);
        if (priceUpdated) {
            for (TransactionItem savedItem : saved.getItems()) {
                if (savedItem.getProduct().getBarcode().equals(barcode)) {
                    savedItem.setPriceChanged(true);
                    savedItem.setOldPrice(oldPrice);
                }
            }
        }
        return saved;
    }

    /**
     * Calls the Quotation API to check the latest price for a product.
     * If the price or tax rate differs, updates the Product entity in the Omnistore DB.
     *
     * @param product the Omnistore product to verify against the Quotation API
     * @return true if the product price was updated, false otherwise
     */
    boolean syncPriceFromQuotationApi(Product product) {
        try {
            log.info("Invoking Quotation API for barcode: {} ({}) to check for price updates with current price: {}",
                    product.getBarcode(), product.getName(), product.getPrice());
            QuotationResponse quotation = quotationWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/barcode/{barcode}")
                            .queryParam("price", product.getPrice())
                            .build(product.getBarcode()))
                    .retrieve()
                    .bodyToMono(QuotationResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (quotation == null) {
                log.warn("Quotation API returned null for barcode: {}", product.getBarcode());
                return false;
            }

            boolean updated = false;
            BigDecimal oldPrice = product.getPrice();
            BigDecimal quotationPrice = quotation.getPrice();
            boolean apiFlaggedChanged = Boolean.TRUE.equals(quotation.getPriceChanged());

            // Compare prices (Authoritative Quotation API flag or local mismatch check)
            if (quotationPrice != null && (apiFlaggedChanged || oldPrice.compareTo(quotationPrice) != 0)) {
                log.warn("=====================================================================");
                log.warn("🚨 DETAILED PRICE MISMATCH REPORT 🚨");
                log.warn("Product Barcode    : {}", product.getBarcode());
                log.warn("Product Name       : {}", product.getName());
                log.warn("Omnistore Price    : {} EUR", oldPrice);
                log.warn("Quotation Price    : {} EUR", quotationPrice);
                log.warn("Price Difference   : {} EUR", quotationPrice.subtract(oldPrice));
                log.warn("API Flagged Change : {}", apiFlaggedChanged);
                log.warn("=====================================================================");

                product.setPrice(quotationPrice);
                updated = true;
            }

            // Compare tax rates
            if (quotation.getTaxRate() != null &&
                    product.getTaxRate().compareTo(quotation.getTaxRate()) != 0) {
                log.info("Tax rate mismatch for {} (barcode: {}): Omnistore={}, Quotation={}",
                        product.getName(), product.getBarcode(), product.getTaxRate(), quotation.getTaxRate());
                product.setTaxRate(quotation.getTaxRate());
                updated = true;
            }

            if (updated) {
                productRepository.save(product);
                log.info("Product {} updated in Omnistore DB from Quotation API", product.getBarcode());
            }

            return updated;
        } catch (Exception e) {
            // Graceful fallback: if Quotation API is unreachable, continue with existing price
            log.warn("Quotation API call failed for barcode: {}. Using existing Omnistore price. Error: {}",
                    product.getBarcode(), e.getMessage());
            return false;
        }
    }

    public Transaction updateItemQuantity(UUID itemId, int quantity) {
        Transaction active = getActiveTransaction();
        TransactionItem item = active.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found in current transaction: " + itemId));

        boolean priceUpdated = false;
        BigDecimal oldPrice = null;

        if (quantity <= 0) {
            active.getItems().remove(item);
            transactionItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            // Cache the current price before synchronization
            Product product = item.getProduct();
            oldPrice = product.getPrice();
            // --- Quotation API Price Sync ---
            priceUpdated = syncPriceFromQuotationApi(product);
            if (priceUpdated) {
                item.setPrice(product.getPrice());
                item.setPriceChanged(true);
                item.setOldPrice(oldPrice);
                log.info("Updated existing cart item price during quantity update for {} to {} (old price was {}), priceChanged flagged",
                        product.getBarcode(), product.getPrice(), oldPrice);
            }
        }

        recalculateTotals(active);
        Transaction saved = transactionRepository.save(active);
        if (quantity > 0 && priceUpdated) {
            for (TransactionItem savedItem : saved.getItems()) {
                if (savedItem.getId().equals(itemId)) {
                    savedItem.setPriceChanged(true);
                    savedItem.setOldPrice(oldPrice);
                }
            }
        }
        return saved;
    }

    public Transaction removeItem(UUID itemId) {
        Transaction active = getActiveTransaction();
        TransactionItem item = active.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found in current transaction: " + itemId));

        active.getItems().remove(item);
        transactionItemRepository.delete(item);

        recalculateTotals(active);
        return transactionRepository.save(active);
    }

    public Transaction suspendActiveTransaction() {
        Transaction active = getActiveTransaction();
        if (active.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot suspend an empty transaction");
        }
        active.setStatus(TransactionStatus.SUSPENDED);
        Transaction saved = transactionRepository.save(active);
        
        // Trigger auto-creation of a new empty active transaction
        getActiveTransaction();
        return saved;
    }

    public List<Transaction> listSuspendedTransactions() {
        return transactionRepository.findByStatusOrderByCreatedAtDesc(TransactionStatus.SUSPENDED);
    }

    public List<Transaction> listAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }

    public Transaction resumeTransaction(UUID transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (tx.getStatus() != TransactionStatus.SUSPENDED) {
            throw new IllegalStateException("Transaction is not in SUSPENDED state");
        }

        // Handle existing active transaction
        Transaction active = getActiveTransaction();
        if (active.getItems().isEmpty()) {
            active.setStatus(TransactionStatus.ABANDONED);
            transactionRepository.save(active);
        } else {
            active.setStatus(TransactionStatus.SUSPENDED);
            transactionRepository.save(active);
        }

        tx.setStatus(TransactionStatus.ACTIVE);
        return transactionRepository.save(tx);
    }

    public Transaction abandonActiveTransaction() {
        Transaction active = getActiveTransaction();
        active.setStatus(TransactionStatus.ABANDONED);
        Transaction saved = transactionRepository.save(active);
        
        // Auto-create new empty transaction
        getActiveTransaction();
        return saved;
    }

    public Transaction addPaymentToActiveTransaction(PaymentMethod method, BigDecimal amount) {
        Transaction active = getActiveTransaction();
        if (active.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot make payment on an empty transaction");
        }
        
        Payment payment = Payment.builder()
                .transaction(active)
                .method(method)
                .amount(amount)
                .build();
        active.getPayments().add(payment);

        recalculateTotals(active);

        // Check if fully paid
        BigDecimal totalPaid = active.getPayments().stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(active.getTotalAmount()) >= 0) {
            active.setStatus(TransactionStatus.PAID);
        }

        return transactionRepository.save(active);
    }

    public Transaction setInvoiceRequired(boolean required) {
        Transaction active = getActiveTransaction();
        active.setInvoiceRequired(required);
        return transactionRepository.save(active);
    }

    private void recalculateTotals(Transaction tx) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        
        for (TransactionItem item : tx.getItems()) {
            BigDecimal itemPrice = item.getPrice();
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            BigDecimal itemTotal = itemPrice.multiply(qty);
            subtotal = subtotal.add(itemTotal);

            // Calculate tax-inclusive European retail VAT (e.g. 20%)
            BigDecimal taxRate = item.getProduct().getTaxRate();
            BigDecimal divisor = BigDecimal.ONE.add(taxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal priceExclTax = itemTotal.divide(divisor, 4, RoundingMode.HALF_UP);
            BigDecimal itemTax = itemTotal.subtract(priceExclTax);
            
            totalTax = totalTax.add(itemTax);
        }

        tx.setTotalAmount(subtotal.setScale(2, RoundingMode.HALF_UP));
        tx.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        // Keep discount at 0.00 unless explicitly supported
        tx.setDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }
}

