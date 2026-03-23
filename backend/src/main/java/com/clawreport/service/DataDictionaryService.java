package com.clawreport.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clawreport.datasource.DataSourceAdapter;
import com.clawreport.datasource.DataSourceAdapterFactory;
import com.clawreport.entity.DataDictionary;
import com.clawreport.entity.DataSource;
import com.clawreport.mapper.DataDictionaryMapper;
import com.clawreport.mapper.DataSourceMapper;
import com.clawreport.security.PasswordEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DataDictionaryService {

    @Autowired
    private DataDictionaryMapper dictionaryMapper;
    
    @Autowired
    private DataSourceMapper dataSourceMapper;
    
    @Autowired
    private DataSourceAdapterFactory adapterFactory;
    
    @Autowired
    private PasswordEncryptor passwordEncryptor;

    /**
     * 获取数据字典（带已保存的注释）
     */
    public List<Map<String, Object>> getDictionary(Long dataSourceId, String database) {
        // 获取已保存的注释
        List<DataDictionary> savedDicts = dictionaryMapper.selectList(
            new LambdaQueryWrapper<DataDictionary>()
                .eq(DataDictionary::getDataSourceId, dataSourceId)
        );
        
        Map<String, DataDictionary> dictMap = new HashMap<>();
        for (DataDictionary d : savedDicts) {
            String key = d.getTableName() + "." + d.getColumnName();
            dictMap.put(key, d);
        }
        
        // 获取数据库实际元数据
        DataSource ds = dataSourceMapper.selectById(dataSourceId);
        DataSourceAdapter adapter = adapterFactory.getAdapter(ds.getDsType());
        DataSourceAdapter.DataSourceConfig config = buildConfig(ds);
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            // 获取所有表
            List<DataSourceAdapter.TableMeta> tables = adapter.getTables(config, database);
            
            for (DataSourceAdapter.TableMeta table : tables) {
                Map<String, Object> tableInfo = new LinkedHashMap<>();
                tableInfo.put("tableName", table.getTableName());
                tableInfo.put("comment", table.getComment());
                
                // 获取表的列
                List<DataSourceAdapter.ColumnMeta> columns = adapter.getColumns(config, database, table.getTableName());
                
                List<Map<String, Object>> columnList = new ArrayList<>();
                for (DataSourceAdapter.ColumnMeta column : columns) {
                    Map<String, Object> colInfo = new LinkedHashMap<>();
                    colInfo.put("columnName", column.getColumnName());
                    colInfo.put("dataType", column.getDataType());
                    colInfo.put("columnType", column.getColumnType());
                    colInfo.put("nullable", column.isNullable() ? "是" : "否");
                    colInfo.put("defaultValue", column.getDefaultValue());
                    colInfo.put("primaryKey", column.isPrimaryKey());
                    colInfo.put("comment", column.getComment());
                    
                    // 合并已保存的注释和枚举配置
                    String key = table.getTableName() + "." + column.getColumnName();
                    DataDictionary saved = dictMap.get(key);
                    if (saved != null) {
                        colInfo.put("savedComment", saved.getColumnComment());
                        colInfo.put("enumConfig", saved.getEnumConfig());
                        colInfo.put("dictionaryId", saved.getId());
                    } else {
                        colInfo.put("savedComment", null);
                        colInfo.put("enumConfig", null);
                        colInfo.put("dictionaryId", null);
                    }
                    
                    columnList.add(colInfo);
                }
                
                tableInfo.put("columns", columnList);
                result.add(tableInfo);
            }
        } catch (Exception e) {
            throw new RuntimeException("获取数据字典失败: " + e.getMessage(), e);
        }
        
        return result;
    }

    /**
     * 同步数据字典（从数据库拉取最新元数据）
     */
    public int syncDictionary(Long dataSourceId, String database) {
        DataSource ds = dataSourceMapper.selectById(dataSourceId);
        DataSourceAdapter adapter = adapterFactory.getAdapter(ds.getDsType());
        DataSourceAdapter.DataSourceConfig config = buildConfig(ds);
        
        int count = 0;
        
        try {
            List<DataSourceAdapter.TableMeta> tables = adapter.getTables(config, database);
            
            for (DataSourceAdapter.TableMeta table : tables) {
                List<DataSourceAdapter.ColumnMeta> columns = adapter.getColumns(config, database, table.getTableName());
                
                for (DataSourceAdapter.ColumnMeta column : columns) {
                    // 检查是否已存在
                    DataDictionary existing = dictionaryMapper.selectOne(
                        new LambdaQueryWrapper<DataDictionary>()
                            .eq(DataDictionary::getDataSourceId, dataSourceId)
                            .eq(DataDictionary::getTableName, table.getTableName())
                            .eq(DataDictionary::getColumnName, column.getColumnName())
                    );
                    
                    if (existing == null) {
                        // 新增
                        DataDictionary dict = new DataDictionary();
                        dict.setDataSourceId(dataSourceId);
                        dict.setTableName(table.getTableName());
                        dict.setColumnName(column.getColumnName());
                        dict.setTableComment(table.getComment());
                        dict.setColumnComment(column.getComment());
                        dict.setColumnType(column.getColumnType());
                        dict.setNullable(column.isNullable() ? "是" : "否");
                        dict.setDefaultValue(column.getDefaultValue());
                        dict.setIsPrimaryKey(column.isPrimaryKey() ? 1 : 0);
                        dictionaryMapper.insert(dict);
                        count++;
                    } else {
                        // 更新元数据（保留用户注释）
                        existing.setTableComment(table.getComment());
                        existing.setColumnComment(column.getComment());
                        existing.setColumnType(column.getColumnType());
                        existing.setNullable(column.isNullable() ? "是" : "否");
                        existing.setDefaultValue(column.getDefaultValue());
                        existing.setIsPrimaryKey(column.isPrimaryKey() ? 1 : 0);
                        dictionaryMapper.updateById(existing);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("同步数据字典失败: " + e.getMessage(), e);
        }
        
        return count;
    }

    /**
     * 更新字段注释
     */
    public void updateColumnComment(Long dataSourceId, String tableName, String columnName, String comment) {
        DataDictionary dict = dictionaryMapper.selectOne(
            new LambdaQueryWrapper<DataDictionary>()
                .eq(DataDictionary::getDataSourceId, dataSourceId)
                .eq(DataDictionary::getTableName, tableName)
                .eq(DataDictionary::getColumnName, columnName)
        );
        
        if (dict == null) {
            // 创建新记录
            dict = new DataDictionary();
            dict.setDataSourceId(dataSourceId);
            dict.setTableName(tableName);
            dict.setColumnName(columnName);
            dict.setColumnComment(comment);
            dictionaryMapper.insert(dict);
        } else {
            dict.setColumnComment(comment);
            dictionaryMapper.updateById(dict);
        }
    }

    /**
     * 更新枚举配置
     */
    public void updateEnumConfig(Long dataSourceId, String tableName, String columnName, String enumConfig) {
        DataDictionary dict = dictionaryMapper.selectOne(
            new LambdaQueryWrapper<DataDictionary>()
                .eq(DataDictionary::getDataSourceId, dataSourceId)
                .eq(DataDictionary::getTableName, tableName)
                .eq(DataDictionary::getColumnName, columnName)
        );
        
        if (dict == null) {
            // 创建新记录
            dict = new DataDictionary();
            dict.setDataSourceId(dataSourceId);
            dict.setTableName(tableName);
            dict.setColumnName(columnName);
            dict.setEnumConfig(enumConfig);
            dictionaryMapper.insert(dict);
        } else {
            dict.setEnumConfig(enumConfig);
            dictionaryMapper.updateById(dict);
        }
    }

    private DataSourceAdapter.DataSourceConfig buildConfig(DataSource ds) {
        DataSourceAdapter.DataSourceConfig config = new DataSourceAdapter.DataSourceConfig();
        config.setHost(ds.getHost());
        config.setPort(ds.getPort());
        config.setDatabase(ds.getDatabaseName());
        config.setUsername(ds.getUsername());
        config.setPassword(passwordEncryptor.decrypt(ds.getPassword()));
        config.setExtraParams(ds.getExtraParams());
        return config;
    }
}
