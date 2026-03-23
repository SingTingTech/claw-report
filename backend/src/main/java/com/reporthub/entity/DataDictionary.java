package com.reporthub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_data_dictionary")
public class DataDictionary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dataSourceId;
    private String tableName;
    private String columnName;
    private String tableComment;
    private String columnComment;
    @com.baomidou.mybatisplus.annotation.TableField("column_type")
    private String columnType;
    private String nullable;
    private String defaultValue;
    private Integer isPrimaryKey;
    private String savedComment;  // 用户保存的备注
    private String enumConfig;   // 枚举配置，格式：1=正常,0=禁用
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
