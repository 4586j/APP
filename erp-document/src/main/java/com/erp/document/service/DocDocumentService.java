package com.erp.document.service;
import com.erp.document.dto.*;
public interface DocDocumentService {
    DocumentPageVO listPage(DocumentQuery q); DocumentVO getById(Long id);
    Long create(DocumentCreateRequest r); void finalize(Long id);
    void delete(Long id);
}