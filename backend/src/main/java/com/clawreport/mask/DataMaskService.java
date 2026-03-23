package com.clawreport.mask;

import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据脱敏服务 - 全遮蔽模式
 * 所有配置的字段值完全遮蔽为 ****
 */
@Service
public class DataMaskService {

    private static final String MASK_VALUE = "****";

    // 手机号全遮蔽
    public String maskPhone(Object value) {
        if (value == null) return null;
        return MASK_VALUE;
    }

    // 身份证全遮蔽
    public String maskIdCard(Object value) {
        if (value == null) return null;
        return MASK_VALUE;
    }

    // 邮箱全遮蔽
    public String maskEmail(Object value) {
        if (value == null) return null;
        return MASK_VALUE;
    }

    // 金额全遮蔽
    public String maskAmount(Object value) {
        if (value == null) return null;
        return MASK_VALUE;
    }

    // 银行卡全遮蔽
    public String maskBankCard(Object value) {
        if (value == null) return null;
        return MASK_VALUE;
    }

    // 自定义正则脱敏（全遮蔽）
    public String maskByPattern(Object value, String pattern, String replacement) {
        if (value == null) return null;
        String str = value.toString();
        if (pattern == null || pattern.isEmpty()) {
            return MASK_VALUE;
        }
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(str);
            // 匹配到就全遮蔽
            if (m.find()) {
                return MASK_VALUE;
            }
            return str;
        } catch (Exception e) {
            return MASK_VALUE;
        }
    }

    // 根据脱敏类型执行脱敏 - 全遮蔽
    public String mask(String type, Object value) {
        if (value == null) return null;
        if (type == null || type.isEmpty()) return value.toString();
        
        switch (type.toUpperCase()) {
            case "PHONE":
                return maskPhone(value);
            case "ID_CARD":
                return maskIdCard(value);
            case "EMAIL":
                return maskEmail(value);
            case "AMOUNT":
                return maskAmount(value);
            case "BANK_CARD":
                return maskBankCard(value);
            case "CUSTOM":
                return MASK_VALUE;
            default:
                return value.toString();
        }
    }

    /**
     * 判断字段是否匹配脱敏类型（用于智能识别）
     */
    public boolean isMatch(String columnName, String maskType) {
        if (columnName == null) return false;
        String lowerName = columnName.toLowerCase();
        
        switch (maskType.toUpperCase()) {
            case "PHONE":
                return lowerName.contains("phone") || lowerName.contains("mobile") || lowerName.contains("tel");
            case "ID_CARD":
                return lowerName.contains("idcard") || lowerName.contains("id_card") || lowerName.contains("identity") || lowerName.contains("身份证");
            case "EMAIL":
                return lowerName.contains("email") || lowerName.contains("mail");
            case "AMOUNT":
                return lowerName.contains("amount") || lowerName.contains("money") || lowerName.contains("salary") || lowerName.contains("金额");
            case "BANK_CARD":
                return lowerName.contains("bank") || lowerName.contains("card") || lowerName.contains("账号");
            default:
                return false;
        }
    }
}
