<template>
  <div class="groups-container">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span class="title">报表分组管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon> 新建分组
          </el-button>
        </div>
      </template>
      
      <el-tree
        :data="treeData"
        :props="treeProps"
        node-key="id"
        default-expand-all
        :expand-on-click-node="false"
      >
        <template #default="{ node, data }">
          <span class="tree-node">
            <span class="node-name">{{ data.name }}</span>
            <span class="node-desc" v-if="data.description">({{ data.description }})</span>
            <span class="node-actions">
              <el-button type="primary" link @click.stop="handleEdit(data)">编辑</el-button>
              <el-button type="primary" link @click.stop="handleAddChild(data)" v-if="data.parentId === 0">添加子分组</el-button>
              <el-button type="danger" link @click.stop="handleDelete(data)" :disabled="data.children && data.children.length > 0">删除</el-button>
            </span>
          </span>
        </template>
      </el-tree>
    </el-card>

    <!-- 创建/编辑分组对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分组' : '新建分组'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="分组名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分组名称" />
        </el-form-item>
        <el-form-item label="父分组" v-if="isEdit || form.parentId">
          <el-select v-model="form.parentId" placeholder="请选择父分组" style="width: 100%" clearable>
            <el-option v-for="g in flatGroups" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api } from '../stores/user'

const loading = ref(false)
const groups = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  parentId: 0,
  description: '',
  sortOrder: 0
})

const rules = {
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }]
}

const treeProps = {
  label: 'name',
  children: 'children'
}

const flatGroups = computed(() => {
  return groups.value.filter(g => g.id !== form.id)
})

const treeData = computed(() => {
  const buildTree = (items, parentId = 0) => {
    return items.filter(item => item.parentId === parentId).map(item => ({
      ...item,
      children: buildTree(items, item.id)
    })).filter(item => item.id !== form.id || isEdit.value)
  }
  return buildTree(groups.value)
})

onMounted(async () => {
  await fetchGroups()
})

async function fetchGroups() {
  loading.value = true
  try {
    const res = await api.get('/group/all')
    groups.value = res.data || []
  } catch (e) {
    ElMessage.error('获取分组列表失败')
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, {
    id: null, name: '', parentId: 0, description: '', sortOrder: 0
  })
  dialogVisible.value = true
}

function handleAddChild(data) {
  isEdit.value = false
  Object.assign(form, {
    id: null, name: '', parentId: data.id, description: '', sortOrder: 0
  })
  dialogVisible.value = true
}

function handleEdit(data) {
  isEdit.value = true
  Object.assign(form, {
    id: data.id,
    name: data.name,
    parentId: data.parentId || 0,
    description: data.description,
    sortOrder: data.sortOrder
  })
  dialogVisible.value = true
}

function handleDelete(data) {
  if (data.children && data.children.length > 0) {
    ElMessage.warning('请先删除子分组')
    return
  }
  ElMessageBox.confirm('确定删除该分组吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    await api.delete(`/group/${data.id}`)
    ElMessage.success('删除成功')
    fetchGroups()
  }).catch(() => {})
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (isEdit.value) {
      await api.put(`/group/${form.id}`, form)
    } else {
      await api.post('/group', form)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    fetchGroups()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}
</script>

<style scoped>
.groups-container {
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

.tree-node {
  display: flex;
  align-items: center;
  flex: 1;
  width: 100%;
}

.node-name {
  font-weight: 500;
  margin-right: 8px;
}

.node-desc {
  color: #909399;
  font-size: 12px;
  margin-right: 16px;
}

.node-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

:deep(.el-tree-node__content) {
  height: auto;
  padding: 8px 0;
}
</style>
