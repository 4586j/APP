package com.erp.document.dto;
import lombok.Data;
@Data
public class DocumentQuery { String docNo; String docType; String status; Integer page=1; Integer size=20; }