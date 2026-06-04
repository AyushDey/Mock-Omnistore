package com.quotation.api.config;

import com.quotation.api.model.QuotationItem;
import com.quotation.api.repository.QuotationItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class QuotationSeeder implements CommandLineRunner {

    private final QuotationItemRepository quotationItemRepository;

    public QuotationSeeder(QuotationItemRepository quotationItemRepository) {
        this.quotationItemRepository = quotationItemRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("Seeding/Resetting quotation items...");
        List<QuotationItem> defaultItems = new ArrayList<>();

        // Item 1 — DIFFERENT PRICE from Omnistore (Omnistore: 13.00, Quotation: 14.50)
        defaultItems.add(QuotationItem.builder()
                .name("CLE MEULEUSE WOLFCRAFT 34/5MM")
                .barcode("4006885245808")
                .price(new BigDecimal("14.50"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/cle_meuleuse.png")
                .description("Clé pour meuleuse Wolfcraft 34/5mm - outil de serrage professionnel")
                .build());

        // Item 2 — Same price as Omnistore (5.99)
        defaultItems.add(QuotationItem.builder()
                .name("PETITES BARQUETTES ALU WEBER")
                .barcode("65529254")
                .price(new BigDecimal("5.99"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/barquettes_alu.png")
                .description("Petites barquettes aluminium Weber pour barbecue")
                .build());

        // Item 3 — Same price as Omnistore (3.50)
        defaultItems.add(QuotationItem.builder()
                .name("TOURNEVIS CRUCIFORME DEXTER PRO")
                .barcode("3276007391124")
                .price(new BigDecimal("3.50"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/tournevis.png")
                .description("Tournevis cruciforme professionnel Dexter")
                .build());

        // Item 4 — Same price as Omnistore (8.90)
        defaultItems.add(QuotationItem.builder()
                .name("MARTEAU RIVOIR DEXTER 32MM")
                .barcode("3276000150964")
                .price(new BigDecimal("8.90"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/marteau.png")
                .description("Marteau rivoir Dexter 32mm pour travaux de bricolage")
                .build());

        // Item 5 — DIFFERENT PRICE from Omnistore (Omnistore: 49.90, Quotation: 52.90)
        defaultItems.add(QuotationItem.builder()
                .name("PERCEUSE A PERCUSSION DEXTER 900W")
                .barcode("3276000703672")
                .price(new BigDecimal("52.90"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/perceuse.png")
                .description("Perceuse à percussion Dexter 900W avec mandrin auto-serrant")
                .build());

        // Item 6 — Same price as Omnistore (6.50)
        defaultItems.add(QuotationItem.builder()
                .name("VIS A BOIS DEXTER 4X40MM 200PCS")
                .barcode("3276000155020")
                .price(new BigDecimal("6.50"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/vis_bois.png")
                .description("Lot de 200 vis à bois Dexter 4x40mm tête fraisée")
                .build());

        // 10 New Products with DIFFERENT PRICES from Omnistore
        defaultItems.add(QuotationItem.builder()
                .name("SCIE CIRCULAIRE MAKITA 1200W")
                .barcode("0088381612036")
                .price(new BigDecimal("109.99"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/scie_circulaire.png")
                .description("Scie circulaire Makita 1200W puissante pour coupes précises")
                .build());

        defaultItems.add(QuotationItem.builder()
                .name("MEULEUSE D'ANGLE BOSCH PROFESSIONAL")
                .barcode("3165140829120")
                .price(new BigDecimal("49.90"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/meuleuse_bosch.png")
                .description("Meuleuse d'angle Bosch Professional compacte et robuste")
                .build());

        defaultItems.add(QuotationItem.builder()
                .name("POSTE A SOUDER DECA I-ARC 120")
                .barcode("8004828014524")
                .price(new BigDecimal("129.50"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/poste_souder.png")
                .description("Poste à souder Deca portable technologie Inverter")
                .build());

        defaultItems.add(QuotationItem.builder()
                .name("COFFRET DE DOUILLES FACOM 38PCS")
                .barcode("3148517894236")
                .price(new BigDecimal("159.99"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/coffret_douilles.png")
                .description("Coffret de douilles et cliquet Facom 38 pièces de qualité supérieure")
                .build());

        defaultItems.add(QuotationItem.builder()
                .name("NIVEAU LASER ROTATIF STANLEY FATMAX")
                .barcode("3253561771370")
                .price(new BigDecimal("245.00"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/niveau_laser.png")
                .description("Niveau laser rotatif Stanley Fatmax de haute précision")
                .build());

        defaultItems.add(QuotationItem.builder()
                .name("NETTOYEUR HAUTE PRESSION KARCHER K4")
                .barcode("4054278120364")
                .price(new BigDecimal("199.99"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/nettoyeur_karcher.png")
                .description("Nettoyeur haute pression Karcher K4 pour le nettoyage extérieur")
                .build());

        defaultItems.add(QuotationItem.builder()
                .name("COMPRESSEUR BICYLINDRE MICHELIN 50L")
                .barcode("3760015907421")
                .price(new BigDecimal("175.00"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/compresseur_michelin.png")
                .description("Compresseur d'air bicylindre Michelin cuve de 50L")
                .build());

        defaultItems.add(QuotationItem.builder()
                .name("ETABLI D'ATELIER EN BOIS DEXTER")
                .barcode("3276007123985")
                .price(new BigDecimal("99.00"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/etabli_dexter.png")
                .description("Établi d'atelier en bois robuste Dexter avec tiroir")
                .build());

        defaultItems.add(QuotationItem.builder()
                .name("ASPIRATEUR DE CHANTIER KARCHER WD3")
                .barcode("4054278049689")
                .price(new BigDecimal("74.90"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/aspirateur_karcher.png")
                .description("Aspirateur eau et poussières Karcher WD3 multifonctionnel")
                .build());

        defaultItems.add(QuotationItem.builder()
                .name("PROJECTEUR DE CHANTIER LED DEXTER")
                .barcode("3276000702453")
                .price(new BigDecimal("24.90"))
                .taxRate(new BigDecimal("20.00"))
                .imageUrl("assets/images/projecteur_dexter.png")
                .description("Projecteur de chantier LED Dexter étanche et puissant")
                .build());

        for (QuotationItem defaultItem : defaultItems) {
            QuotationItem itemToSave = quotationItemRepository.findByBarcode(defaultItem.getBarcode())
                    .map(existing -> {
                        existing.setName(defaultItem.getName());
                        existing.setPrice(defaultItem.getPrice());
                        existing.setTaxRate(defaultItem.getTaxRate());
                        existing.setImageUrl(defaultItem.getImageUrl());
                        existing.setDescription(defaultItem.getDescription());
                        return existing;
                    })
                    .orElse(defaultItem);
            quotationItemRepository.save(itemToSave);
        }
        System.out.println("Quotation items seeded/reset successfully.");
    }
}
