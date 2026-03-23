package com.clawreport.datasource;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL 数据源适配器（预留扩展）
 */
@Component
public class PostgreSQLAdapter implements DataSourceAdapter {

    @Override
    public String getType() {
        return "POSTGRESQL";
    }

    @Override
    public Connection getConnection(DataSourceConfig config) throws Exception {
        // TODO: 实现 PostgreSQL 连接
        throw new UnsupportedOperationException("PostgreSQL 适配器待实现");
    }

    @Override
    public void testConnection(DataSourceConfig config) throws Exception {
        throw new UnsupportedOperationException("PostgreSQL 适配器待实现");
    }

    @Override
    public List<String> getDatabases(DataSourceConfig config) throws Exception {
        throw new UnsupportedOperationException("PostgreSQL 适配器待实现");
    }

    @Override
    public List<TableMeta> getTables(DataSourceConfig config, String database) throws Exception {
        throw new UnsupportedOperationException("PostgreSQL 适配器待实现");
    }

    @Override
    public List<ColumnMeta> getColumns(DataSourceConfig config, String database, String table) throws Exception {
        throw new UnsupportedOperationException("PostgreSQL 适配器待实现");
    }

    @Override
    public QueryResult executeQuery(DataSourceConfig config, String sql, Map<String, Object> params, int page, int pageSize) throws Exception {
        throw new UnsupportedOperationException("PostgreSQL 适配器待实现");
    }

    @Override
    public UpdateResult executeUpdate(DataSourceConfig config, String sql, Map<String, Object> params) throws Exception {
        throw new UnsupportedOperationException("PostgreSQL 适配器待实现");
    }

    @Override
    public void close(Connection conn) throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
}
