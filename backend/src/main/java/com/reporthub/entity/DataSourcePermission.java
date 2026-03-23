package com.reporthub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_datasource_permission")
public class DataSourcePermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dataSourceId;
    private Long userId;
    private LocalDateTime createTime;
}
