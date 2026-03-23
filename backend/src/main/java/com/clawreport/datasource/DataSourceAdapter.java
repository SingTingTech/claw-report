package com.clawreport.datasource;

import lombok.Data;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * 数据源适配器接口
 * 新数据源只需实现此类，即可接入报表系统
 */
public interface DataSourceAdapter {

    /**
     * 获取数据源类型标识
     */
    String getType();

    /**
     * 建立数据库连接
     */
    Connection getConnection(DataSourceConfig config) throws Exception;

    /**
     * 测试连接
     */
    void testConnection(DataSourceConfig config) throws Exception;

    /**
     * 获取数据库列表
     */
    List<String> getDatabases(DataSourceConfig config) throws Exception;

    /**
     * 获取表列表
     */
    List<TableMeta> getTables(DataSourceConfig config, String database) throws Exception;

    /**
     * 获取列信息
     */
    List<ColumnMeta> getColumns(DataSourceConfig config, String database, String table) throws Exception;

    /**
     * 执行分页查询
     */
    QueryResult executeQuery(DataSourceConfig config, String sql, Map<String, Object> params, int page, int pageSize) throws Exception;

    /**
     * 执行更新语句（DDL/DML）
     */
    UpdateResult executeUpdate(DataSourceConfig config, String sql, Map<String, Object> params) throws Exception;

    /**
     * 关闭连接
     */
    void close(Connection conn) throws Exception;

    // ---- 内嵌类：配置 ----
    @Data
    class DataSourceConfig {
        private String host;
        private Integer port;
        private String database;
        private String username;
        private String password;
        private String extraParams; // JSON
    }

    // ---- 内嵌类：表元数据 ----
    @Data
    class TableMeta {
        private String tableName;
        private String comment;
        private String engine;
    }

    // ---- 内嵌类：列元数据 ----
    @Data
    class ColumnMeta {
        private String columnName;
        private String dataType;
        private String columnType;
        private String comment;
        private boolean nullable;
        private String defaultValue;
        private boolean primaryKey;
    }

    // ---- 内嵌类：查询结果 ----
    @Data
    class QueryResult {
        private List<Map<String, Object>> records;
        private long total;
        private int executeTime; // ms
        private List<ColumnInfo> columns;
    }

    // ---- 内嵌类：更新结果 ----
    @Data
    class UpdateResult {
        private int affectedRows;
        private long executeTime;
    }

    // ---- 内嵌类：列信息 ----
    @Data
    class ColumnInfo {
        private String name;
        private String type;
        private String label;
    }
}
