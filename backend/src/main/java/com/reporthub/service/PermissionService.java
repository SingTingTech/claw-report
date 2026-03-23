package com.reporthub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reporthub.entity.*;
import com.reporthub.mapper.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务
 * 统一处理菜单权限和数据权限，支持直接分配和角色分配
 */
@Service
public class PermissionService {

    @Autowired
    private MenuPermissionMapper menuPermissionMapper;
    
    @Autowired
    private DataPermissionMapper dataPermissionMapper;
    
    @Autowired
    private UserMenuPermissionMapper userMenuPermissionMapper;
    
    @Autowired
    private UserDataPermissionMapper userDataPermissionMapper;
    
    @Autowired
    private RoleMenuPermissionMapper roleMenuPermissionMapper;
    
    @Autowired
    private RoleDataPermissionMapper roleDataPermissionMapper;
    
    @Autowired
    private RoleMapper roleMapper;
    
    @Autowired
    private UserRoleMapper userRoleMapper;
    
    @Autowired
    private ReportGroupMapper reportGroupMapper;
    
    @Autowired
    private UserGroupMapper userGroupMapper;

    // ==================== 菜单权限 ====================

    /**
     * 获取用户所有菜单权限（含直接分配 + 角色分配）
     */
    public Set<String> getUserMenuPermissions(Long userId) {
        Set<String> permissions = new HashSet<>();
        
        // 1. 直接分配给用户的权限
        List<UserMenuPermission> directMenuPerms = userMenuPermissionMapper.selectList(
            new LambdaQueryWrapper<UserMenuPermission>().eq(UserMenuPermission::getUserId, userId)
        );
        for (UserMenuPermission up : directMenuPerms) {
            MenuPermission mp = menuPermissionMapper.selectById(up.getPermissionId());
            if (mp != null) {
                permissions.add(mp.getPermissionCode());
            }
        }
        
        // 2. 通过角色分配的权限
        List<UserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
        for (UserRole ur : userRoles) {
            List<RoleMenuPermission> roleMenuPerms = roleMenuPermissionMapper.selectList(
                new LambdaQueryWrapper<RoleMenuPermission>().eq(RoleMenuPermission::getRoleId, ur.getRoleId())
            );
            for (RoleMenuPermission rp : roleMenuPerms) {
                MenuPermission mp = menuPermissionMapper.selectById(rp.getPermissionId());
                if (mp != null) {
                    permissions.add(mp.getPermissionCode());
                }
            }
        }
        
        return permissions;
    }

    /**
     * 检查用户是否拥有指定菜单权限
     */
    public boolean hasMenuPermission(Long userId, String permissionCode) {
        return getUserMenuPermissions(userId).contains(permissionCode);
    }

    /**
     * 分配直接菜单权限给用户
     */
    public void assignMenuPermissionToUser(Long userId, List<Long> permissionIds) {
        // 删除旧权限
        userMenuPermissionMapper.delete(
            new LambdaQueryWrapper<UserMenuPermission>().eq(UserMenuPermission::getUserId, userId)
        );
        // 插入新权限
        for (Long permId : permissionIds) {
            UserMenuPermission ump = new UserMenuPermission();
            ump.setUserId(userId);
            ump.setPermissionId(permId);
            userMenuPermissionMapper.insert(ump);
        }
    }

    // ==================== 数据权限 ====================

    /**
     * 获取用户所有数据权限（含直接分配 + 角色分配）
     * 返回可访问的数据源ID列表和可访问的分组ID列表（含子分组）
     */
    public UserDataScope getUserDataScope(Long userId) {
        Set<Long> allowedDataSourceIds = new HashSet<>();
        Set<Long> allowedGroupIds = new HashSet<>();
        
        // 1. 直接分配的数据权限
        List<UserDataPermission> directDataPerms = userDataPermissionMapper.selectList(
            new LambdaQueryWrapper<UserDataPermission>().eq(UserDataPermission::getUserId, userId)
        );
        for (UserDataPermission up : directDataPerms) {
            DataPermission dp = dataPermissionMapper.selectById(up.getPermissionId());
            if (dp != null) {
                addDataPermission(dp, allowedDataSourceIds, allowedGroupIds);
            }
        }
        
        // 2. 通过角色分配的数据权限
        List<UserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
        for (UserRole ur : userRoles) {
            // 检查是否是 ADMIN 角色
            Role role = roleMapper.selectById(ur.getRoleId());
            if (role != null && "ADMIN".equals(role.getRoleCode())) {
                // ADMIN 角色拥有所有数据权限
                return new UserDataScope(null, null); // null 表示全部
            }
            
            List<RoleDataPermission> roleDataPerms = roleDataPermissionMapper.selectList(
                new LambdaQueryWrapper<RoleDataPermission>().eq(RoleDataPermission::getRoleId, ur.getRoleId())
            );
            for (RoleDataPermission rp : roleDataPerms) {
                DataPermission dp = dataPermissionMapper.selectById(rp.getPermissionId());
                if (dp != null) {
                    addDataPermission(dp, allowedDataSourceIds, allowedGroupIds);
                }
            }
        }
        
        return new UserDataScope(allowedDataSourceIds, allowedGroupIds);
    }

    private void addDataPermission(DataPermission dp, Set<Long> allowedDataSourceIds, Set<Long> allowedGroupIds) {
        if ("DATASOURCE".equals(dp.getPermissionType())) {
            allowedDataSourceIds.add(dp.getTargetId());
        } else if ("REPORT_GROUP".equals(dp.getPermissionType())) {
            // 添加分组及其所有子分组
            Set<Long> groupTree = getGroupAndDescendants(dp.getTargetId());
            allowedGroupIds.addAll(groupTree);
        }
    }

    /**
     * 获取分组及其所有子分组ID
     */
    public Set<Long> getGroupAndDescendants(Long groupId) {
        Set<Long> result = new HashSet<>();
        result.add(groupId);
        
        // 递归获取子分组
        Queue<Long> queue = new LinkedList<>();
        queue.offer(groupId);
        
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            List<ReportGroup> children = reportGroupMapper.selectList(
                new LambdaQueryWrapper<ReportGroup>()
                    .eq(ReportGroup::getParentId, currentId)
                    .eq(ReportGroup::getDeleted, 0)
            );
            for (ReportGroup child : children) {
                result.add(child.getId());
                queue.offer(child.getId());
            }
        }
        
        return result;
    }

    /**
     * 分配直接数据权限给用户
     */
    public void assignDataPermissionToUser(Long userId, List<Long> permissionIds) {
        userDataPermissionMapper.delete(
            new LambdaQueryWrapper<UserDataPermission>().eq(UserDataPermission::getUserId, userId)
        );
        for (Long permId : permissionIds) {
            UserDataPermission udp = new UserDataPermission();
            udp.setUserId(userId);
            udp.setPermissionId(permId);
            userDataPermissionMapper.insert(udp);
        }
    }

    // ==================== 角色权限 ====================

    /**
     * 分配菜单权限给角色
     */
    public void assignMenuPermissionToRole(Long roleId, List<Long> permissionIds) {
        roleMenuPermissionMapper.delete(
            new LambdaQueryWrapper<RoleMenuPermission>().eq(RoleMenuPermission::getRoleId, roleId)
        );
        for (Long permId : permissionIds) {
            RoleMenuPermission rmp = new RoleMenuPermission();
            rmp.setRoleId(roleId);
            rmp.setPermissionId(permId);
            roleMenuPermissionMapper.insert(rmp);
        }
    }

    /**
     * 分配数据权限给角色
     */
    public void assignDataPermissionToRole(Long roleId, List<Long> permissionIds) {
        roleDataPermissionMapper.delete(
            new LambdaQueryWrapper<RoleDataPermission>().eq(RoleDataPermission::getRoleId, roleId)
        );
        for (Long permId : permissionIds) {
            RoleDataPermission rdp = new RoleDataPermission();
            rdp.setRoleId(roleId);
            rdp.setPermissionId(permId);
            roleDataPermissionMapper.insert(rdp);
        }
    }

    /**
     * 获取角色的菜单权限
     */
    public List<Long> getRoleMenuPermissionIds(Long roleId) {
        return roleMenuPermissionMapper.selectList(
            new LambdaQueryWrapper<RoleMenuPermission>().eq(RoleMenuPermission::getRoleId, roleId)
        ).stream().map(RoleMenuPermission::getPermissionId).collect(Collectors.toList());
    }

    /**
     * 获取角色的数据权限
     */
    public List<Long> getRoleDataPermissionIds(Long roleId) {
        return roleDataPermissionMapper.selectList(
            new LambdaQueryWrapper<RoleDataPermission>().eq(RoleDataPermission::getRoleId, roleId)
        ).stream().map(RoleDataPermission::getPermissionId).collect(Collectors.toList());
    }

    // ==================== 用户-分组关联 ====================

    /**
     * 分配用户到分组
     */
    public void assignUserToGroups(Long userId, List<Long> groupIds) {
        userGroupMapper.delete(
            new LambdaQueryWrapper<UserGroup>().eq(UserGroup::getUserId, userId)
        );
        for (Long groupId : groupIds) {
            UserGroup ug = new UserGroup();
            ug.setUserId(userId);
            ug.setGroupId(groupId);
            userGroupMapper.insert(ug);
        }
    }

    /**
     * 获取用户所在的分组ID列表
     */
    public Set<Long> getUserGroupIds(Long userId) {
        return userGroupMapper.selectList(
            new LambdaQueryWrapper<UserGroup>().eq(UserGroup::getUserId, userId)
        ).stream().map(UserGroup::getGroupId).collect(Collectors.toSet());
    }

    /**
     * 获取用户可访问的所有分组ID（含继承的父分组）
     */
    public Set<Long> getUserAccessibleGroupIds(Long userId) {
        Set<Long> directGroupIds = getUserGroupIds(userId);
        Set<Long> allGroupIds = new HashSet<>(directGroupIds);
        
        // 对于每个直接分组，添加其所有祖先分组
        for (Long groupId : directGroupIds) {
            Set<Long> ancestors = getGroupAncestors(groupId);
            allGroupIds.addAll(ancestors);
        }
        
        return allGroupIds;
    }

    /**
     * 获取分组的所有祖先ID
     */
    public Set<Long> getGroupAncestors(Long groupId) {
        Set<Long> ancestors = new HashSet<>();
        Long currentId = groupId;
        
        while (currentId != null && currentId != 0L) {
            ReportGroup group = reportGroupMapper.selectById(currentId);
            if (group == null || group.getParentId() == null || group.getParentId() == 0) {
                break;
            }
            ancestors.add(group.getParentId());
            currentId = group.getParentId();
        }
        
        return ancestors;
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取所有菜单权限
     */
    public List<MenuPermission> getAllMenuPermissions() {
        return menuPermissionMapper.selectList(
            new LambdaQueryWrapper<MenuPermission>().eq(MenuPermission::getDeleted, 0)
        );
    }

    /**
     * 获取所有数据权限
     */
    public List<DataPermission> getAllDataPermissions() {
        return dataPermissionMapper.selectList(
            new LambdaQueryWrapper<DataPermission>().eq(DataPermission::getDeleted, 0)
        );
    }

    // ==================== 内部类 ====================

    /**
     * 用户数据范围
     */
    @Data
    public static class UserDataScope {
        private Set<Long> allowedDataSourceIds; // null 表示全部可访问
        private Set<Long> allowedGroupIds;       // null 表示全部可访问
        
        public UserDataScope(Set<Long> dataSourceIds, Set<Long> groupIds) {
            this.allowedDataSourceIds = dataSourceIds;
            this.allowedGroupIds = groupIds;
        }
        
        public boolean hasAccessToAll() {
            return allowedDataSourceIds == null && allowedGroupIds == null;
        }
        
        public boolean canAccessDataSource(Long dataSourceId) {
            if (hasAccessToAll()) return true;
            return allowedDataSourceIds != null && allowedDataSourceIds.contains(dataSourceId);
        }
        
        public boolean canAccessGroup(Long groupId) {
            if (hasAccessToAll()) return true;
            return allowedGroupIds != null && allowedGroupIds.contains(groupId);
        }
    }
}
