import { get, post, put, del } from './request'
import type { PageResult } from './system'

/* ---- 客户 ---- */
export interface CustomerQuery {
  page?: number; size?: number; keyword?: string
  country?: string; customerType?: string
}

export interface CustomerVO {
  id: number; customerCode: string; customerName: string; customerNameEn?: string
  customerType?: string; country?: string; contactPerson?: string; contactEmail?: string
  contactPhone?: string; contactFax?: string; website?: string; address?: string
  creditLimit?: number; paymentTerms?: string; taxId?: string; swiftCode?: string
  bankName?: string; bankAccount?: string; status: number; createdAt?: string
}

export interface CustomerCreateRequest {
  customerCode: string; customerName: string; customerNameEn?: string
  customerType?: string; country?: string; contactPerson?: string; contactEmail?: string
  contactPhone?: string; paymentTerms?: string; creditLimit?: number
  taxId?: string; swiftCode?: string
}

export function listCustomers(params: CustomerQuery) { return get<PageResult<CustomerVO>>('/customers', params) }
export function getCustomer(id: number) { return get<CustomerVO>('/customers/' + id) }
export function createCustomer(data: CustomerCreateRequest) { return post<number>('/customers', data) }
export function updateCustomer(id: number, data: Partial<CustomerCreateRequest>) { return put<void>('/customers/' + id, data) }
export function deleteCustomer(id: number) { return del<void>('/customers/' + id) }

/* ---- 供应商 ---- */
export interface SupplierQuery {
  page?: number; size?: number; keyword?: string
  province?: string; rating?: number
}

export interface SupplierVO {
  id: number; supplierCode: string; supplierName: string
  province?: string; city?: string; contactPerson?: string; contactPhone?: string
  contactEmail?: string; address?: string; rating?: number; paymentTerms?: string
  bankName?: string; bankAccount?: string; taxId?: string; mainProducts?: string
  cooperationYears?: number; status: number; createdAt?: string
}

export interface SupplierCreateRequest {
  supplierCode: string; supplierName: string; province?: string; city?: string
  contactPerson?: string; contactPhone?: string; contactEmail?: string; address?: string
  rating?: number; paymentTerms?: string; mainProducts?: string
}

export function listSuppliers(params: SupplierQuery) { return get<PageResult<SupplierVO>>('/suppliers', params) }
export function getSupplier(id: number) { return get<SupplierVO>('/suppliers/' + id) }
export function createSupplier(data: SupplierCreateRequest) { return post<number>('/suppliers', data) }
export function updateSupplier(id: number, data: Partial<SupplierCreateRequest>) { return put<void>('/suppliers/' + id, data) }
export function deleteSupplier(id: number) { return del<void>('/suppliers/' + id) }
