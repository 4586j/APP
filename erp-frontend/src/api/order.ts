import { get, post, put, del } from './request'
import type { PageResult } from './system'

export interface SalesOrderQuery {
  page?: number; size?: number; keyword?: string; customerId?: number
  status?: string; dateFrom?: string; dateTo?: string
}

export interface SalesOrderItemVO {
  id?: number; lineNo?: number; productId: number; productCode?: string
  productName?: string; hsCode?: string; specification?: string
  quantity: number; unit?: string; unitPrice: number; totalPrice?: number
}

export interface SalesOrderVO {
  id: number; orderNo: string; customerId: number; customerName?: string
  customerOrderNo?: string; orderDate: string; currency: string
  tradeTerms?: string; paymentTerms?: string; portLoading?: string
  portDestination?: string; expectedDelivery?: string
  totalAmount?: number; totalCnyAmount?: number; exchangeRate?: number
  remarks?: string; status: string; items: SalesOrderItemVO[]
  createdBy?: number; createdAt?: string; updatedAt?: string
}

export interface SalesOrderCreateRequest {
  customerId: number; customerOrderNo?: string; orderDate: string
  currency?: string; tradeTerms?: string; paymentTerms?: string
  portLoading?: string; portDestination?: string; expectedDelivery?: string
  exchangeRate?: number; remarks?: string
  items: { productId: number; productCode?: string; productName?: string; hsCode?: string; specification?: string; quantity: number; unit?: string; unitPrice: number }[]
}

export function listSalesOrders(params: SalesOrderQuery) { return get<PageResult<SalesOrderVO>>('/sales-orders', params) }
export function getSalesOrder(id: number) { return get<SalesOrderVO>('/sales-orders/' + id) }
export function createSalesOrder(data: SalesOrderCreateRequest) { return post<SalesOrderVO>('/sales-orders', data) }
export function updateSalesOrderStatus(id: number, toStatus: string, remark?: string) { return put<SalesOrderVO>('/sales-orders/' + id + '/status', { toStatus, remark }) }
export function deleteSalesOrder(id: number) { return del<void>('/sales-orders/' + id) }

/* ---- 采购订单 ---- */
export interface PurchaseOrderQuery {
  page?: number; size?: number; keyword?: string; supplierId?: number; status?: string
}
export interface PurchaseOrderItemVO {
  id?: number; lineNo?: number; productId: number; productName?: string
  quantity: number; unitPrice: number; totalPrice?: number
}
export interface PurchaseOrderVO {
  id: number; orderNo: string; supplierId: number; supplierName?: string
  orderDate: string; expectedDelivery?: string; totalAmount?: number
  currency?: string; paymentTerms?: string; status: string
  items: PurchaseOrderItemVO[]; createdAt?: string; updatedAt?: string
}
export function listPurchaseOrders(params: PurchaseOrderQuery) { return get<PageResult<PurchaseOrderVO>>('/purchase-orders', params) }
export function getPurchaseOrder(id: number) { return get<PurchaseOrderVO>('/purchase-orders/' + id) }
export function deletePurchaseOrder(id: number) { return del<void>('/purchase-orders/' + id) }
