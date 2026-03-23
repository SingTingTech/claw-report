<template>
  <div class="query-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="currentDataSource" placeholder="请选择数据源" style="width: 200px" @change="handleDataSourceChange">
          <el-option v-for="ds in dataSources" :key="ds.id" :label="ds.name" :value="ds.id" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button @click="handleSave" :disabled="!sql.trim()">
          <el-icon><Document /></el-icon> 保存查询
        </el-button>
        <el-dropdown @command="handleLoadQuery" trigger="click">
          <el-button>
            <el-icon><FolderOpened /></el-icon> 加载查询
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="q in savedQueries" :key="q.id" :command="q.id">
                {{ q.name }}
              </el-dropdown-item>
              <el-dropdown-item v-if="savedQueries.length === 0" disabled>暂无保存的查询</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button @click="handleClear">
          <el-icon><Delete /></el-icon> 清空
        </el-button>
      </div>
    </div>

    <!-- 可拖拽分割区域 -->
    <div class="split-container" ref="splitContainer">
      <!-- SQL 编辑器 -->
      <div class="editor-pane" :style="{ height: editorHeight + 'px' }">
        <div class="pane-header">
          <span>SQL 编辑器</span>
          <el-button type="primary" size="small" @click="handleExecute" :loading="executing">
            <el-icon><VideoPlay /></el-icon> 执行
          </el-button>
        </div>
        <div class="editor-content">
          <el-input
            v-model="sql"
            type="textarea"
            :rows="8"
            placeholder="请输入 SQL 查询语句，支持 ${param} 参数占位符"
            class="sql-textarea"
          />
        </div>
        <!-- 参数区域 -->
        <div class="params-section" v-if="params.length > 0">
          <div class="params-title">
            <span>查询参数</span>
            <el-button type="primary" link size="small" @click="showParamConfigDialog">
              <el-icon><Setting /></el-icon> 配置参数
            </el-button>
          </div>
          <el-row :gutter="16">
            <el-col :span="8" v-for="param in params" :key="param.name">
              <el-input 
                v-if="param.type === 'TEXT' || !param.type" 
                v-model="paramValues[param.name]" 
                :placeholder="param.label || param.name"
              >
                <template #prepend>{{ param.label || param.name }}</template>
              </el-input>
              <el-select 
                v-else-if="param.type === 'SELECT'" 
                v-model="paramValues[param.name]" 
                :placeholder="param.label || param.name"
                style="width: 100%"
              >
                <el-option 
                  v-for="opt in (param.options || [])" 
                  :key="opt.value" 
                  :label="opt.label" 
                  :value="opt.value" 
                />
              </el-select>
              <el-date-picker
                v-else-if="param.type === 'DATE'"
                v-model="paramValues[param.name]"
                type="date"
                :placeholder="param.label || param.name"
                style="width: 100%"
              />
              <el-input-number
                v-else-if="param.type === 'NUMBER'"
                v-model="paramValues[param.name]"
                :min="0"
                style="width: 100%"
              />
              <el-input v-else v-model="paramValues[param.name]" :placeholder="param.label || param.name">
                <template #prepend>{{ param.label || param.name }}</template>
              </el-input>
            </el-col>
          </el-row>
        </div>
      </div>

      <!-- 拖拽分隔条 -->
      <div class="splitter" @mousedown="startDrag"></div>

      <!-- 结果区域 -->
      <div class="result-pane" :style="{ height: 'calc(100% - ' + editorHeight + 'px)' }">
        <div class="pane-header">
          <span>查询结果</span>
          <span v-if="result" class="result-info">
            共 {{ result.total }} 条，耗时 {{ result.executeTime }}ms
          </span>
        </div>
        <div class="result-content">
          <el-table 
            :data="result?.records || []" 
            stripe 
            border 
            height="100%"
            v-if="result && result.records.length > 0"
          >
            <el-table-column 
              v-for="col in result.columns" 
              :key="col.name" 
              :prop="col.name" 
              :label="col.label" 
              min-width="150" 
              show-overflow-tooltip 
            />
          </el-table>
          
          <el-empty v-else-if="!executing && !error" description="请执行查询以查看结果" />
          
          <div v-if="error" class="error-message">
            <el-alert type="error" :title="error" show-icon :closable="false" />
          </div>
        </div>
        
        <!-- 分页 -->
        <div class="pagination" v-if="result && result.total > 0">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="result.total"
            layout="total, prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 保存查询对话框 -->
    <el-dialog v-model="saveDialogVisible" title="保存查询" width="500px">
      <el-form :model="saveForm" :rules="saveRules" ref="saveFormRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="saveForm.name" placeholder="请输入查询名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="saveForm.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveConfirm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 参数配置对话框 -->
    <el-dialog v-model="paramConfigVisible" title="配置查询参数" width="700px">
      <el-table :data="params" border size="small">
        <el-table-column prop="name" label="参数名" width="100" />
        <el-table-column prop="label" label="标签" width="100">
          <template #default="{ row }">
            <el-input v-model="row.label" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-select v-model="row.type" size="small" style="width: 80px">
              <el-option label="文本" value="TEXT" />
              <el-option label="下拉" value="SELECT" />
              <el-option label="日期" value="DATE" />
              <el-option label="数字" value="NUMBER" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="defaultValue" label="默认值" width="120">
          <template #default="{ row }">
            <el-input v-model="row.defaultValue" size="small" placeholder="默认值" />
          </template>
        </el-table-column>
        <el-table-column prop="options" label="选项配置">
          <template #default="{ row }">
            <el-input 
              v-if="row.type === 'SELECT'" 
              v-model="row.optionsSql" 
              size="small" 
              placeholder="SQL或：1=启用,0=禁用"
            />
            <span v-else style="color: #909399; font-size: 12px">-</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="paramConfigVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, FolderOpened, Delete, VideoPlay, Setting } from '@element-plus/icons-vue'
import { api } from '../stores/user'

const dataSources = ref([])
const currentDataSource = ref(null)
const sql = ref('')
const executing = ref(false)
const result = ref(null)
const error = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const savedQueries = ref([])
const params = ref([])
const paramValues = reactive({})
const splitContainer = ref(null)
const editorHeight = ref(280)
const isDragging = ref(false)

const saveDialogVisible = ref(false)
const paramConfigVisible = ref(false)
const saveFormRef = ref(null)
const saveForm = reactive({
  name: '',
  description: ''
})
const saveRules = {
  name: [{ required: true, message: '请输入查询名称', trigger: 'blur' }]
}

// 监听SQL变化，提取参数
watch(sql, (newSql) => {
  extractParams(newSql)
})

onMounted(async () => {
  await fetchDataSources()
  await fetchSavedQueries()
})

async function fetchDataSources() {
  try {
    const res = await api.get('/datasource')
    dataSources.value = res.data || []
    if (dataSources.value.length > 0) {
      currentDataSource.value = dataSources.value[0].id
    }
  } catch (e) {
    console.error(e)
  }
}

async function fetchSavedQueries() {
  try {
    const res = await api.get('/report')
    // 复用报表接口获取已保存的查询
    savedQueries.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

function extractParams(sqlText) {
  if (!sqlText) {
    params.value = []
    return
  }
  const regex = /\$\{(\w+)\}/g
  const found = []
  let match
  while ((match = regex.exec(sqlText)) !== null) {
    if (!found.find(p => p.name === match[1])) {
      found.push({ 
        name: match[1], 
        label: match[1],
        type: 'TEXT',
        defaultValue: '',
        options: [],
        optionsSql: ''
      })
    }
  }
  params.value = found
  
  // 初始化 paramValues
  params.value.forEach(p => {
    if (!(p.name in paramValues) && p.defaultValue) {
      paramValues[p.name] = p.defaultValue
    }
  })
}

function handleDataSourceChange() {
  // 切换数据源时清空结果
  result.value = null
  error.value = ''
}

async function handleExecute() {
  if (!currentDataSource.value) {
    ElMessage.warning('请选择数据源')
    return
  }
  if (!sql.value.trim()) {
    ElMessage.warning('请输入 SQL 语句')
    return
  }
  
  executing.value = true
  error.value = ''
  result.value = null
  
  try {
    const res = await api.post('/query/execute', {
      dataSourceId: currentDataSource.value,
      sql: sql.value,
      params: paramValues,
      page: currentPage.value,
      pageSize: pageSize.value
    })
    
    if (res.code === 200) {
      result.value = res.data
      if (res.data.records.length === 0) {
        ElMessage.info('查询成功，无数据返回')
      }
    } else {
      error.value = res.message || '查询失败'
    }
  } catch (e) {
    error.value = e.message || '查询失败'
  } finally {
    executing.value = false
  }
}

async function handlePageChange(page) {
  currentPage.value = page
  await handleExecute()
}

function handleClear() {
  sql.value = ''
  result.value = null
  error.value = ''
  Object.keys(paramValues).forEach(key => delete paramValues[key])
}

function handleSave() {
  if (!sql.value.trim()) {
    ElMessage.warning('请先输入SQL语句')
    return
  }
  saveForm.name = ''
  saveForm.description = ''
  saveDialogVisible.value = true
}

function showParamConfigDialog() {
  paramConfigVisible.value = true
}

async function handleSaveConfirm() {
  await saveFormRef.value.validate()
  
  try {
    await api.post('/report', {
      name: saveForm.name,
      description: saveForm.description,
      dataSourceId: currentDataSource.value,
      sqlContent: sql.value,
      chartType: 'TABLE'
    })
    ElMessage.success('保存成功')
    saveDialogVisible.value = false
    await fetchSavedQueries()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleLoadQuery(queryId) {
  try {
    const res = await api.get(`/report/${queryId}`)
    const report = res.data
    sql.value = report.sqlContent
    currentDataSource.value = report.dataSourceId
    currentPage.value = 1
    result.value = null
    error.value = ''
    ElMessage.success('已加载查询')
  } catch (e) {
    ElMessage.error('加载查询失败')
  }
}

// 拖拽功能
function startDrag(e) {
  isDragging.value = true
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  e.preventDefault()
}

function onDrag(e) {
  if (!isDragging.value || !splitContainer.value) return
  const rect = splitContainer.value.getBoundingClientRect()
  const newHeight = e.clientY - rect.top
  if (newHeight >= 150 && newHeight <= rect.height - 150) {
    editorHeight.value = newHeight
  }
}

function stopDrag() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}
</script>

<style scoped>
.query-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.split-container {
  flex: 1;
  position: relative;
  background: #fff;
}

.editor-pane {
  display: flex;
  flex-direction: column;
  border-bottom: 1px solid #e6e6e6;
}

.pane-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e6e6e6;
  font-weight: 500;
}

.result-info {
  color: #909399;
  font-size: 12px;
  font-weight: normal;
}

.editor-content {
  flex: 1;
  padding: 16px;
  overflow: auto;
}

.sql-textarea :deep(textarea) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
}

.params-section {
  padding: 12px 16px;
  background: #fafafa;
  border-top: 1px solid #e6e6e6;
}

.params-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.splitter {
  height: 6px;
  background: #e6e6e6;
  cursor: row-resize;
  position: relative;
}

.splitter:hover {
  background: #409eff;
}

.result-pane {
  display: flex;
  flex-direction: column;
}

.result-content {
  flex: 1;
  padding: 0;
  overflow: hidden;
}

.error-message {
  padding: 20px;
}

.pagination {
  padding: 12px 16px;
  border-top: 1px solid #e6e6e6;
  display: flex;
  justify-content: flex-end;
}
</style>
