<template>
  <div class="page-container">
    <div class="page-header">
      <h2>角色管理</h2>
      <p class="page-desc">管理系统角色、数据范围及用户分配</p>
    </div>

    <div class="search-bar">
      <el-button
        v-if="hasPerm('role:create')"
        type="primary"
        :icon="Plus"
        @click="openRoleDialog()"
      >
        新增角色
      </el-button>
    </div>

    <div class="table-container">
      <div class="table-header">
        <h3>角色列表</h3>
        <el-tag type="success" size="small">DB</el-tag>
      </div>
      <el-table v-loading="loading" :data="roles as any[]" stripe size="small">
        <el-table-column prop="roleName" label="角色名称" width="140" />
        <el-table-column prop="roleCode" label="角色编码" width="160" />
        <el-table-column prop="dataScope" label="数据范围" width="140">
          <template #default="{ row }">
            <span>{{ dataScopeText(row.dataScope) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="hasPerm('role:update')"
              type="primary"
              link
              size="small"
              @click="openRoleDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="hasPerm('role:assign-user')"
              type="primary"
              link
              size="small"
              @click="openAssignDialog(row)"
            >
              分配用户
            </el-button>
            <el-button
              v-if="hasPerm('role:delete')"
              type="danger"
              link
              size="small"
              @click="doDeleteRole(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog v-model="roleDialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="520px" @closed="resetRoleForm">
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" placeholder="请输入角色名称" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model="roleForm.roleCode"
            placeholder="ROLE_ 前缀，大写字母数字下划线"
            maxlength="64"
            show-word-limit
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="roleForm.dataScope" style="width:100%">
            <el-option label="本人数据" :value="1" />
            <el-option label="本部门数据" :value="2" />
            <el-option label="全部数据" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="2" placeholder="角色描述" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="roleForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="onRoleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配用户弹窗 -->
    <el-dialog v-model="assignDialogVisible" title="分配用户" width="860px" @closed="resetAssign">
      <div style="display:flex;gap:12px;height:420px">
        <!-- 左侧：未分配用户 -->
        <div style="flex:1;display:flex;flex-direction:column;border:1px solid #ebeef5;border-radius:4px">
          <div style="padding:10px;border-bottom:1px solid #ebeef5">
            <div style="font-weight:600;margin-bottom:8px">未分配用户</div>
            <el-input v-model="unassignedQuery.keyword" placeholder="搜索用户名/姓名" clearable size="small" @keyup.enter="loadUnassigned">
              <template #append>
                <el-button :icon="Search" @click="loadUnassigned" />
              </template>
            </el-input>
          </div>
          <el-table
            v-loading="unassignedLoading"
            :data="unassignedUsers"
            size="small"
            height="280"
            @selection-change="onUnassignedSelect"
          >
            <el-table-column type="selection" width="40" />
            <el-table-column prop="username" label="用户名" width="100" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="departmentName" label="部门" min-width="100" />
          </el-table>
          <div style="padding:8px;text-align:right">
            <el-pagination
              small
              layout="prev, pager, next"
              :total="unassignedTotal"
              :page-size="unassignedQuery.size"
              v-model:current-page="unassignedQuery.page"
              @current-change="loadUnassigned"
            />
          </div>
        </div>

        <!-- 中间操作按钮 -->
        <div style="display:flex;flex-direction:column;justify-content:center;gap:12px">
          <el-button type="primary" :icon="ArrowRight" :disabled="!selectedUnassigned.length" @click="addUsers">添加</el-button>
          <el-button type="danger" :icon="ArrowLeft" :disabled="!selectedAssigned.length" @click="removeUsers">移除</el-button>
        </div>

        <!-- 右侧：已分配用户 -->
        <div style="flex:1;display:flex;flex-direction:column;border:1px solid #ebeef5;border-radius:4px">
          <div style="padding:10px;border-bottom:1px solid #ebeef5">
            <div style="font-weight:600;margin-bottom:8px">已分配用户</div>
            <el-input v-model="assignedQuery.keyword" placeholder="搜索用户名/姓名" clearable size="small" @keyup.enter="loadAssigned">
              <template #append>
                <el-button :icon="Search" @click="loadAssigned" />
              </template>
            </el-input>
          </div>
          <el-table
            v-loading="assignedLoading"
            :data="assignedUsers"
            size="small"
            height="280"
            @selection-change="onAssignedSelect"
          >
            <el-table-column type="selection" width="40" />
            <el-table-column prop="username" label="用户名" width="100" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="departmentName" label="部门" min-width="100" />
          </el-table>
          <div style="padding:8px;text-align:right">
            <el-pagination
              small
              layout="prev, pager, next"
              :total="assignedTotal"
              :page-size="assignedQuery.size"
              v-model:current-page="assignedQuery.page"
              @current-change="loadAssigned"
            />
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignSaving" @click="saveAssign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search, ArrowRight, ArrowLeft } from '@element-plus/icons-vue'
import {
  listRoles,
  createRole,
  updateRole,
  deleteRole,
  assignRoleUsers,
  listUsers,
  type Role,
  type SystemUser,
  type Id,
} from '@/api/system'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const hasPerm = (perm: string) => userStore.hasPermission(perm)

const loading = ref(false)
const roles = ref<Role[]>([])

// 角色弹窗
const roleDialogVisible = ref(false)
const isEdit = ref(false)
const roleSubmitting = ref(false)
const roleFormRef = ref<FormInstance>()
const roleForm = reactive({
  id: undefined as Id | undefined,
  roleName: '',
  roleCode: '',
  dataScope: 2,
  description: '',
  status: 1,
})
const roleRules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

function dataScopeText(scope?: number) {
  const map: Record<number, string> = { 1: '本人数据', 2: '本部门数据', 4: '全部数据' }
  return map[scope ?? 2] || '未知'
}

async function loadRoles() {
  loading.value = true
  try {
    roles.value = await listRoles()
  } finally {
    loading.value = false
  }
}

function openRoleDialog(row?: any) {
  if (row) {
    isEdit.value = true
    roleForm.id = row.id
    roleForm.roleName = row.roleName
    roleForm.roleCode = row.roleCode
    roleForm.dataScope = row.dataScope ?? 2
    roleForm.description = row.description || ''
    roleForm.status = row.status ?? 1
  } else {
    isEdit.value = false
    resetRoleForm()
  }
  roleDialogVisible.value = true
}

function resetRoleForm() {
  roleForm.id = undefined
  roleForm.roleName = ''
  roleForm.roleCode = ''
  roleForm.dataScope = 2
  roleForm.description = ''
  roleForm.status = 1
  roleFormRef.value?.resetFields()
}

async function onRoleSubmit() {
  const valid = await roleFormRef.value?.validate().catch(() => false)
  if (!valid) return
  roleSubmitting.value = true
  try {
    if (isEdit.value && roleForm.id != null) {
      await updateRole(roleForm.id, {
        roleName: roleForm.roleName,
        dataScope: roleForm.dataScope,
        description: roleForm.description,
        status: roleForm.status,
      })
      ElMessage.success('更新成功')
    } else {
      await createRole({
        roleName: roleForm.roleName,
        roleCode: roleForm.roleCode,
        dataScope: roleForm.dataScope,
        description: roleForm.description,
        status: roleForm.status,
      })
      ElMessage.success('创建成功')
    }
    roleDialogVisible.value = false
    await loadRoles()
  } finally {
    roleSubmitting.value = false
  }
}

async function doDeleteRole(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除角色 "${row.roleName}" 吗？`, '提示', { type: 'warning' })
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    await loadRoles()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

// 分配用户弹窗
const assignDialogVisible = ref(false)
const currentRole = ref<Role | null>(null)
const assignSaving = ref(false)

const unassignedLoading = ref(false)
const unassignedUsers = ref<SystemUser[]>([])
const unassignedTotal = ref(0)
const selectedUnassigned = ref<SystemUser[]>([])
const unassignedQuery = reactive({ page: 1, size: 10, keyword: '' })

const assignedLoading = ref(false)
const assignedUsers = ref<SystemUser[]>([])
const assignedTotal = ref(0)
const selectedAssigned = ref<SystemUser[]>([])
const assignedQuery = reactive({ page: 1, size: 10, keyword: '' })

function openAssignDialog(role: any) {
  currentRole.value = role
  assignDialogVisible.value = true
  resetAssign()
  loadUnassigned()
  loadAssigned()
}

function resetAssign() {
  unassignedQuery.page = 1
  unassignedQuery.keyword = ''
  assignedQuery.page = 1
  assignedQuery.keyword = ''
  selectedUnassigned.value = []
  selectedAssigned.value = []
}

async function loadUnassigned() {
  if (!currentRole.value) return
  unassignedLoading.value = true
  try {
    const res = await listUsers({
      page: unassignedQuery.page,
      size: unassignedQuery.size,
      username: unassignedQuery.keyword,
      realName: unassignedQuery.keyword,
      excludeRoleId: currentRole.value.id,
    })
    unassignedUsers.value = res.records || []
    unassignedTotal.value = res.total || 0
  } finally {
    unassignedLoading.value = false
  }
}

async function loadAssigned() {
  if (!currentRole.value) return
  assignedLoading.value = true
  try {
    const res = await listUsers({
      page: assignedQuery.page,
      size: assignedQuery.size,
      username: assignedQuery.keyword,
      realName: assignedQuery.keyword,
      roleId: currentRole.value.id,
    })
    assignedUsers.value = res.records || []
    assignedTotal.value = res.total || 0
  } finally {
    assignedLoading.value = false
  }
}

function onUnassignedSelect(val: SystemUser[]) {
  selectedUnassigned.value = val
}

function onAssignedSelect(val: SystemUser[]) {
  selectedAssigned.value = val
}

function addUsers() {
  for (const u of selectedUnassigned.value) {
    if (!assignedUsers.value.some((a) => a.id === u.id)) {
      assignedUsers.value.unshift(u)
      assignedTotal.value++
    }
  }
  // 从未分配列表中移除
  const ids = new Set(selectedUnassigned.value.map((u) => u.id))
  unassignedUsers.value = unassignedUsers.value.filter((u) => !ids.has(u.id))
  unassignedTotal.value = Math.max(0, unassignedTotal.value - ids.size)
  selectedUnassigned.value = []
}

function removeUsers() {
  for (const u of selectedAssigned.value) {
    if (!unassignedUsers.value.some((a) => a.id === u.id)) {
      unassignedUsers.value.unshift(u)
      unassignedTotal.value++
    }
  }
  const ids = new Set(selectedAssigned.value.map((u) => u.id))
  assignedUsers.value = assignedUsers.value.filter((u) => !ids.has(u.id))
  assignedTotal.value = Math.max(0, assignedTotal.value - ids.size)
  selectedAssigned.value = []
}

async function saveAssign() {
  if (!currentRole.value) return
  assignSaving.value = true
  try {
    const userIds = assignedUsers.value.map((u) => u.id)
    await assignRoleUsers(currentRole.value.id, userIds as Id[])
    ElMessage.success('保存成功')
    assignDialogVisible.value = false
  } finally {
    assignSaving.value = false
  }
}

onMounted(loadRoles)
</script>
