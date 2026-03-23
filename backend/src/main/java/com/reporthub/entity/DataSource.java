package com.reporthub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_data_source")
public class DataSource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String dsType;        // MYSQL, POSTGRESQL
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String password;      // 加密存储
    private String extraParams;   // JSON
    private Integer status;
    @TableLogic
    private Integer deleted;
    private Long createUserId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
