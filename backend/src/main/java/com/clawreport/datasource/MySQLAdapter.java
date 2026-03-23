package com.clawreport.datasource;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQL 数据源适配器
 */
@Component
public class MySQLAdapter implements DataSourceAdapter {

    // 用于提取 ${param} 格式的参数名
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    @Override
    public String getType() {
        return "MYSQL";
    }

    @Override
    public Connection getConnection(DataSourceConfig config) throws Exception {
        String url = buildJdbcUrl(config);
        Properties props = new Properties();
        props.setProperty("user", config.getUsername());
        props.setProperty("password", config.getPassword());
        props.setProperty("useUnicode", "true");
        props.setProperty("characterEncoding", "UTF-8");
        props.setProperty("serverTimezone", "Asia/Shanghai");
        props.setProperty("allowPublicKeyRetrieval", "true");
        props.setProperty("useSSL", "false");
        
        // 解析额外参数
        if (config.getExtraParams() != null && !config.getExtraParams().isEmpty()) {
            try {
                String[] params = config.getExtraParams().split(";");
                for (String param : params) {
                    String[] kv = param.split("=");
                    if (kv.length == 2) {
                        props.setProperty(kv[0].trim(), kv[1].trim());
                    }
                }
            } catch (Exception ignored) {}
        }
        
        return DriverManager.getConnection(url, props);
    }

    private String buildJdbcUrl(DataSourceConfig config) {
        return String.format("jdbc:mysql://%s:%d/%s",
                config.getHost(), config.getPort(), config.getDatabase());
    }

    @Override
    public void testConnection(DataSourceConfig config) throws Exception {
        Connection conn = null;
        try {
            conn = getConnection(config);
            conn.createStatement().execute("SELECT 1");
        } finally {
            close(conn);
        }
    }

    @Override
    public List<String> getDatabases(DataSourceConfig config) throws Exception {
        Connection conn = null;
        ResultSet rs = null;
        List<String> databases = new ArrayList<>();
        try {
            DataSourceConfig globalConfig = new DataSourceConfig();
            globalConfig.setHost(config.getHost());
            globalConfig.setPort(config.getPort());
            globalConfig.setUsername(config.getUsername());
            globalConfig.setPassword(config.getPassword());
            globalConfig.setExtraParams(config.getExtraParams());
            globalConfig.setDatabase("");
            
            conn = getConnection(globalConfig);
            rs = conn.createStatement().executeQuery("SHOW DATABASES");
            while (rs.next()) {
                databases.add(rs.getString(1));
            }
        } finally {
            if (rs != null) rs.close();
            close(conn);
        }
        return databases;
    }

    @Override
    public List<TableMeta> getTables(DataSourceConfig config, String database) throws Exception {
        Connection conn = null;
        ResultSet rs = null;
        List<TableMeta> tables = new ArrayList<>();
        try {
            conn = getConnection(config);
            DatabaseMetaData metaData = conn.getMetaData();
            rs = metaData.getTables(database, null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                TableMeta table = new TableMeta();
                table.setTableName(rs.getString("TABLE_NAME"));
                table.setComment(rs.getString("REMARKS"));
                tables.add(table);
            }
        } finally {
            if (rs != null) rs.close();
            close(conn);
        }
        return tables;
    }

    @Override
    public List<ColumnMeta> getColumns(DataSourceConfig config, String database, String table) throws Exception {
        Connection conn = null;
        ResultSet rs = null;
        List<ColumnMeta> columns = new ArrayList<>();
        try {
            conn = getConnection(config);
            DatabaseMetaData metaData = conn.getMetaData();
            rs = metaData.getColumns(database, null, table, null);
            while (rs.next()) {
                ColumnMeta column = new ColumnMeta();
                column.setColumnName(rs.getString("COLUMN_NAME"));
                column.setDataType(rs.getString("TYPE_NAME"));
                column.setColumnType(rs.getString("TYPE_NAME") + "(" + rs.getInt("COLUMN_SIZE") + ")");
                column.setComment(rs.getString("REMARKS"));
                column.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.setDefaultValue(rs.getString("COLUMN_DEF"));
                columns.add(column);
            }
            
            // 获取主键信息
            ResultSet pkRs = metaData.getPrimaryKeys(database, null, table);
            Set<String> pkColumns = new HashSet<>();
            while (pkRs.next()) {
                pkColumns.add(pkRs.getString("COLUMN_NAME"));
            }
            pkRs.close();
            
            for (ColumnMeta column : columns) {
                column.setPrimaryKey(pkColumns.contains(column.getColumnName()));
            }
        } finally {
            if (rs != null) rs.close();
            close(conn);
        }
        return columns;
    }

    @Override
    public QueryResult executeQuery(DataSourceConfig config, String sql, Map<String, Object> params, int page, int pageSize) throws Exception {
        long startTime = System.currentTimeMillis();
        Connection conn = null;
        try {
            conn = getConnection(config);
            
            // 提取参数名列表并转换为安全的 SQL（使用 ? 占位符）
            List<String> paramNames = new ArrayList<>();
            String safeSql = convertToParameterizedSql(sql, paramNames);
            
            // 检查必填参数是否都已提供
            for (String paramName : paramNames) {
                if (params == null || !params.containsKey(paramName) || params.get(paramName) == null) {
                    throw new IllegalArgumentException("SQL参数「" + paramName + "」未填写，请检查必填参数");
                }
            }
            
            // 查询总数（使用参数化查询）
            String countSql = "SELECT COUNT(*) FROM (" + safeSql + ") AS _count_tbl";
            long total = 0;
            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                setParameters(countStmt, paramNames, params);
                ResultSet countRs = countStmt.executeQuery();
                if (countRs.next()) {
                    total = countRs.getLong(1);
                }
            }
            
            // 分页查询
            String paginatedSql = safeSql + " LIMIT " + (page - 1) * pageSize + ", " + pageSize;
            List<Map<String, Object>> records = new ArrayList<>();
            List<ColumnInfo> columns = new ArrayList<>();
            
            try (PreparedStatement pstmt = conn.prepareStatement(paginatedSql)) {
                setParameters(pstmt, paramNames, params);
                ResultSet rs = pstmt.executeQuery();
                
                ResultSetMetaData rsMetaData = rs.getMetaData();
                int columnCount = rsMetaData.getColumnCount();
                
                // 获取列信息
                for (int i = 1; i <= columnCount; i++) {
                    ColumnInfo col = new ColumnInfo();
                    col.setName(rsMetaData.getColumnLabel(i));
                    col.setType(rsMetaData.getColumnTypeName(i));
                    col.setLabel(rsMetaData.getColumnLabel(i));
                    columns.add(col);
                }
                
                // 获取数据
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(rsMetaData.getColumnLabel(i), rs.getObject(i));
                    }
                    records.add(row);
                }
            }
            
            QueryResult result = new QueryResult();
            result.setRecords(records);
            result.setTotal(total);
            result.setExecuteTime((int) (System.currentTimeMillis() - startTime));
            result.setColumns(columns);
            return result;
            
        } finally {
            close(conn);
        }
    }

    @Override
    public UpdateResult executeUpdate(DataSourceConfig config, String sql, Map<String, Object> params) throws Exception {
        long startTime = System.currentTimeMillis();
        Connection conn = null;
        try {
            conn = getConnection(config);
            
            // 提取参数名列表并转换为安全的 SQL
            List<String> paramNames = new ArrayList<>();
            String safeSql = convertToParameterizedSql(sql, paramNames);
            
            try (PreparedStatement pstmt = conn.prepareStatement(safeSql)) {
                setParameters(pstmt, paramNames, params);
                int affectedRows = pstmt.executeUpdate();
                UpdateResult result = new UpdateResult();
                result.setAffectedRows(affectedRows);
                result.setExecuteTime((int) (System.currentTimeMillis() - startTime));
                return result;
            }
        } finally {
            close(conn);
        }
    }

    @Override
    public void close(Connection conn) throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    /**
     * 将 ${param} 格式的 SQL 转换为使用 ? 占位符的参数化 SQL
     * @param sql 原始 SQL
     * @param paramNames 用于收集参数名的列表
     * @return 安全的参数化 SQL
     */
    private String convertToParameterizedSql(String sql, List<String> paramNames) {
        Matcher matcher = PARAM_PATTERN.matcher(sql);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (!paramNames.contains(paramName)) {
                paramNames.add(paramName);
            }
            matcher.appendReplacement(result, "?");
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * 设置 PreparedStatement 的参数
     */
    private void setParameters(PreparedStatement pstmt, List<String> paramNames, Map<String, Object> params) throws SQLException {
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            Object value = params.get(paramName);
            pstmt.setObject(i + 1, value);
        }
    }
}
