package com.reporthub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_query_history")
public class QueryHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long dataSourceId;
    private String sqlContent;
    private String params;        // JSON
    private Integer executeTime;
    private Integer status;       // 0-失败 1-成功
    private String errorMessage;
    private LocalDateTime createTime;
}
