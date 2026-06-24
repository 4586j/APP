import type { ApiResult } from '@/types/api'
import { get, post, put, del } from './request'

/* ────── Exchange Rate ────── */

export interface ExchangeRateVO {
  id: number
  currencyFrom: string
  currencyTo: string
  rate: number
  rateDate: string
  source: string
  createdAt: string
}

export interface ExchangeRatePageVO {
  records: ExchangeRateVO[]
  total: number
  size: number
  current: number
}

export function listExchangeRates(params: { date?: string; currency?: string; page?: number; size?: number }) {
  return get<ExchangeRatePageVO>('/exchange-rates', params)
}

export function createExchangeRate(data: {
  currencyFrom: string
  currencyTo: string
  rate: number
  rateDate: string
  source?: string
}) {
  return post<number>('/exchange-rates', data)
}

export function deleteExchangeRate(id: number) {
  return del<void>(`/exchange-rates/${id}`)
}

/* ────── Fund Approval ────── */

export interface FundApprovalVO {
  id: number
  requestNo: string
  title: string
  fundType: string
  amount: number
  currency: string
  supplierId: number | null
  supplierName: string | null
  description: string | null
  status: string
  applicant: number | null
  createdAt: string
}

export interface FundApprovalPageVO {
  records: FundApprovalVO[]
  total: number
  size: number
  current: number
}

export function listFundApprovals(params: { requestNo?: string; fundType?: string; status?: string; page?: number; size?: number }) {
  return get<FundApprovalPageVO>('/fund-approvals', params)
}

export function createFundApproval(data: {
  title: string
  fundType: string
  amount: number
  currency?: string
  supplierId?: number | null
  description?: string
}) {
  return post<number>('/fund-approvals', data)
}

export function approveFundApproval(id: number) {
  return put<void>(`/fund-approvals/${id}/approve`)
}

export function rejectFundApproval(id: number, comment: string) {
  return put<void>(`/fund-approvals/${id}/reject`, comment)
}
