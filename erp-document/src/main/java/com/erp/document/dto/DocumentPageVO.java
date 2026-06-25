package com.erp.document.dto;
import lombok.Builder; import lombok.Data; import java.util.List;
@Data @Builder
public class DocumentPageVO { List<DocumentVO> records; long total; long size; long current; }