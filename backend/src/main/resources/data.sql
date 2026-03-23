-- ================================================
-- ClawReport V2: 初始化数据
-- 包含超级管理员、角色、菜单权限、测试数据源和报表
-- ================================================

USE reporthub;

-- 创建超级管理员用户 (密码: admin123)
-- BCrypt hash for 'admin123': $2a$10$uSOE/qomCgwmurjFcViZPuaoCG8OTdafP9Wcvf3B6N2w6nGjGJ9Ae
INSERT INTO rh_user (username, password, real_name, email, status, deleted) VALUES
('admin', '$2a$10$uSOE/qomCgwmurjFcViZPuaoCG8OTdafP9Wcvf3B6N2w6nGjGJ9Ae', '系统管理员', 'admin@clawreport.com', 1, 0);

-- 创建普通用户 (密码: user123)
-- BCrypt hash for 'user123': $2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36ZfPIoQ0HmHGPmNzV.KqYi
INSERT INTO rh_user (username, password, real_name, email, status, deleted) VALUES
('testuser', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36ZfPIoQ0HmHGPmNzV.KqYi', '测试用户', 'test@clawreport.com', 1, 0);

-- 创建角色
INSERT INTO rh_role (role_name, role_code, description) VALUES
('超级管理员', 'ADMIN', '拥有系统所有权限'),
('数据分析师', 'ANALYST', '可查询和分析报表'),
('普通用户', 'USER', '基础查看权限');

-- 管理员分配 ADMIN 角色
INSERT INTO rh_user_role (user_id, role_id)
SELECT id, (SELECT id FROM rh_role WHERE role_code = 'ADMIN') FROM rh_user WHERE username = 'admin';

-- 测试用户分配 ANALYST 角色
INSERT INTO rh_user_role (user_id, role_id)
SELECT id, (SELECT id FROM rh_role WHERE role_code = 'ANALYST') FROM rh_user WHERE username = 'testuser';

-- 创建菜单权限
INSERT INTO rh_menu_permission (menu_name, menu_code, parent_id, menu_type, path, icon, sort_order) VALUES
-- 一级菜单
('系统管理', 'system', 0, 'menu', '/system', 'setting', 100),
('用户管理', 'menu:user', 0, 'menu', '/users', 'user', 10),
('角色管理', 'menu:role', 0, 'menu', '/roles', 'team', 20),
('分组管理', 'menu:group', 0, 'menu', '/groups', 'folder', 30),
('数据源管理', 'menu:datasource', 0, 'menu', '/datasources', 'database', 40),
('数据脱敏', 'menu:mask', 0, 'menu', '/masks', 'lock', 50),
('报表管理', 'menu:report', 0, 'menu', '/reports', 'file-text', 60),
('数据查询', 'menu:query', 0, 'menu', '/query', 'search', 70),
('数据字典', 'menu:dictionary', 0, 'menu', '/dictionary', 'book', 80),
-- 用户管理子菜单
('用户查看', 'menu:user:view', (SELECT id FROM rh_menu_permission WHERE menu_code='menu:user'), 'button', '', '', 1),
('用户编辑', 'menu:user:edit', (SELECT id FROM rh_menu_permission WHERE menu_code='menu:user'), 'button', '', '', 2),
-- 角色管理子菜单
('角色查看', 'menu:role:view', (SELECT id FROM rh_menu_permission WHERE menu_code='menu:role'), 'button', '', '', 1),
('角色编辑', 'menu:role:edit', (SELECT id FROM rh_menu_permission WHERE menu_code='menu:role'), 'button', '', '', 2),
-- 数据源管理子菜单
('数据源查看', 'menu:datasource:view', (SELECT id FROM rh_menu_permission WHERE menu_code='menu:datasource'), 'button', '', '', 1),
('数据源编辑', 'menu:datasource:edit', (SELECT id FROM rh_menu_permission WHERE menu_code='menu:datasource'), 'button', '', '', 2),
-- 报表管理子菜单
('报表查看', 'menu:report:view', (SELECT id FROM rh_menu_permission WHERE menu_code='menu:report'), 'button', '', '', 1),
('报表编辑', 'menu:report:edit', (SELECT id FROM rh_menu_permission WHERE menu_code='menu:report'), 'button', '', '', 2),
-- 查询子菜单
('查询执行', 'menu:query:execute', (SELECT id FROM rh_menu_permission WHERE menu_code='menu:query'), 'button', '', '', 1),
-- 脱敏子菜单
('脱敏编辑', 'menu:mask:edit', (SELECT id FROM rh_menu_permission WHERE menu_code='menu:mask'), 'button', '', '', 1),
-- 分组子菜单
('分组编辑', 'menu:group:edit', (SELECT id FROM rh_menu_permission WHERE menu_code='menu:group'), 'button', '', '', 1);

-- 给 ADMIN 角色分配所有菜单权限
INSERT INTO rh_role_menu_permission (role_id, permission_id)
SELECT (SELECT id FROM rh_role WHERE role_code = 'ADMIN'), id FROM rh_menu_permission;

-- 给 ANALYST 角色分配部分菜单权限
INSERT INTO rh_role_menu_permission (role_id, permission_id)
SELECT (SELECT id FROM rh_role WHERE role_code = 'ANALYST'), id FROM rh_menu_permission
WHERE menu_code IN ('menu:report', 'menu:report:view', 'menu:query', 'menu:query:execute', 'menu:datasource', 'menu:datasource:view', 'menu:dictionary', 'menu:dictionary:view');

-- 创建测试数据源 (MySQL)
INSERT INTO rh_data_source (ds_name, ds_type, host, port, database_name, username, password, charset, create_by) VALUES
('ClawReport本地MySQL', 'mysql', 'mysql', 3306, 'reporthub', 'clawreport', 'ReportHub@2026', 'utf8mb4', 1),
('ClawReport本地MySQL_root', 'mysql', 'mysql', 3306, 'reporthub', 'root', 'Root@2026', 'utf8mb4', 1);

-- 创建报表分组
INSERT INTO rh_report_group (group_name, parent_id, sort_order) VALUES
('销售报表', 0, 10),
('运营报表', 0, 20),
('财务报表', 0, 30),
('销售-华东区', (SELECT id FROM rh_report_group WHERE group_name='销售报表'), 1),
('销售-华北区', (SELECT id FROM rh_report_group WHERE group_name='销售报表'), 2);

-- 创建报表分类
INSERT INTO rh_report_category (category_name, category_code, icon, sort_order) VALUES
('数据表格', 'table', 'table', 10),
('折线图', 'line', 'line-chart', 20),
('柱状图', 'bar', 'bar-chart', 30),
('饼图', 'pie', 'pie-chart', 40),
('仪表盘', 'dashboard', 'dashboard', 50);

-- 创建测试报表
INSERT INTO rh_report (report_name, report_code, category_id, group_id, ds_id, query_sql, chart_type, config, create_by, status) VALUES
-- 销售汇总表
('销售汇总表', 'sales_summary', 
 (SELECT id FROM rh_report_category WHERE category_code='table'),
 (SELECT id FROM rh_report_group WHERE group_name='销售报表'),
 1,
 'SELECT DATE_FORMAT(create_time, ''%Y-%m'') as month, COUNT(*) as order_count, SUM(1000 + (RAND()*5000)) as amount FROM rh_user GROUP BY month ORDER BY month',
 'table', 
 '{"columns":[{"key":"month","label":"月份"},{"key":"order_count","label":"订单数"},{"key":"amount","label":"销售额"}]}',
 1, 1),

-- 月度销售趋势
('月度销售趋势', 'monthly_sales_trend',
 (SELECT id FROM rh_report_category WHERE category_code='line'),
 (SELECT id FROM rh_report_group WHERE group_name='销售报表'),
 1,
 'SELECT DATE_FORMAT(create_time, ''%Y-%m'') as month, COUNT(*) as user_count FROM rh_user GROUP BY month ORDER BY month',
 'chart',
 '{"xAxis":"month","yAxis":"user_count","seriesName":"用户数"}',
 1, 1),

-- 用户分布统计
('用户分布统计', 'user_distribution',
 (SELECT id FROM rh_report_category WHERE category_code='pie'),
 (SELECT id FROM rh_report_group WHERE group_name='销售报表'),
 1,
 'SELECT status as name, COUNT(*) as value FROM rh_user GROUP BY status',
 'chart',
 '{"seriesName":"用户数"}',
 1, 1),

-- 数据源监控
('数据源连接监控', 'ds_monitor',
 (SELECT id FROM rh_report_category WHERE category_code='dashboard'),
 (SELECT id FROM rh_report_group WHERE group_name='运营报表'),
 1,
 'SELECT ds_name, ds_type, host, port, database_name, create_time FROM rh_data_source ORDER BY create_time DESC LIMIT 20',
 'table',
 '{"columns":[{"key":"ds_name","label":"数据源名称"},{"key":"ds_type","label":"类型"},{"key":"host","label":"主机"},{"key":"port","label":"端口"},{"key":"database_name","label":"数据库"},{"key":"create_time","label":"创建时间"}]}',
 1, 1),

-- 角色权限报表
('角色权限矩阵', 'role_permission_matrix',
 (SELECT id FROM rh_report_category WHERE category_code='table'),
 (SELECT id FROM rh_report_group WHERE group_name='系统管理'),
 1,
 'SELECT r.role_name, GROUP_CONCAT(m.menu_code) as permissions FROM rh_role r LEFT JOIN rh_role_menu_permission rmp ON r.id = rmp.role_id LEFT JOIN rh_menu_permission m ON rmp.permission_id = m.id GROUP BY r.id, r.role_name',
 'table',
 '{"columns":[{"key":"role_name","label":"角色"},{"key":"permissions","label":"权限列表"}]}',
 1, 1);

-- 给报表添加参数
INSERT INTO rh_report_param (report_id, param_name, param_label, param_type, param_default, required) VALUES
((SELECT id FROM rh_report WHERE report_code='sales_summary'), 'start_date', '开始日期', 'date', '2024-01-01', 0),
((SELECT id FROM rh_report WHERE report_code='sales_summary'), 'end_date', '结束日期', 'date', '2024-12-31', 0),
((SELECT id FROM rh_report WHERE report_code='monthly_sales_trend'), 'months', '月份数', 'select', '12', 0);

-- 创建测试数据脱敏规则
INSERT INTO rh_data_source_mask (ds_id, field_name, mask_type, mask_rule) VALUES
(1, 'password', 'encrypt', 'AES'),
(1, 'email', 'mask', '****');

-- 创建数据字典表（用于存储数据源的表/列元数据，如字段注释、枚举配置）
CREATE TABLE IF NOT EXISTS rh_data_dictionary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    column_name VARCHAR(100) NOT NULL COMMENT '列名',
    table_comment VARCHAR(500) COMMENT '表注释',
    column_comment VARCHAR(500) COMMENT '列注释',
    column_type VARCHAR(100) COMMENT '列类型',
    nullable VARCHAR(10) COMMENT '是否可空',
    default_value VARCHAR(200) COMMENT '默认值',
    is_primary_key TINYINT DEFAULT 0 COMMENT '是否主键',
    saved_comment VARCHAR(500) COMMENT '用户保存的备注',
    enum_config TEXT COMMENT '枚举配置，格式：1=正常,0=禁用',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ds_table (data_source_id, table_name),
    INDEX idx_ds_table_column (data_source_id, table_name, column_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- 清理测试查询历史
DELETE FROM rh_query_history WHERE 1=1;
