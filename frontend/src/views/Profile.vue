<template>
  <div class="profile-container">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span class="title">个人中心</span>
        </div>
      </template>
      
      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="info">
          <el-form :model="infoForm" label-width="100px" style="max-width: 500px">
            <el-form-item label="用户名">
              <el-input v-model="infoForm.username" disabled />
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input v-model="infoForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="infoForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdateInfo" :loading="infoLoading">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px" style="max-width: 500px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { api, useUserStore } from '../stores/user'

const userStore = useUserStore()
const activeTab = ref('info')
const infoLoading = ref(false)
const passwordLoading = ref(false)

const infoForm = reactive({
  username: '',
  realName: '',
  email: '',
  phone: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const passwordFormRef = ref(null)

onMounted(() => {
  // 加载当前用户信息
  infoForm.username = userStore.user?.username || ''
  infoForm.realName = userStore.user?.realName || ''
  infoForm.email = userStore.user?.email || ''
  infoForm.phone = userStore.user?.phone || ''
})

async function handleUpdateInfo() {
  infoLoading.value = true
  try {
    await api.put('/user/profile', {
      realName: infoForm.realName,
      email: infoForm.email,
      phone: infoForm.phone
    })
    // 更新本地存储的用户信息
    if (userStore.user) {
      userStore.user.realName = infoForm.realName
      userStore.user.email = infoForm.email
      userStore.user.phone = infoForm.phone
    }
    ElMessage.success('信息更新成功')
  } catch (e) {
    ElMessage.error(e.message || '更新失败')
  } finally {
    infoLoading.value = false
  }
}

async function handleChangePassword() {
  await passwordFormRef.value.validate()
  
  passwordLoading.value = true
  try {
    await api.put('/user/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    // 退出登录
    userStore.logout()
    window.location.href = '/login'
  } catch (e) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.profile-container {
  height: 100%;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 18px;
  font-weight: 600;
}
</style>
