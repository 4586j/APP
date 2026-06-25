import { get, post, put, del } from './request'

export interface DocumentVO {
  id: number; docNo: string; docType: string; orderId: number | null; orderNo: string | null
  shipmentId: number | null; title: string; status: string; templateCode: string | null
  filePath: string | null; fileName: string | null; fileSize: number | null
  generatedBy: number | null; generatedAt: string | null; remark: string | null; createdAt: string
}
export interface DocumentPageVO { records: DocumentVO[]; total: number; size: number; current: number }

export function listDocuments(params: { docNo?: string; docType?: string; status?: string; page?: number; size?: number }) {
  return get<DocumentPageVO>('/documents', params)
}
export function getDocument(id: number) { return get<DocumentVO>('/documents/'+id) }
export function createDocument(data: { docType: string; title: string; orderId?: number; orderNo?: string; shipmentId?: number; templateCode?: string; remark?: string }) {
  return post<number>('/documents', data)
}
export function finalizeDocument(id: number) { return put<void>('/documents/'+id+'/finalize') }
export function deleteDocument(id: number) { return del<void>('/documents/'+id) }
