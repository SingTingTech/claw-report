<template>
  <div class="dictionary-container">
    <el-card>
      <template #header>
        <div class="header-actions">
          <el-breadcrumb>
            <el-breadcrumb-item :to="{ path: '/datasources' }">数据源</el-breadcrumb-item>
            <el-breadcrumb-item>{{ dataSourceName }}</el-breadcrumb-item>
            <el-breadcrumb-item>数据字典</el-breadcrumb-item>
          </el-breadcrumb>
          <div class="header-buttons">
            <el-button type="primary" @click="handleSync" :loading="syncing">
              <el-icon><Refresh /></el-icon> 同步元数据
            </el-button>
          </div>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="数据表" name="tables">
          <el-table :data="tableList" stripe v-loading="loading" @row-click="handleTableClick">
            <el-table-column prop="tableName" label="表名" min-width="200">
              <template #default="{ row }">
                <el-link type="primary" @click.stop="selectTable(row)">{{ row.tableName }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="comment" label="说明" min-width="200" show-overflow-tooltip />
            <el-table-column prop="columnCount" label="字段数" width="100" align="center" />
          </el-table>
        </el-tab-pane>
        
        <el-tab-pane label="字段详情" name="columns">
          <div class="table-toolbar" v-if="selectedTableName">
            <span class="table-title">
              <strong>{{ selectedTableName }}</strong>
              <el-tag type="info" size="small" style="margin-left: 8px">{{ tableComment || '无说明' }}</el-tag>
            </span>
            <el-button size="small" @click="activeTab = 'tables'">
              <el-icon><ArrowLeft /></el-icon> 返回列表
            </el-button>
          </div>
          
          <el-table v-if="columnList.length > 0" :data="columnList" stripe v-loading="loadingColumns">
            <el-table-column prop="columnName" label="字段名" width="180">
              <template #default="{ row }">
                <span :class="{ 'pk-text': row.primaryKey }">
                  {{ row.columnName }}
                  <el-tag v-if="row.primaryKey" type="warning" size="small" style="margin-left: 4px">PK</el-tag>
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="dataType" label="数据类型" width="120" />
            <el-table-column prop="columnType" label="列类型" width="150" />
            <el-table-column prop="nullable" label="可空" width="80" align="center">
              <template #default="{ row }">
                {{ row.nullable }}
              </template>
            </el-table-column>
            <el-table-column prop="defaultValue" label="默认值" width="120" />
            <el-table-column prop="comment" label="原始注释" width="150">
              <template #default="{ row }">
                <span :class="{ 'no-comment': !row.comment }">{{ row.comment || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="我的备注" min-width="200">
              <template #default="{ row }">
                <span v-if="!row.editing">{{ row.savedComment || '-' }}</span>
                <el-input 
                  v-else 
                  v-model="row.editComment" 
                  size="small" 
                  placeholder="添加备注..."
                  @blur="handleSaveComment(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="枚举映射" min-width="180">
              <template #default="{ row }">
                <span v-if="!row.editingEnum" class="enum-config" @click="startEditEnum(row)">
                  {{ row.enumConfig || '-' }}
                </span>
                <div v-else class="enum-edit">
                  <el-input 
                    v-model="row.editEnumConfig" 
                    size="small" 
                    placeholder="如: 1=正常,0=禁用"
                    style="width: 150px"
                  />
                  <el-button type="success" link size="small" @click="handleSaveEnum(row)">保存</el-button>
                  <el-button type="info" link size="small" @click="cancelEditEnum(row)">取消</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button 
                  v-if="!row.editing" 
                  type="primary" 
                  link 
                  size="small" 
                  @click="startEditComment(row)"
                >
                  {{ row.savedComment ? '编辑' : '添加备注' }}
                </el-button>
                <el-button 
                  v-else 
                  type="success" 
                  link 
                  size="small" 
                  @click="handleSaveComment(row)"
                >
                  保存
                </el-button>
                <el-button 
                  v-if="row.editing" 
                  type="info" 
                  link 
                  size="small" 
                  @click="cancelEditComment(row)"
                >
                  取消
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <el-empty v-else-if="!selectedTableName" description="请从左侧选择一个表" />
          <el-empty v-else description="该表暂无字段信息" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, ArrowLeft } from '@element-plus/icons-vue'
import { api } from '../stores/user'

const route = useRoute()

const loading = ref(false)
const loadingColumns = ref(false)
const syncing = ref(false)
const dataSourceName = ref('')
const dataSourceDatabase = ref('')
const activeTab = ref('tables')
const tableList = ref([])
const columnList = ref([])
const selectedTableName = ref('')
const tableComment = ref('')

onMounted(async () => {
  await fetchDataSource()
  await fetchDictionary()
})

async function fetchDataSource() {
  try {
    const res = await api.get(`/datasource/${route.params.id}`)
    dataSourceName.value = res.data.name
    dataSourceDatabase.value = res.data.databaseName
  } catch (e) {
    ElMessage.error('获取数据源信息失败')
  }
}

async function fetchDictionary() {
  if (!dataSourceDatabase.value) return
  loading.value = true
  try {
    const res = await api.get(`/datasource/${route.params.id}/dictionary?database=${dataSourceDatabase.value}`)
    tableList.value = res.data || []
    // 计算每个表的字段数
    tableList.value.forEach(table => {
      table.columnCount = table.columns ? table.columns.length : 0
    })
  } catch (e) {
    ElMessage.error('获取数据字典失败: ' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

async function handleSync() {
  if (!dataSourceDatabase.value) {
    ElMessage.warning('数据源未配置数据库')
    return
  }
  syncing.value = true
  try {
    const res = await api.post(`/datasource/${route.params.id}/dictionary/sync?database=${dataSourceDatabase.value}`)
    ElMessage.success(res.message || '同步成功')
    await fetchDictionary()
    // 如果当前在查看某个表，也刷新该表
    if (selectedTableName.value) {
      await selectTableByName(selectedTableName.value)
    }
  } catch (e) {
    ElMessage.error('同步失败: ' + (e.message || ''))
  } finally {
    syncing.value = false
  }
}

function handleTableClick(row) {
  selectTable(row)
}

function selectTable(row) {
  selectedTableName.value = row.tableName
  tableComment.value = row.comment
  columnList.value = (row.columns || []).map(col => ({
    ...col,
    editing: false,
    editComment: col.savedComment || ''
  }))
  activeTab.value = 'columns'
}

async function selectTableByName(tableName) {
  const table = tableList.value.find(t => t.tableName === tableName)
  if (table) {
    await fetchDictionary()
    selectTable(table)
  }
}

function startEditComment(row) {
  row.editing = true
  row.editComment = row.savedComment || ''
}

function cancelEditComment(row) {
  row.editing = false
  row.editComment = row.savedComment || ''
}

async function handleSaveComment(row) {
  try {
    await api.put(`/datasource/${route.params.id}/dictionary/column`, {
      tableName: selectedTableName.value,
      columnName: row.columnName,
      comment: row.editComment
    })
    row.savedComment = row.editComment
    row.editing = false
    ElMessage.success('备注保存成功')
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || ''))
  }
}

function startEditEnum(row) {
  row.editingEnum = true
  row.editEnumConfig = row.enumConfig || ''
}

function cancelEditEnum(row) {
  row.editingEnum = false
  row.editEnumConfig = row.enumConfig || ''
}

async function handleSaveEnum(row) {
  try {
    await api.put(`/datasource/${route.params.id}/dictionary/enum-config`, {
      tableName: selectedTableName.value,
      columnName: row.columnName,
      enumConfig: row.editEnumConfig
    })
    row.enumConfig = row.editEnumConfig
    row.editingEnum = false
    ElMessage.success('枚举映射保存成功')
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || ''))
  }
}
</script>

<style scoped>
.dictionary-container {
  height: 100%;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-buttons {
  display: flex;
  gap: 8px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.table-title {
  font-size: 16px;
}

.pk-text {
  color: #e6a23c;
  font-weight: 500;
}

.no-comment {
  color: #c0c4cc;
}

.enum-config {
  cursor: pointer;
  color: #409eff;
}

.enum-config:hover {
  text-decoration: underline;
}

.enum-edit {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
