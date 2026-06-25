package com.erp.logistics.controller;
import com.erp.common.model.R; import com.erp.logistics.dto.*; import com.erp.logistics.service.LogShipmentService;
import jakarta.validation.Valid; import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/shipments") @RequiredArgsConstructor
public class ShipmentController {
    final LogShipmentService service;
    @GetMapping public R<ShipmentPageVO> list(ShipmentQuery q){return R.ok(service.listPage(q));}
    @GetMapping("/{id}") public R<ShipmentVO> get(@PathVariable Long id){return R.ok(service.getById(id));}
    @PostMapping public R<Long> create(@Valid @RequestBody ShipmentCreateRequest r){return R.ok(service.create(r));}
    @PutMapping("/{id}/status") public R<Void> updateStatus(@PathVariable Long id, @RequestBody String body){service.updateStatus(id,body.replace("\"",""));return R.ok();}
    @DeleteMapping("/{id}") public R<Void> delete(@PathVariable Long id){service.delete(id);return R.ok();}
    @PostMapping("/tracking") public R<TrackingVO> addTracking(@Valid @RequestBody TrackingCreateRequest r){return R.ok(service.addTracking(r));}
    @GetMapping("/{id}/trackings") public R<java.util.List<TrackingVO>> getTrackings(@PathVariable Long id){return R.ok(service.getTrackings(id));}
}
