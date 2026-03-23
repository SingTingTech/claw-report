package com.reporthub.controller;

import com.reporthub.common.PageResult;
import com.reporthub.common.R;
import com.reporthub.entity.Role;
import com.reporthub.entity.User;
import com.reporthub.mapper.RoleMapper;
import com.reporthub.mapper.UserRoleMapper;
import com.reporthub.security.LoginUser;
import com.reporthub.service.PermissionService;
import com.reporthub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private UserRoleMapper userRoleMapper;
    
    @Autowired
    private RoleMapper roleMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('menu:user:view')")
    public R<PageResult<User>> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        return R.success(userService.list(page, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:user:view')")
    public R<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return R.success(user);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('menu:user:edit')")
    public R<Void> create(@RequestBody User user) {
        userService.create(user);
        return R.success(null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:user:edit')")
    public R<Void> update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.update(user);
        return R.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('menu:user:edit')")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.success(null);
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('menu:user:view')")
    public R<List<Role>> getUserRoles(@PathVariable Long id) {
        return R.success(userService.getRoles(id));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('menu:user:edit')")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        List<Long> roleIds = params.get("roleIds");
        userService.assignRoles(id, roleIds);
        return R.success(null);
    }

    @GetMapping("/{id}/permissions")
    public R<Map<String, Object>> getUserPermissions(@PathVariable Long id) {
        Set<String> menuPermissions = permissionService.getUserMenuPermissions(id);
        PermissionService.UserDataScope dataScope = permissionService.getUserDataScope(id);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("menuPermissions", menuPermissions);
        result.put("dataScope", dataScope);
        result.put("groupIds", permissionService.getUserGroupIds(id));
        
        return R.success(result);
    }

    @PostMapping("/{id}/menu-permissions")
    @PreAuthorize("hasAuthority('menu:user:edit')")
    public R<Void> assignMenuPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        List<Long> permissionIds = params.get("permissionIds");
        permissionService.assignMenuPermissionToUser(id, permissionIds);
        return R.success(null);
    }

    @PostMapping("/{id}/data-permissions")
    @PreAuthorize("hasAuthority('menu:user:edit')")
    public R<Void> assignDataPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        List<Long> permissionIds = params.get("permissionIds");
        permissionService.assignDataPermissionToUser(id, permissionIds);
        return R.success(null);
    }

    @PostMapping("/{id}/groups")
    @PreAuthorize("hasAuthority('menu:user:edit')")
    public R<Void> assignGroups(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        List<Long> groupIds = params.get("groupIds");
        permissionService.assignUserToGroups(id, groupIds);
        return R.success(null);
    }

    @GetMapping("/permissions")
    public R<Map<String, Object>> getCurrentUserPermissions(@AuthenticationPrincipal LoginUser user) {
        Set<String> menuPermissions = permissionService.getUserMenuPermissions(user.getUserId());
        PermissionService.UserDataScope dataScope = permissionService.getUserDataScope(user.getUserId());

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("menuPermissions", menuPermissions);
        result.put("dataScope", dataScope);
        result.put("groupIds", permissionService.getUserGroupIds(user.getUserId()));

        return R.success(result);
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/profile")
    public R<Void> updateProfile(@AuthenticationPrincipal LoginUser loginUser, @RequestBody Map<String, String> params) {
        userService.updateProfile(loginUser.getUserId(), params);
        return R.success(null);
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public R<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser, @RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        userService.changePassword(loginUser.getUserId(), oldPassword, newPassword);
        return R.success(null);
    }
}
