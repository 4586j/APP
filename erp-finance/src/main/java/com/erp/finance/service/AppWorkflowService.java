package com.erp.finance.service;
import com.erp.finance.dto.*;
import java.util.List;
/** 工作流配置 CRUD（审批引擎可配置节点） */
public interface AppWorkflowService {
    List<WorkflowVO> list();
    WorkflowVO getById(Long id);
    Long create(WorkflowSaveRequest r);
    void update(Long id, WorkflowSaveRequest r);
    void delete(Long id);
}
