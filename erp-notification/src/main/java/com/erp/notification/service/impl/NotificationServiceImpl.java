package com.erp.notification.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.exception.BusinessException;
import com.erp.notification.dto.*;
import com.erp.notification.entity.NtfNotification;
import com.erp.notification.entity.NtfUserNotification;
import com.erp.notification.mapper.NtfNotificationMapper;
import com.erp.notification.mapper.NtfUserNotificationMapper;
import com.erp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    final NtfNotificationMapper notificationMapper;
    final NtfUserNotificationMapper userNotificationMapper;

    @Override @Transactional public Long create(NotificationCreateRequest r){
        var n=new NtfNotification();
        n.setTitle(r.getTitle()); n.setContent(r.getContent()); n.setType(r.getType());
        n.setSourceType(r.getSourceType()); n.setSourceId(r.getSourceId());
        notificationMapper.insert(n);
        for(Long uid : r.getReceiverIds()){
            if(uid==null) continue;
            var un=new NtfUserNotification();
            un.setNotificationId(n.getId()); un.setUserId(uid); un.setIsRead(0);
            userNotificationMapper.insert(un);
        }
        return n.getId();
    }

    @Override @Transactional public Long send(String title, String content, String type, String sourceType, Long sourceId, Long receiverId){
        var req=new NotificationCreateRequest();
        req.setTitle(title); req.setContent(content); req.setType(type);
        req.setSourceType(sourceType); req.setSourceId(sourceId);
        req.setReceiverIds(List.of(receiverId));
        return create(req);
    }

    @Override public NotificationPageVO myNotifications(Long userId, NotificationQuery q){
        if(q.getPage()==null||q.getPage()<1)q.setPage(1); if(q.getSize()==null||q.getSize()<1)q.setSize(20);
        var w=new LambdaQueryWrapper<NtfUserNotification>().eq(NtfUserNotification::getUserId,userId);
        if(q.getIsRead()!=null) w.eq(NtfUserNotification::getIsRead,q.getIsRead());
        w.orderByDesc(NtfUserNotification::getCreatedAt);
        var p=userNotificationMapper.selectPage(new Page<>(q.getPage(),Math.min(q.getSize(),100)),w);
        var records=p.getRecords().stream().map(un->{
            var n=notificationMapper.selectById(un.getNotificationId());
            return NotificationVO.builder()
                .id(un.getId()).notificationId(un.getNotificationId())
                .title(n==null?null:n.getTitle()).content(n==null?null:n.getContent())
                .type(n==null?null:n.getType()).sourceType(n==null?null:n.getSourceType())
                .sourceId(n==null?null:n.getSourceId())
                .isRead(un.getIsRead()).readAt(un.getReadAt()).createdAt(un.getCreatedAt()).build();
        }).filter(vo-> q.getType()==null || q.getType().equals(vo.getType())).toList();
        return NotificationPageVO.builder().records(records).total(p.getTotal()).size(p.getSize()).current(p.getCurrent()).build();
    }

    @Override @Transactional public void markRead(Long userNotificationId, Long userId){
        var un=userNotificationMapper.selectById(userNotificationId);
        if(un==null) throw new BusinessException(404,"通知不存在: "+userNotificationId);
        if(!userId.equals(un.getUserId())) throw new BusinessException(403,"无权操作他人通知");
        if(un.getIsRead()!=null && un.getIsRead()==1) return;
        un.setIsRead(1); un.setReadAt(LocalDateTime.now());
        userNotificationMapper.updateById(un);
    }

    @Override @Transactional public int batchRead(List<Long> userNotificationIds, Long userId){
        var uw=new LambdaUpdateWrapper<NtfUserNotification>()
            .eq(NtfUserNotification::getUserId,userId)
            .eq(NtfUserNotification::getIsRead,0)
            .set(NtfUserNotification::getIsRead,1)
            .set(NtfUserNotification::getReadAt,LocalDateTime.now());
        if(userNotificationIds!=null && !userNotificationIds.isEmpty()){
            uw.in(NtfUserNotification::getId,userNotificationIds);
        }
        return userNotificationMapper.update(null,uw);
    }

    @Override public long unreadCount(Long userId){
        var w=new LambdaQueryWrapper<NtfUserNotification>()
            .eq(NtfUserNotification::getUserId,userId)
            .eq(NtfUserNotification::getIsRead,0);
        return userNotificationMapper.selectCount(w);
    }
}
