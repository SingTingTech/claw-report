package com.clawreport.controller;

import com.clawreport.common.R;
import com.clawreport.service.DataDictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasource/{dataSourceId}/dictionary")
//@PreAuthorize("hasAuthority('menu:datasource:view')")
public class DataDictionaryController {

    @Autowired
    private DataDictionaryService dictionaryService;

    /**
     * 获取数据字典列表
     */
    @GetMapping
    public R<List<Map<String, Object>>> getDictionary(
            @PathVariable Long dataSourceId,
            @RequestParam String database) {
        return R.success(dictionaryService.getDictionary(dataSourceId, database));
    }

    /**
     * 同步数据字典（从数据库拉取最新元数据）
     */
    @PostMapping("/sync")
    public R<Integer> syncDictionary(
            @PathVariable Long dataSourceId,
            @RequestParam String database) {
        int count = dictionaryService.syncDictionary(dataSourceId, database);
        return R.success(count);
    }

    /**
     * 更新字段注释
     */
    @PutMapping("/column")
    public R<Void> updateColumnComment(
            @PathVariable Long dataSourceId,
            @RequestBody Map<String, String> params) {
        String tableName = params.get("tableName");
        String columnName = params.get("columnName");
        String comment = params.get("comment");
        
        dictionaryService.updateColumnComment(dataSourceId, tableName, columnName, comment);
        return R.success(null);
    }

    /**
     * 更新枚举配置
     */
    @PutMapping("/enum-config")
    public R<Void> updateEnumConfig(
            @PathVariable Long dataSourceId,
            @RequestBody Map<String, String> params) {
        String tableName = params.get("tableName");
        String columnName = params.get("columnName");
        String enumConfig = params.get("enumConfig");
        
        dictionaryService.updateEnumConfig(dataSourceId, tableName, columnName, enumConfig);
        return R.success(null);
    }
}
