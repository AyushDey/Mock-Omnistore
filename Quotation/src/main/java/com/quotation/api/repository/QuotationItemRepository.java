package com.quotation.api.repository;

import com.quotation.api.model.QuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuotationItemRepository extends JpaRepository<QuotationItem, UUID> {
    Optional<QuotationItem> findByBarcode(String barcode);
    List<QuotationItem> findByNameContainingIgnoreCaseOrBarcodeContainingIgnoreCase(String name, String barcode);
}
