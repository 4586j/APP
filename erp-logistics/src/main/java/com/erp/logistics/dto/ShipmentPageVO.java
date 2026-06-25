package com.erp.logistics.dto;
import lombok.Builder; import lombok.Data; import java.util.List;
@Data @Builder
public class ShipmentPageVO { List<ShipmentVO> records; long total; long size; long current; }