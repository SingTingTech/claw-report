package com.clawreport.controller;

import com.clawreport.common.R;
import com.clawreport.entity.ReportGroup;
import com.clawreport.mapper.ReportGroupMapper;
import com.clawreport.security.LoginUser;
import com.clawreport.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/group")
public class GroupController {

    @Autowired
    private ReportGroupMapper reportGroupMapper;
    
    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public R<List<Map<String, Object>>> list(@AuthenticationPrincipal LoginUser user) {
        // 获取用户可访问的分组
        Set<Long> accessibleGroupIds = permissionService.getUserAccessibleGroupIds(user.getUserId());
        
        List<ReportGroup> allGroups = reportGroupMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReportGroup>()
                .eq(ReportGroup::getDeleted, 0)
                .orderByAsc(ReportGroup::getSortOrder)
        );
        
        // 如果不是管理员，按权限过滤
        List<ReportGroup> filteredGroups = allGroups;
        PermissionService.UserDataScope dataScope = permissionService.getUserDataScope(user.getUserId());
        if (!dataScope.hasAccessToAll()) {
            final Set<Long> finalAccessible = accessibleGroupIds;
            filteredGroups = allGroups.stream()
                .filter(g -> finalAccessible.contains(g.getId()))
                .collect(Collectors.toList());
        }
        
        // 转换为树形结构
        List<Map<String, Object>> tree = buildTree(filteredGroups, 0L);
        return R.success(tree);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('menu:group:edit')")
    public R<List<ReportGroup>> listAll() {
        List<ReportGroup> groups = reportGroupMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReportGroup>()
                .eq(ReportGroup::getDeleted, 0)
                .orderByAsc(ReportGroup::getSortOrder)
        );
        return R.success(groups);
    }

    @GetMapping("/{id}")
    public R<ReportGroup> getById(@PathVariable Long id) {
        ReportGroup group = reportGroupMapper.selectById(id);
        return R.success(group);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('menu:group:edit')")
    public R<Void> create(@RequestBody ReportGroup group) {
        reportGroupMapper.insert(group);
        return R.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:group:edit')")
    public R<Void> update(@PathVariable Long id, @RequestBody ReportGroup group) {
        group.setId(id);
        reportGroupMapper.updateById(group);
        return R.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:group:edit')")
    public R<Void> delete(@PathVariable Long id) {
        // 检查是否有子分组
        Long count = reportGroupMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReportGroup>()
                .eq(ReportGroup::getParentId, id)
                .eq(ReportGroup::getDeleted, 0)
        );
        if (count > 0) {
            return R.fail(400, "请先删除子分组");
        }
        reportGroupMapper.deleteById(id);
        return R.success(null);
    }

    /**
     * 获取分组树（包含子分组）
     */
    @GetMapping("/{id}/tree")
    public R<Set<Long>> getGroupTree(@PathVariable Long id) {
        Set<Long> groupTree = permissionService.getGroupAndDescendants(id);
        return R.success(groupTree);
    }

    /**
     * 构建树形结构
     */
    private List<Map<String, Object>> buildTree(List<ReportGroup> groups, Long parentId) {
        return groups.stream()
            .filter(g -> {
                Long pid = g.getParentId();
                return (pid == null && parentId == 0L) || (pid != null && pid.equals(parentId));
            })
            .map(g -> {
                Map<String, Object> node = new java.util.HashMap<>();
                node.put("id", g.getId());
                node.put("name", g.getName());
                node.put("parentId", g.getParentId());
                node.put("description", g.getDescription());
                node.put("sortOrder", g.getSortOrder());
                
                List<Map<String, Object>> children = buildTree(groups, g.getId());
                if (!children.isEmpty()) {
                    node.put("children", children);
                }
                return node;
            })
            .collect(Collectors.toList());
    }
}
