<template>
  <div class="datasources-container">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span class="title">数据源管理</span>
          <el-button type="primary" @click="handleCreate" v-if="userStore.hasPermission('menu:datasource:edit')">
            <el-icon><Plus /></el-icon> 新建数据源
          </el-button>
        </div>
      </template>
      
      <el-table :data="dataSources" stripe v-loading="loading">
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="dsType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.dsType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="连接信息" min-width="200">
          <template #default="{ row }">
            {{ row.host }}:{{ row.port }}/{{ row.databaseName }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleTest(row)">测试</el-button>
            <el-button type="primary" link @click="goToDictionary(row)">数据字典</el-button>
            <el-button type="primary" link @click="handleEdit(row)" v-if="userStore.hasPermission('menu:datasource:edit')">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)" v-if="userStore.hasPermission('menu:datasource:edit')">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑数据源对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑数据源' : '新建数据源'" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入数据源名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="类型" prop="dsType">
          <el-select v-model="form.dsType" placeholder="请选择类型" style="width: 100%">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="PostgreSQL" value="POSTGRESQL" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机" prop="host">
          <el-input v-model="form.host" placeholder="localhost 或 IP 地址" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="数据库名" prop="databaseName">
          <el-input v-model="form.databaseName" placeholder="请输入数据库名" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="handleTestConnection">测试连接</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api, useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const dataSources = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  description: '',
  dsType: 'MYSQL',
  host: 'localhost',
  port: 3306,
  databaseName: '',
  username: '',
  password: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  dsType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  databaseName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

onMounted(async () => {
  await fetchDataSources()
})

async function fetchDataSources() {
  loading.value = true
  try {
    const res = await api.get('/datasource')
    dataSources.value = res.data || []
  } catch (e) {
    ElMessage.error('获取数据源列表失败')
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, {
    id: null, name: '', description: '', dsType: 'MYSQL',
    host: 'localhost', port: 3306, databaseName: '', username: '', password: ''
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    description: row.description,
    dsType: row.dsType,
    host: row.host,
    port: row.port,
    databaseName: row.databaseName,
    username: row.username,
    password: '' // 不返回密码
  })
  dialogVisible.value = true
}

function handleDelete(row) {
  ElMessageBox.confirm('确定删除该数据源吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await api.delete(`/datasource/${row.id}`)
    ElMessage.success('删除成功')
    fetchDataSources()
  }).catch(() => {})
}

async function handleTest(row) {
  try {
    await api.post(`/datasource/${row.id}/test`, row)
    ElMessage.success('连接成功')
  } catch (e) {
    ElMessage.error('连接失败: ' + e.message)
  }
}

function goToDictionary(row) {
  router.push(`/datasources/${row.id}/dictionary`)
}

async function handleTestConnection() {
  try {
    await api.post('/datasource/0/test', form)
    ElMessage.success('连接成功')
  } catch (e) {
    ElMessage.error('连接失败: ' + e.message)
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (isEdit.value) {
      await api.put(`/datasource/${form.id}`, form)
    } else {
      await api.post('/datasource', form)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchDataSources()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}
</script>

<style scoped>
.datasources-container {
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
