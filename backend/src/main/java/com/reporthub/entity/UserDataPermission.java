package com.reporthub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_user_data_permission")
public class UserDataPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long permissionId;
    private LocalDateTime createTime;
}
