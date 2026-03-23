package com.clawreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_report_group")
public class ReportGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long parentId;     // 0表示顶级
    private Integer sortOrder;
    private String description;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
