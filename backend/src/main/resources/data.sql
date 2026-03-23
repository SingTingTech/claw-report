-- ================================================
-- ClawReport V2: 初始化测试数据
-- 在 init.sql 之后执行
-- ================================================

USE reporthub;
SET NAMES utf8mb4;

-- 创建测试用户 (密码: admin123 / user123)
INSERT IGNORE INTO rh_user (username, password, real_name, email, status, deleted) VALUES
('admin', '$2a$10$X6EBUZy4aDjU1lKkSQv6o.K.rsgzeOs.ARkfEgaOUtOkINx/PTEXm', '系统管理员', 'admin@clawreport.com', 1, 0);

INSERT IGNORE INTO rh_user (username, password, real_name, email, status, deleted) VALUES
('testuser', '$2a$10$Xv5Pcs3wgodXEwhF3HfiaOiPiqmkZM8.m7cx44XS2/GE2gQ3jCFvO', '测试用户', 'test@clawreport.com', 1, 0);

-- 分配用户角色
INSERT IGNORE INTO rh_user_role (user_id, role_id)
SELECT id, (SELECT id FROM rh_role WHERE role_code = 'ADMIN') FROM rh_user WHERE username = 'admin';

INSERT IGNORE INTO rh_user_role (user_id, role_id)
SELECT id, (SELECT id FROM rh_role WHERE role_code = 'REPORTER') FROM rh_user WHERE username = 'testuser';

-- 创建测试数据源
INSERT IGNORE INTO rh_data_source (name, description, ds_type, host, port, database_name, username, password, extra_params, status, deleted, create_user_id) VALUES
('ClawReport本地MySQL', '本地MySQL测试数据源', 'mysql', 'mysql', 3306, 'reporthub', 'root', 'Root@2026', '{}', 1, 0, 1);

INSERT IGNORE INTO rh_data_source (name, description, ds_type, host, port, database_name, username, password, extra_params, status, deleted, create_user_id) VALUES
('ClawReport测试数据源', '用于测试的本地数据源', 'mysql', 'mysql', 3306, 'reporthub', 'clawreport', 'ReportHub@2026', '{}', 1, 0, 1);

-- 创建报表分组
INSERT IGNORE INTO rh_report_group (name, parent_id, sort_order, description, deleted) VALUES
('经营分析', 0, 10, '经营分析类报表', 0),
('销售报表', 0, 20, '销售数据报表', 0),
('财务统计', 0, 30, '财务报表统计', 0),
('系统监控', 0, 40, '系统运维监控', 0);

-- 创建测试报表
INSERT IGNORE INTO rh_report (name, description, data_source_id, sql_content, group_id, params_config, chart_type, status, deleted, create_user_id) VALUES
('月度销售汇总表', '按月统计销售数据报表', 1, 'SELECT DATE_FORMAT(create_time, "%Y-%m") as month, COUNT(*) as order_count, SUM(1000 + (RAND()*5000)) as amount FROM rh_user GROUP BY month ORDER BY month', 2, '{}', 'TABLE', 1, 0, 1);

INSERT IGNORE INTO rh_report (name, description, data_source_id, sql_content, group_id, params_config, chart_type, status, deleted, create_user_id) VALUES
('用户增长趋势', '每月用户注册增长趋势', 1, 'SELECT DATE_FORMAT(create_time, "%Y-%m") as month, COUNT(*) as user_count FROM rh_user GROUP BY month ORDER BY month', 2, '{}', 'LINE', 1, 0, 1);

INSERT IGNORE INTO rh_report (name, description, data_source_id, sql_content, group_id, params_config, chart_type, status, deleted, create_user_id) VALUES
('用户状态分布', '用户状态饼图统计', 1, 'SELECT CASE WHEN status=1 THEN "正常" ELSE "禁用" END as name, COUNT(*) as value FROM rh_user GROUP BY status', 2, '{}', 'PIE', 1, 0, 1);

INSERT IGNORE INTO rh_report (name, description, data_source_id, sql_content, group_id, params_config, chart_type, status, deleted, create_user_id) VALUES
('数据源连接监控', '监控各数据源连接状态', 1, 'SELECT name, ds_type, host, port, database_name, create_time FROM rh_data_source', 4, '{}', 'TABLE', 1, 0, 1);

-- 创建测试数据脱敏规则
INSERT IGNORE INTO rh_data_source_mask (data_source_id, table_name, column_name, mask_type, mask_pattern, mask_replacement, enabled, deleted) VALUES
(1, 'rh_user', 'password', 'CUSTOM', '.+', '****', 1, 0);

INSERT IGNORE INTO rh_data_source_mask (data_source_id, table_name, column_name, mask_type, mask_pattern, mask_replacement, enabled, deleted) VALUES
(1, 'rh_user', 'email', 'CUSTOM', '.+@.+', '****@****', 1, 0);
