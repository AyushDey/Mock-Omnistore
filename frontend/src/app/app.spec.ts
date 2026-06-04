import { TestBed, ComponentFixture } from '@angular/core/testing';
import { App } from './app';
import { TransactionService } from './transaction.service';
import { signal, computed } from '@angular/core';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('App Component', () => {
  let component: App;
  let fixture: ComponentFixture<App>;
  let mockTransactionService: any;

  beforeEach(async () => {
    // Define mock signals and spies in Vitest format
    const activeTxSignal = signal<any>({
      id: 'tx-123',
      status: 'ACTIVE',
      totalAmount: 18.99,
      taxAmount: 3.17,
      discountAmount: 0.0,
      invoiceRequired: false,
      items: [
        {
          id: 'item-1',
          product: { name: 'CLE MEULEUSE WOLFCRAFT', barcode: '4006885245808', price: 13.0, taxRate: 20 },
          quantity: 1,
          price: 13.0
        }
      ],
      payments: []
    });

    const suspendedListSignal = signal<any[]>([]);

    mockTransactionService = {
      activeTransaction: activeTxSignal,
      priceChangeAlert: signal<any>(null),
      suspendedTransactions: suspendedListSignal,
      allTransactions: signal<any[]>([]),
      loading: signal(false),
      errorMessage: signal(null),
      
      // Computed mock approximations using standard signals
      cartItems: computed(() => activeTxSignal()?.items ?? []),
      totalAmount: computed(() => activeTxSignal()?.totalAmount ?? 0),
      taxAmount: computed(() => activeTxSignal()?.taxAmount ?? 0),
      discountAmount: computed(() => activeTxSignal()?.discountAmount ?? 0),
      invoiceRequired: computed(() => activeTxSignal()?.invoiceRequired ?? false),
      payments: computed(() => activeTxSignal()?.payments ?? []),
      totalPaid: computed(() => 0),
      amountDue: computed(() => activeTxSignal()?.totalAmount ?? 0),

      loadActiveTransaction: vi.fn(),
      loadAllTransactions: vi.fn(),
      scanAndAddItem: vi.fn().mockReturnValue(of(activeTxSignal())),
      updateItemQuantity: vi.fn(),
      removeItem: vi.fn(),
      suspendTransaction: vi.fn().mockReturnValue(of({ status: 'SUSPENDED' })),
      loadSuspendedTransactions: vi.fn(),
      resumeTransaction: vi.fn().mockReturnValue(of(activeTxSignal())),
      abandonTransaction: vi.fn().mockReturnValue(of({ items: [] })),
      addPayment: vi.fn().mockReturnValue(of(activeTxSignal())),
      toggleInvoiceRequired: vi.fn(),
      searchProducts: vi.fn().mockReturnValue(of([]))
    };

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        { provide: TransactionService, useValue: mockTransactionService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(App);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the POS app component', () => {
    expect(component).toBeTruthy();
  });

  it('should default to transaction view (isPaymentScreen should be false)', () => {
    expect(component.isPaymentScreen()).toBe(false);
  });

  it('should switch to payment screen when clickPayer is called and cart is not empty', () => {
    component.clickPayer();
    expect(component.isPaymentScreen()).toBe(true);
  });

  it('should switch back to transaction view when clickRetour is called', () => {
    component.isPaymentScreen.set(true);
    component.clickRetour();
    expect(component.isPaymentScreen()).toBe(false);
  });

  it('should dismiss any active modal when switching tabs via selectTab', () => {
    component.activeModal.set('CLIENT');
    expect(component.activeModal()).toBe('CLIENT');
    
    component.selectTab('ARTICLE_LOOKUP');
    expect(component.activeModal()).toBeNull();
    expect(component.activeTab()).toBe('ARTICLE_LOOKUP');
  });

  it('should open the manager PIN dialog and grant access with code 1234', () => {
    component.codeA4Chiffres();
    expect(component.activeModal()).toBe('PIN');
    
    component.pinKeyPress('1');
    component.pinKeyPress('2');
    component.pinKeyPress('3');
    component.pinKeyPress('4');
    expect(component.pinBuffer()).toBe('1234');

    component.submitPin();
    expect(component.managerAuthorized()).toBe(true);
    expect(component.activeModal()).toBeNull();
  });

  it('should reject wrong manager PIN code', () => {
    component.codeA4Chiffres();
    component.pinKeyPress('9');
    component.pinKeyPress('9');
    component.pinKeyPress('9');
    component.pinKeyPress('9');
    component.submitPin();
    
    expect(component.managerAuthorized()).toBe(false);
    expect(component.pinError()).toBe(true);
  });

  it('should open the PRICE_CHANGED modal when priceChangeAlert signal is set', () => {
    mockTransactionService.priceChangeAlert.set({ message: 'Price changed test message' });
    fixture.detectChanges();
    expect(component.activeModal()).toBe('PRICE_CHANGED');
  });

  it('should clear priceChangeAlert signal when PRICE_CHANGED modal is closed', () => {
    component.activeModal.set('PRICE_CHANGED');
    mockTransactionService.priceChangeAlert.set({ message: 'Price changed test message' });
    component.closeModal();
    expect(mockTransactionService.priceChangeAlert()).toBeNull();
    expect(component.activeModal()).toBeNull();
  });
});
