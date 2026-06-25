package com.erp.finance.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import com.erp.finance.dto.*;
import com.erp.finance.entity.AppWorkflow;
import com.erp.finance.entity.AppWorkflowNode;
import com.erp.finance.mapper.AppWorkflowMapper;
import com.erp.finance.mapper.AppWorkflowNodeMapper;
import com.erp.finance.service.AppWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor
public class AppWorkflowServiceImpl implements AppWorkflowService {
    final AppWorkflowMapper workflowMapper;
    final AppWorkflowNodeMapper nodeMapper;

    private List<WorkflowVO.NodeVO> loadNodes(Long workflowId){
        return nodeMapper.selectList(new LambdaQueryWrapper<AppWorkflowNode>()
                .eq(AppWorkflowNode::getWorkflowId, workflowId)
                .orderByAsc(AppWorkflowNode::getNodeOrder))
            .stream().map(n -> WorkflowVO.NodeVO.builder().id(n.getId()).workflowId(n.getWorkflowId())
                .nodeOrder(n.getNodeOrder()).nodeName(n.getNodeName()).approverRole(n.getApproverRole())
                .minAmount(n.getMinAmount()).maxAmount(n.getMaxAmount()).build()).toList();
    }
    private WorkflowVO toVO(AppWorkflow w){
        return WorkflowVO.builder().id(w.getId()).workflowCode(w.getWorkflowCode())
            .workflowName(w.getWorkflowName()).targetType(w.getTargetType())
            .description(w.getDescription()).status(w.getStatus()).createdAt(w.getCreatedAt())
            .nodes(loadNodes(w.getId())).build();
    }

    @Override public List<WorkflowVO> list(){
        return workflowMapper.selectList(new LambdaQueryWrapper<AppWorkflow>()
                .orderByDesc(AppWorkflow::getId))
            .stream().map(this::toVO).toList();
    }
    @Override public WorkflowVO getById(Long id){
        var w = workflowMapper.selectById(id);
        return w == null ? null : toVO(w);
    }
    @Override @Transactional public Long create(WorkflowSaveRequest r){
        var w = new AppWorkflow();
        w.setWorkflowCode(r.getWorkflowCode()); w.setWorkflowName(r.getWorkflowName());
        w.setTargetType(r.getTargetType()); w.setDescription(r.getDescription());
        w.setStatus(r.getStatus()!=null?r.getStatus():1);
        workflowMapper.insert(w);
        saveNodes(w.getId(), r);
        return w.getId();
    }
    @Override @Transactional public void update(Long id, WorkflowSaveRequest r){
        var w = workflowMapper.selectById(id);
        if(w==null) throw new BusinessException(R.CODE_NOT_FOUND,"工作流不存在");
        w.setWorkflowCode(r.getWorkflowCode()); w.setWorkflowName(r.getWorkflowName());
        w.setTargetType(r.getTargetType()); w.setDescription(r.getDescription());
        if(r.getStatus()!=null) w.setStatus(r.getStatus());
        workflowMapper.updateById(w);
        // 节点整体覆盖：先删后插
        nodeMapper.delete(new LambdaQueryWrapper<AppWorkflowNode>().eq(AppWorkflowNode::getWorkflowId, id));
        saveNodes(id, r);
    }
    @Override @Transactional public void delete(Long id){
        workflowMapper.deleteById(id);
        nodeMapper.delete(new LambdaQueryWrapper<AppWorkflowNode>().eq(AppWorkflowNode::getWorkflowId, id));
    }
    private void saveNodes(Long workflowId, WorkflowSaveRequest r){
        if(r.getNodes()==null) return;
        int order = 1;
        for(var nr : r.getNodes()){
            var n = new AppWorkflowNode();
            n.setWorkflowId(workflowId);
            n.setNodeOrder(nr.getNodeOrder()!=null?nr.getNodeOrder():order);
            n.setNodeName(nr.getNodeName()); n.setApproverRole(nr.getApproverRole());
            n.setMinAmount(nr.getMinAmount()); n.setMaxAmount(nr.getMaxAmount());
            nodeMapper.insert(n);
            order++;
        }
    }
}
