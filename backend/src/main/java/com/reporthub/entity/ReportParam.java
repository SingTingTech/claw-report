package com.reporthub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_report_param")
public class ReportParam {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private String paramName;
    private String paramLabel;
    private String paramType;     // TEXT, SELECT, DATE, DATETIME, NUMBER
    private String defaultValue;
    private String optionsSql;    // 选项SQL
    private Integer required;
    private LocalDateTime createTime;
}
