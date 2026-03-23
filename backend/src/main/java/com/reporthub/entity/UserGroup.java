package com.reporthub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rh_user_group")
public class UserGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long groupId;
    private LocalDateTime createTime;
}
