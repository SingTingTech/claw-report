package com.clawreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_report_share")
public class ReportShare {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private String shareToken;
    private LocalDateTime expiresAt;
    private Long createdBy;
    private LocalDateTime createTime;
}
