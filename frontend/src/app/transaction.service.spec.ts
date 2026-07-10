import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TransactionService, Transaction, Product } from './transaction.service';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('TransactionService', () => {
  let service: TransactionService;
  let httpMock: HttpTestingController;

  const mockProduct: Product = {
    id: 'prod-123',
    name: 'Test Product',
    barcode: '12345678',
    price: 10.00,
    imageUrl: '',
    taxRate: 20
  };

  const mockTransaction: Transaction = {
    id: 'tx-999',
    status: 'ACTIVE',
    totalAmount: 12.00,
    taxAmount: 2.00,
    discountAmount: 0.00,
    invoiceRequired: false,
    items: [
      {
        id: 'item-1',
        product: mockProduct,
        quantity: 1,
        price: 10.00
      }
    ],
    payments: [],
    createdAt: '2026-06-18T05:00:00',
    updatedAt: '2026-06-18T05:00:00'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TransactionService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(TransactionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
    expect(service.activeTransaction()).toBeNull();
  });

  it('should load active transaction', () => {
    service.loadActiveTransaction();
    expect(service.loading()).toBe(true);

    const req = httpMock.expectOne('http://localhost:8080/api/transactions/active');
    expect(req.request.method).toBe('GET');
    req.flush(mockTransaction);

    expect(service.activeTransaction()).toEqual(mockTransaction);
    expect(service.loading()).toBe(false);
  });

  it('should scan and add item', () => {
    let resultTx: Transaction | undefined;
    service.scanAndAddItem('12345678', 2).subscribe(tx => {
      resultTx = tx;
    });

    const req = httpMock.expectOne(request => 
      request.url === 'http://localhost:8080/api/transactions/items' &&
      request.params.get('barcode') === '12345678' &&
      request.params.get('quantity') === '2'
    );
    expect(req.request.method).toBe('POST');
    req.flush(mockTransaction);

    expect(resultTx).toEqual(mockTransaction);
    expect(service.activeTransaction()).toEqual(mockTransaction);
  });

  it('should update item quantity', () => {
    service.updateItemQuantity('item-1', 5);

    const req = httpMock.expectOne(request => 
      request.url === 'http://localhost:8080/api/transactions/items/item-1' &&
      request.params.get('quantity') === '5'
    );
    expect(req.request.method).toBe('PUT');
    req.flush(mockTransaction);

    expect(service.activeTransaction()).toEqual(mockTransaction);
  });

  it('should remove item', () => {
    service.removeItem('item-1');

    const req = httpMock.expectOne('http://localhost:8080/api/transactions/items/item-1');
    expect(req.request.method).toBe('DELETE');
    req.flush({ ...mockTransaction, items: [] });

    expect(service.activeTransaction()?.items.length).toBe(0);
  });

  it('should suspend transaction and load new active transaction', () => {
    service.suspendTransaction().subscribe();

    const suspendReq = httpMock.expectOne('http://localhost:8080/api/transactions/suspend');
    expect(suspendReq.request.method).toBe('POST');
    suspendReq.flush(mockTransaction);

    // After suspension, loadActiveTransaction should be triggered
    const activeReq = httpMock.expectOne('http://localhost:8080/api/transactions/active');
    expect(activeReq.request.method).toBe('GET');
    activeReq.flush(null);

    expect(service.activeTransaction()).toBeNull();
  });

  it('should load suspended transactions', () => {
    service.loadSuspendedTransactions();

    const req = httpMock.expectOne('http://localhost:8080/api/transactions/suspended');
    expect(req.request.method).toBe('GET');
    req.flush([mockTransaction]);

    expect(service.suspendedTransactions()).toEqual([mockTransaction]);
  });

  it('should load all transactions', () => {
    service.loadAllTransactions();

    const req = httpMock.expectOne('http://localhost:8080/api/transactions');
    expect(req.request.method).toBe('GET');
    req.flush([mockTransaction]);

    expect(service.allTransactions()).toEqual([mockTransaction]);
  });

  it('should resume transaction', () => {
    service.resumeTransaction('tx-999').subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/transactions/resume/tx-999');
    expect(req.request.method).toBe('POST');
    req.flush(mockTransaction);

    expect(service.activeTransaction()).toEqual(mockTransaction);
  });

  it('should abandon transaction', () => {
    service.abandonTransaction().subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/transactions/abandon');
    expect(req.request.method).toBe('POST');
    req.flush(null);

    expect(service.activeTransaction()).toBeNull();
  });

  it('should add payment', () => {
    service.addPayment('CASH', 10.00).subscribe();

    const req = httpMock.expectOne(request => 
      request.url === 'http://localhost:8080/api/transactions/pay' &&
      request.params.get('method') === 'CASH' &&
      request.params.get('amount') === '10'
    );
    expect(req.request.method).toBe('POST');
    
    const updatedTx: Transaction = {
      ...mockTransaction,
      payments: [{ id: 'pay-1', method: 'CASH', amount: 10.00, createdAt: '2026-06-18T05:00:00' }]
    };
    req.flush(updatedTx);

    expect(service.activeTransaction()).toEqual(updatedTx);
  });

  it('should toggle invoice required status', () => {
    service.toggleInvoiceRequired(true);

    const req = httpMock.expectOne(request => 
      request.url === 'http://localhost:8080/api/transactions/invoice' &&
      request.params.get('required') === 'true'
    );
    expect(req.request.method).toBe('POST');
    req.flush({ ...mockTransaction, invoiceRequired: true });

    expect(service.activeTransaction()?.invoiceRequired).toBe(true);
  });

  it('should search products', () => {
    let results: Product[] | undefined;
    service.searchProducts('WOLFCRAFT').subscribe(products => {
      results = products;
    });

    const req = httpMock.expectOne(request => 
      request.url === 'http://localhost:8080/api/products/search' &&
      request.params.get('query') === 'WOLFCRAFT'
    );
    expect(req.request.method).toBe('GET');
    req.flush([mockProduct]);

    expect(results).toEqual([mockProduct]);
  });

  describe('Computed Signals', () => {
    it('should calculate cart details correctly', () => {
      // Set the active transaction signal manually
      service.activeTransaction.set(mockTransaction);

      expect(service.cartItems()).toEqual(mockTransaction.items);
      expect(service.totalAmount()).toBe(12.00);
      expect(service.taxAmount()).toBe(2.00);
      expect(service.discountAmount()).toBe(0.00);
      expect(service.invoiceRequired()).toBe(false);
      expect(service.payments()).toEqual([]);
      expect(service.totalPaid()).toBe(0.00);
      expect(service.amountDue()).toBe(12.00);
    });

    it('should calculate amount due correctly when payments are made', () => {
      const activeTxWithPayments: Transaction = {
        ...mockTransaction,
        totalAmount: 12.00,
        payments: [
          { id: 'p1', method: 'CASH', amount: 5.00, createdAt: '' },
          { id: 'p2', method: 'CARD', amount: 4.50, createdAt: '' }
        ]
      };

      service.activeTransaction.set(activeTxWithPayments);

      expect(service.totalPaid()).toBe(9.50);
      expect(service.amountDue()).toBe(2.50);
    });
  });
});
