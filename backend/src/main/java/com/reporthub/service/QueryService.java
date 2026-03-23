package com.reporthub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reporthub.common.PageResult;
import com.reporthub.datasource.DataSourceAdapter;
import com.reporthub.datasource.DataSourceAdapterFactory;
import com.reporthub.entity.DataSource;
import com.reporthub.entity.DataSourceMask;
import com.reporthub.entity.QueryHistory;
import com.reporthub.mapper.DataSourceMapper;
import com.reporthub.mapper.DataSourceMaskMapper;
import com.reporthub.mapper.QueryHistoryMapper;
import com.reporthub.mask.DataMaskService;
import com.reporthub.security.PasswordEncryptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QueryService {

    @Autowired
    private DataSourceMapper dataSourceMapper;
    
    @Autowired
    private DataSourceMaskMapper dataSourceMaskMapper;
    
    @Autowired
    private QueryHistoryMapper queryHistoryMapper;
    
    @Autowired
    private PasswordEncryptor passwordEncryptor;
    
    @Autowired
    private DataSourceAdapterFactory adapterFactory;
    
    @Autowired
    private DataMaskService dataMaskService;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 执行查询
     */
    public DataSourceAdapter.QueryResult executeQuery(Long dataSourceId, String sql, 
                                                       Map<String, Object> params, 
                                                       int page, int pageSize, Long userId) {
        DataSource ds = dataSourceMapper.selectById(dataSourceId);
        if (ds == null) {
            throw new RuntimeException("数据源不存在");
        }
        
        DataSourceAdapter adapter = adapterFactory.getAdapter(ds.getDsType());
        DataSourceAdapter.DataSourceConfig config = buildConfig(ds);
        
        QueryHistory history = new QueryHistory();
        history.setUserId(userId);
        history.setDataSourceId(dataSourceId);
        history.setSqlContent(sql);
        history.setParams(toJson(params));
        
        DataSourceAdapter.QueryResult result;
        try {
            result = adapter.executeQuery(config, sql, params, page, pageSize);
            history.setStatus(1);
            history.setExecuteTime(result.getExecuteTime());
            
            // 应用脱敏规则
            result = applyMaskRules(result, dataSourceId);
            
        } catch (Exception e) {
            history.setStatus(0);
            history.setErrorMessage(e.getMessage());
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        } finally {
            queryHistoryMapper.insert(history);
        }
        
        return result;
    }

    /**
     * 应用脱敏规则
     */
    private DataSourceAdapter.QueryResult applyMaskRules(DataSourceAdapter.QueryResult result, Long dataSourceId) {
        List<DataSourceMask> maskRules = dataSourceMaskMapper.selectList(
            new LambdaQueryWrapper<DataSourceMask>()
                .eq(DataSourceMask::getDataSourceId, dataSourceId)
                .eq(DataSourceMask::getEnabled, 1)
        );
        
        if (maskRules.isEmpty() || result.getRecords() == null || result.getRecords().isEmpty()) {
            return result;
        }
        
        // 按表名+列名建立索引
        Map<String, DataSourceMask> ruleMap = new HashMap<>();
        for (DataSourceMask rule : maskRules) {
            // 只用列名作为key（同一数据源内列名唯一）
            ruleMap.put(rule.getColumnName().toLowerCase(), rule);
        }
        
        // 遍历每行数据并脱敏
        List<Map<String, Object>> maskedRecords = new ArrayList<>();
        for (Map<String, Object> record : result.getRecords()) {
            Map<String, Object> maskedRecord = new LinkedHashMap<>(record);
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                String colName = entry.getKey().toLowerCase();
                if (ruleMap.containsKey(colName)) {
                    DataSourceMask rule = ruleMap.get(colName);
                    Object maskedValue = maskValue(entry.getValue(), rule);
                    maskedRecord.put(entry.getKey(), maskedValue);
                }
            }
            maskedRecords.add(maskedRecord);
        }
        
        result.setRecords(maskedRecords);
        return result;
    }

    private Object maskValue(Object value, DataSourceMask rule) {
        if ("CUSTOM".equals(rule.getMaskType())) {
            return dataMaskService.maskByPattern(value, rule.getMaskPattern(), rule.getMaskReplacement());
        }
        return dataMaskService.mask(rule.getMaskType(), value);
    }

    /**
     * 获取查询历史
     */
    public PageResult<QueryHistory> getHistory(Long userId, int page, int pageSize) {
        var wrapped = new LambdaQueryWrapper<QueryHistory>()
            .eq(QueryHistory::getUserId, userId)
            .orderByDesc(QueryHistory::getCreateTime);
        
        var records = queryHistoryMapper.selectPage(
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize), wrapped
        );
        
        return PageResult.of(records.getRecords(), records.getTotal(), page, pageSize);
    }

    private String toJson(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return null;
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
