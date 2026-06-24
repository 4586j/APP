package com.erp.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * 订单状态机 — 销售订单 / 采购订单通用。
 *
 * <p>状态流转（仅声明合法迁移，校验由 Service 层做）：
 * <pre>
 *   DRAFT ──┐
 *           ├──▶ PENDING_APPROVAL ──▶ APPROVED ──▶ IN_PRODUCTION ──▶ READY_TO_SHIP
 *           │                                                            │
 *           ▼                                                            ▼
 *       CANCELLED ◀──────────────────────────────────────────────── SHIPPED ──▶ DELIVERED ──▶ COMPLETED
 *                                                                                                │
 *                                                                                                ▼
 *                                                                                            CLOSED
 * </pre>
 *
 * <p>code 用 Integer 是因为状态机会做范围查询 / 排序（例如"未完结订单 = code < 80"），
 * 与 {@link Department} / {@link Currency} 不同。
 */
@Getter
@AllArgsConstructor
public enum OrderStatus implements IEnum<Integer> {

    DRAFT             (10,  "草稿",     "Draft"),
    PENDING_APPROVAL  (20,  "待审批",   "Pending Approval"),
    APPROVED          (30,  "已审批",   "Approved"),
    IN_PRODUCTION     (40,  "生产中",   "In Production"),
    READY_TO_SHIP     (50,  "待发货",   "Ready to Ship"),
    SHIPPED           (60,  "已发货",   "Shipped"),
    DELIVERED         (70,  "已送达",   "Delivered"),
    COMPLETED         (80,  "已完成",   "Completed"),
    CLOSED            (90,  "已结案",   "Closed"),
    CANCELLED         (99,  "已取消",   "Cancelled");

    private final Integer code;
    private final String  desc;
    private final String  descEn;

    /** 终态集合（不可再变迁） */
    private static final Set<OrderStatus> TERMINAL = EnumSet.of(COMPLETED, CLOSED, CANCELLED);

    @Override
    @JsonValue
    public Integer getValue() {
        return code;
    }

    /** 按 code 安全查找 */
    public static OrderStatus fromCode(Integer code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /** 是否终态 */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** 是否未完结（用于过滤"进行中订单"） */
    public boolean isActive() {
        return !isTerminal();
    }

    /**
     * 是否允许从当前状态迁移到目标状态。
     *
     * <p>规则：
     * <ul>
     *   <li>终态不可再变迁</li>
     *   <li>任意非终态都可跳 CANCELLED</li>
     *   <li>其他迁移必须 code 严格递增（向前流转）</li>
     * </ul>
     */
    public boolean canTransitionTo(OrderStatus next) {
        if (next == null) return false;
        if (this == next) return false;
        if (this.isTerminal()) return false;
        if (next == CANCELLED) return true;
        return next.code > this.code;
    }
}
