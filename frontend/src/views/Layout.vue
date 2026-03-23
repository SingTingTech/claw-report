<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="aside">
      <div class="logo">
        <h3>ReportHub</h3>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/reports">
          <el-icon><Document /></el-icon>
          <span>报表管理</span>
        </el-menu-item>
        
        <el-menu-item index="/query" v-if="hasPermission('menu:query:execute')">
          <el-icon><Monitor /></el-icon>
          <span>SQL查询</span>
        </el-menu-item>
        
        <el-menu-item index="/datasources" v-if="hasPermission('menu:datasource:view')">
          <el-icon><Connection /></el-icon>
          <span>数据源</span>
        </el-menu-item>
        
        <el-menu-item index="/groups" v-if="hasPermission('menu:group:edit')">
          <el-icon><FolderOpened /></el-icon>
          <span>报表分组</span>
        </el-menu-item>
        
        <el-menu-item index="/masks" v-if="hasPermission('menu:mask:edit')">
          <el-icon><Lock /></el-icon>
          <span>脱敏规则</span>
        </el-menu-item>
        
        <el-menu-item index="/users" v-if="hasPermission('menu:user:view')">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        
        <el-menu-item index="/roles" v-if="hasPermission('menu:role:view')">
          <el-icon><Avatar /></el-icon>
          <span>角色管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.name !== 'Reports'">{{ route.name }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.userInfo?.username || '加载中...' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import {
  Document, Monitor, Connection, FolderOpened,
  Lock, User, Avatar, UserFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

// 安全检查权限
function hasPermission(permission) {
  if (!userStore.token) return false
  // ADMIN角色拥有所有权限
  if (userStore.permissions.includes('menu:user:edit')) return true
  return userStore.permissions.includes(permission)
}

onMounted(async () => {
  // 如果有token但未初始化权限，先初始化
  if (userStore.token && !userStore.initialized) {
    await userStore.init()
  }
})

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #304156;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #263445;
}

.logo h3 {
  color: #fff;
  font-size: 20px;
}

.menu {
  border-right: none;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
