package com.reporthub.controller;

import com.reporthub.common.R;
import com.reporthub.entity.MenuPermission;
import com.reporthub.entity.Role;
import com.reporthub.mapper.RoleMapper;
import com.reporthub.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/role")
@PreAuthorize("hasAuthority('menu:role:view')")
public class RoleController {

    @Autowired
    private RoleMapper roleMapper;
    
    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public R<List<Role>> list() {
        List<Role> roles = roleMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Role>()
                .eq(Role::getDeleted, 0)
                .orderByAsc(Role::getId)
        );
        return R.success(roles);
    }

    @GetMapping("/{id}")
    public R<Role> getById(@PathVariable Long id) {
        Role role = roleMapper.selectById(id);
        return R.success(role);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('menu:role:edit')")
    public R<Void> create(@RequestBody Role role) {
        roleMapper.insert(role);
        return R.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:role:edit')")
    public R<Void> update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        roleMapper.updateById(role);
        return R.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:role:edit')")
    public R<Void> delete(@PathVariable Long id) {
        roleMapper.deleteById(id);
        return R.success(null);
    }

    @GetMapping("/{id}/permissions")
    public R<Map<String, Object>> getRolePermissions(@PathVariable Long id) {
        List<Long> menuPermIds = permissionService.getRoleMenuPermissionIds(id);
        List<Long> dataPermIds = permissionService.getRoleDataPermissionIds(id);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("menuPermissionIds", menuPermIds);
        result.put("dataPermissionIds", dataPermIds);
        
        return R.success(result);
    }

    @PostMapping("/{id}/menu-permissions")
    @PreAuthorize("hasAuthority('menu:role:edit')")
    public R<Void> assignMenuPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        List<Long> permissionIds = params.get("permissionIds");
        permissionService.assignMenuPermissionToRole(id, permissionIds);
        return R.success(null);
    }

    @PostMapping("/{id}/data-permissions")
    @PreAuthorize("hasAuthority('menu:role:edit')")
    public R<Void> assignDataPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        List<Long> permissionIds = params.get("permissionIds");
        permissionService.assignDataPermissionToRole(id, permissionIds);
        return R.success(null);
    }

    @GetMapping("/permissions")
    public R<List<MenuPermission>> getAllMenuPermissions() {
        return R.success(permissionService.getAllMenuPermissions());
    }

    @GetMapping("/data-permissions")
    public R<List<com.reporthub.entity.DataPermission>> getAllDataPermissions() {
        return R.success(permissionService.getAllDataPermissions());
    }
}
