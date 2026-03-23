package com.clawreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_data_source_mask")
public class DataSourceMask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dataSourceId;
    private String tableName;
    private String columnName;
    private String maskType;      // PHONE, ID_CARD, EMAIL, AMOUNT, BANK_CARD, CUSTOM
    private String maskPattern;   // 自定义正则
    private String maskReplacement;
    private Integer enabled;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
