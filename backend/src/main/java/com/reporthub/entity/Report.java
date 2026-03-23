package com.reporthub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Long dataSourceId;
    private String sqlContent;
    private Long groupId;
    private String paramsConfig;  // JSON
    private String chartType;     // TABLE, LINE, BAR, PIE
    private Integer status;
    @TableLogic
    private Integer deleted;
    private Long createUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
