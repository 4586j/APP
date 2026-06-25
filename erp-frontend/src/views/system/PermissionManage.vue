<template>
  <div class="page-container">
    <div class="page-header">
      <h2>权限管理</h2>
      <p class="page-desc">管理系统菜单、按钮及API权限节点</p>
    </div>

    <div class="search-bar">
      <el-button
        v-if="hasPerm('permission:create')"
        type="primary"
        :icon="Plus"
        @click="openDialog()"
      >
        新增权限
      </el-button>
    </div>

    <div class="table-container">
      <div class="table-header">
        <h3>权限树</h3>
        <el-tag type="success" size="small">树形</el-tag>
      </div>
      <el-table
        v-loading="loading"
        :data="flatPermissions"
        stripe
        size="small"
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        default-expand-all
      >
        <el-table-column prop="name" label="权限名称" min-width="180">
          <template #default="{ row }">
            <span>{{ indent(row) }}{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="权限编码" min-width="180" />
        <el-table-column prop="type" label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="160" />
        <el-table-column prop="sortOrder" label="排序" width="70" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="hasPerm('permission:create')"
              type="primary"
              link
              size="small"
              @click="openDialog(row)"
            >
              新增子项
            </el-button>
            <el-button
              v-if="hasPerm('permission:update')"
              type="primary"
              link
              size="small"
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="hasPerm('permission:delete')"
              type="danger"
              link
              size="small"
              @click="doDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级权限">
          <el-tree-select
            v-model="form.parentId"
            :data="permTreeSelect"
            check-strictly
            placeholder="请选择上级权限（0=顶级）"
            clearable
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="form.name" placeholder="如：用户管理" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="权限编码" prop="code">
          <el-input v-model="form.code" placeholder="如：system:user" maxlength="128" show-word-limit :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="权限类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="菜单" value="menu" />
            <el-option label="按钮" value="button" />
            <el-option label="API" value="api" />
          </el-select>
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="form.path" placeholder="前端路由路径，如：/system/user" maxlength="128" />
        </el-form-item>
        <el-form-item label="组件路径">
          <el-input v-model="form.component" placeholder="前端组件路径，如：system/user/index" maxlength="256" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名，如：User" maxlength="64" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" style="width:120px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listPermissionTree,
  createPermission,
  updatePermission,
  deletePermission,
  type PermissionNode,
  type Id,
} from '@/api/system'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const hasPerm = (perm: string) => userStore.hasPermission(perm)

const loading = ref(false)
const permTree = ref<PermissionNode[]>([])

// 扁平化用于表格展示
const flatPermissions = computed(() => {
  const result: any[] = []
  function walk(nodes: PermissionNode[], depth: number) {
    for (const n of nodes || []) {
      result.push({ ...n, depth, children: n.children?.length ? [] : undefined })
      if (n.children?.length) walk(n.children, depth + 1)
    }
  }
  walk(permTree.value, 0)
  return result
})

// tree-select 选项
const permTreeSelect = computed(() => {
  function toSelect(nodes: PermissionNode[]): any[] {
    return (nodes || []).map((n) => ({
      value: n.id,
      label: n.name,
      children: n.children?.length ? toSelect(n.children) : undefined,
    }))
  }
  return [{ value: 0, label: '顶级权限' }, ...toSelect(permTree.value)]
})

function indent(row: any) {
  return '  '.repeat(row.depth || 0)
}

function typeText(type?: string) {
  const map: Record<string, string> = { menu: '菜单', button: '按钮', api: 'API' }
  return map[type || ''] || type
}

function typeTag(type?: string) {
  const map: Record<string, any> = { menu: 'primary', button: 'warning', api: 'info' }
  return map[type || ''] || ''
}

async function loadData() {
  loading.value = true
  try {
    permTree.value = await listPermissionTree()
  } finally {
    loading.value = false
  }
}

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const isChild = ref(false)
const dialogTitle = computed(() => {
  if (isChild.value) return '新增子权限'
  return isEdit.value ? '编辑权限' : '新增权限'
})
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as Id | undefined,
  parentId: 0 as Id,
  name: '',
  code: '',
  type: 'menu' as 'menu' | 'button' | 'api',
  path: '',
  component: '',
  icon: '',
  sortOrder: 0,
  status: 1,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
}

function openDialog(parent?: any) {
  isEdit.value = false
  isChild.value = !!parent
  resetForm()
  if (parent) {
    form.parentId = parent.id
  }
  dialogVisible.value = true
}

function openEditDialog(row: any) {
  isEdit.value = true
  isChild.value = false
  form.id = row.id
  form.parentId = row.parentId ?? 0
  form.name = row.name
  form.code = row.code
  form.type = (row.type as any) || 'menu'
  form.path = row.path || ''
  form.component = row.component || ''
  form.icon = row.icon || ''
  form.sortOrder = row.sortOrder ?? 0
  form.status = row.status ?? 1
  dialogVisible.value = true
}

function resetForm() {
  form.id = undefined
  form.parentId = 0
  form.name = ''
  form.code = ''
  form.type = 'menu'
  form.path = ''
  form.component = ''
  form.icon = ''
  form.sortOrder = 0
  form.status = 1
  formRef.value?.resetFields()
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && form.id != null) {
      await updatePermission(form.id, {
        name: form.name,
        sortOrder: form.sortOrder,
        path: form.path,
        icon: form.icon,
        status: form.status,
      })
      ElMessage.success('更新成功')
    } else {
      await createPermission({
        parentId: form.parentId,
        name: form.name,
        code: form.code,
        type: form.type,
        path: form.path,
        component: form.component,
        icon: form.icon,
        sortOrder: form.sortOrder,
        status: form.status,
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function doDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除权限 "${row.name}" 吗？其子权限也会被删除。`, '提示', { type: 'warning' })
    await deletePermission(row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

onMounted(loadData)
</script>
