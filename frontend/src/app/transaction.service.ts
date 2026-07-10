import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface Product {
  id: string;
  name: string;
  barcode: string;
  price: number;
  imageUrl: string;
  taxRate: number;
}

export interface TransactionItem {
  id: string;
  product: Product;
  quantity: number;
  price: number;
  priceChanged?: boolean;
  oldPrice?: number;
}

export interface Payment {
  id: string;
  method: string;
  amount: number;
  createdAt: string;
}

export interface Transaction {
  id: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'PAID' | 'ABANDONED';
  totalAmount: number;
  taxAmount: number;
  discountAmount: number;
  invoiceRequired: boolean;
  items: TransactionItem[];
  payments: Payment[];
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  readonly http = inject(HttpClient);
  readonly baseUrl = 'http://localhost:8080/api';

  // Signals for state management
  readonly activeTransaction = signal<Transaction | null>(null);
  readonly suspendedTransactions = signal<Transaction[]>([]);
  readonly allTransactions = signal<Transaction[]>([]);
  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);

  // Computed signals
  readonly cartItems = computed(() => this.activeTransaction()?.items ?? []);
  readonly totalAmount = computed(() => this.activeTransaction()?.totalAmount ?? 0);
  readonly taxAmount = computed(() => this.activeTransaction()?.taxAmount ?? 0);
  readonly discountAmount = computed(() => this.activeTransaction()?.discountAmount ?? 0);
  readonly invoiceRequired = computed(() => this.activeTransaction()?.invoiceRequired ?? false);
  readonly payments = computed(() => this.activeTransaction()?.payments ?? []);
  
  readonly totalPaid = computed(() => {
    return this.payments().reduce((sum, p) => sum + p.amount, 0);
  });
  
  readonly amountDue = computed(() => {
    const total = this.totalAmount();
    const paid = this.totalPaid();
    return Math.max(0, parseFloat((total - paid).toFixed(2)));
  });

  loadActiveTransaction(): void {
    this.loading.set(true);
    this.http.get<Transaction>(`${this.baseUrl}/transactions/active`).subscribe({
      next: (tx) => {
        this.activeTransaction.set(tx);
        this.loading.set(false);
      },
      error: (err) => this.handleError('Failed to load active transaction', err)
    });
  }

  scanAndAddItem(barcode: string, quantity = 1): Observable<Transaction> {
    this.loading.set(true);
    const params = new HttpParams()
      .set('barcode', barcode)
      .set('quantity', quantity.toString());
      
    return this.http.post<Transaction>(`${this.baseUrl}/transactions/items`, {}, { params }).pipe(
      tap({
        next: (tx) => {
          this.activeTransaction.set(tx);
          this.errorMessage.set(null);
          this.loading.set(false);
        },
        error: (err) => this.handleError('Item barcode scan failed', err)
      })
    );
  }

  updateItemQuantity(itemId: string, quantity: number): void {
    this.loading.set(true);
    const params = new HttpParams().set('quantity', quantity.toString());
    
    this.http.put<Transaction>(`${this.baseUrl}/transactions/items/${itemId}`, {}, { params }).subscribe({
      next: (tx) => {
        this.activeTransaction.set(tx);
        this.loading.set(false);
      },
      error: (err) => this.handleError('Failed to update quantity', err)
    });
  }

  removeItem(itemId: string): void {
    this.loading.set(true);
    this.http.delete<Transaction>(`${this.baseUrl}/transactions/items/${itemId}`).subscribe({
      next: (tx) => {
        this.activeTransaction.set(tx);
        this.loading.set(false);
      },
      error: (err) => this.handleError('Failed to remove item', err)
    });
  }

  suspendTransaction(): Observable<Transaction> {
    this.loading.set(true);
    return this.http.post<Transaction>(`${this.baseUrl}/transactions/suspend`, {}).pipe(
      tap({
        next: (tx) => {
          this.activeTransaction.set(null);
          this.loadActiveTransaction(); // reload/create new active transaction
          this.loading.set(false);
        },
        error: (err) => this.handleError('Failed to suspend transaction', err)
      })
    );
  }

  loadSuspendedTransactions(): void {
    this.loading.set(true);
    this.http.get<Transaction[]>(`${this.baseUrl}/transactions/suspended`).subscribe({
      next: (txs) => {
        this.suspendedTransactions.set(txs);
        this.loading.set(false);
      },
      error: (err) => this.handleError('Failed to load suspended transactions', err)
    });
  }

  loadAllTransactions(): void {
    this.loading.set(true);
    this.http.get<Transaction[]>(`${this.baseUrl}/transactions`).subscribe({
      next: (txs) => {
        this.allTransactions.set(txs);
        this.loading.set(false);
      },
      error: (err) => this.handleError('Failed to load all transactions', err)
    });
  }

  resumeTransaction(id: string): Observable<Transaction> {
    this.loading.set(true);
    return this.http.post<Transaction>(`${this.baseUrl}/transactions/resume/${id}`, {}).pipe(
      tap({
        next: (tx) => {
          this.activeTransaction.set(tx);
          this.loading.set(false);
        },
        error: (err) => this.handleError('Failed to resume transaction', err)
      })
    );
  }

  abandonTransaction(): Observable<Transaction> {
    this.loading.set(true);
    return this.http.post<Transaction>(`${this.baseUrl}/transactions/abandon`, {}).pipe(
      tap({
        next: (tx) => {
          this.activeTransaction.set(tx);
          this.loading.set(false);
        },
        error: (err) => this.handleError('Failed to abandon transaction', err)
      })
    );
  }

  addPayment(method: string, amount: number): Observable<Transaction> {
    this.loading.set(true);
    const params = new HttpParams()
      .set('method', method)
      .set('amount', amount.toString());
      
    return this.http.post<Transaction>(`${this.baseUrl}/transactions/pay`, {}, { params }).pipe(
      tap({
        next: (tx) => {
          this.activeTransaction.set(tx);
          this.loading.set(false);
        },
        error: (err) => this.handleError('Failed to process payment', err)
      })
    );
  }

  toggleInvoiceRequired(required: boolean): void {
    this.loading.set(true);
    const params = new HttpParams().set('required', required.toString());
    
    this.http.post<Transaction>(`${this.baseUrl}/transactions/invoice`, {}, { params }).subscribe({
      next: (tx) => {
        this.activeTransaction.set(tx);
        this.loading.set(false);
      },
      error: (err) => this.handleError('Failed to toggle invoice status', err)
    });
  }

  searchProducts(query: string): Observable<Product[]> {
    const params = new HttpParams().set('query', query);
    return this.http.get<Product[]>(`${this.baseUrl}/products/search`, { params });
  }

  private handleError(message: string, error: any): void {
    console.error(message, error);
    this.loading.set(false);
    this.errorMessage.set(`${message}: ${error.error?.message || error.message || 'Unknown error'}`);
  }
}
