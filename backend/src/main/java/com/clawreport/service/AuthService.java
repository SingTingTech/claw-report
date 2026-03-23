package com.clawreport.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clawreport.entity.User;
import com.clawreport.entity.UserRole;
import com.clawreport.mapper.UserMapper;
import com.clawreport.mapper.UserRoleMapper;
import com.clawreport.security.JwtTokenProvider;
import com.clawreport.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserRoleMapper userRoleMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PermissionService permissionService;

    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username).eq(User::getStatus, 1)
        );
        
        if (user == null) {
            throw new RuntimeException("用户不存在或已被禁用");
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 获取用户菜单权限
        Set<String> menuPermissions = permissionService.getUserMenuPermissions(user.getId());
        
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), menuPermissions);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        
        // 获取用户角色
        List<UserRole> roles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId())
        );
        if (!roles.isEmpty()) {
            result.put("roleIds", roles.stream().map(UserRole::getRoleId).toList());
        }
        
        return result;
    }

    public LoginUser getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return new LoginUser(user.getId(), user.getUsername());
    }

    public void register(User user) {
        // 检查用户名是否存在
        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())
        );
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);
    }
}
