package com.omnistore.mockui.config;

import com.omnistore.mockui.model.Product;
import com.omnistore.mockui.model.Transaction;
import com.omnistore.mockui.model.TransactionItem;
import com.omnistore.mockui.model.TransactionStatus;
import com.omnistore.mockui.repository.ProductRepository;
import com.omnistore.mockui.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;

    public DatabaseSeeder(ProductRepository productRepository,
                          TransactionRepository transactionRepository) {
        this.productRepository = productRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @SuppressWarnings("null")
    public void run(String... args) {
        System.out.println("Seeding/Resetting products...");
        List<Product> defaultProducts = new ArrayList<>();
        
        // Original 6 products
        defaultProducts.add(Product.builder()
                .name("CLE MEULEUSE WOLFCRAFT 34/5MM")
                .barcode("4006885245808")
                .price(new BigDecimal("13.00"))
                .imageUrl("assets/images/cle_meuleuse.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("PETITES BARQUETTES ALU WEBER")
                .barcode("65529254")
                .price(new BigDecimal("5.99"))
                .imageUrl("assets/images/barquettes_alu.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("TOURNEVIS CRUCIFORME DEXTER PRO")
                .barcode("3276007391124")
                .price(new BigDecimal("3.50"))
                .imageUrl("assets/images/tournevis.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("MARTEAU RIVOIR DEXTER 32MM")
                .barcode("3276000150964")
                .price(new BigDecimal("8.90"))
                .imageUrl("assets/images/marteau.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("PERCEUSE A PERCUSSION DEXTER 900W")
                .barcode("3276000703672")
                .price(new BigDecimal("49.90"))
                .imageUrl("assets/images/perceuse.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("VIS A BOIS DEXTER 4X40MM 200PCS")
                .barcode("3276000155020")
                .price(new BigDecimal("6.50"))
                .imageUrl("assets/images/vis_bois.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        // 10 New Products
        defaultProducts.add(Product.builder()
                .name("SCIE CIRCULAIRE MAKITA 1200W")
                .barcode("0088381612036")
                .price(new BigDecimal("99.90"))
                .imageUrl("assets/images/scie_circulaire.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("MEULEUSE D'ANGLE BOSCH PROFESSIONAL")
                .barcode("3165140829120")
                .price(new BigDecimal("45.00"))
                .imageUrl("assets/images/meuleuse_bosch.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("POSTE A SOUDER DECA I-ARC 120")
                .barcode("8004828014524")
                .price(new BigDecimal("119.00"))
                .imageUrl("assets/images/poste_souder.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("COFFRET DE DOUILLES FACOM 38PCS")
                .barcode("3148517894236")
                .price(new BigDecimal("149.00"))
                .imageUrl("assets/images/coffret_douilles.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("NIVEAU LASER ROTATIF STANLEY FATMAX")
                .barcode("3253561771370")
                .price(new BigDecimal("229.00"))
                .imageUrl("assets/images/niveau_laser.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("NETTOYEUR HAUTE PRESSION KARCHER K4")
                .barcode("4054278120364")
                .price(new BigDecimal("189.90"))
                .imageUrl("assets/images/nettoyeur_karcher.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("COMPRESSEUR BICYLINDRE MICHELIN 50L")
                .barcode("3760015907421")
                .price(new BigDecimal("159.00"))
                .imageUrl("assets/images/compresseur_michelin.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("ETABLI D'ATELIER EN BOIS DEXTER")
                .barcode("3276007123985")
                .price(new BigDecimal("89.00"))
                .imageUrl("assets/images/etabli_dexter.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("ASPIRATEUR DE CHANTIER KARCHER WD3")
                .barcode("4054278049689")
                .price(new BigDecimal("69.90"))
                .imageUrl("assets/images/aspirateur_karcher.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        defaultProducts.add(Product.builder()
                .name("PROJECTEUR DE CHANTIER LED DEXTER")
                .barcode("3276000702453")
                .price(new BigDecimal("19.90"))
                .imageUrl("assets/images/projecteur_dexter.png")
                .taxRate(new BigDecimal("20.00"))
                .build());

        for (Product defaultProduct : defaultProducts) {
            Product productToSave = productRepository.findByBarcode(defaultProduct.getBarcode())
                    .map(existing -> {
                        existing.setName(defaultProduct.getName());
                        existing.setPrice(defaultProduct.getPrice());
                        existing.setImageUrl(defaultProduct.getImageUrl());
                        existing.setTaxRate(defaultProduct.getTaxRate());
                        return existing;
                    })
                    .orElse(defaultProduct);
            productRepository.save(productToSave);
        }
        System.out.println("Products seeded/reset successfully.");

        // Pre-fill or reset the active transaction
        Optional<Transaction> activeTxOpt = transactionRepository
                .findFirstByStatusOrderByCreatedAtDesc(TransactionStatus.ACTIVE);

        if (activeTxOpt.isPresent()) {
            Transaction active = activeTxOpt.get();
            System.out.println("Resetting existing active transaction prices to baseline...");
            boolean changed = false;
            for (TransactionItem item : active.getItems()) {
                Product product = item.getProduct();
                if (product != null && product.getId() != null) {
                    UUID productId = product.getId();
                    Product dbProduct = productRepository.findById(productId).orElse(null);
                    if (dbProduct != null && item.getPrice().compareTo(dbProduct.getPrice()) != 0) {
                        item.setPrice(dbProduct.getPrice());
                        changed = true;
                    }
                }
            }
            if (changed) {
                recalculateTotals(active);
                transactionRepository.save(active);
                System.out.println("Active transaction prices reset to baseline successfully.");
            }
        } else {
            System.out.println("Pre-filling active transaction cart to match screenshot...");

            Product wrench = productRepository.findByBarcode("4006885245808").orElse(null);
            Product trays = productRepository.findByBarcode("65529254").orElse(null);

            if (wrench != null && trays != null) {
                Transaction active = Transaction.builder()
                        .status(TransactionStatus.ACTIVE)
                        .totalAmount(BigDecimal.ZERO)
                        .taxAmount(BigDecimal.ZERO)
                        .discountAmount(BigDecimal.ZERO)
                        .invoiceRequired(false)
                        .items(new ArrayList<>())
                        .payments(new ArrayList<>())
                        .build();

                TransactionItem wrenchItem = TransactionItem.builder()
                        .transaction(active)
                        .product(wrench)
                        .quantity(1)
                        .price(wrench.getPrice())
                        .build();

                TransactionItem traysItem = TransactionItem.builder()
                        .transaction(active)
                        .product(trays)
                        .quantity(1)
                        .price(trays.getPrice())
                        .build();

                active.getItems().add(wrenchItem);
                active.getItems().add(traysItem);

                recalculateTotals(active);
                transactionRepository.save(active);
                System.out.println("Active transaction pre-filled successfully.");
            }
        }
    }

    private void recalculateTotals(Transaction tx) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        
        for (TransactionItem item : tx.getItems()) {
            BigDecimal itemPrice = item.getPrice();
            BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
            BigDecimal itemTotal = itemPrice.multiply(qty);
            subtotal = subtotal.add(itemTotal);

            BigDecimal taxRate = item.getProduct().getTaxRate();
            BigDecimal divisor = BigDecimal.ONE.add(taxRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal priceExclTax = itemTotal.divide(divisor, 4, RoundingMode.HALF_UP);
            BigDecimal itemTax = itemTotal.subtract(priceExclTax);
            
            totalTax = totalTax.add(itemTax);
        }

        tx.setTotalAmount(subtotal.setScale(2, RoundingMode.HALF_UP));
        tx.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        tx.setDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }
}
