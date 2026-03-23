package com.clawreport.controller;

import com.clawreport.common.R;
import com.clawreport.entity.DataSource;
import com.clawreport.entity.DataSourceMask;
import com.clawreport.security.LoginUser;
import com.clawreport.service.DataSourceService;
import com.clawreport.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasource")
public class DataSourceController {

    @Autowired
    private DataSourceService dataSourceService;
    
    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public R<List<DataSource>> list(@AuthenticationPrincipal LoginUser user) {
        PermissionService.UserDataScope dataScope = permissionService.getUserDataScope(user.getUserId());
        List<DataSource> allDs = dataSourceService.list(user.getUserId());
        
        // 按数据权限过滤
        if (dataScope.hasAccessToAll()) {
            return R.success(allDs);
        }
        
        List<DataSource> filtered = allDs.stream()
            .filter(ds -> dataScope.canAccessDataSource(ds.getId()))
            .collect(java.util.stream.Collectors.toList());
        
        return R.success(filtered);
    }

    @GetMapping("/{id}")
    public R<DataSource> getById(@PathVariable Long id) {
        DataSource ds = dataSourceService.getById(id);
        if (ds != null) {
            ds.setPassword(null); // 不返回密码
        }
        return R.success(ds);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('menu:datasource:edit')")
    public R<Void> create(@RequestBody DataSource ds, @AuthenticationPrincipal LoginUser user) {
        dataSourceService.create(ds, user.getUserId());
        return R.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:datasource:edit')")
    public R<Void> update(@PathVariable Long id, @RequestBody DataSource ds) {
        ds.setId(id);
        dataSourceService.update(ds);
        return R.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:datasource:edit')")
    public R<Void> delete(@PathVariable Long id) {
        dataSourceService.delete(id);
        return R.success(null);
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('menu:datasource:view')")
    public R<Void> testConnection(@PathVariable Long id, @RequestBody DataSource ds) {
        try {
            dataSourceService.testConnection(ds);
            return R.<Void>success(null);
        } catch (Exception e) {
            return R.fail(400, "连接失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/databases")
    @PreAuthorize("hasAuthority('menu:datasource:view')")
    public R<List<String>> getDatabases(@PathVariable Long id) {
        return R.success(dataSourceService.getDatabases(id));
    }

    @GetMapping("/{id}/tables")
    @PreAuthorize("hasAuthority('menu:datasource:view')")
    public R<List<com.clawreport.datasource.DataSourceAdapter.TableMeta>> getTables(
            @PathVariable Long id, @RequestParam String database) {
        return R.success(dataSourceService.getTables(id, database));
    }

    @GetMapping("/{id}/columns")
    @PreAuthorize("hasAuthority('menu:datasource:view')")
    public R<List<com.clawreport.datasource.DataSourceAdapter.ColumnMeta>> getColumns(
            @PathVariable Long id, @RequestParam String database, @RequestParam String table) {
        return R.success(dataSourceService.getColumns(id, database, table));
    }
}
