package com.omnistore.mockui.model;

import lombok.*;
import java.math.BigDecimal;

/**
 * DTO for deserializing responses from the Quotation API.
 * Used when the Omnistore backend calls the Quotation API to verify product pricing.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationResponse {

    private String name;
    private String barcode;
    private BigDecimal price;
    private BigDecimal taxRate;
    private String imageUrl;
    private String description;
    private Boolean priceChanged;
    private BigDecimal oldPrice;
}
