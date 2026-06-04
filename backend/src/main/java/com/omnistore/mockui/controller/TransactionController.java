package com.omnistore.mockui.controller;

import com.omnistore.mockui.model.PaymentMethod;
import com.omnistore.mockui.model.Transaction;
import com.omnistore.mockui.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/active")
    public Transaction getActive() {
        return transactionService.getActiveTransaction();
    }

    @PostMapping("/items")
    public Transaction addItem(@RequestParam("barcode") String barcode,
                               @RequestParam(value = "quantity", defaultValue = "1") int quantity) {
        return transactionService.addItemToActiveTransaction(barcode, quantity);
    }

    @PutMapping("/items/{itemId}")
    public Transaction updateItemQuantity(@PathVariable("itemId") UUID itemId,
                                          @RequestParam("quantity") int quantity) {
        return transactionService.updateItemQuantity(itemId, quantity);
    }

    @DeleteMapping("/items/{itemId}")
    public Transaction removeItem(@PathVariable("itemId") UUID itemId) {
        return transactionService.removeItem(itemId);
    }

    @PostMapping("/suspend")
    public Transaction suspend() {
        return transactionService.suspendActiveTransaction();
    }

    @GetMapping("/suspended")
    public List<Transaction> getSuspended() {
        return transactionService.listSuspendedTransactions();
    }

    @GetMapping
    public List<Transaction> getAll() {
        return transactionService.listAllTransactions();
    }

    @PostMapping("/resume/{id}")
    public Transaction resume(@PathVariable("id") UUID id) {
        return transactionService.resumeTransaction(id);
    }

    @PostMapping("/abandon")
    public Transaction abandon() {
        return transactionService.abandonActiveTransaction();
    }

    @PostMapping("/pay")
    public Transaction pay(@RequestParam("method") PaymentMethod method,
                           @RequestParam("amount") BigDecimal amount) {
        return transactionService.addPaymentToActiveTransaction(method, amount);
    }

    @PostMapping("/invoice")
    public Transaction toggleInvoice(@RequestParam("required") boolean required) {
        return transactionService.setInvoiceRequired(required);
    }
}
