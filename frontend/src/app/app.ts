import { Component, OnInit, signal, computed, inject, effect, HostListener } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, DatePipe } from '@angular/common';
import { TransactionService, Product, TransactionItem } from './transaction.service';
import { of, Subject } from 'rxjs';
import { take, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'warning';
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule, DecimalPipe, DatePipe],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  protected readonly transactionService = inject(TransactionService);

  // Core visual state
  readonly isPaymentScreen = signal<boolean>(false);
  readonly activeTab = signal<'CART' | 'ARTICLE_LOOKUP' | 'TRANSACTION_LOOKUP'>('CART');
  readonly transactionSubTab = signal<'FONCTIONS' | 'HISTORIQUE'>('FONCTIONS');
  
  // Transaction Lookup Panel State
  readonly transactionLookupSubTab = signal<'INFO' | 'ITEMS'>('INFO');
  readonly lookupTxBarcode = signal<string>('');
  readonly lookupTxOrderId = signal<string>('');
  readonly lookupTxPhone = signal<string>('');
  readonly lookupTxStartDate = signal<string>('2026-05-20'); // Expanded default start date
  readonly lookupTxEndDate = signal<string>('2026-06-05'); // Expanded default end date to include today (2026-05-29) safely
  readonly lookupTxMinAmount = signal<number>(0);
  readonly lookupTxMaxAmount = signal<number>(1000);
  readonly lookupTxNonOmnistore = signal<boolean>(false);
  readonly lookupTxResult = signal<any | null>(null);
  readonly lookupTxResults = signal<any[]>([]); // New signal to hold all matching transactions

  private readonly mockNonOmnistoreTransactions = [
    {
      id: "NON-OMNI-TX-98765",
      status: "PAID",
      totalAmount: 145.50,
      taxAmount: 24.25,
      discountAmount: 0.00,
      invoiceRequired: false,
      createdAt: "2026-05-29T10:30:00",
      updatedAt: "2026-05-29T10:30:00",
      items: [
        {
          id: "item-101",
          product: { id: "p1", name: "PETITES BARQUETTES ALU WEBER", barcode: "65529254", price: 5.99, imageUrl: "", taxRate: 20 },
          quantity: 10,
          price: 5.99
        },
        {
          id: "item-102",
          product: { id: "p2", name: "PERCEUSE A PERCUSSION DEXTER 900W", barcode: "3276000703672", price: 49.90, imageUrl: "", taxRate: 20 },
          quantity: 1,
          price: 49.90
        }
      ],
      payments: [
        { id: "pay-101", method: "CARD", amount: 145.50, createdAt: "2026-05-29T10:30:00" }
      ]
    },
    {
      id: "NON-OMNI-TX-12345",
      status: "PAID",
      totalAmount: 39.00,
      taxAmount: 6.50,
      discountAmount: 0.00,
      invoiceRequired: true,
      createdAt: "2026-05-28T14:20:00",
      updatedAt: "2026-05-28T14:20:00",
      items: [
        {
          id: "item-103",
          product: { id: "p3", name: "CLE MEULEUSE WOLFCRAFT 34/5MM", barcode: "4006885245808", price: 13.00, imageUrl: "", taxRate: 20 },
          quantity: 3,
          price: 13.00
        }
      ],
      payments: [
        { id: "pay-102", method: "CASH", amount: 39.00, createdAt: "2026-05-28T14:20:00" }
      ]
    }
  ];

  readonly lookupCode = signal<string>('');
  readonly lookupDescription = signal<string>('');
  readonly lookupRayon = signal<string>('');
  readonly lookupSubRayon = signal<string>('');
  readonly lookupMaxPrice = signal<number>(1000);
  readonly lookupResults = signal<Product[]>([]);
  readonly lookupSearched = signal<boolean>(false);
  readonly managerAuthorized = signal<boolean>(false);
  readonly currentTime = signal<Date>(new Date());
  
  // Search & Scanner
  readonly searchQuery = signal<string>('');
  readonly searchResults = signal<Product[]>([]);
  readonly showSearchDropdown = signal<boolean>(false);

  // Keypads & Modals
  readonly activeModal = signal<string | null>(null); // 'CASH' | 'CARD' | 'ONEY' | 'GIFT_CARD' | 'VOUCHER' | 'CLIENT' | 'PIN' | 'SUSPENDED' | 'DOCUMENT' | 'PRICE_CHANGED'
  readonly priceChangeDetails = signal<{ productName: string, barcode: string, oldPrice: number, newPrice: number } | null>(null);
  readonly pinBuffer = signal<string>('');
  readonly pinError = signal<boolean>(false);
  
  // Payment Drawer calculations
  readonly cashReceivedInput = signal<string>('');
  readonly cashChangeDue = signal<number>(0);
  readonly selectedOneyTerm = signal<number>(3); // 3x or 4x
  readonly cardSimState = signal<'IDLE' | 'PROCESSING' | 'SUCCESS' | 'FAILED'>('IDLE');
  readonly genericCodeInput = signal<string>('');

  // Customer & Invoice Details
  readonly customerName = signal<string>('ANIRBAN - 20101594');
  readonly selectedCustomer = signal<any | null>(null);
  readonly invoiceName = signal<string>('');
  readonly invoiceAddress = signal<string>('');
  readonly invoiceSiret = signal<string>('');

  // Document Printing
  readonly printedDocType = signal<'BULLETIN' | 'COMMANDE' | 'RECEIPT' | 'INVOICE'>('RECEIPT');
  readonly lastCompletedTransaction = signal<any | null>(null);

  // Toasts
  readonly toasts = signal<Toast[]>([]);
  private toastIdCounter = 0;
  private readonly searchTerms = new Subject<string>();

  constructor() {
    // Ticker for header time
    setInterval(() => {
      this.currentTime.set(new Date());
    }, 1000);

    // Watch active transaction to auto-trigger invoice dialog fields if customer is pre-set
    effect(() => {
      const activeTx = this.transactionService.activeTransaction();
      if (activeTx?.invoiceRequired) {
        // Prefill default customer values if invoice selected
        if (!this.invoiceName()) {
          this.invoiceName.set(this.selectedCustomer()?.name || 'ANIRBAN BOE S.A.S.');
          this.invoiceAddress.set(this.selectedCustomer()?.address || '12 Avenue du Midi, 47000 Agen');
          this.invoiceSiret.set(this.selectedCustomer()?.siret || '482 910 482 00018');
        }
      }
    });

    // Watch active transaction to automatically pop up price update modal on any change (e.g. quantity adjustment)
    effect(() => {
      const activeTx = this.transactionService.activeTransaction();
      if (activeTx) {
        this.checkForPriceChange(activeTx);
      }
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    // Load default transaction on boot
    this.transactionService.loadActiveTransaction();

    // Set up debounced search autocomplete
    this.searchTerms.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((term: string) => {
        if (term.length >= 3) {
          return this.transactionService.searchProducts(term);
        } else {
          return of([]);
        }
      })
    ).subscribe({
      next: (products) => {
        if (this.searchQuery().trim().length >= 3) {
          this.searchResults.set(products);
          this.showSearchDropdown.set(true);
        } else {
          this.searchResults.set([]);
          this.showSearchDropdown.set(false);
        }
      }
    });
  }

  // Toast System
  showToast(message: string, type: 'success' | 'error' | 'warning' = 'success'): void {
    const id = this.toastIdCounter++;
    const newToast: Toast = { id, message, type };
    this.toasts.update(tList => [...tList, newToast]);
    
    setTimeout(() => {
      this.toasts.update(tList => tList.filter(t => t.id !== id));
    }, 3500);
  }

  // Scan or Search execution
  onSearchInput(): void {
    const query = this.searchQuery().trim();
    if (query.length < 3) {
      this.searchResults.set([]);
      this.showSearchDropdown.set(false);
      this.searchTerms.next('');
    } else {
      this.searchTerms.next(query);
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.search-bar-wrapper') && !target.closest('.search-dropdown')) {
      this.showSearchDropdown.set(false);
    }
  }

  private checkForPriceChange(tx: any): boolean {
    const changedItem = tx.items?.find((item: any) => item.priceChanged);
    if (changedItem) {
      this.priceChangeDetails.set({
        productName: changedItem.product.name,
        barcode: changedItem.product.barcode,
        oldPrice: changedItem.oldPrice,
        newPrice: changedItem.price
      });
      this.activeModal.set('PRICE_CHANGED');
      return true;
    }
    return false;
  }

  onSearchSubmit(event?: Event): void {
    if (event) event.preventDefault();
    const query = this.searchQuery().trim();
    if (!query) return;

    this.showSearchDropdown.set(false);
    this.searchQuery.set('');

    // 1. Try to scan as barcode
    this.transactionService.scanAndAddItem(query).pipe(take(1)).subscribe({
      next: (tx) => {
        if (!this.checkForPriceChange(tx)) {
          this.showToast('Article ajouté au panier !');
        }
      },
      error: () => {
        // 2. Barcode not found, search as name
        this.transactionService.searchProducts(query).pipe(take(1)).subscribe({
          next: (products) => {
            if (products.length === 1) {
              this.addCatalogProduct(products[0]);
            } else if (products.length > 1) {
              this.searchResults.set(products);
              this.showSearchDropdown.set(true);
              this.showToast('Plusieurs articles correspondants trouvés.', 'warning');
            } else {
              this.showToast("Aucun article trouvé avec ce code ou nom", 'error');
            }
          }
        });
      }
    });
  }

  addCatalogProduct(product: Product): void {
    this.showSearchDropdown.set(false);
    this.searchQuery.set('');
    this.transactionService.scanAndAddItem(product.barcode).pipe(take(1)).subscribe({
      next: (tx) => {
        if (!this.checkForPriceChange(tx)) {
          this.showToast(`${product.name} ajouté !`);
        }
      },
      error: (err) => this.showToast(`Erreur d'ajout: ${err.message || err}`, 'error')
    });
  }

  // Item adjustments
  incrementQty(item: TransactionItem): void {
    this.transactionService.updateItemQuantity(item.id, item.quantity + 1);
  }

  decrementQty(item: TransactionItem): void {
    if (item.quantity > 1) {
      this.transactionService.updateItemQuantity(item.id, item.quantity - 1);
    } else {
      this.removeItem(item);
    }
  }

  removeItem(item: TransactionItem): void {
    this.transactionService.removeItem(item.id);
    this.showToast(`${item.product.name} retiré du panier.`, 'warning');
  }

  // Header Actions
  openClientLookup(): void {
    this.activeModal.set('CLIENT');
  }
  selectTab(tab: 'CART' | 'ARTICLE_LOOKUP' | 'TRANSACTION_LOOKUP'): void {
    this.closeModal();
    this.activeTab.set(tab);
    if (tab === 'ARTICLE_LOOKUP') {
      this.clearLookupForm();
    } else if (tab === 'TRANSACTION_LOOKUP') {
      this.transactionService.loadAllTransactions();
      // Pre-load completed or recent transactions as preview out-of-the-box
      this.transactionService.http.get<any[]>(`${this.transactionService.baseUrl}/transactions`).pipe(take(1)).subscribe({
        next: (txs) => {
          if (txs && txs.length > 0) {
            const completed = txs.filter(t => t.status === 'PAID' || t.status === 'SUSPENDED');
            const list = completed.length > 0 ? completed : txs;
            this.lookupTxResults.set(list);
            this.lookupTxResult.set(list[0]);
          }
        }
      });
    }
  }

  submitTransactionLookup(event?: Event): void {
    if (event) event.preventDefault();
    const query = this.searchQuery().trim();
    
    // Core search filtering handler
    const performSearch = (txs: any[]) => {
      let matched = txs;
      
      if (!query) {
        const bar = this.lookupTxBarcode().trim();
        const orderId = this.lookupTxOrderId().trim();
        const phone = this.lookupTxPhone().trim();
        const min = this.lookupTxMinAmount();
        const max = this.lookupTxMaxAmount();
        const startD = this.lookupTxStartDate();
        const endD = this.lookupTxEndDate();
        
        if (bar) {
          matched = matched.filter(t => t.items.some((i: any) => 
            i.product.barcode.includes(bar) || 
            i.product.name.toLowerCase().includes(bar.toLowerCase())
          ));
        }
        if (orderId) {
          matched = matched.filter(t => t.id.toLowerCase().includes(orderId.toLowerCase()));
        }
        if (phone) {
          matched = matched.filter(t => t.id.toLowerCase().includes(phone.toLowerCase()));
        }
        if (min > 0) {
          matched = matched.filter(t => t.totalAmount >= min);
        }
        if (max < 1000) {
          matched = matched.filter(t => t.totalAmount <= max);
        }
        if (startD) {
          matched = matched.filter(t => t.createdAt >= startD);
        }
        if (endD) {
          matched = matched.filter(t => t.createdAt <= endD + 'T23:59:59');
        }
      } else {
        matched = matched.filter(t => t.id.toLowerCase().includes(query.toLowerCase()));
      }

      if (matched.length > 0) {
        this.lookupTxResults.set(matched);
        this.lookupTxResult.set(matched[0]); // default preview to the first matched transaction
        this.showToast(`${matched.length} transactions correspondantes trouvées !`);
      } else {
        this.lookupTxResults.set([]);
        this.lookupTxResult.set(null);
        this.showToast("Aucune transaction correspondante trouvée.", "warning");
      }
    };

    if (this.lookupTxNonOmnistore()) {
      // Search from simulated Non-Omnistore transaction data
      performSearch(this.mockNonOmnistoreTransactions);
    } else {
      // Search from live Omnistore database transactions
      this.transactionService.http.get<any[]>(`${this.transactionService.baseUrl}/transactions`).pipe(take(1)).subscribe({
        next: (txs) => {
          performSearch(txs);
        },
        error: (err) => {
          this.showToast("Erreur lors de la récupération des transactions.", "error");
        }
      });
    }
  }

  onLookupTxSelect(id: string): void {
    const found = this.lookupTxResults().find(t => t.id === id);
    if (found) {
      this.lookupTxResult.set(found);
    }
  }

  clearTransactionLookupForm(): void {
    this.lookupTxBarcode.set('');
    this.lookupTxOrderId.set('');
    this.lookupTxPhone.set('');
    this.lookupTxStartDate.set('2026-05-20');
    this.lookupTxEndDate.set('2026-06-05');
    this.lookupTxMinAmount.set(0);
    this.lookupTxMaxAmount.set(1000);
    this.lookupTxNonOmnistore.set(false);
    this.lookupTxResults.set([]);
    this.lookupTxResult.set(null);
    this.showToast("Formulaire de recherche effacé.");
  }

  clearLookupForm(): void {
    this.lookupCode.set('');
    this.lookupDescription.set('');
    this.lookupRayon.set('');
    this.lookupSubRayon.set('');
    this.lookupMaxPrice.set(1000);
    this.lookupResults.set([]);
    this.lookupSearched.set(false);
  }

  submitLookupSearch(event?: Event): void {
    if (event) event.preventDefault();
    this.lookupSearched.set(true);
    
    const desc = this.lookupDescription().trim();
    const code = this.lookupCode().trim();
    const query = code ? code : desc;
    
    this.transactionService.searchProducts(query).pipe(take(1)).subscribe({
      next: (products) => {
        const maxPrice = this.lookupMaxPrice();
        const rayon = this.lookupRayon();
        const subRayon = this.lookupSubRayon();
        
        let filtered = products.filter(p => p.price <= maxPrice);
        
        // Rayon name-based filters
        if (rayon) {
          filtered = filtered.filter(p => {
            const name = p.name.toUpperCase();
            if (rayon === 'OUTILLAGE') {
              return name.includes('TOURNEVIS') || name.includes('MARTEAU') || name.includes('PERCEUSE') || name.includes('CLE') || name.includes('MEULEUSE');
            } else if (rayon === 'ELECTRICITE') {
              return name.includes('PERCEUSE') || name.includes('ELECTRICITE');
            } else if (rayon === 'QUINCAILLERIE') {
              return name.includes('VIS') || name.includes('BARQUETTES') || name.includes('ALU');
            }
            return true;
          });
        }
        
        // Sub-Rayon name-based filters
        if (subRayon) {
          filtered = filtered.filter(p => {
            const name = p.name.toUpperCase();
            if (subRayon === 'TOURNEVIS') {
              return name.includes('TOURNEVIS') || name.includes('CLE') || name.includes('CLÉ');
            } else if (subRayon === 'FIXATIONS') {
              return name.includes('VIS') || name.includes('FIXATION') || name.includes('BOIS');
            } else if (subRayon === 'MATERIEL') {
              return name.includes('PERCEUSE') || name.includes('MEULEUSE') || name.includes('MARTEAU');
            }
            return true;
          });
        }

        this.lookupResults.set(filtered);
        
        if (filtered.length > 0) {
          this.showToast(`${filtered.length} articles trouvés !`);
        } else {
          this.showToast('Aucun article correspondant trouvé.', 'warning');
        }
      }
    });
  }

  addLookupItemToCart(product: Product): void {
    this.transactionService.scanAndAddItem(product.barcode).pipe(take(1)).subscribe({
      next: (tx) => {
        if (!this.checkForPriceChange(tx)) {
          this.showToast(`${product.name} ajouté au panier !`);
        }
      },
      error: (err) => this.showToast(`Erreur d'ajout: ${err.message || err}`, 'error')
    });
  }

  selectCustomerMock(customerType: 'standard' | 'pro' | 'none'): void {
    if (customerType === 'standard') {
      this.selectedCustomer.set({
        name: 'M. ANIRBAN ROY',
        email: 'anirban.roy@outlook.com',
        phone: '06 12 34 56 78',
        address: '15 Rue Lafayette, 47000 Agen',
        loyaltyPoints: 345
      });
      this.showToast('Client associé : ANIRBAN ROY');
    } else if (customerType === 'pro') {
      this.selectedCustomer.set({
        name: 'ENTREPRISE ANIRBAN BOE S.A.S.',
        email: 'pro@anirbangroup.fr',
        phone: '05 53 12 34 56',
        address: '12 Avenue du Midi, 47000 Agen',
        siret: '482 910 482 00018',
        loyaltyPoints: 1250
      });
      this.showToast('Client PRO associé : ANIRBAN BOE S.A.S.');
    } else {
      this.selectedCustomer.set(null);
      this.showToast('Client détaché du panier.', 'warning');
    }
    this.closeModal();
  }

  // Footer Actions
  clickPayer(): void {
    const activeTx = this.transactionService.activeTransaction();
    if (!activeTx || activeTx.items.length === 0) {
      this.showToast('Le panier est vide. Ajoutez des articles avant de payer.', 'warning');
      return;
    }
    this.isPaymentScreen.set(true);
  }

  clickRetour(): void {
    this.isPaymentScreen.set(false);
  }

  // Left Panel Actions
  bulletinDeVente(): void {
    this.printedDocType.set('BULLETIN');
    this.activeModal.set('DOCUMENT');
  }

  bonDeCommande(): void {
    this.printedDocType.set('COMMANDE');
    this.activeModal.set('DOCUMENT');
  }

  codeA4Chiffres(): void {
    this.pinBuffer.set('');
    this.pinError.set(false);
    this.activeModal.set('PIN');
  }

  submitPin(): void {
    if (this.pinBuffer() === '1234') {
      this.managerAuthorized.set(true);
      this.showToast('Autorisation Manager accordée ! Fonctions étendues déverrouillées.');
      this.closeModal();
    } else {
      this.pinError.set(true);
      this.pinBuffer.set('');
      this.showToast('Code PIN erroné. Réessayez.', 'error');
    }
  }

  pinKeyPress(digit: string): void {
    this.pinError.set(false);
    if (this.pinBuffer().length < 4) {
      this.pinBuffer.update(p => p + digit);
    }
  }

  clearPin(): void {
    this.pinBuffer.set('');
  }

  suspendreTransaction(): void {
    this.transactionService.suspendTransaction().pipe(take(1)).subscribe({
      next: () => {
        this.showToast('Transaction suspendue et sauvegardée !');
        this.isPaymentScreen.set(false);
      }
    });
  }

  reprendreTransaction(): void {
    this.transactionService.http.get<any[]>(`${this.transactionService.baseUrl}/transactions/suspended`).pipe(take(1)).subscribe({
      next: (txs) => {
        if (txs && txs.length > 0) {
          const mostRecent = txs[0];
          this.transactionService.resumeTransaction(mostRecent.id).pipe(take(1)).subscribe({
            next: (res) => {
              this.showToast(`Panier de ${res.totalAmount} € repris !`);
              this.closeModal();
            }
          });
        } else {
          this.showToast("Aucun panier suspendu trouvé.", "warning");
        }
      },
      error: (err) => {
        this.showToast("Erreur lors de la récupération des paniers suspendus.", "error");
      }
    });
  }

  resumeSuspendedCart(tx: any): void {
    this.transactionService.resumeTransaction(tx.id).pipe(take(1)).subscribe({
      next: () => {
        this.showToast(`Panier de ${tx.totalAmount} € repris !`);
        this.closeModal();
      }
    });
  }

  abandonnerTransaction(): void {
    if (confirm('Voulez-vous vraiment annuler la transaction en cours ?')) {
      this.transactionService.abandonTransaction().pipe(take(1)).subscribe({
        next: () => {
          this.showToast('Transaction abandonnée. Le panier a été vidé.', 'warning');
          this.isPaymentScreen.set(false);
        }
      });
    }
  }

  reimprimerTicket(): void {
    if (this.lastCompletedTransaction()) {
      this.printedDocType.set('RECEIPT');
      this.activeModal.set('DOCUMENT');
    } else {
      this.showToast("Aucune transaction n'a été complétée durant cette session.", 'warning');
    }
  }

  // Payment Selection Screen Button Actions
  clickPayByCash(): void {
    this.cashReceivedInput.set('');
    this.cashChangeDue.set(0);
    this.activeModal.set('CASH');
  }

  cashRegisterKeyPress(val: string): void {
    if (val === 'C') {
      this.cashReceivedInput.set('');
      this.cashChangeDue.set(0);
    } else if (val === '.') {
      if (!this.cashReceivedInput().includes('.')) {
        this.cashReceivedInput.update(c => c + '.');
      }
    } else {
      this.cashReceivedInput.update(c => c + val);
    }
    this.calculateChange();
  }

  addCashQuickAmount(amount: number): void {
    const current = parseFloat(this.cashReceivedInput()) || 0;
    this.cashReceivedInput.set((current + amount).toFixed(2));
    this.calculateChange();
  }

  calculateChange(): void {
    const received = parseFloat(this.cashReceivedInput()) || 0;
    const due = this.transactionService.amountDue();
    if (received >= due) {
      this.cashChangeDue.set(parseFloat((received - due).toFixed(2)));
    } else {
      this.cashChangeDue.set(0);
    }
  }

  submitCashPayment(): void {
    const received = parseFloat(this.cashReceivedInput()) || 0;
    const due = this.transactionService.amountDue();
    
    if (received <= 0) {
      this.showToast('Veuillez saisir le montant reçu.', 'warning');
      return;
    }

    const payAmount = Math.min(received, due);
    this.transactionService.addPayment('CASH', payAmount).pipe(take(1)).subscribe({
      next: (tx) => {
        this.showToast(`Paiement de ${payAmount.toFixed(2)} € enregistré.`);
        
        if (tx.status === 'PAID') {
          this.completeCheckoutFlow(tx);
        } else {
          this.closeModal();
        }
      }
    });
  }

  clickPayByCard(): void {
    this.cardSimState.set('PROCESSING');
    this.activeModal.set('CARD');
    
    setTimeout(() => {
      this.cardSimState.set('SUCCESS');
      setTimeout(() => {
        const due = this.transactionService.amountDue();
        this.transactionService.addPayment('CARD', due).pipe(take(1)).subscribe({
          next: (tx) => {
            this.showToast(`Paiement carte de ${due.toFixed(2)} € approuvé.`);
            this.completeCheckoutFlow(tx);
          }
        });
      }, 1000);
    }, 2000);
  }

  clickPayByOney(): void {
    this.activeModal.set('ONEY');
  }

  submitOneyPayment(): void {
    const due = this.transactionService.amountDue();
    this.transactionService.addPayment('ONEY', due).pipe(take(1)).subscribe({
      next: (tx) => {
        this.showToast(`Financement Oney ${this.selectedOneyTerm()}x approuvé.`);
        this.completeCheckoutFlow(tx);
      }
    });
  }

  clickPayGiftCard(): void {
    this.genericCodeInput.set('');
    this.activeModal.set('GIFT_CARD');
  }

  submitGiftCardPayment(): void {
    const code = this.genericCodeInput().trim();
    if (!code) {
      this.showToast('Veuillez saisir le code de la carte cadeau.', 'warning');
      return;
    }
    
    // Simulate gift card with 15.00 € value
    const giftValue = 15.00;
    const due = this.transactionService.amountDue();
    const payAmount = Math.min(giftValue, due);

    this.transactionService.addPayment('GIFT_CARD', payAmount).pipe(take(1)).subscribe({
      next: (tx) => {
        this.showToast(`Carte cadeau de ${payAmount.toFixed(2)} € appliquée.`);
        if (tx.status === 'PAID') {
          this.completeCheckoutFlow(tx);
        } else {
          this.closeModal();
        }
      }
    });
  }

  clickPayVoucher(): void {
    this.genericCodeInput.set('');
    this.activeModal.set('VOUCHER');
  }

  submitVoucherPayment(): void {
    const code = this.genericCodeInput().trim();
    if (!code) {
      this.showToast("Veuillez saisir le numéro du bon d'achat.", 'warning');
      return;
    }

    // Simulate standard coupon of 10.00 €
    const voucherValue = 10.00;
    const due = this.transactionService.amountDue();
    const payAmount = Math.min(voucherValue, due);

    this.transactionService.addPayment('VOUCHER', payAmount).pipe(take(1)).subscribe({
      next: (tx) => {
        this.showToast(`Bon d'achat de ${payAmount.toFixed(2)} € appliqué.`);
        if (tx.status === 'PAID') {
          this.completeCheckoutFlow(tx);
        } else {
          this.closeModal();
        }
      }
    });
  }

  clickPayGenericOther(): void {
    const due = this.transactionService.amountDue();
    if (confirm(`Enregistrer un paiement de ${due.toFixed(2)} € par Autre Moyen (Chèque/Virement) ?`)) {
      this.transactionService.addPayment('OTHER', due).pipe(take(1)).subscribe({
        next: (tx) => {
          this.showToast(`Paiement de ${due.toFixed(2)} € enregistré par Chèque/Virement.`);
          this.completeCheckoutFlow(tx);
        }
      });
    }
  }

  toggleInvoice(event: any): void {
    const checked = event.target.checked;
    this.transactionService.toggleInvoiceRequired(checked);
    this.showToast(checked ? 'Facture activée pour cette transaction' : 'Facture désactivée');
  }

  // Complete checkout handler
  private completeCheckoutFlow(tx: any): void {
    this.lastCompletedTransaction.set(tx);
    
    // Save invoice details if invoice required
    if (tx.invoiceRequired) {
      this.printedDocType.set('INVOICE');
    } else {
      this.printedDocType.set('RECEIPT');
    }
    
    // Show Document Modal
    this.activeModal.set('DOCUMENT');
    this.showToast('Vente complétée avec succès ! Impression du reçu...');

    // Clear session details
    this.isPaymentScreen.set(false);
    this.invoiceName.set('');
    this.invoiceAddress.set('');
    this.invoiceSiret.set('');
    
    // Reset Cart
    this.transactionService.loadActiveTransaction();
  }

  // Helpers
  closeModal(): void {
    this.activeModal.set(null);
    this.pinBuffer.set('');
    this.pinError.set(false);
    this.cashReceivedInput.set('');
    this.cashChangeDue.set(0);
    this.cardSimState.set('IDLE');
    this.priceChangeDetails.set(null);
  }

  printCurrentDocument(): void {
    window.print();
    this.showToast('Document envoyé à l\'imprimante.');
    this.closeModal();
  }

  parseFloat(val: string): number {
    return parseFloat(val);
  }
}
