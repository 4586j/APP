import { get, post, put, del } from './request'
import type { PageResult } from './system'

/* ---- 产品 ---- */
export interface ProductQuery {
  page?: number
  size?: number
  keyword?: string
  categoryId?: number
  hsCode?: string
}

export interface ProductVO {
  id: number
  productCode: string
  productName: string
  productNameEn?: string
  categoryId?: number
  categoryName?: string
  hsCodeId?: number
  hsCode?: string
  unit: string
  specification?: string
  originCountry?: string
  brand?: string
  purchasePrice?: number
  salesPrice?: number
  costPrice?: number
  weightKg?: number
  volumeCbm?: number
  moq?: number
  status: number
  createdAt?: string
}

export interface ProductPageVO {
  records: ProductVO[]
  total: number
  size: number
  current: number
}

export interface ProductCreateRequest {
  productCode: string
  productName: string
  productNameEn?: string
  categoryId?: number
  hsCodeId?: number
  unit?: string
  specification?: string
  originCountry?: string
  brand?: string
  purchasePrice?: number
  salesPrice?: number
  costPrice?: number
  weightKg?: number
  volumeCbm?: number
  moq?: number
}

export function listProducts(params: ProductQuery) {
  return get<ProductPageVO>('/products', params)
}

export function getProduct(id: number) {
  return get<ProductVO>(`/products/${id}`)
}

export function createProduct(data: ProductCreateRequest) {
  return post<number>('/products', data)
}

export function updateProduct(id: number, data: Partial<ProductCreateRequest>) {
  return put<void>(`/products/${id}`, data)
}

export function deleteProduct(id: number) {
  return del<void>(`/products/${id}`)
}

/* ---- 分类 ---- */
export interface CategoryNode {
  id: number
  parentId?: number
  catName: string
  catCode: string
  sortOrder?: number
  children?: CategoryNode[]
}

export interface CategoryCreateRequest {
  catName: string
  catCode: string
  parentId?: number
  sortOrder?: number
}

export function listCategoryTree() {
  return get<CategoryNode[]>('/categories/tree')
}

export function getCategory(id: number) {
  return get<CategoryNode>(`/categories/${id}`)
}

export function createCategory(data: CategoryCreateRequest) {
  return post<number>('/categories', data)
}

export function updateCategory(id: number, data: Partial<CategoryCreateRequest>) {
  return put<void>(`/categories/${id}`, data)
}

export function deleteCategory(id: number) {
  return del<void>(`/categories/${id}`)
}
