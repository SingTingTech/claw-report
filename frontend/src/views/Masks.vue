<template>
  <div class="masks-container">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span class="title">脱敏规则管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon> 新建规则
          </el-button>
        </div>
      </template>
      
      <el-table :data="masks" stripe v-loading="loading" :row-key="row => row.id">
        <el-table-column prop="dataSourceName" label="数据源" width="150" />
        <el-table-column prop="tableName" label="表名" width="150" />
        <el-table-column prop="columnName" label="字段名" width="150" />
        <el-table-column prop="maskType" label="脱敏类型" width="150">
          <template #default="{ row }">
            <el-tag>{{ getMaskTypeName(row.maskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maskPattern" label="匹配模式" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.maskPattern || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="handleToggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑规则对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑脱敏规则' : '新建脱敏规则'" width="600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="数据源" prop="dataSourceId">
          <el-select v-model="form.dataSourceId" placeholder="请选择数据源" style="width: 100%" @change="handleDataSourceChange">
            <el-option v-for="ds in dataSources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="表名" prop="tableName">
          <el-select v-model="form.tableName" placeholder="请选择表" style="width: 100%" @change="handleTableChange" filterable allow-create>
            <el-option v-for="t in tables" :key="t.tableName" :label="t.tableName" :value="t.tableName" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="字段名" prop="columnName">
          <el-select v-model="form.columnName" placeholder="请选择字段" style="width: 100%" filterable allow-create>
            <el-option v-for="c in columns" :key="c.columnName" :label="c.columnName" :value="c.columnName" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="脱敏类型" prop="maskType">
          <el-select v-model="form.maskType" placeholder="请选择脱敏类型" style="width: 100%">
            <el-option label="手机号" value="PHONE" />
            <el-option label="身份证" value="ID_CARD" />
            <el-option label="邮箱" value="EMAIL" />
            <el-option label="金额" value="AMOUNT" />
            <el-option label="银行卡" value="BANK_CARD" />
            <el-option label="自定义正则" value="CUSTOM" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="匹配正则" v-if="form.maskType === 'CUSTOM'" prop="maskPattern">
          <el-input v-model="form.maskPattern" placeholder="请输入正则表达式，如 \d{4}" />
        </el-form-item>
        
        <el-form-item label="说明">
          <el-alert type="info" :closable="false" show-icon>
            所有配置的字段在查询结果中将显示为 ****（完全遮蔽）
          </el-alert>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '../stores/user'

const loading = ref(false)
const masks = ref([])
const dataSources = ref([])
const tables = ref([])
const columns = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  dataSourceId: null,
  tableName: '',
  columnName: '',
  maskType: 'PHONE',
  maskPattern: '',
  maskReplacement: '****',
  enabled: 1
})

const rules = {
  dataSourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  tableName: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  columnName: [{ required: true, message: '请输入字段名', trigger: 'blur' }],
  maskType: [{ required: true, message: '请选择脱敏类型', trigger: 'change' }]
}

const maskTypeNames = {
  PHONE: '手机号',
  ID_CARD: '身份证',
  EMAIL: '邮箱',
  AMOUNT: '金额',
  BANK_CARD: '银行卡',
  CUSTOM: '自定义'
}

function getMaskTypeName(type) {
  return maskTypeNames[type] || type
}

onMounted(async () => {
  await fetchDataSources()
  await fetchMasks()
})

async function fetchDataSources() {
  try {
    const res = await api.get('/datasource')
    dataSources.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

async function fetchMasks() {
  loading.value = true
  try {
    // 获取所有数据源的脱敏规则
    const allMasks = []
    for (const ds of dataSources.value) {
      try {
        const res = await api.get(`/mask/datasource/${ds.id}`)
        const dsMasks = res.data || []
        dsMasks.forEach(m => m.dataSourceName = ds.name)
        allMasks.push(...dsMasks)
      } catch (e) {}
    }
    masks.value = allMasks
  } catch (e) {
    ElMessage.error('获取脱敏规则失败')
  } finally {
    loading.value = false
  }
}

async function handleDataSourceChange(dsId) {
  if (!dsId) return
  try {
    const ds = dataSources.value.find(d => d.id === dsId)
    const res = await api.get(`/datasource/${dsId}/databases`)
    // 获取第一个数据库的表
    if (res.data && res.data.length > 0) {
      const tablesRes = await api.get(`/datasource/${dsId}/tables?database=${res.data[0]}`)
      tables.value = tablesRes.data || []
    }
  } catch (e) {
    console.error(e)
  }
}

async function handleTableChange(tableName) {
  if (!tableName || !form.dataSourceId) return
  try {
    const ds = dataSources.value.find(d => d.id === form.dataSourceId)
    const res = await api.get(`/datasource/${form.dataSourceId}/columns?database=${ds.databaseName}&table=${tableName}`)
    columns.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, {
    id: null, dataSourceId: null, tableName: '', columnName: '',
    maskType: 'PHONE', maskPattern: '', maskReplacement: '****', enabled: 1
  })
  tables.value = []
  columns.value = []
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    dataSourceId: row.dataSourceId,
    tableName: row.tableName,
    columnName: row.columnName,
    maskType: row.maskType,
    maskPattern: row.maskPattern,
    maskReplacement: row.maskReplacement,
    enabled: row.enabled
  })
  dialogVisible.value = true
}

async function handleToggle(row) {
  try {
    await api.put(`/mask/${row.id}`, { enabled: row.enabled ? 1 : 0 })
    ElMessage.success('更新成功')
  } catch (e) {
    ElMessage.error('更新失败')
    row.enabled = !row.enabled
  }
}

function handleDelete(row) {
  ElMessageBox.confirm('确定删除该脱敏规则吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await api.delete(`/mask/${row.id}`)
    ElMessage.success('删除成功')
    fetchMasks()
  }).catch(() => {})
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (isEdit.value) {
      await api.put(`/mask/${form.id}`, form)
    } else {
      await api.post('/mask', form)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchMasks()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}
</script>

<style scoped>
.masks-container {
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
