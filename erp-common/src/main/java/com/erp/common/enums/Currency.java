package com.erp.common.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 货币枚举 — 外贸常用 8 种。
 *
 * <p>ISO 4217 三字母代码，便于和银行 / 报关系统对接。
 *
 * <p>对应前端 formatMoney(currency) 的 symbol 映射。
 */
@Getter
@AllArgsConstructor
public enum Currency implements IEnum<String> {

    CNY("CNY", "人民币", "¥",  2),
    USD("USD", "美元",   "$",  2),
    EUR("EUR", "欧元",   "€",  2),
    JPY("JPY", "日元",   "¥",  0),  // 日元无小数
    GBP("GBP", "英镑",   "£",  2),
    HKD("HKD", "港币",   "HK$", 2),
    AUD("AUD", "澳元",   "A$", 2),
    KRW("KRW", "韩元",   "₩",  0);  // 韩元无小数

    /** ISO 4217 代码（同时也是 MP IEnum value） */
    private final String code;

    /** 中文名 */
    private final String desc;

    /** 货币符号 */
    private final String symbol;

    /** 小数位数（标准会计精度，给金额校验用） */
    private final Integer fractionDigits;

    @Override
    @JsonValue
    public String getValue() {
        return code;
    }

    /** 按 code 安全查找 */
    public static Currency fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(c -> c.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
