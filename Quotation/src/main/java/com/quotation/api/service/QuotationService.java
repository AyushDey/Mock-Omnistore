package com.quotation.api.service;

import com.quotation.api.model.QuotationItem;
import com.quotation.api.repository.QuotationItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class QuotationService {

    private final QuotationItemRepository quotationItemRepository;

    public QuotationService(QuotationItemRepository quotationItemRepository) {
        this.quotationItemRepository = quotationItemRepository;
    }

    public List<QuotationItem> getAll() {
        return quotationItemRepository.findAll();
    }

    public Optional<QuotationItem> getByBarcode(String barcode) {
        return quotationItemRepository.findByBarcode(barcode);
    }

    public Optional<QuotationItem> getById(UUID id) {
        return quotationItemRepository.findById(id);
    }

    public List<QuotationItem> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAll();
        }
        return quotationItemRepository.findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase(query, query);
    }
}
