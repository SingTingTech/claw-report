-- ReportHub 内置数据库初始化脚本
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS reporthub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE reporthub;
SET NAMES utf8mb4;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(Bcrypt)',
    real_name VARCHAR(100) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 2. 角色表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色代码',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) COMMENT '描述',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 预置角色
INSERT INTO rh_role (role_code, role_name, description) VALUES
('ADMIN', '系统管理员', '拥有所有权限'),
('DATASOURCE_ADMIN', '数据源管理员', '管理数据源和SQL编写'),
('REPORTER', '报表开发者', '编写SQL和管理报表'),
('VIEWER', '普通查看者', '仅可查看报表');

-- ----------------------------
-- 3. 用户角色关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- ----------------------------
-- 4. 菜单权限定义表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_menu_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限代码',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    menu_path VARCHAR(255) COMMENT '菜单路径',
    description VARCHAR(255) COMMENT '描述',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限定义表';

-- 预置菜单权限
INSERT INTO rh_menu_permission (permission_code, permission_name, menu_path, description) VALUES
('menu:user:view', '用户查看', '/users', '查看用户列表'),
('menu:user:edit', '用户编辑', '/users', '创建/编辑/删除用户'),
('menu:role:view', '角色查看', '/roles', '查看角色列表'),
('menu:role:edit', '角色编辑', '/roles', '创建/编辑/删除角色'),
('menu:datasource:view', '数据源查看', '/datasources', '查看数据源列表'),
('menu:datasource:edit', '数据源编辑', '/datasources', '创建/编辑/删除数据源'),
('menu:query:execute', 'SQL执行', '/query', '执行SQL查询'),
('menu:report:view', '报表查看', '/reports', '查看报表'),
('menu:report:edit', '报表编辑', '/reports', '创建/编辑/删除报表'),
('menu:mask:edit', '脱敏规则管理', '/masks', '管理脱敏规则'),
('menu:group:edit', '分组管理', '/groups', '管理报表分组');

-- ----------------------------
-- 5. 数据权限定义表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_data_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限代码',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    permission_type VARCHAR(50) NOT NULL COMMENT '权限类型: DATASOURCE, REPORT_GROUP',
    target_id BIGINT COMMENT '目标ID(数据源ID或分组ID)',
    description VARCHAR(255) COMMENT '描述',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据权限定义表';

-- ----------------------------
-- 6. 用户直接菜单权限关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_user_menu_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_permission (user_id, permission_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户直接菜单权限关联';

-- ----------------------------
-- 7. 用户直接数据权限关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_user_data_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_permission (user_id, permission_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户直接数据权限关联';

-- ----------------------------
-- 8. 角色菜单权限关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_role_menu_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单权限关联';

-- ----------------------------
-- 9. 角色数据权限关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_role_data_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色数据权限关联';

-- ----------------------------
-- 10. 报表分组表（树形结构）
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_report_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '分组名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分组ID，0表示顶级',
    sort_order INT DEFAULT 0 COMMENT '排序',
    description VARCHAR(255) COMMENT '描述',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表分组表';

-- 默认分组
INSERT INTO rh_report_group (name, parent_id, sort_order, description) VALUES
('经营分析', 0, 1, '经营分析类报表'),
('销售报表', 0, 2, '销售数据报表'),
('财务统计', 0, 3, '财务报表统计'),
('系统监控', 0, 4, '系统运维监控');

-- ----------------------------
-- 11. 用户-分组关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_user_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_group (user_id, group_id),
    INDEX idx_user_id (user_id),
    INDEX idx_group_id (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户分组关联表';

-- ----------------------------
-- 12. 数据源配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_data_source (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    description VARCHAR(255) COMMENT '描述',
    ds_type VARCHAR(50) NOT NULL COMMENT '类型: MYSQL, POSTGRESQL',
    host VARCHAR(255) NOT NULL COMMENT '主机',
    port INT NOT NULL COMMENT '端口',
    database_name VARCHAR(100) NOT NULL COMMENT '数据库名',
    username VARCHAR(100) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    extra_params VARCHAR(500) COMMENT '额外参数(JSON)',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    deleted TINYINT DEFAULT 0,
    create_user_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_create_user (create_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';

-- ----------------------------
-- 13. 数据源脱敏规则表（表+字段级全遮蔽）
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_data_source_mask (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    column_name VARCHAR(100) NOT NULL COMMENT '列名（字段名）',
    mask_type VARCHAR(50) NOT NULL COMMENT '脱敏类型: PHONE, ID_CARD, EMAIL, AMOUNT, BANK_CARD, CUSTOM',
    mask_pattern VARCHAR(255) COMMENT '自定义正则(当类型为CUSTOM时)',
    mask_replacement VARCHAR(100) DEFAULT '****' COMMENT '替换字符，默认全遮蔽****',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_datasource (data_source_id),
    UNIQUE KEY uk_table_column (data_source_id, table_name, column_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源脱敏规则表';

-- ----------------------------
-- 14. 报表配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '报表名称',
    description VARCHAR(500) COMMENT '描述',
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    sql_content TEXT NOT NULL COMMENT 'SQL查询语句',
    group_id BIGINT COMMENT '所属分组ID',
    params_config TEXT COMMENT '参数配置(JSON)',
    chart_type VARCHAR(50) COMMENT '图表类型: TABLE, LINE, BAR, PIE',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    deleted TINYINT DEFAULT 0,
    create_user_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_datasource (data_source_id),
    INDEX idx_group (group_id),
    INDEX idx_create_user (create_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表配置表';

-- ----------------------------
-- 15. 报表参数表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_report_param (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL COMMENT '报表ID',
    param_name VARCHAR(100) NOT NULL COMMENT '参数名',
    param_label VARCHAR(100) COMMENT '参数标签',
    param_type VARCHAR(50) DEFAULT 'TEXT' COMMENT '类型: TEXT, SELECT, DATE, DATETIME, NUMBER',
    default_value VARCHAR(255) COMMENT '默认值',
    options_sql TEXT COMMENT '选项SQL(用于SELECT类型)',
    required TINYINT DEFAULT 0 COMMENT '是否必填',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_report (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表参数表';

-- ----------------------------
-- 16. 查询历史表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_query_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    sql_content TEXT NOT NULL COMMENT 'SQL语句',
    params TEXT COMMENT '参数JSON',
    execute_time INT COMMENT '执行耗时(ms)',
    status TINYINT COMMENT '状态: 0-失败 1-成功',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_datasource (data_source_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查询历史表';

-- ----------------------------
-- 17. 报表分享表
-- ----------------------------
CREATE TABLE IF NOT EXISTS rh_report_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL COMMENT '报表ID',
    share_token VARCHAR(100) NOT NULL UNIQUE COMMENT '分享Token',
    expires_at DATETIME COMMENT '过期时间(NULL表示永不过期)',
    created_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token (share_token),
    INDEX idx_report (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表分享表';

-- ----------------------------
-- 初始化管理员账户 (密码: admin123)
-- ----------------------------
INSERT INTO rh_user (username, password, real_name, email, status) VALUES
('admin', '$2a$10$zHp/eZ6Jr7whwtD4alEfkeFUNiUZJip9deiA2l/Uwe6oi9DJOnXtW', '系统管理员', 'admin@reporthub.com', 1);

-- 管理员分配 ADMIN 角色
INSERT INTO rh_user_role (user_id, role_id) 
SELECT (SELECT id FROM rh_user WHERE username = 'admin'), id 
FROM rh_role WHERE role_code = 'ADMIN';

-- 给 ADMIN 角色分配所有菜单权限
INSERT INTO rh_role_menu_permission (role_id, permission_id)
SELECT r.id, p.id FROM rh_role r, rh_menu_permission p WHERE r.role_code = 'ADMIN';

-- 给 ADMIN 角色分配所有数据权限（预留，后期动态插入）
INSERT INTO rh_role_data_permission (role_id, permission_id)
SELECT r.id, p.id FROM rh_role r, rh_data_permission p WHERE r.role_code = 'ADMIN';

-- ----------------------------
-- 角色默认菜单权限配置
-- ----------------------------
-- DATASOURCE_ADMIN: datasource + query
INSERT INTO rh_role_menu_permission (role_id, permission_id)
SELECT r.id, p.id FROM rh_role r, rh_menu_permission p 
WHERE r.role_code = 'DATASOURCE_ADMIN' 
AND p.permission_code IN ('menu:datasource:view', 'menu:datasource:edit', 'menu:query:execute');

-- REPORTER: query + report + group
INSERT INTO rh_role_menu_permission (role_id, permission_id)
SELECT r.id, p.id FROM rh_role r, rh_menu_permission p 
WHERE r.role_code = 'REPORTER' 
AND p.permission_code IN ('menu:query:execute', 'menu:report:view', 'menu:report:edit', 'menu:group:edit');

-- VIEWER: report view only
INSERT INTO rh_role_menu_permission (role_id, permission_id)
SELECT r.id, p.id FROM rh_role r, rh_menu_permission p 
WHERE r.role_code = 'VIEWER' 
AND p.permission_code IN ('menu:report:view');
