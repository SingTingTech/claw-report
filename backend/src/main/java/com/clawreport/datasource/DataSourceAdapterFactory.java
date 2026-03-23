package com.clawreport.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源适配器工厂
 * 根据类型获取对应的适配器实例
 */
@Component
public class DataSourceAdapterFactory {

    @Autowired
    private Map<String, DataSourceAdapter> adapters;

    private final Map<String, DataSourceAdapter> adapterMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (DataSourceAdapter adapter : adapters.values()) {
            adapterMap.put(adapter.getType(), adapter);
        }
    }

    public DataSourceAdapter getAdapter(String type) {
        DataSourceAdapter adapter = adapterMap.get(type);
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的数据源类型: " + type);
        }
        return adapter;
    }

    public boolean isSupported(String type) {
        return adapterMap.containsKey(type);
    }

    public String[] getSupportedTypes() {
        return adapterMap.keySet().toArray(new String[0]);
    }
}
