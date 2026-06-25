package com.erp.logistics.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.logistics.dto.*;
import com.erp.logistics.entity.*;
import com.erp.logistics.mapper.*;
import com.erp.logistics.service.LogShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate; import java.time.format.DateTimeFormatter;
import java.util.List;

@Service @RequiredArgsConstructor
public class LogShipmentServiceImpl implements LogShipmentService {
    final LogShipmentMapper shipmentMapper;
    final LogTrackingMapper trackingMapper;

    @Override public ShipmentPageVO listPage(ShipmentQuery q){
        if(q.getPage()==null||q.getPage()<1)q.setPage(1); if(q.getSize()==null||q.getSize()<1)q.setSize(20);
        var w=new LambdaQueryWrapper<LogShipment>().eq(LogShipment::getDeleted,0);
        if(q.getShipmentNo()!=null) w.like(LogShipment::getShipmentNo,"%"+q.getShipmentNo()+"%");
        if(q.getMethod()!=null) w.eq(LogShipment::getMethod,q.getMethod());
        if(q.getStatus()!=null) w.eq(LogShipment::getStatus,q.getStatus());
        if(q.getKeyword()!=null) w.like(LogShipment::getCarrier,"%"+q.getKeyword()+"%").or().like(LogShipment::getOrderNo,"%"+q.getKeyword()+"%").or().like(LogShipment::getCustomerName,"%"+q.getKeyword()+"%");
        w.orderByDesc(LogShipment::getCreatedAt);
        var p=shipmentMapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)),w);
        var records=p.getRecords().stream().map(e->ShipmentVO.builder().id(e.getId()).shipmentNo(e.getShipmentNo())
            .orderId(e.getOrderId()).orderNo(e.getOrderNo()).customerId(e.getCustomerId()).customerName(e.getCustomerName())
            .method(e.getMethod()).status(e.getStatus()).carrier(e.getCarrier()).vesselFlight(e.getVesselFlight())
            .containerNo(e.getContainerNo()).sealNo(e.getSealNo()).blNo(e.getBlNo())
            .etd(e.getEtd()).eta(e.getEta()).portLoading(e.getPortLoading()).portDischarge(e.getPortDischarge())
            .grossWeight(e.getGrossWeight()).netWeight(e.getNetWeight()).volume(e.getVolume()).packageCount(e.getPackageCount())
            .shippingMarks(e.getShippingMarks()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build()).toList();
        return ShipmentPageVO.builder().records(records).total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }
    @Override public ShipmentVO getById(Long id){
        var e=shipmentMapper.selectById(id); if(e==null)return null;
        return ShipmentVO.builder().id(e.getId()).shipmentNo(e.getShipmentNo()).orderId(e.getOrderId()).orderNo(e.getOrderNo())
            .customerId(e.getCustomerId()).customerName(e.getCustomerName()).method(e.getMethod()).status(e.getStatus())
            .carrier(e.getCarrier()).vesselFlight(e.getVesselFlight()).containerNo(e.getContainerNo()).sealNo(e.getSealNo())
            .blNo(e.getBlNo()).etd(e.getEtd()).eta(e.getEta()).portLoading(e.getPortLoading()).portDischarge(e.getPortDischarge())
            .grossWeight(e.getGrossWeight()).netWeight(e.getNetWeight()).volume(e.getVolume()).packageCount(e.getPackageCount())
            .shippingMarks(e.getShippingMarks()).remark(e.getRemark()).createdAt(e.getCreatedAt()).build();
    }
    @Override @Transactional public Long create(ShipmentCreateRequest r){
        var datePart=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var e=new LogShipment(); e.setShipmentNo("SH-"+datePart+"-"+System.currentTimeMillis()%100000);
        e.setMethod(r.getMethod()); e.setOrderId(r.getOrderId()); e.setOrderNo(r.getOrderNo());
        e.setCustomerId(r.getCustomerId()); e.setCustomerName(r.getCustomerName());
        e.setCarrier(r.getCarrier()); e.setVesselFlight(r.getVesselFlight());
        e.setContainerNo(r.getContainerNo()); e.setSealNo(r.getSealNo()); e.setBlNo(r.getBlNo());
        e.setEtd(r.getEtd()); e.setEta(r.getEta()); e.setPortLoading(r.getPortLoading()); e.setPortDischarge(r.getPortDischarge());
        e.setGrossWeight(r.getGrossWeight()); e.setNetWeight(r.getNetWeight()); e.setVolume(r.getVolume()); e.setPackageCount(r.getPackageCount());
        e.setShippingMarks(r.getShippingMarks()); e.setRemark(r.getRemark());
        shipmentMapper.insert(e); return e.getId();
    }
    @Override @Transactional public void updateStatus(Long id, String status){
        var e=shipmentMapper.selectById(id); if(e!=null){e.setStatus(status);shipmentMapper.updateById(e);}
    }
    @Override @Transactional public void delete(Long id){shipmentMapper.deleteById(id);}
    @Override @Transactional public TrackingVO addTracking(TrackingCreateRequest r){
        var e=new LogTracking(); e.setShipmentId(r.getShipmentId()); e.setTrackingDate(r.getTrackingDate());
        e.setLocation(r.getLocation()); e.setEventCode(r.getEventCode()); e.setDescription(r.getDescription()); e.setOperator(r.getOperator());
        trackingMapper.insert(e);
        return TrackingVO.builder().id(e.getId()).shipmentId(e.getShipmentId()).trackingDate(e.getTrackingDate())
            .location(e.getLocation()).eventCode(e.getEventCode()).description(e.getDescription()).operator(e.getOperator()).createdAt(e.getCreatedAt()).build();
    }
    @Override public List<TrackingVO> getTrackings(Long shipmentId){
        var w=new LambdaQueryWrapper<LogTracking>().eq(LogTracking::getShipmentId,shipmentId).orderByDesc(LogTracking::getTrackingDate);
        return trackingMapper.selectList(w).stream().map(e->TrackingVO.builder().id(e.getId()).shipmentId(e.getShipmentId())
            .trackingDate(e.getTrackingDate()).location(e.getLocation()).eventCode(e.getEventCode())
            .description(e.getDescription()).operator(e.getOperator()).createdAt(e.getCreatedAt()).build()).toList();
    }
}