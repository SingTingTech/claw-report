package com.reporthub.controller;

import com.reporthub.common.PageResult;
import com.reporthub.common.R;
import com.reporthub.datasource.DataSourceAdapter;
import com.reporthub.entity.QueryHistory;
import com.reporthub.security.LoginUser;
import com.reporthub.service.PermissionService;
import com.reporthub.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    @Autowired
    private QueryService queryService;
    
    @Autowired
    private PermissionService permissionService;

    @PostMapping("/execute")
    @PreAuthorize("hasAuthority('menu:query:execute')")
    public R<DataSourceAdapter.QueryResult> execute(
            @RequestBody Map<String, Object> params,
            @AuthenticationPrincipal LoginUser user) {
        
        Long dataSourceId = Long.parseLong(params.get("dataSourceId").toString());
        String sql = params.get("sql").toString();
        int page = params.containsKey("page") ? Integer.parseInt(params.get("page").toString()) : 1;
        int pageSize = params.containsKey("pageSize") ? Integer.parseInt(params.get("pageSize").toString()) : 20;
        
        @SuppressWarnings("unchecked")
        Map<String, Object> queryParams = (Map<String, Object>) params.getOrDefault("params", null);
        
        // 检查数据权限
        PermissionService.UserDataScope dataScope = permissionService.getUserDataScope(user.getUserId());
        if (!dataScope.hasAccessToAll() && !dataScope.canAccessDataSource(dataSourceId)) {
            return R.fail(403, "没有权限访问该数据源");
        }
        
        DataSourceAdapter.QueryResult result = queryService.executeQuery(
            dataSourceId, sql, queryParams, page, pageSize, user.getUserId()
        );
        
        return R.success(result);
    }

    @GetMapping("/history")
    public R<PageResult<QueryHistory>> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal LoginUser user) {
        return R.success(queryService.getHistory(user.getUserId(), page, pageSize));
    }
}
