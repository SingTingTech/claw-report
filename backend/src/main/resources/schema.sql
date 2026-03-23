-- ================================================
-- ClawReport V1: 初始化建表脚本
-- ================================================

-- 用户表
CREATE TABLE IF NOT EXISTS rh_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志 0-未删 1-已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS rh_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色代码',
    description VARCHAR(200) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS rh_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 菜单权限表
CREATE TABLE IF NOT EXISTS rh_menu_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    menu_code VARCHAR(50) NOT NULL UNIQUE COMMENT '菜单代码',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    menu_type VARCHAR(10) DEFAULT 'menu' COMMENT '类型 menu/button',
    path VARCHAR(200) COMMENT '路径',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- 角色菜单权限关联表
CREATE TABLE IF NOT EXISTS rh_role_menu_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单权限关联表';

-- 数据源表
CREATE TABLE IF NOT EXISTS rh_data_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ds_name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    ds_type VARCHAR(20) NOT NULL COMMENT '类型 mysql/postgresql',
    host VARCHAR(200) NOT NULL COMMENT '主机',
    port INT DEFAULT 3306 COMMENT '端口',
    database_name VARCHAR(100) NOT NULL COMMENT '数据库名',
    username VARCHAR(50) COMMENT '用户名',
    password VARCHAR(200) COMMENT '密码(加密)',
    charset VARCHAR(20) DEFAULT 'utf8mb4',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源表';

-- 数据权限表
CREATE TABLE IF NOT EXISTS rh_data_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限代码',
    ds_id BIGINT COMMENT '关联数据源',
    filter_sql TEXT COMMENT '过滤SQL条件',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据权限表';

-- 角色数据权限关联表
CREATE TABLE IF NOT EXISTS rh_role_data_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色数据权限关联表';

-- 用户数据权限关联表
CREATE TABLE IF NOT EXISTS rh_user_data_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户数据权限关联表';

-- 报表分组表
CREATE TABLE IF NOT EXISTS rh_report_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(100) NOT NULL COMMENT '分组名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分组ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表分组表';

-- 报表分类表
CREATE TABLE IF NOT EXISTS rh_report_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    category_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类代码',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表分类表';

-- 报表表
CREATE TABLE IF NOT EXISTS rh_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_name VARCHAR(200) NOT NULL COMMENT '报表名称',
    report_code VARCHAR(100) NOT NULL UNIQUE COMMENT '报表代码',
    category_id BIGINT COMMENT '分类ID',
    group_id BIGINT COMMENT '分组ID',
    ds_id BIGINT COMMENT '数据源ID',
    query_sql TEXT COMMENT '查询SQL',
    chart_type VARCHAR(20) DEFAULT 'table' COMMENT '图表类型 table/chart',
    config JSON COMMENT '图表配置',
    params JSON COMMENT '参数配置',
    create_by BIGINT COMMENT '创建人',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表表';

-- 报表参数表
CREATE TABLE IF NOT EXISTS rh_report_param (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    param_name VARCHAR(50) NOT NULL COMMENT '参数名',
    param_label VARCHAR(50) COMMENT '参数标签',
    param_type VARCHAR(20) DEFAULT 'text' COMMENT '类型 text/select/date',
    param_default VARCHAR(200) COMMENT '默认值',
    param_options TEXT COMMENT '选项 JSON',
    required TINYINT DEFAULT 0 COMMENT '是否必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表参数表';

-- 报表分享表
CREATE TABLE IF NOT EXISTS rh_report_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    share_token VARCHAR(50) NOT NULL UNIQUE COMMENT '分享Token',
    expires_at DATETIME COMMENT '过期时间',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表分享表';

-- 用户菜单权限关联表
CREATE TABLE IF NOT EXISTS rh_user_menu_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户菜单权限关联表';

-- 用户分组关联表
CREATE TABLE IF NOT EXISTS rh_user_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户分组关联表';

-- 数据源脱敏规则表
CREATE TABLE IF NOT EXISTS rh_data_source_mask (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ds_id BIGINT NOT NULL COMMENT '数据源ID',
    field_name VARCHAR(50) NOT NULL COMMENT '字段名',
    mask_type VARCHAR(20) DEFAULT 'mask' COMMENT '脱敏类型 mask/encrypt/hide',
    mask_rule VARCHAR(100) COMMENT '脱敏规则',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源脱敏规则表';

-- 数据源权限关联表
CREATE TABLE IF NOT EXISTS rh_data_source_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ds_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源权限关联表';

-- 查询历史表
CREATE TABLE IF NOT EXISTS rh_query_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    report_id BIGINT,
    query_sql TEXT COMMENT '执行的SQL',
    params JSON COMMENT '查询参数',
    execute_time INT COMMENT '执行时长(ms)',
    status TINYINT COMMENT '状态 0-失败 1-成功',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查询历史表';
