package com.erp.logistics.service;
import com.erp.logistics.dto.*;
public interface LogShipmentService {
    ShipmentPageVO listPage(ShipmentQuery q); ShipmentVO getById(Long id);
    Long create(ShipmentCreateRequest r); void updateStatus(Long id, String status);
    void delete(Long id);
    TrackingVO addTracking(TrackingCreateRequest r);
    java.util.List<TrackingVO> getTrackings(Long shipmentId);
}