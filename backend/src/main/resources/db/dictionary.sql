USE reporthub;
SET NAMES utf8mb4;

-- 数据字典表（存储表和字段的注释/说明）
CREATE TABLE IF NOT EXISTS rh_data_dictionary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    column_name VARCHAR(100) NOT NULL COMMENT '列名',
    table_comment VARCHAR(500) COMMENT '表注释',
    column_comment VARCHAR(500) COMMENT '字段注释',
    column_type VARCHAR(100) COMMENT '字段类型',
    nullable VARCHAR(10) COMMENT '是否可空',
    default_value VARCHAR(255) COMMENT '默认值',
    is_primary_key TINYINT DEFAULT 0 COMMENT '是否主键',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ds_table_column (data_source_id, table_name, column_name),
    INDEX idx_datasource (data_source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';
