import { get, post, put, del } from './request'

export interface ShipmentVO {
  id: number; shipmentNo: string; orderId: number | null; orderNo: string | null
  customerId: number | null; customerName: string | null
  method: string; status: string; carrier: string | null; vesselFlight: string | null
  containerNo: string | null; sealNo: string | null; blNo: string | null
  etd: string | null; eta: string | null; portLoading: string | null; portDischarge: string | null
  grossWeight: number | null; netWeight: number | null; volume: number | null; packageCount: number | null
  shippingMarks: string | null; remark: string | null; createdAt: string
}
export interface ShipmentPageVO { records: ShipmentVO[]; total: number; size: number; current: number }
export interface TrackingVO { id: number; shipmentId: number; trackingDate: string; location: string | null; eventCode: string | null; description: string | null; operator: string | null; createdAt: string }

export function listShipments(params: { shipmentNo?: string; method?: string; status?: string; keyword?: string; page?: number; size?: number }) {
  return get<ShipmentPageVO>('/shipments', params)
}
export function getShipment(id: number) { return get<ShipmentVO>('/shipments/'+id) }
export function createShipment(data: any) { return post<number>('/shipments', data) }
export function updateShipmentStatus(id: number, status: string) { return put<void>('/shipments/'+id+'/status', JSON.stringify(status)) }
export function deleteShipment(id: number) { return del<void>('/shipments/'+id) }
export function addTracking(data: { shipmentId: number; trackingDate: string; location?: string; eventCode?: string; description?: string; operator?: string }) {
  return post<TrackingVO>('/shipments/tracking', data)
}
export function getTrackings(shipmentId: number) { return get<TrackingVO[]>('/shipments/'+shipmentId+'/trackings') }
