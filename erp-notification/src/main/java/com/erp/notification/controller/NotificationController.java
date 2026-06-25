package com.erp.notification.controller;
import com.erp.common.model.R;
import com.erp.notification.dto.*;
import com.erp.notification.service.NotificationService;
import com.erp.security.annotation.CurrentUser;
import com.erp.security.user.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/v1/notifications") @RequiredArgsConstructor
public class NotificationController {
    final NotificationService service;

    private Long uid(LoginUser u){ return u!=null && u.getId()!=null ? u.getId() : 1L; }

    /** 我的通知列表 */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('notification:view')")
    public R<NotificationPageVO> my(@CurrentUser LoginUser user, NotificationQuery q){
        return R.ok(service.myNotifications(uid(user), q));
    }

    /** 我的未读数量 */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('notification:view')")
    public R<Long> unreadCount(@CurrentUser LoginUser user){
        return R.ok(service.unreadCount(uid(user)));
    }

    /** 标记单条已读 */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('notification:read')")
    public R<Void> read(@CurrentUser LoginUser user, @PathVariable Long id){
        service.markRead(id, uid(user)); return R.ok();
    }

    /** 批量标记已读（body.ids 为空=全部已读），返回更新条数 */
    @PutMapping("/batch-read")
    @PreAuthorize("hasAuthority('notification:read')")
    public R<Integer> batchRead(@CurrentUser LoginUser user, @RequestBody(required=false) Map<String,List<Long>> body){
        List<Long> ids = body==null ? null : body.get("ids");
        return R.ok(service.batchRead(ids, uid(user)));
    }

    /** 创建通知 */
    @PostMapping
    @PreAuthorize("hasAuthority('notification:create')")
    public R<Long> create(@Valid @RequestBody NotificationCreateRequest r){
        return R.ok(service.create(r));
    }
}
