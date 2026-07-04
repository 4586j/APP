<template>
  <div class="page-container">
    <div class="page-header">
      <h2>部门管理</h2>
      <p class="page-desc">管理组织架构及部门功能权限，部门内用户自动继承部门权限</p>
    </div>

    <div style="display:flex;gap:16px">
      <!-- 左侧部门树 -->
      <div class="dept-tree-panel">
        <div style="padding:14px 16px;border-bottom:1px solid #f0f0f0;display:flex;justify-content:space-between;align-items:center">
          <h4 style="font-size:14px;font-weight:600">部门架构</h4>
          <el-tag type="success" size="small">树形</el-tag>
        </div>
        <div style="padding:12px">
          <el-input v-model="treeFilter" placeholder="搜索部门" clearable :prefix-icon="Search" size="small" />
        </div>
        <el-tree
          ref="treeRef"
          :data="deptTree"
          :props="{ label: 'name', children: 'children' }"
          :filter-node-method="filterNode as any"
          node-key="id"
          highlight-current
          default-expand-all
          @node-click="onNodeClick"
        >
          <template #default="{ node, data }">
            <span>
              <span>{{ node.label }}</span>
              <el-tag v-if="data.status === 0" type="danger" size="small" style="margin-left:6px">禁用</el-tag>
            </span>
          </template>
        </el-tree>
      </div>

      <!-- 右侧表格 -->
      <div class="dept-table-panel">
        <div class="search-bar">
          <el-button
            v-if="hasPerm('department:create')"
            type="primary"
            :icon="Plus"
            @click="openDialog()"
          >
            新增部门
          </el-button>
          <el-button
            v-if="hasPerm('department:update')"
            :icon="Edit"
            :disabled="!selectedDept"
            @click="openDialog(selectedDept!)"
          >
            编辑
          </el-button>
          <el-button
            v-if="hasPerm('department:delete')"
            type="danger"
            :icon="Delete"
            :disabled="!selectedDept"
            @click="doDelete(selectedDept!)"
          >
            删除
          </el-button>
        </div>

        <div class="table-container">
          <div class="table-header">
            <h3>部门列表 — {{ currentDeptName || '全部' }}</h3>
          </div>
          <el-table v-loading="loading" :data="tableData as any[]" stripe size="small">
            <el-table-column prop="name" label="部门名称" min-width="160" />
            <el-table-column prop="code" label="部门编码" width="120" />
            <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="170" />
            <el-table-column label="操作" width="260" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="hasPerm('department:update')"
                  type="primary"
                  link
                  size="small"
                  @click="openDialog(row)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="hasPerm('department:update')"
                  type="primary"
                  link
                  size="small"
                  @click="openPermDialog(row)"
                >
                  配置权限
                </el-button>
                <el-button
                  v-if="hasPerm('department:update')"
                  type="primary"
                  link
                  size="small"
                  @click="openUserPermDialog(row)"
                >
                  用户权限
                </el-button>
                <el-button
                  v-if="hasPerm('department:delete')"
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
      </div>
    </div>

    <!-- 新增/编辑部门弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="560px" @closed="resetForm">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" style="margin-top:12px">
            <el-form-item label="上级部门">
              <DepartmentSelect v-model="form.parentId" :disabled="isEdit && form.id === form.parentId" />
            </el-form-item>
            <el-form-item label="部门编码" prop="code">
              <el-input v-model="form.code" placeholder="如：SALES" maxlength="32" show-word-limit />
            </el-form-item>
            <el-form-item label="部门名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入部门名称" maxlength="64" show-word-limit />
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
        </el-tab-pane>

        <el-tab-pane label="权限配置" name="perm">
          <div style="margin-top:12px">
            <p style="color:#909399;font-size:13px;margin-bottom:12px">
              勾选该部门可访问的功能权限，部门内所有用户将自动继承这些权限（与角色权限取并集）
            </p>
            <el-tree
              ref="permTreeRef"
              :data="permTree"
              show-checkbox
              node-key="id"
              default-expand-all
              :props="{ label: 'name', children: 'children' }"
              style="max-height:360px;overflow-y:auto"
            >
              <template #default="{ data }">
                <span>
                  {{ data.name }}
                  <el-tag size="small" style="margin-left:6px">{{ data.code }}</el-tag>
                  <el-tag v-if="data.type === 'button'" type="warning" size="small" style="margin-left:4px">按钮</el-tag>
                  <el-tag v-else-if="data.type === 'api'" type="info" size="small" style="margin-left:4px">API</el-tag>
                </span>
              </template>
            </el-tree>
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 独立权限配置弹窗（表格操作列用） -->
    <el-dialog v-model="permDialogVisible" title="配置部门权限" width="560px" @closed="resetPermDialog">
      <p style="color:#606266;margin-bottom:12px">
        当前部门：<strong>{{ permDialogDeptName }}</strong>
      </p>
      <p style="color:#909399;font-size:13px;margin-bottom:12px">
        部门权限与角色权限取并集，用户最终能访问的功能为两者之和
      </p>
      <el-tree
        ref="standalonePermTreeRef"
        :data="permTree"
        show-checkbox
        node-key="id"
        default-expand-all
        :props="{ label: 'name', children: 'children' }"
        style="max-height:420px;overflow-y:auto"
      >
        <template #default="{ data }">
          <span>
            {{ data.name }}
            <el-tag size="small" style="margin-left:6px">{{ data.code }}</el-tag>
            <el-tag v-if="data.type === 'button'" type="warning" size="small" style="margin-left:4px">按钮</el-tag>
            <el-tag v-else-if="data.type === 'api'" type="info" size="small" style="margin-left:4px">API</el-tag>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSaving" @click="savePermDialog">保存</el-button>
      </template>
    </el-dialog>

    <!-- 用户权限配置弹窗 -->
    <el-dialog v-model="userPermDialogVisible" title="用户权限配置" width="620px" @closed="resetUserPermDialog">
      <p style="color:#606266;margin-bottom:12px">
        当前部门：<strong>{{ userPermDialogDeptName }}</strong>
      </p>
      <p style="color:#909399;font-size:13px;margin-bottom:16px">
        为部门内的用户分配具体权限，仅对部员/部长生效（管理员拥有所有权限）
      </p>

      <el-table :data="deptUsers" stripe size="small" max-height="300" v-loading="userLoading">
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="roleCodes" label="职称" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.roleCodes?.[0] === 'ROLE_ADMIN' ? '管理员' : row.roleCodes?.[0] === 'ROLE_N002' ? '部长' : '部员' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" link size="small" :disabled="row.roleCodes?.includes('ROLE_ADMIN')" @click="editUserPerm(row)">
              配置权限
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 单个用户权限编辑弹窗 -->
    <el-dialog v-model="singleUserPermVisible" :title="'权限配置 - ' + singleUserPermUser?.realName" width="500px" @closed="resetSingleUserPerm">
      <el-tree
        ref="userPermTreeRef"
        :data="assignablePermTree"
        show-checkbox
        node-key="id"
        default-expand-all
        :props="{ label: 'name', children: 'children' }"
        style="max-height:420px;overflow-y:auto"
      >
        <template #default="{ data }">
          <span>
            {{ data.name }}
            <el-tag size="small" style="margin-left:6px">{{ data.code }}</el-tag>
            <el-tag v-if="data.type === 'button'" type="warning" size="small" style="margin-left:4px">按钮</el-tag>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="singleUserPermVisible = false">取消</el-button>
        <el-button type="primary" :loading="userPermSaving" @click="saveUserPerm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  listDepartments,
  createDepartment,
  updateDepartment,
  deleteDepartment,
  listPermissionTree,
  getDeptPermissionIds,
  assignDeptPermissions,
  getDeptUserPermissionIds,
  assignDeptUserPermissions,
  listUsers,
  type DepartmentNode,
  type PermissionNode,
  type Id,
  type SystemUser,
} from '@/api/system'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const hasPerm = (perm: string) => userStore.hasPermission(perm)

const loading = ref(false)
const treeRef = ref<any>()
const deptTree = ref<DepartmentNode[]>([])
const treeFilter = ref('')
const selectedDept = ref<DepartmentNode | undefined>(undefined)
const tableData = ref<DepartmentNode[]>([])
const permTree = ref<PermissionNode[]>([])

const currentDeptName = computed(() => selectedDept.value?.name)

async function loadTree() {
  loading.value = true
  try {
    const [deptData, permData] = await Promise.all([listDepartments(), listPermissionTree()])
    deptTree.value = deptData
    permTree.value = permData
    refreshTable()
  } finally {
    loading.value = false
  }
}

function refreshTable() {
  if (!selectedDept.value) {
    const result: DepartmentNode[] = []
    function walk(nodes: DepartmentNode[]) {
      for (const n of nodes || []) {
        result.push(n)
        if (n.children?.length) walk(n.children)
      }
    }
    walk(deptTree.value)
    tableData.value = result
  } else {
    const result: DepartmentNode[] = []
    function findAndCollect(nodes: DepartmentNode[]) {
      for (const n of nodes || []) {
        if (n.id === selectedDept.value!.id) {
          result.push(n)
          if (n.children?.length) collectChildren(n.children)
          return true
        }
        if (n.children?.length && findAndCollect(n.children)) return true
      }
      return false
    }
    function collectChildren(nodes: DepartmentNode[]) {
      for (const n of nodes || []) {
        result.push(n)
        if (n.children?.length) collectChildren(n.children)
      }
    }
    findAndCollect(deptTree.value)
    tableData.value = result
  }
}

function onNodeClick(data: DepartmentNode) {
  selectedDept.value = data
  refreshTable()
}

function filterNode(value: string, data: DepartmentNode) {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

watch(treeFilter, (val) => {
  treeRef.value?.filter(val)
})

// 部门弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const activeTab = ref('basic')
const formRef = ref<FormInstance>()
const permTreeRef = ref<any>()
const form = reactive({
  id: undefined as Id | undefined,
  parentId: undefined as Id | undefined,
  code: '',
  name: '',
  sortOrder: 0,
  status: 1,
})

const rules: FormRules = {
  code: [{ required: true, message: '请输入部门编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
}

async function openDialog(row?: any) {
  await loadPermTreeIfNeeded()
  if (row) {
    isEdit.value = true
    form.id = row.id
    form.parentId = row.parentId
    form.code = row.code
    form.name = row.name
    form.sortOrder = row.sortOrder ?? 0
    form.status = row.status ?? 1
    // 加载部门权限
    if (permTreeRef.value) {
      try {
        const ids = await getDeptPermissionIds(row.id)
        permTreeRef.value.setCheckedKeys(ids || [])
      } catch (e) {
        console.error('Failed to load dept permissions:', e)
      }
    }
  } else {
    isEdit.value = false
    resetForm()
    form.parentId = selectedDept.value?.id
    if (permTreeRef.value) permTreeRef.value.setCheckedKeys([])
  }
  activeTab.value = 'basic'
  dialogVisible.value = true
}

function resetForm() {
  form.id = undefined
  form.parentId = undefined
  form.code = ''
  form.name = ''
  form.sortOrder = 0
  form.status = 1
  formRef.value?.resetFields()
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    let deptId: Id
    if (isEdit.value && form.id != null) {
      await updateDepartment(form.id, {
        parentId: form.parentId,
        code: form.code,
        name: form.name,
        sortOrder: form.sortOrder,
        status: form.status,
      })
      deptId = form.id
      ElMessage.success('更新成功')
    } else {
      deptId = await createDepartment({
        parentId: form.parentId ? String(form.parentId) : '0',
        code: form.code,
        name: form.name,
        sortOrder: form.sortOrder,
        status: form.status,
      })
      ElMessage.success('创建成功')
    }
    // 保存权限
    const checked = permTreeRef.value?.getCheckedKeys(false) || []
    const halfChecked = permTreeRef.value?.getHalfCheckedKeys?.() || []
    const permIds = Array.from(new Set([...checked, ...halfChecked]))
    await assignDeptPermissions(deptId, permIds)

    dialogVisible.value = false
    await loadTree()
  } finally {
    submitting.value = false
  }
}

async function loadPermTreeIfNeeded() {
  if (!permTree.value.length) {
    permTree.value = await listPermissionTree()
  }
}

async function doDelete(row: any) {
  if (!row) return
  try {
    await ElMessageBox.confirm(`确定删除部门 "${row.name}" 吗？`, '提示', { type: 'warning' })
    await deleteDepartment(row.id)
    ElMessage.success('删除成功')
    if (selectedDept.value?.id === row.id) selectedDept.value = undefined
    await loadTree()
  } catch (e: any) {
    if (e !== 'cancel') {
      const msg = e?.message || '删除失败'
      ElMessage.error(msg)
    }
  }
}

// 独立权限配置弹窗
const permDialogVisible = ref(false)
const permSaving = ref(false)
const permDialogDeptId = ref<Id | undefined>(undefined)
const permDialogDeptName = ref('')
const standalonePermTreeRef = ref<any>()

async function openPermDialog(row: any) {
  await loadPermTreeIfNeeded()
  permDialogDeptId.value = row.id
  permDialogDeptName.value = row.name
  permDialogVisible.value = true
  // 等 DOM 更新后设置选中
  setTimeout(async () => {
    try {
      const ids = await getDeptPermissionIds(row.id)
      standalonePermTreeRef.value?.setCheckedKeys(ids || [])
    } catch (e) {
      console.error('Failed to load dept permissions:', e)
    }
  }, 100)
}

function resetPermDialog() {
  permDialogDeptId.value = undefined
  permDialogDeptName.value = ''
  standalonePermTreeRef.value?.setCheckedKeys([])
}

async function savePermDialog() {
  if (!permDialogDeptId.value) return
  permSaving.value = true
  try {
    const checked = standalonePermTreeRef.value?.getCheckedKeys(false) || []
    const halfChecked = standalonePermTreeRef.value?.getHalfCheckedKeys?.() || []
    const permIds = Array.from(new Set([...checked, ...halfChecked]))
    await assignDeptPermissions(permDialogDeptId.value, permIds)
    ElMessage.success('权限配置已保存')
    permDialogVisible.value = false
  } finally {
    permSaving.value = false
  }
}

// 用户权限配置弹窗
const userPermDialogVisible = ref(false)
const userPermDialogDeptId = ref<Id | undefined>(undefined)
const userPermDialogDeptName = ref('')
const deptUsers = ref<SystemUser[]>([])
const userLoading = ref(false)
const assignablePermTree = ref<PermissionNode[]>([])
const assignablePermIds = ref<Set<Id>>(new Set())

function filterPermissionTreeByIds(nodes: PermissionNode[], allowed: Set<Id>): PermissionNode[] {
  const result: PermissionNode[] = []
  for (const node of nodes || []) {
    const children = filterPermissionTreeByIds(node.children || [], allowed)
    if (allowed.has(node.id) || children.length > 0) {
      result.push({ ...node, children })
    }
  }
  return result
}

async function openUserPermDialog(row: any) {
  userPermDialogDeptId.value = row.id
  userPermDialogDeptName.value = row.name
  userPermDialogVisible.value = true
  await loadPermTreeIfNeeded()
  const deptPermIds = await getDeptPermissionIds(row.id)
  assignablePermIds.value = new Set(deptPermIds || [])
  assignablePermTree.value = filterPermissionTreeByIds(permTree.value, assignablePermIds.value)
  await loadDeptUsers(row.id)
}

async function loadDeptUsers(deptId: Id) {
  userLoading.value = true
  try {
    const res = await listUsers({ page: 1, size: 999, departmentId: deptId })
    deptUsers.value = (res as any).records || []
  } catch (e: any) {
    console.error('Failed to load dept users:', e)
    ElMessage.error('加载用户列表失败')
  } finally {
    userLoading.value = false
  }
}

function resetUserPermDialog() {
  userPermDialogDeptId.value = undefined
  userPermDialogDeptName.value = ''
  deptUsers.value = []
  assignablePermTree.value = []
  assignablePermIds.value = new Set()
}

// 单个用户权限编辑
const singleUserPermVisible = ref(false)
const singleUserPermUser = ref<any>(null)
const userPermTreeRef = ref<any>()
const userPermSaving = ref(false)

async function editUserPerm(row: any) {
  singleUserPermUser.value = row
  singleUserPermVisible.value = true
  setTimeout(async () => {
    try {
      const ids = await getDeptUserPermissionIds(userPermDialogDeptId.value!, row.id)
      userPermTreeRef.value?.setCheckedKeys(ids || [])
    } catch (e) {
      console.error('Failed to load user permissions:', e)
    }
  }, 100)
}

function resetSingleUserPerm() {
  singleUserPermUser.value = null
  userPermTreeRef.value?.setCheckedKeys([])
}

async function saveUserPerm() {
  if (!singleUserPermUser.value || !userPermDialogDeptId.value) return
  userPermSaving.value = true
  try {
    const checked = userPermTreeRef.value?.getCheckedKeys(false) || []
    const halfChecked = userPermTreeRef.value?.getHalfCheckedKeys?.() || []
    const permIds = Array.from(new Set([...checked, ...halfChecked]))
      .filter((id) => assignablePermIds.value.has(id))
    await assignDeptUserPermissions(userPermDialogDeptId.value, singleUserPermUser.value.id, permIds)
    ElMessage.success('用户权限已保存')
    singleUserPermVisible.value = false
  } finally {
    userPermSaving.value = false
  }
}

onMounted(loadTree)
</script>

<style scoped lang="scss">
.dept-tree-panel {
  width: 300px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  overflow: hidden;
}
.dept-table-panel {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  padding: 16px;
}
</style>
