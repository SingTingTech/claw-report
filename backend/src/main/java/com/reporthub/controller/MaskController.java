package com.reporthub.controller;

import com.reporthub.common.R;
import com.reporthub.entity.DataSourceMask;
import com.reporthub.service.DataSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mask")
@PreAuthorize("hasAuthority('menu:mask:edit')")
public class MaskController {

    @Autowired
    private DataSourceService dataSourceService;

    @GetMapping
    public R<List<DataSourceMask>> list() {
        // 获取当前用户可管理的数据源的脱敏规则
        // TODO: 按权限过滤
        return R.success(List.of());
    }

    @GetMapping("/datasource/{dataSourceId}")
    public R<List<DataSourceMask>> listByDataSource(@PathVariable Long dataSourceId) {
        return R.success(dataSourceService.getMaskRules(dataSourceId));
    }

    @PostMapping
    public R<Void> create(@RequestBody DataSourceMask mask) {
        dataSourceService.saveMaskRule(mask);
        return R.success(null);
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody DataSourceMask mask) {
        mask.setId(id);
        dataSourceService.saveMaskRule(mask);
        return R.success(null);
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        dataSourceService.deleteMaskRule(id);
        return R.success(null);
    }
}
