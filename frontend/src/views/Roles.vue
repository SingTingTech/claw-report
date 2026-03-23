<template>
  <div class="roles-container">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span class="title">角色管理</span>
          <el-button type="primary" @click="handleCreate" v-if="userStore.hasPermission('menu:role:edit')">
            <el-icon><Plus /></el-icon> 新建角色
          </el-button>
        </div>
      </template>
      
      <el-table :data="roles" stripe v-loading="loading">
        <el-table-column prop="roleCode" label="角色代码" width="150" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleAssignPermissions(row)">分配权限</el-button>
            <el-button type="primary" link @click="handleEdit(row)" v-if="userStore.hasPermission('menu:role:edit')">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)" v-if="userStore.hasPermission('menu:role:edit') && row.roleCode !== 'ADMIN'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑角色对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新建角色'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="角色代码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="如: CUSTOM_ROLE" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限对话框 -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="800px">
      <el-tabs>
        <el-tab-pane label="菜单权限">
          <div class="perm-grid">
            <el-checkbox v-for="perm in menuPerms" :key="perm.id" v-model="selectedMenuPermIds" :label="perm.id">
              {{ perm.permissionName }}
              <span class="perm-code">({{ perm.permissionCode }})</span>
            </el-checkbox>
          </div>
        </el-tab-pane>
        <el-tab-pane label="数据权限">
          <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px">
            数据权限在创建数据源/分组时自动生成
          </el-alert>
          <div class="perm-grid" v-if="dataPerms.length > 0">
            <el-checkbox v-for="perm in dataPerms" :key="perm.id" v-model="selectedDataPermIds" :label="perm.id">
              {{ perm.permissionName }}
              <span class="perm-code">({{ perm.permissionType }})</span>
            </el-checkbox>
          </div>
          <el-empty v-else description="暂无数据权限" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api, useUserStore } from '../stores/user'

const userStore = useUserStore()

const loading = ref(false)
const roles = ref([])
const menuPerms = ref([])
const dataPerms = ref([])
const dialogVisible = ref(false)
const permDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const selectedRoleId = ref(null)
const selectedMenuPermIds = ref([])
const selectedDataPermIds = ref([])

const form = reactive({
  id: null,
  roleCode: '',
  roleName: '',
  description: ''
})

const rules = {
  roleCode: [{ required: true, message: '请输入角色代码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

onMounted(async () => {
  await fetchRoles()
  await fetchPermissions()
})

async function fetchRoles() {
  loading.value = true
  try {
    const res = await api.get('/role')
    roles.value = res.data || []
  } catch (e) {
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchPermissions() {
  try {
    const menuRes = await api.get('/role/permissions')
    menuPerms.value = menuRes.data || []
    const dataRes = await api.get('/role/data-permissions')
    dataPerms.value = dataRes.data || []
  } catch (e) {}
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, { id: null, roleCode: '', roleName: '', description: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, { id: row.id, roleCode: row.roleCode, roleName: row.roleName, description: row.description })
  dialogVisible.value = true
}

function handleDelete(row) {
  if (row.roleCode === 'ADMIN') {
    ElMessage.warning('不能删除管理员角色')
    return
  }
  ElMessageBox.confirm('确定删除该角色吗？', '提示', { type: 'warning' })
    .then(async () => {
      await api.delete(`/role/${row.id}`)
      ElMessage.success('删除成功')
      fetchRoles()
    }).catch(() => {})
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (isEdit.value) {
      await api.put(`/role/${form.id}`, form)
    } else {
      await api.post('/role', form)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchRoles()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleAssignPermissions(row) {
  selectedRoleId.value = row.id
  selectedMenuPermIds.value = []
  selectedDataPermIds.value = []
  try {
    const res = await api.get(`/role/${row.id}/permissions`)
    selectedMenuPermIds.value = res.data?.menuPermissionIds || []
    selectedDataPermIds.value = res.data?.dataPermissionIds || []
    permDialogVisible.value = true
  } catch (e) {
    ElMessage.error('获取角色权限失败')
  }
}

async function handleSavePermissions() {
  try {
    await api.post(`/role/${selectedRoleId.value}/menu-permissions`, { permissionIds: selectedMenuPermIds.value })
    await api.post(`/role/${selectedRoleId.value}/data-permissions`, { permissionIds: selectedDataPermIds.value })
    ElMessage.success('分配成功')
    permDialogVisible.value = false
  } catch (e) {
    ElMessage.error('分配失败')
  }
}
</script>

<style scoped>
.roles-container {
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

.perm-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.perm-code {
  color: #909399;
  font-size: 12px;
}
</style>
