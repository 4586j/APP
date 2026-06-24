package com.erp.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 部门枚举 — 5 个部门，对应业务白皮书 §1.2。
 *
 * <p>编码用 String 而非 Integer：方便前端展示、日志可读，且部门是稳定集合。
 *
 * <p>对应前端 erp-frontend/src/types/auth.ts 的 Department type。
 */
@Getter
@AllArgsConstructor
public enum Department implements IEnum<String> {

    SALES("SALES", "销售部", "Sales"),
    PURCHASE("PURCHASE", "采购部", "Purchase"),
    FINANCE("FINANCE", "财务部", "Finance"),
    LOGISTICS("LOGISTICS", "物流部", "Logistics"),
    MANAGEMENT("MANAGEMENT", "管理层", "Management");

    /** 数据库存储值（同时也是 MP IEnum value） */
    private final String code;

    /** 中文名 */
    private final String desc;

    /** 英文名（i18n 用） */
    private final String descEn;

    @Override
    @JsonValue
    public String getValue() {
        return code;
    }

    /** 按 code 安全查找，找不到返回 null */
    public static Department fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(d -> d.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
