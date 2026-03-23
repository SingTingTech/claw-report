<template>
  <div class="report-detail-container">
    <el-card v-if="report">
      <template #header>
        <div class="header-actions">
          <div class="title-section">
            <h2>{{ report.name }}</h2>
            <p class="description" v-if="report.description">{{ report.description }}</p>
          </div>
          <div class="actions">
            <el-button @click="handleRefresh">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
            <el-select v-model="pageSize" style="width: 120px; margin-left: 10px" @change="fetchData">
              <el-option :value="20" label="20条/页" />
              <el-option :value="50" label="50条/页" />
              <el-option :value="100" label="100条/页" />
            </el-select>
          </div>
        </div>
      </template>

      <!-- 参数区域 -->
      <div class="params-section" v-if="params.length > 0">
        <el-form :inline="true" :model="paramValues">
          <el-form-item v-for="param in params" :key="param.id" :label="param.paramLabel || param.paramName">
            <el-input 
              v-if="param.paramType === 'TEXT'" 
              v-model="paramValues[param.paramName]" 
              :placeholder="param.paramLabel" 
              style="width: 150px"
            />
            <el-select 
              v-else-if="param.paramType === 'SELECT'" 
              v-model="paramValues[param.paramName]" 
              placeholder="请选择" 
              style="width: 150px"
            >
              <el-option 
                v-for="opt in (paramOptions[param.paramName] || [])" 
                :key="opt.value" 
                :label="opt.label" 
                :value="opt.value" 
              />
              <el-option v-if="!paramOptions[param.paramName]?.length" label="（无选项）" value="" disabled />
            </el-select>
            <el-date-picker 
              v-else-if="param.paramType === 'DATE'" 
              v-model="paramValues[param.paramName]" 
              type="date" 
              placeholder="选择日期"
              style="width: 150px"
            />
            <el-input-number
              v-else-if="param.paramType === 'NUMBER'"
              v-model="paramValues[param.paramName]"
              :min="0"
              style="width: 150px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">查询</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 图表视图切换 -->
      <div class="view-toggle" v-if="records && records.length > 0">
        <el-radio-group v-model="viewMode">
          <el-radio-button value="table">表格</el-radio-button>
          <el-radio-button value="line">折线图</el-radio-button>
          <el-radio-button value="bar">柱状图</el-radio-button>
          <el-radio-button value="pie">饼图</el-radio-button>
        </el-radio-group>
        <el-select v-model="xAxisColumn" placeholder="选择X轴字段" style="width: 150px; margin-left: 12px" v-if="viewMode !== 'pie'">
          <el-option v-for="col in columns" :key="col.name" :label="col.label" :value="col.name" />
        </el-select>
        <el-select v-model="yAxisColumn" placeholder="选择Y轴字段" style="width: 150px; margin-left: 12px" v-if="viewMode !== 'pie'">
          <el-option v-for="col in numericColumns" :key="col.name" :label="col.label" :value="col.name" />
        </el-select>
        <el-select v-model="pieColumn" placeholder="选择分类字段" style="width: 150px; margin-left: 12px" v-if="viewMode === 'pie'">
          <el-option v-for="col in columns" :key="col.name" :label="col.label" :value="col.name" />
        </el-select>
        <el-select v-model="pieValueColumn" placeholder="选择数值字段" style="width: 150px; margin-left: 12px" v-if="viewMode === 'pie'">
          <el-option v-for="col in numericColumns" :key="col.name" :label="col.label" :value="col.name" />
        </el-select>
      </div>

      <!-- 图表容器 -->
      <div class="chart-container" v-if="records && records.length > 0 && viewMode !== 'table'" ref="chartRef"></div>

      <!-- 数据表格 -->
      <div class="data-section" v-loading="loading" v-if="viewMode === 'table'">
        <el-table 
          :data="records" 
          stripe 
          border 
          height="500"
          v-if="records && records.length > 0"
        >
          <el-table-column 
            v-for="col in columns" 
            :key="col.name" 
            :prop="col.name" 
            :label="col.label" 
            min-width="150" 
            show-overflow-tooltip 
            :formatter="(row) => formatCellValue(col.name, row[col.name])"
          />
        </el-table>
        
        <el-empty v-else-if="!loading" description="暂无数据" />
        
        <div v-if="error" class="error-section">
          <el-alert type="error" :title="error" show-icon :closable="false" />
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
        <div class="result-info">
          共 {{ total }} 条，耗时 {{ executeTime }}ms
        </div>
      </div>
    </el-card>

    <el-empty v-else description="报表不存在" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { api } from '../stores/user'
import * as echarts from 'echarts'

const route = useRoute()

const loading = ref(false)
const report = ref(null)
const params = ref([])
const paramOptions = ref({})  // 存储下拉选项 { paramName: [{label, value}] }
const enumConfigs = ref({})    // 存储枚举配置 { columnName: { value: label } }
const records = ref([])
const columns = ref([])
const total = ref(0)
const executeTime = ref(0)
const error = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const paramValues = reactive({})

// 图表相关
const viewMode = ref('table')
const chartRef = ref(null)

// 监听路由query变化，初始化图表类型
watch(() => route.query.chartType, (newChartType) => {
  if (newChartType) {
    viewMode.value = newChartType.toString().toLowerCase()
  }
}, { immediate: true })
const xAxisColumn = ref('')
const yAxisColumn = ref('')
const pieColumn = ref('')
const pieValueColumn = ref('')
let chartInstance = null

// 计算数值列（用于Y轴）
const numericColumns = computed(() => {
  return columns.value.filter(col => {
    const sample = records.value[0]?.[col.name]
    return typeof sample === 'number'
  })
})

onMounted(async () => {
  await fetchReport()
})

// 监听路由参数变化，重新获取报表数据
watch(() => route.params.id, async (newId) => {
  if (newId) {
    await fetchReport()
  }
})

async function fetchReport() {
  try {
    const id = route.params.id
    const res = await api.get(`/report/${id}`)
    report.value = res.data
    
    // 获取参数定义
    const paramsRes = await api.get(`/report/${id}/params`)
    params.value = paramsRes.data || []
    
    // 设置默认值
    params.value.forEach(p => {
      if (p.defaultValue) {
        paramValues[p.paramName] = p.defaultValue
      }
    })
    
    // 加载下拉选项
    await loadSelectOptions()
    
    // 加载枚举配置
    await loadEnumConfigs()
    
    // 获取数据
    await fetchData()
  } catch (e) {
    ElMessage.error('获取报表失败')
  }
}

async function loadEnumConfigs() {
  if (!report.value?.dataSourceId) return
  try {
    const res = await api.get(`/datasource/${report.value.dataSourceId}/dictionary?database=${report.value.databaseName || 'clawreport'}`)
    if (res.code === 200 && res.data) {
      const configs = {}
      res.data.forEach(table => {
        ;(table.columns || []).forEach(col => {
          if (col.enumConfig) {
            // 解析枚举配置，格式：1=正常,0=禁用
            const mapping = {}
            col.enumConfig.split(',').forEach(item => {
              const [value, label] = item.split('=')
              if (value !== undefined && label !== undefined) {
                mapping[value.trim()] = label.trim()
              }
            })
            if (Object.keys(mapping).length > 0) {
              configs[col.columnName] = mapping
            }
          }
        })
      })
      enumConfigs.value = configs
    }
  } catch (e) {
    console.error('加载枚举配置失败', e)
  }
}

async function loadSelectOptions() {
  for (const param of params.value) {
    if (param.paramType === 'SELECT' && param.optionsSql) {
      const sql = param.optionsSql.trim()
      if (sql.toLowerCase().startsWith('select')) {
        try {
          const res = await api.post('/query/execute', {
            sql: sql,
            dataSourceId: report.value.dataSourceId
          })
          if (res.code === 200 && res.data?.records) {
            paramOptions.value[param.paramName] = res.data.records.map(r => {
              const keys = Object.keys(r)
              return {
                value: r[keys[0]],
                label: r[keys[1]] || r[keys[0]]
              }
            })
          }
        } catch (e) {
          console.error('加载选项失败', e)
        }
      } else {
        paramOptions.value[param.paramName] = sql.split(',').map(item => {
          item = item.trim()
          if (item.includes('=')) {
            const [value, ...labelParts] = item.split('=')
            return { value: value.trim(), label: labelParts.join('=').trim() }
          } else {
            return { value: item, label: item }
          }
        })
      }
    }
  }
}

// 格式化单元格值，应用枚举映射
function formatCellValue(columnName, value) {
  if (value === null || value === undefined) return '-'
  
  const enumMap = enumConfigs.value[columnName]
  if (enumMap && enumMap[value] !== undefined) {
    return enumMap[value]
  }
  
  return value
}

// 渲染图表
function renderChart() {
  if (!chartRef.value) return
  
  // 销毁旧实例
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  
  if (viewMode.value === 'table') return
  
  // 创建新实例
  chartInstance = echarts.init(chartRef.value)
  
  let option = {}
  
  if (viewMode.value === 'line') {
    option = createLineOption()
  } else if (viewMode.value === 'bar') {
    option = createBarOption()
  } else if (viewMode.value === 'pie') {
    option = createPieOption()
  }
  
  chartInstance.setOption(option)
}

function createLineOption() {
  const xData = records.value.map(r => formatCellValue(xAxisColumn.value, r[xAxisColumn.value]))
  const yData = records.value.map(r => r[yAxisColumn.value])
  
  return {
    title: { text: report.value?.name || '报表', left: 'center' },
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: xData },
    yAxis: { type: 'value' },
    series: [{
      name: columns.value.find(c => c.name === yAxisColumn.value)?.label || yAxisColumn.value,
      type: 'line',
      data: yData,
      smooth: true,
      label: { show: true }
    }]
  }
}

function createBarOption() {
  const xData = records.value.map(r => formatCellValue(xAxisColumn.value, r[xAxisColumn.value]))
  const yData = records.value.map(r => r[yAxisColumn.value])
  
  return {
    title: { text: report.value?.name || '报表', left: 'center' },
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: xData },
    yAxis: { type: 'value' },
    series: [{
      name: columns.value.find(c => c.name === yAxisColumn.value)?.label || yAxisColumn.value,
      type: 'bar',
      data: yData,
      label: { show: true }
    }]
  }
}

function createPieOption() {
  const data = records.value.map(r => ({
    name: formatCellValue(pieColumn.value, r[pieColumn.value]),
    value: r[pieValueColumn.value]
  }))
  
  return {
    title: { text: report.value?.name || '报表', left: 'center' },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 10, left: 'center' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: data,
      label: { formatter: '{b}: {c} ({d}%)' }
    }]
  }
}

// 监听视图模式变化
watch(viewMode, async (newMode) => {
  if (newMode !== 'table' && records.value.length > 0) {
    await nextTick()
    renderChart()
  }
}, { immediate: true })

// 监听数据变化，重新渲染图表
watch([records, xAxisColumn, yAxisColumn, pieColumn, pieValueColumn], () => {
  if (viewMode.value !== 'table' && chartRef.value) {
    renderChart()
  }
})

// 自动设置默认轴
watch(columns, (newColumns) => {
  if (newColumns.length > 0 && !xAxisColumn.value) {
    xAxisColumn.value = newColumns[0].name
  }
  if (newColumns.length > 0 && !yAxisColumn.value) {
    const numericCol = newColumns.find(c => {
      const sample = records.value[0]?.[c.name]
      return typeof sample === 'number'
    })
    if (numericCol) {
      yAxisColumn.value = numericCol.name
    }
  }
  if (newColumns.length > 0 && !pieColumn.value) {
    pieColumn.value = newColumns[0].name
  }
  if (newColumns.length > 0 && !pieValueColumn.value) {
    const numericCol = newColumns.find(c => {
      const sample = records.value[0]?.[c.name]
      return typeof sample === 'number'
    })
    if (numericCol) {
      pieValueColumn.value = numericCol.name
    }
  }
}, { immediate: true })

async function fetchData() {
  if (!report.value) return
  
  // 校验参数是否已填写
  for (const param of params.value) {
    if (param.required === 1 && !paramValues[param.paramName]) {
      ElMessage.warning(`参数「${param.paramLabel || param.paramName}」为必填项`)
      return
    }
  }
  
  loading.value = true
  error.value = ''
  
  try {
    const queryParams = { ...paramValues, page: currentPage.value, pageSize: pageSize.value }
    const res = await api.get(`/report/${report.value.id}/data`, { params: queryParams })
    
    if (res.code === 200) {
      records.value = res.data?.records || []
      columns.value = res.data?.columns || []
      total.value = res.data?.total || 0
      executeTime.value = res.data?.executeTime || 0
      
      // 数据加载完成后，如果是非表格视图，渲染图表
      if (viewMode.value !== 'table' && records.value.length > 0) {
        await nextTick()
        renderChart()
      }
    } else {
      error.value = res.message
    }
  } catch (e) {
    error.value = e.message || '获取数据失败'
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  currentPage.value = 1
  fetchData()
}

function handleRefresh() {
  fetchData()
}

function handlePageChange(page) {
  currentPage.value = page
  fetchData()
}
</script>

<style scoped>
.report-detail-container {
  height: 100%;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title-section h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
}

.description {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.actions {
  display: flex;
  align-items: center;
}

.params-section {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 16px;
}

.data-section {
  min-height: 300px;
}

.error-section {
  padding: 20px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-info {
  color: #909399;
  font-size: 12px;
}

.view-toggle {
  padding: 12px 0;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 16px;
}

.chart-container {
  width: 100%;
  height: 500px;
  min-height: 400px;
}
</style>
