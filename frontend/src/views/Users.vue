<template>
  <div class="users-container">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span class="title">用户管理</span>
          <el-button type="primary" @click="handleCreate" v-if="userStore.hasPermission('menu:user:edit')">
            <el-icon><Plus /></el-icon> 新建用户
          </el-button>
        </div>
      </template>
      
      <!-- 批量操作栏 -->
      <div class="batch-actions">
        <span class="selected-count">已选择 {{ selectedUsers.length }} 个用户</span>
        <el-button type="primary" @click="handleBatchAssignRoles" :disabled="selectedUsers.length === 0">批量分配角色</el-button>
        <el-button type="primary" @click="handleBatchAssignGroups" :disabled="selectedUsers.length === 0">批量分配分组</el-button>
        <el-button type="danger" @click="handleBatchDelete" :disabled="selectedUsers.length === 0">批量删除</el-button>
      </div>
      
      <el-table 
        :data="users" 
        stripe 
        v-loading="loading" 
        :cell-style="{ whiteSpace: 'nowrap' }"
        @selection-change="handleSelectionChange"
        ref="tableRef"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              active-text="正常"
              inactive-text="禁用"
              inline-prompt
              @change="handleToggleStatus(row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)" v-if="userStore.hasPermission('menu:user:edit')">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)" v-if="userStore.hasPermission('menu:user:edit')">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchUsers"
        />
      </div>
    </el-card>

    <!-- 创建/编辑用户对话框 -->
    <el-dialog v-model="userDialogVisible" :title="isEdit ? '编辑用户' : '新建用户'" width="500px">
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="userForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
          <el-input v-model="userForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUserSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="500px">
      <el-checkbox-group v-model="selectedRoleIds">
        <el-checkbox v-for="role in allRoles" :key="role.id" :label="role.id">
          {{ role.roleName }} ({{ role.roleCode }})
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRoles">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配分组对话框 -->
    <el-dialog v-model="groupDialogVisible" title="分配分组" width="500px">
      <el-tree
        ref="groupTreeRef"
        :data="groupOptions"
        :props="groupProps"
        node-key="id"
        show-checkbox
        default-expand-all
      />
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveGroups">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限对话框 -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="700px">
      <el-tabs>
        <el-tab-pane label="菜单权限">
          <el-checkbox-group v-model="selectedMenuPermIds">
            <el-checkbox v-for="perm in allMenuPerms" :key="perm.id" :label="perm.id" style="width: 200px">
              {{ perm.permissionName }}
            </el-checkbox>
          </el-checkbox-group>
        </el-tab-pane>
        <el-tab-pane label="数据权限">
          <el-checkbox-group v-model="selectedDataPermIds">
            <el-checkbox v-for="perm in allDataPerms" :key="perm.id" :label="perm.id" style="width: 200px">
              {{ perm.permissionName }}
            </el-checkbox>
          </el-checkbox-group>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermissions">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api, useUserStore } from '../stores/user'

const userStore = useUserStore()

const loading = ref(false)
const users = ref([])
const selectedUsers = ref([])
const tableRef = ref(null)
const allRoles = ref([])
const groups = ref([])
const allMenuPerms = ref([])
const allDataPerms = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const userDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const groupDialogVisible = ref(false)
const permDialogVisible = ref(false)
const isEdit = ref(false)
const userFormRef = ref(null)
const groupTreeRef = ref(null)

const selectedUserId = ref(null)
const selectedRoleIds = ref([])
const selectedMenuPermIds = ref([])
const selectedDataPermIds = ref([])

const userForm = reactive({
  id: null,
  username: '',
  realName: '',
  email: '',
  phone: '',
  password: ''
})

const userRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const groupProps = { label: 'name', children: 'children', value: 'id', checkStrictly: true }

const groupOptions = computed(() => {
  const buildTree = (items, parentId = 0) => {
    return items.filter(item => item.parentId === parentId).map(item => ({
      ...item,
      children: buildTree(items, item.id)
    }))
  }
  return buildTree(groups.value)
})

onMounted(async () => {
  await fetchUsers()
  await fetchRoles()
  await fetchGroups()
  await fetchPermissions()
})

async function fetchUsers() {
  loading.value = true
  try {
    const res = await api.get('/user', { params: { page: currentPage.value, pageSize: pageSize.value } })
    // 过滤掉超级管理员
    users.value = (res.data?.records || []).filter(u => u.username !== 'admin')
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchRoles() {
  try {
    const res = await api.get('/role')
    allRoles.value = res.data || []
  } catch (e) {}
}

async function fetchGroups() {
  try {
    const res = await api.get('/group/all')
    groups.value = res.data || []
  } catch (e) {}
}

async function fetchPermissions() {
  try {
    const menuRes = await api.get('/role/permissions')
    allMenuPerms.value = menuRes.data || []
    const dataRes = await api.get('/role/data-permissions')
    allDataPerms.value = dataRes.data || []
  } catch (e) {}
}

function handleCreate() {
  isEdit.value = false
  Object.assign(userForm, { id: null, username: '', realName: '', email: '', phone: '', password: '' })
  userDialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(userForm, {
    id: row.id, username: row.username, realName: row.realName,
    email: row.email, phone: row.phone, password: ''
  })
  userDialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' })
    .then(async () => {
      await api.delete(`/user/${row.id}`)
      ElMessage.success('删除成功')
      fetchUsers()
    }).catch(() => {})
}

function handleSelectionChange(selection) {
  selectedUsers.value = selection
}

function handleBatchAssignRoles() {
  if (selectedUsers.value.length === 0) return
  const firstUser = selectedUsers.value[0]
  selectedUserId.value = firstUser.id
  selectedRoleIds.value = selectedUsers.value.map(u => u.id)
  roleDialogVisible.value = true
}

function handleBatchAssignGroups() {
  if (selectedUsers.value.length === 0) return
  const firstUser = selectedUsers.value[0]
  selectedUserId.value = firstUser.id
  groupDialogVisible.value = true
}

function handleBatchDelete() {
  if (selectedUsers.value.length === 0) return
  const usernames = selectedUsers.value.map(u => u.username).join(', ')
  ElMessageBox.confirm(`确定删除选中的 ${selectedUsers.value.length} 个用户吗？\n${usernames}`, '提示', { type: 'warning' })
    .then(async () => {
      for (const user of selectedUsers.value) {
        await api.delete(`/user/${user.id}`)
      }
      ElMessage.success('删除成功')
      selectedUsers.value = []
      fetchUsers()
    }).catch(() => {})
}

async function handleToggleStatus(row, newStatus) {
  try {
    await api.put(`/user/${row.id}`, { ...row, status: newStatus })
    ElMessage.success(`${newStatus === 1 ? '启用' : '禁用'}成功`)
    fetchUsers()
  } catch (e) {
    ElMessage.error('操作失败')
    fetchUsers()
  }
}

async function handleUserSubmit() {
  await userFormRef.value.validate()
  try {
    if (isEdit.value) {
      await api.put(`/user/${userForm.id}`, userForm)
    } else {
      await api.post('/user', userForm)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    userDialogVisible.value = false
    fetchUsers()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleAssignRoles(row) {
  selectedUserId.value = row.id
  selectedRoleIds.value = []
  try {
    const res = await api.get(`/user/${row.id}/roles`)
    selectedRoleIds.value = (res.data || []).map(r => r.id)
    roleDialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取用户角色失败')
  }
}

async function handleSaveRoles() {
  try {
    await api.post(`/user/${selectedUserId.value}/roles`, { roleIds: selectedRoleIds.value })
    ElMessage.success('分配成功')
    roleDialogVisible.value = false
  } catch (e) {
    ElMessage.error('分配失败')
  }
}

async function handleAssignGroups(row) {
  selectedUserId.value = row.id
  try {
    const res = await api.get(`/user/${row.id}/permissions`)
    const groupIds = res.data?.groupIds || []
    groupTreeRef.value?.setCheckedKeys(groupIds)
    groupDialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取用户分组失败')
  }
}

async function handleSaveGroups() {
  const checkedKeys = groupTreeRef.value?.getCheckedKeys() || []
  try {
    await api.post(`/user/${selectedUserId.value}/groups`, { groupIds: checkedKeys })
    ElMessage.success('分配成功')
    groupDialogVisible.value = false
  } catch (e) {
    ElMessage.error('分配失败')
  }
}

async function handleAssignPermissions(row) {
  selectedUserId.value = row.id
  selectedMenuPermIds.value = []
  selectedDataPermIds.value = []
  try {
    const res = await api.get(`/user/${row.id}/permissions`)
    selectedMenuPermIds.value = []
    selectedDataPermIds.value = []
    permDialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取用户权限失败')
  }
}

async function handleSavePermissions() {
  try {
    await api.post(`/user/${selectedUserId.value}/menu-permissions`, { permissionIds: selectedMenuPermIds.value })
    await api.post(`/user/${selectedUserId.value}/data-permissions`, { permissionIds: selectedDataPermIds.value })
    ElMessage.success('分配成功')
    permDialogVisible.value = false
  } catch (e) {
    ElMessage.error('分配失败')
  }
}
</script>

<style scoped>
.users-container {
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

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.batch-actions {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.selected-count {
  color: #606266;
  font-size: 14px;
  margin-right: 8px;
}
</style>
