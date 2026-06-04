package com.quotation.api.controller;

import com.quotation.api.model.QuotationItem;
import com.quotation.api.service.QuotationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/quotation")
public class QuotationController {

    private static final Logger log = LoggerFactory.getLogger(QuotationController.class);

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @GetMapping
    public List<QuotationItem> getAll() {
        log.info("Received request to retrieve all quotation items");
        List<QuotationItem> items = quotationService.getAll();
        log.info("Retrieved {} quotation items", items.size());
        return items;
    }

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<QuotationItem> getByBarcode(
            @PathVariable("barcode") String barcode,
            @RequestParam(value = "currentPrice", required = false) BigDecimal currentPrice) {
        log.info("Received request for quotation by barcode: {} (current price passed: {})", barcode, currentPrice);
        return quotationService.getByBarcode(barcode)
                .map(item -> {
                    if (currentPrice != null && item.getPrice().compareTo(currentPrice) != 0) {
                        item.setPriceChanged(true);
                        item.setOldPrice(currentPrice);
                    } else {
                        item.setPriceChanged(false);
                    }
                    log.info("Quotation found for barcode: {} (price: {}, name: {})", barcode, item.getPrice(), item.getName());
                    return ResponseEntity.ok(item);
                })
                .orElseGet(() -> {
                    log.warn("Quotation not found for barcode: {}", barcode);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/search")
    public List<QuotationItem> search(@RequestParam("query") String query) {
        log.info("Received request to search quotations with query: {}", query);
        List<QuotationItem> results = quotationService.search(query);
        log.info("Found {} search results for query: {}", results.size(), query);
        return results;
    }
}
