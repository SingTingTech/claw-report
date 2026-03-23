package com.reporthub.controller;

import com.reporthub.common.R;
import com.reporthub.entity.Report;
import com.reporthub.entity.ReportParam;
import com.reporthub.entity.ReportShare;
import com.reporthub.security.LoginUser;
import com.reporthub.service.PermissionService;
import com.reporthub.service.QueryService;
import com.reporthub.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private QueryService queryService;
    
    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public R<List<Report>> list(@AuthenticationPrincipal LoginUser user) {
        PermissionService.UserDataScope dataScope = permissionService.getUserDataScope(user.getUserId());
        List<Report> allReports = reportService.list();
        
        if (dataScope.hasAccessToAll()) {
            return R.success(allReports);
        }
        
        // 按数据权限过滤
        List<Report> filtered = allReports.stream()
            .filter(r -> {
                if (r.getGroupId() == null) return true;
                return dataScope.canAccessGroup(r.getGroupId());
            })
            .collect(java.util.stream.Collectors.toList());
        
        return R.success(filtered);
    }

    @GetMapping("/{id}")
    public R<Report> getById(@PathVariable Long id) {
        return R.success(reportService.getById(id));
    }

    @GetMapping("/{id}/params")
    public R<List<ReportParam>> getParams(@PathVariable Long id) {
        return R.success(reportService.getParams(id));
    }
    
    @PostMapping("/{id}/params")
    public R<Void> saveParams(@PathVariable Long id, @RequestBody List<ReportParam> params) {
        reportService.saveParams(id, params);
        return R.success(null);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('menu:report:edit')")
    public R<Void> create(@RequestBody Report report, @AuthenticationPrincipal LoginUser user) {
        reportService.create(report, user.getUserId());
        return R.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:report:edit')")
    public R<Void> update(@PathVariable Long id, @RequestBody Report report) {
        report.setId(id);
        reportService.update(report);
        return R.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:report:edit')")
    public R<Void> delete(@PathVariable Long id) {
        reportService.delete(id);
        return R.success(null);
    }

    @GetMapping("/{id}/data")
    public R<com.reporthub.datasource.DataSourceAdapter.QueryResult> getReportData(
            @PathVariable Long id,
            @RequestParam(required = false) Map<String, Object> params,
            @AuthenticationPrincipal LoginUser loginUser) {
        Report report = reportService.getById(id);
        if (report == null) {
            return R.fail(404, "报表不存在");
        }
        
        int page = params.containsKey("page") ? Integer.parseInt(params.get("page").toString()) : 1;
        int pageSize = params.containsKey("pageSize") ? Integer.parseInt(params.get("pageSize").toString()) : 20;
        
        // 移除分页参数
        params.remove("page");
        params.remove("pageSize");
        
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        
        try {
            var result = queryService.executeQuery(report.getDataSourceId(), report.getSqlContent(), params, page, pageSize, userId);
            return R.success(result);
        } catch (RuntimeException e) {
            return R.fail(400, e.getMessage());
        }
    }

    @GetMapping("/share/{token}")
    public R<Map<String, Object>> getByShareToken(@PathVariable String token) {
        ReportShare share = reportService.getByShareToken(token);
        if (share == null) {
            return R.fail(404, "分享不存在或已过期");
        }
        
        Report report = reportService.getById(share.getReportId());
        if (report == null) {
            return R.fail(404, "报表不存在");
        }
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("report", report);
        result.put("params", reportService.getParams(report.getId()));
        return R.success(result);
    }

    @PostMapping("/{id}/share")
    @PreAuthorize("hasAuthority('menu:report:edit')")
    public R<String> createShare(@PathVariable Long id, 
                                  @RequestBody(required = false) Map<String, String> params,
                                  @AuthenticationPrincipal LoginUser user) {
        LocalDateTime expiresAt = null;
        if (params != null && params.get("expiresAt") != null) {
            expiresAt = LocalDateTime.parse(params.get("expiresAt"));
        }
        String token = reportService.createShare(id, user.getUserId(), expiresAt);
        return R.success(token);
    }
}
