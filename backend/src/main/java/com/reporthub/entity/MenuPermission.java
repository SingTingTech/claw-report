package com.reporthub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_menu_permission")
public class MenuPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String menuPath;
    private String description;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
