<template>
  <div class="reports-container">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span class="title">报表管理</span>
          <el-button type="primary" @click="handleCreate" v-if="userStore.hasPermission('menu:report:edit')">
            <el-icon><Plus /></el-icon> 新建报表
          </el-button>
        </div>
      </template>
      
      <div class="filter-section">
        <el-cascader
          v-model="filterGroupValues"
          :options="groupTreeData"
          :props="{ 
            multiple: true, 
            value: 'id', 
            label: 'name',
            checkStrictly: false,
            emitPath: false
          }"
          placeholder="按分组筛选"
          clearable
          collapse-tags
          collapse-tags-tooltip
          :max-collapse-tags="3"
          style="width: 280px; margin-right: 12px"
        />
        <el-input v-model="filterKeyword" placeholder="搜索报表名称" clearable style="width: 200px">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      
      <el-table :data="filteredReports" stripe v-loading="loading" class="mt-16">
        <el-table-column prop="name" label="报表名称" min-width="150">
          <template #default="{ row }">
            <el-link type="primary" @click="handleView(row)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="chartType" label="图表类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.chartType">{{ row.chartType }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="groupId" label="所属分组" width="150">
          <template #default="{ row }">
            {{ getGroupName(row.groupId) }}
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
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)" v-if="userStore.hasPermission('menu:report:edit')">编辑</el-button>
            <el-button type="primary" link @click="handleShare(row)">分享</el-button>
            <el-button type="danger" link @click="handleDelete(row)" v-if="userStore.hasPermission('menu:report:edit')">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑报表对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑报表' : '新建报表'" width="700px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="报表名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入报表名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="数据源" prop="dataSourceId">
          <el-select v-model="form.dataSourceId" placeholder="请选择数据源" style="width: 100%">
            <el-option v-for="ds in dataSources" :key="ds.id" :label="ds.name" :value="ds.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属分组">
          <el-cascader 
            v-model="form.groupId" 
            :options="groupTreeData" 
            :props="{ value: 'id', label: 'name', checkStrictly: true, emitPath: false }"
            placeholder="请选择分组（不选则属于根分组）" 
            style="width: 100%" 
            clearable
          />
        </el-form-item>
        <el-form-item label="图表类型">
          <el-select v-model="form.chartType" placeholder="请选择图表类型" style="width: 100%">
            <el-option label="表格" value="TABLE" />
            <el-option label="折线图" value="LINE" />
            <el-option label="柱状图" value="BAR" />
            <el-option label="饼图" value="PIE" />
          </el-select>
        </el-form-item>
        <el-form-item label="SQL查询" prop="sqlContent">
          <el-input 
            v-model="form.sqlContent" 
            type="textarea" 
            :rows="8" 
            placeholder="SELECT * FROM table_name WHERE &#36;{param}" 
            class="sql-input"
          />
        </el-form-item>
        
        <!-- 参数配置 -->
        <el-form-item label="参数配置">
          <div class="params-config">
            <div class="param-header">
              <span>参数名</span>
              <span>标签</span>
              <span>类型</span>
              <span>默认值</span>
              <span>必填</span>
              <span>操作</span>
            </div>
            <div v-for="(param, index) in form.params" :key="index" class="param-row">
              <el-input v-model="param.paramName" placeholder="如 status" />
              <el-input v-model="param.paramLabel" placeholder="如 状态" />
              <el-select v-model="param.paramType" style="width: 100px" @change="onParamTypeChange(param)">
                <el-option label="文本" value="TEXT" />
                <el-option label="下拉" value="SELECT" />
                <el-option label="日期" value="DATE" />
                <el-option label="数字" value="NUMBER" />
              </el-select>
              <el-input v-model="param.defaultValue" placeholder="默认值" />
              <el-checkbox v-model="param.required" />
              <el-button type="danger" link @click="removeParam(index)">删除</el-button>
            </div>
            <!-- 下拉选项配置（只显示SELECT类型的） -->
            <template v-for="(param, index) in form.params" :key="'opts-' + index">
              <div v-if="param.paramType === 'SELECT'" class="param-options">
                <span class="options-label">选项配置（【{{ param.paramName || '参数' + (index+1) }}】的下拉选项）：</span>
                <el-input 
                  v-model="param.optionsSql" 
                  type="textarea"
                  :rows="2"
                  placeholder="SQL：SELECT id, name FROM table_name，或直接填写选项如：1=启用,0=禁用"
                  style="width: 500px"
                />
              </div>
            </template>
            <el-button type="primary" link @click="addParam">+ 添加参数</el-button>
            <div class="param-tip">
              提示：SQL中引用参数请使用 &#36;{参数名}，如 &#36;{status}
            </div>
          </div>
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { api, useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const reports = ref([])
const dataSources = ref([])
const allGroups = ref([])
const filterGroupValues = ref([])
const filterKeyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  description: '',
  dataSourceId: null,
  groupId: null,
  chartType: 'TABLE',
  sqlContent: '',
  params: []
})

const rules = {
  name: [{ required: true, message: '请输入报表名称', trigger: 'blur' }],
  dataSourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  sqlContent: [{ required: true, message: '请输入SQL查询', trigger: 'blur' }]
}

// 构建分组树
const groupTreeData = computed(() => {
  const buildTree = (items, parentId = 0) => {
    return items
      .filter(item => item.parentId === parentId)
      .map(item => ({
        ...item,
        children: buildTree(items, item.id)
      }))
  }
  return buildTree(allGroups.value)
})

// 获取分组及其所有子分组ID
function getGroupAndDescendants(groupId) {
  const result = new Set([groupId])
  const queue = [groupId]
  
  while (queue.length > 0) {
    const currentId = queue.shift()
    const children = allGroups.value.filter(g => g.parentId === currentId)
    for (const child of children) {
      result.add(child.id)
      queue.push(child.id)
    }
  }
  
  return result
}

// 获取所有选中分组及其子分组ID
const allSelectedGroupIds = computed(() => {
  const ids = new Set()
  for (const groupId of filterGroupValues.value) {
    const descendants = getGroupAndDescendants(groupId)
    descendants.forEach(id => ids.add(id))
  }
  return ids
})

// 过滤报表
const filteredReports = computed(() => {
  let result = reports.value
  
  // 按分组过滤（包含子分组）
  if (allSelectedGroupIds.value.size > 0) {
    result = result.filter(r => r.groupId && allSelectedGroupIds.value.has(r.groupId))
  }
  
  // 按关键词过滤
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    result = result.filter(r => 
      r.name.toLowerCase().includes(kw) || 
      (r.description && r.description.toLowerCase().includes(kw))
    )
  }
  
  return result
})

onMounted(async () => {
  await fetchReports()
  await fetchDataSources()
  await fetchGroups()
})

async function fetchReports() {
  loading.value = true
  try {
    const res = await api.get('/report')
    reports.value = res.data || []
  } catch (e) {
    ElMessage.error('获取报表列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchDataSources() {
  try {
    const res = await api.get('/datasource')
    dataSources.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

async function fetchGroups() {
  try {
    const res = await api.get('/group/all')
    allGroups.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

function getGroupName(groupId) {
  if (!groupId) return '-'
  const group = allGroups.value.find(g => g.id === groupId)
  return group ? group.name : '-'
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, {
    id: null, name: '', description: '', dataSourceId: dataSources.value[0]?.id || null,
    groupId: null, chartType: 'TABLE', sqlContent: '', params: []
  })
  dialogVisible.value = true
}

function addParam() {
  form.params.push({
    paramName: '',
    paramLabel: '',
    paramType: 'TEXT',
    defaultValue: '',
    required: false,
    optionsSql: ''
  })
}

function onParamTypeChange(param) {
  // 当类型改变时，重置选项SQL
  if (param.paramType !== 'SELECT') {
    param.optionsSql = ''
  }
}

function removeParam(index) {
  form.params.splice(index, 1)
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    description: row.description,
    dataSourceId: row.dataSourceId,
    groupId: row.groupId,
    chartType: row.chartType,
    sqlContent: row.sqlContent,
    params: []
  })
  // 加载已有的参数定义
  loadReportParams(row.id)
  dialogVisible.value = true
}

async function loadReportParams(reportId) {
  try {
    const res = await api.get(`/report/${reportId}/params`)
    form.params = (res.data || []).map(p => ({
      paramName: p.paramName,
      paramLabel: p.paramLabel,
      paramType: p.paramType,
      defaultValue: p.defaultValue,
      required: p.required === 1,
      optionsSql: p.optionsSql || ''
    }))
  } catch (e) {
    console.error('加载参数失败', e)
  }
}

function handleView(row) {
  router.push(`/reports/${row.id}?chartType=${row.chartType || 'TABLE'}`)
}

function handleDelete(row) {
  ElMessageBox.confirm('确定删除该报表吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await api.delete(`/report/${row.id}`)
    ElMessage.success('删除成功')
    fetchReports()
  }).catch(() => {})
}

function handleShare(row) {
  ElMessageBox.prompt('请输入分享链接有效期（小时），留空表示永不过期', '创建分享链接', {
    inputValue: '24'
  }).then(async ({ value }) => {
    const params = {}
    if (value) {
      const expiresAt = new Date(Date.now() + parseInt(value) * 3600000).toISOString()
      params.expiresAt = expiresAt
    }
    const res = await api.post(`/report/${row.id}/share`, params)
    const url = `${window.location.origin}/report/share/${res.data}`
    ElMessageBox.alert(`分享链接：<a href="${url}" target="_blank">${url}</a>`, '分享链接', {
      dangerouslyUseHTMLString: true
    })
  }).catch(() => {})
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    // 如果 groupId 是嵌套数组（cascader），取最后一个值
    const groupId = Array.isArray(form.groupId) ? form.groupId[form.groupId.length - 1] : form.groupId
    
    let reportId = form.id
    
    if (isEdit.value) {
      await api.put(`/report/${form.id}`, { ...form, groupId })
    } else {
      const res = await api.post('/report', { ...form, groupId })
      reportId = res.data?.id
    }
    
    // 保存参数配置
    if (reportId && form.params.length > 0) {
      const paramsData = form.params.map(p => ({
        paramName: p.paramName,
        paramLabel: p.paramLabel,
        paramType: p.paramType || 'TEXT',
        defaultValue: p.defaultValue || '',
        required: p.required ? 1 : 0,
        optionsSql: p.optionsSql || ''
      }))
      await api.post(`/report/${reportId}/params`, paramsData)
    }
    
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchReports()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}
</script>

<style scoped>
.reports-container {
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

.filter-section {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.mt-16 {
  margin-top: 16px;
}

.sql-input :deep(textarea) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
}

.params-config {
  width: 100%;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.param-header {
  display: flex;
  gap: 8px;
  padding: 8px 0;
  font-size: 13px;
  color: #606266;
}

.param-header span {
  width: 100px;
}

.param-row {
  display: flex;
  gap: 8px;
  padding: 6px 0;
  align-items: center;
}

.param-row .el-input,
.param-row .el-select {
  width: 100px;
}

.param-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
