package com.reporthub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_data_permission")
public class DataPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String permissionType;  // DATASOURCE, REPORT_GROUP
    private Long targetId;           // 数据源ID或分组ID
    private String description;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
