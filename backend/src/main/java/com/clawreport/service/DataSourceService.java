package com.clawreport.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clawreport.datasource.DataSourceAdapter;
import com.clawreport.datasource.DataSourceAdapterFactory;
import com.clawreport.entity.DataSource;
import com.clawreport.entity.DataSourceMask;
import com.clawreport.mapper.DataSourceMapper;
import com.clawreport.mapper.DataSourceMaskMapper;
import com.clawreport.security.PasswordEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DataSourceService {

    @Autowired
    private DataSourceMapper dataSourceMapper;
    
    @Autowired
    private DataSourceMaskMapper dataSourceMaskMapper;
    
    @Autowired
    private PasswordEncryptor passwordEncryptor;
    
    @Autowired
    private DataSourceAdapterFactory adapterFactory;

    public List<DataSource> list(Long userId) {
        // TODO: 根据权限过滤
        return dataSourceMapper.selectList(
            new LambdaQueryWrapper<DataSource>()
                .eq(DataSource::getStatus, 1)
                .orderByDesc(DataSource::getCreateTime)
        );
    }

    public DataSource getById(Long id) {
        return dataSourceMapper.selectById(id);
    }

    public void create(DataSource ds, Long createUserId) {
        // 加密密码
        ds.setPassword(passwordEncryptor.encrypt(ds.getPassword()));
        ds.setCreateUserId(createUserId);
        ds.setStatus(1);
        dataSourceMapper.insert(ds);
    }

    public void update(DataSource ds) {
        // 如果密码被修改，则重新加密
        if (ds.getPassword() != null && !ds.getPassword().startsWith("$2a$")) {
            ds.setPassword(passwordEncryptor.encrypt(ds.getPassword()));
        }
        dataSourceMapper.updateById(ds);
    }

    public void delete(Long id) {
        dataSourceMapper.deleteById(id);
    }

    public void testConnection(DataSource ds) {
        DataSourceAdapter adapter = adapterFactory.getAdapter(ds.getDsType());
        DataSourceAdapter.DataSourceConfig config = buildConfig(ds);
        try {
            adapter.testConnection(config);
        } catch (Exception e) {
            throw new RuntimeException("连接测试失败: " + e.getMessage(), e);
        }
    }

    public List<DataSourceAdapter.TableMeta> getTables(Long id, String database) {
        DataSource ds = dataSourceMapper.selectById(id);
        DataSourceAdapter adapter = adapterFactory.getAdapter(ds.getDsType());
        DataSourceAdapter.DataSourceConfig config = buildConfig(ds);
        try {
            return adapter.getTables(config, database);
        } catch (Exception e) {
            throw new RuntimeException("获取表列表失败: " + e.getMessage(), e);
        }
    }

    public List<DataSourceAdapter.ColumnMeta> getColumns(Long id, String database, String table) {
        DataSource ds = dataSourceMapper.selectById(id);
        DataSourceAdapter adapter = adapterFactory.getAdapter(ds.getDsType());
        DataSourceAdapter.DataSourceConfig config = buildConfig(ds);
        try {
            return adapter.getColumns(config, database, table);
        } catch (Exception e) {
            throw new RuntimeException("获取列信息失败: " + e.getMessage(), e);
        }
    }

    public List<String> getDatabases(Long id) {
        DataSource ds = dataSourceMapper.selectById(id);
        DataSourceAdapter adapter = adapterFactory.getAdapter(ds.getDsType());
        DataSourceAdapter.DataSourceConfig config = buildConfig(ds);
        try {
            return adapter.getDatabases(config);
        } catch (Exception e) {
            throw new RuntimeException("获取数据库列表失败: " + e.getMessage(), e);
        }
    }

    // 脱敏相关
    public List<DataSourceMask> getMaskRules(Long dataSourceId) {
        return dataSourceMaskMapper.selectList(
            new LambdaQueryWrapper<DataSourceMask>()
                .eq(DataSourceMask::getDataSourceId, dataSourceId)
                .eq(DataSourceMask::getEnabled, 1)
        );
    }

    public void saveMaskRule(DataSourceMask mask) {
        if (mask.getId() == null) {
            dataSourceMaskMapper.insert(mask);
        } else {
            dataSourceMaskMapper.updateById(mask);
        }
    }

    public void deleteMaskRule(Long id) {
        dataSourceMaskMapper.deleteById(id);
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
