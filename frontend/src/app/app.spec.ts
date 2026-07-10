import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { App } from './app';
import { TransactionService, Transaction, Product } from './transaction.service';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { of, throwError } from 'rxjs';

describe('App Component', () => {
  let component: App;
  let fixture: ComponentFixture<App>;
  let transactionService: TransactionService;
  let httpMock: HttpTestingController;

  const mockProduct: Product = {
    id: 'prod-1',
    name: 'CLE MEULEUSE WOLFCRAFT',
    barcode: '4006885245808',
    price: 13.00,
    imageUrl: '',
    taxRate: 20
  };

  const mockActiveTx: Transaction = {
    id: 'tx-1',
    status: 'ACTIVE',
    totalAmount: 13.00,
    taxAmount: 2.17,
    discountAmount: 0.00,
    invoiceRequired: false,
    items: [
      {
        id: 'item-1',
        product: mockProduct,
        quantity: 1,
        price: 13.00
      }
    ],
    payments: [],
    createdAt: '2026-06-18T05:00:00',
    updatedAt: '2026-06-18T05:00:00'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    fixture = TestBed.createComponent(App);
    component = fixture.componentInstance;
    transactionService = TestBed.inject(TransactionService);
    httpMock = TestBed.inject(HttpTestingController);

    // Mock initial active transaction fetch during ngOnInit
    fixture.detectChanges();
    const req = httpMock.expectOne('http://localhost:8080/api/transactions/active');
    req.flush(mockActiveTx);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create the app and load active transaction on init', () => {
    expect(component).toBeTruthy();
    expect(transactionService.activeTransaction()).toEqual(mockActiveTx);
  });

  describe('Toast System', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.restoreAllMocks();
    });

    it('should add a toast and automatically remove it', () => {
      component.showToast('Test Message', 'success');
      expect(component.toasts().length).toBe(1);
      expect(component.toasts()[0].message).toBe('Test Message');

      // Fast-forward toast timeout
      vi.advanceTimersByTime(3500);
      expect(component.toasts().length).toBe(0);
    });
  });

  describe('Navigation & Tabs', () => {
    it('should switch tabs and load appropriate data', () => {
      component.selectTab('ARTICLE_LOOKUP');
      expect(component.activeTab()).toBe('ARTICLE_LOOKUP');

      component.selectTab('TRANSACTION_LOOKUP');
      expect(component.activeTab()).toBe('TRANSACTION_LOOKUP');

      // TRANSACTION_LOOKUP triggers loadAllTransactions (GET /api/transactions)
      // AND pre-load call to /api/transactions. Thus, 2 identical requests are made.
      const listReqs = httpMock.match('http://localhost:8080/api/transactions');
      expect(listReqs.length).toBe(2);
      listReqs[0].flush([mockActiveTx]);
      listReqs[1].flush([mockActiveTx]);
    });

    it('should control payment screen visibility', () => {
      expect(component.isPaymentScreen()).toBe(false);
      
      // Try payer with items in cart
      component.clickPayer();
      expect(component.isPaymentScreen()).toBe(true);

      component.clickRetour();
      expect(component.isPaymentScreen()).toBe(false);
    });

    it('should prevent moving to payment screen if cart is empty', () => {
      // Empty the cart
      transactionService.activeTransaction.set({
        ...mockActiveTx,
        items: [],
        totalAmount: 0.00
      });

      component.clickPayer();
      expect(component.isPaymentScreen()).toBe(false);
      expect(component.toasts().length).toBe(1);
    });
  });

  describe('Search & Scan Interaction', () => {
    it('should perform product search on query input >= 3 chars', () => {
      vi.useFakeTimers();
      component.searchQuery.set('WOL');
      component.onSearchInput();
      vi.advanceTimersByTime(300);

      const req = httpMock.expectOne('http://localhost:8080/api/products/search?query=WOL');
      req.flush([mockProduct]);

      expect(component.searchResults()).toEqual([mockProduct]);
      expect(component.showSearchDropdown()).toBe(true);
      vi.useRealTimers();
    });

    it('should clear search results if query is < 3 chars', () => {
      component.searchQuery.set('WO');
      component.onSearchInput();
      expect(component.searchResults().length).toBe(0);
      expect(component.showSearchDropdown()).toBe(false);
    });

    it('should scan and add item successfully when barcode exists', () => {
      component.searchQuery.set('4006885245808');
      
      // Mock scanAndAddItem behavior
      const spy = vi.spyOn(transactionService, 'scanAndAddItem').mockReturnValue(of(mockActiveTx));
      
      component.onSearchSubmit();

      expect(spy).toHaveBeenCalledWith('4006885245808');
      expect(component.searchQuery()).toBe('');
      expect(component.showSearchDropdown()).toBe(false);
    });

    it('should display warning modal when price mismatch is detected', () => {
      const txWithPriceMismatch: Transaction = {
        ...mockActiveTx,
        items: [
          {
            id: 'item-1',
            product: mockProduct,
            quantity: 1,
            price: 15.00, // Quotation price
            priceChanged: true,
            oldPrice: 13.00 // Product price
          }
        ]
      };

      vi.spyOn(transactionService, 'scanAndAddItem').mockReturnValue(of(txWithPriceMismatch));
      
      component.searchQuery.set('4006885245808');
      component.onSearchSubmit();

      expect(component.activeModal()).toBe('PRICE_CHANGED');
      expect(component.priceChangeDetails()).toEqual({
        productName: 'CLE MEULEUSE WOLFCRAFT',
        barcode: '4006885245808',
        oldPrice: 13.00,
        newPrice: 15.00
      });
    });

    it('should fall back to product search by name if barcode scan fails', () => {
      vi.spyOn(transactionService, 'scanAndAddItem').mockReturnValue(throwError(() => new Error('Not found')));
      const searchSpy = vi.spyOn(transactionService, 'searchProducts').mockReturnValue(of([mockProduct]));

      component.searchQuery.set('WOLFCRAFT');
      component.onSearchSubmit();

      expect(searchSpy).toHaveBeenCalledWith('WOLFCRAFT');
    });
  });

  describe('Item Quantity Modifications', () => {
    it('should increment quantity', () => {
      const item = mockActiveTx.items[0];

      component.incrementQty(item);

      const req = httpMock.expectOne(request => 
        request.url === 'http://localhost:8080/api/transactions/items/item-1' &&
        request.params.get('quantity') === '2'
      );
      expect(req.request.method).toBe('PUT');
      req.flush(mockActiveTx);
    });

    it('should decrement quantity when qty > 1', () => {
      const item = { ...mockActiveTx.items[0], quantity: 3 };

      component.decrementQty(item);

      const req = httpMock.expectOne(request => 
        request.url === 'http://localhost:8080/api/transactions/items/item-1' &&
        request.params.get('quantity') === '2'
      );
      expect(req.request.method).toBe('PUT');
      req.flush(mockActiveTx);
    });

    it('should remove item when decrementing quantity of 1', () => {
      const item = mockActiveTx.items[0];

      component.decrementQty(item);

      const req = httpMock.expectOne('http://localhost:8080/api/transactions/items/item-1');
      expect(req.request.method).toBe('DELETE');
      req.flush(mockActiveTx);
    });

    it('should display warning modal when quantity update detects price mismatch', () => {
      const item = mockActiveTx.items[0];
      const txWithPriceMismatch: Transaction = {
        ...mockActiveTx,
        items: [
          {
            id: 'item-1',
            product: mockProduct,
            quantity: 2,
            price: 15.00,
            priceChanged: true,
            oldPrice: 13.00
          }
        ]
      };

      component.incrementQty(item);

      const req = httpMock.expectOne(request => 
        request.url === 'http://localhost:8080/api/transactions/items/item-1' &&
        request.params.get('quantity') === '2'
      );
      expect(req.request.method).toBe('PUT');
      req.flush(txWithPriceMismatch);

      fixture.detectChanges();

      expect(component.activeModal()).toBe('PRICE_CHANGED');
      expect(component.priceChangeDetails()).toEqual({
        productName: 'CLE MEULEUSE WOLFCRAFT',
        barcode: '4006885245808',
        oldPrice: 13.00,
        newPrice: 15.00
      });
    });
  });

  describe('Manager PIN Authorization', () => {
    it('should grant manager authorization on correct PIN', () => {
      component.codeA4Chiffres();
      expect(component.activeModal()).toBe('PIN');

      component.pinKeyPress('1');
      component.pinKeyPress('2');
      component.pinKeyPress('3');
      component.pinKeyPress('4');

      component.submitPin();

      expect(component.managerAuthorized()).toBe(true);
      expect(component.activeModal()).toBeNull();
    });

    it('should fail and show error on incorrect PIN', () => {
      component.codeA4Chiffres();
      component.pinKeyPress('9');
      component.pinKeyPress('9');
      component.pinKeyPress('9');
      component.pinKeyPress('9');

      component.submitPin();

      expect(component.managerAuthorized()).toBe(false);
      expect(component.pinError()).toBe(true);
      expect(component.pinBuffer()).toBe('');
    });
  });

  describe('Payment Workflows', () => {
    it('should handle Cash payment', () => {
      component.clickPayByCash();
      expect(component.activeModal()).toBe('CASH');

      component.cashReceivedInput.set('20');
      
      component.submitCashPayment();
      
      const req = httpMock.expectOne(r => 
        r.url === 'http://localhost:8080/api/transactions/pay' &&
        r.params.get('method') === 'CASH' &&
        r.params.get('amount') === '13'
      );
      req.flush({ ...mockActiveTx, status: 'PAID' });

      // completeCheckoutFlow triggers active transaction load
      const activeReq = httpMock.expectOne('http://localhost:8080/api/transactions/active');
      activeReq.flush(mockActiveTx);
    });

    it('should simulate Card payment workflow', () => {
      vi.useFakeTimers();

      const paySpy = vi.spyOn(transactionService, 'addPayment').mockReturnValue(of({ ...mockActiveTx, status: 'PAID' }));

      component.clickPayByCard();
      expect(component.activeModal()).toBe('CARD');
      expect(component.cardSimState()).toBe('PROCESSING');

      // Wait for card process simulation
      vi.advanceTimersByTime(2000);
      expect(component.cardSimState()).toBe('SUCCESS');

      // Wait for payment API submission trigger
      vi.advanceTimersByTime(1000);
      expect(paySpy).toHaveBeenCalled();

      // completeCheckoutFlow triggers active transaction load
      const activeReq = httpMock.expectOne('http://localhost:8080/api/transactions/active');
      activeReq.flush(mockActiveTx);

      vi.restoreAllMocks();
    });
  });

  describe('Customer and Invoice Settings', () => {
    it('should mock associate a standard client', () => {
      component.openClientLookup();
      expect(component.activeModal()).toBe('CLIENT');

      component.selectCustomerMock('standard');

      expect(component.selectedCustomer()).not.toBeNull();
      expect(component.selectedCustomer().name).toBe('M. ANIRBAN ROY');
      expect(component.activeModal()).toBeNull();
    });

    it('should toggle invoice setting', () => {
      component.toggleInvoice({ target: { checked: true } });

      const req = httpMock.expectOne(request => 
        request.url === 'http://localhost:8080/api/transactions/invoice' &&
        request.params.get('required') === 'true'
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockActiveTx);
    });
  });
});
