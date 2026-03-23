package com.clawreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_user_menu_permission")
public class UserMenuPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long permissionId;
    private LocalDateTime createTime;
}
