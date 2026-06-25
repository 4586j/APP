<template>
  <div class="page-container">
    <div class="page-header">
      <h2>用户管理</h2>
      <p class="page-desc">管理系统用户账号、部门归属及状态</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="用户名" clearable style="width:160px" @keyup.enter="loadUsers" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" placeholder="姓名" clearable style="width:140px" @keyup.enter="loadUsers" />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="query.departmentId"
            :data="departmentOptions"
            check-strictly
            placeholder="全部"
            clearable
            style="width:160px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:100px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadUsers">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-container">
      <div class="table-header">
        <h3>用户列表</h3>
        <div style="display:flex;gap:8px">
          <el-button
            v-if="hasPerm('user:create')"
            type="primary"
            :icon="Plus"
            @click="openUserDialog()"
          >
            新建用户
          </el-button>
          <el-button
            v-if="hasPerm('user:create')"
            :icon="Upload"
            @click="openImportDialog"
          >
            批量导入
          </el-button>
          <el-button :icon="Download" @click="doExport">导出</el-button>
        </div>
      </div>
      <el-table v-loading="loading" :data="users as any[]" stripe size="small">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="departmentName" label="部门" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.departmentName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="role in row.roleCodes || []" :key="role" size="small" style="margin-right:4px">
              {{ role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="170" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="hasPerm('user:update')"
              type="primary"
              link
              size="small"
              @click="openUserDialog(row)"
            >
              编辑
            </el-button>
            <el-popconfirm title="确定删除该用户？" @confirm="doDelete(row.id)"
            >
              <template #reference>
                <el-button
                  v-if="hasPerm('user:delete')"
                  type="danger"
                  link
                  size="small"
                  :disabled="row.username === 'admin'"
                >
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="loadUsers"
          @size-change="loadUsers"
        />
      </div>
    </div>

    <!-- 新建/编辑用户弹窗 -->
    <el-dialog v-model="userDialogVisible" :title="isEdit ? '编辑用户' : '新建用户'" width="560px" @closed="resetUserForm">
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="3-32位字母开头" :disabled="isEdit" maxlength="32" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" maxlength="64" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" placeholder="密码长度8-64位" show-password />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="确认密码" prop="confirmPassword">
          <el-input v-model="userForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="example@domain.com" maxlength="128" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="11位手机号" maxlength="32" />
        </el-form-item>
        <el-form-item label="所属部门">
          <DepartmentSelect v-model="userForm.departmentId" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.roleIds" multiple placeholder="请选择角色" style="width:100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="userForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="userSubmitting" @click="onUserSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="importDialogVisible" title="批量导入用户" width="720px" @closed="resetImport">
      <div style="margin-bottom:16px">
        <el-button :icon="Document" @click="downloadTemplate">下载模板</el-button>
      </div>

      <el-upload
        ref="uploadRef"
        drag
        action="#"
        :auto-upload="false"
        :limit="1"
        :on-change="onFileChange"
        accept=".xlsx,.xls"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽 Excel 文件到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 .xlsx / .xls，文件大小不超过 10MB</div>
        </template>
      </el-upload>

      <div v-if="previewData.length" style="margin-top:16px">
        <h4>数据预览（前10行）</h4>
        <el-table :data="previewData" size="small" border style="margin-top:8px">
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="realName" label="姓名" width="100" />
          <el-table-column prop="email" label="邮箱" min-width="160" />
          <el-table-column prop="phone" label="手机号" width="120" />
          <el-table-column prop="departmentName" label="部门" width="120" />
          <el-table-column prop="roleCode" label="角色编码" width="120" />
        </el-table>
      </div>

      <div v-if="importResult" style="margin-top:16px">
        <el-alert
          :title="`导入完成：成功 ${importResult.successCount} 条，失败 ${importResult.failList.length} 条`"
          :type="importResult.failList.length ? 'warning' : 'success'"
          show-icon
        />
        <el-table v-if="importResult.failList.length" :data="importResult.failList" size="small" border style="margin-top:8px">
          <el-table-column prop="index" label="Excel行号" width="100" />
          <el-table-column prop="name" label="用户名" width="120" />
          <el-table-column prop="reason" label="失败原因" />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" :disabled="!selectedFile" @click="doImport">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile, type UploadInstance } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Upload,
  Download,
  UploadFilled,
  Document,
} from '@element-plus/icons-vue'
import {
  listDepartments,
  listUsers,
  listRoles,
  createUser,
  updateUser,
  deleteUser,
  getUser,
  assignUserRoles,
  importUsersExcel,
  downloadUserTemplate,
  type DepartmentNode,
  type SystemUser,
  type UserQuery,
  type Role,
  type Id,
} from '@/api/system'
import { useUserStore } from '@/store/user'
import * as XLSX from 'xlsx'

const userStore = useUserStore()
const hasPerm = (perm: string) => userStore.hasPermission(perm)

const loading = ref(false)
const users = ref<SystemUser[]>([])
const total = ref(0)
type TreeSelectNode = { label: string; value: Id; children?: TreeSelectNode[] }
const departmentOptions = ref<TreeSelectNode[]>([])
const query = reactive<UserQuery>({ page: 1, size: 10 })

async function loadUsers() {
  loading.value = true
  try {
    const data = await listUsers(query)
    users.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function toTreeSelect(nodes: DepartmentNode[]): TreeSelectNode[] {
  return (nodes || []).map((node) => ({
    label: node.name,
    value: node.id,
    children: node.children?.length ? toTreeSelect(node.children) : undefined,
  }))
}

async function loadDepartments() {
  departmentOptions.value = toTreeSelect(await listDepartments())
}

function resetQuery() {
  query.username = undefined
  query.realName = undefined
  query.departmentId = undefined
  query.status = undefined
  query.page = 1
  loadUsers()
}

async function doDelete(id: Id) {
  try {
    await deleteUser(id)
    ElMessage.success('删除成功')
    loadUsers()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

// 用户弹窗
const userDialogVisible = ref(false)
const isEdit = ref(false)
const userSubmitting = ref(false)
const userFormRef = ref<FormInstance>()
const roleOptions = ref<Role[]>([])

const userForm = reactive({
  id: undefined as Id | undefined,
  username: '',
  realName: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  departmentId: undefined as Id | undefined,
  roleIds: [] as Id[],
  status: 1,
})

const validateConfirmPassword = (_rule: any, value: string, callback: Function) => {
  if (!isEdit.value && value !== userForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const userRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]{2,31}$/, message: '3-32位，字母开头，仅含字母数字下划线', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  email: [
    { pattern: /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/, message: '邮箱格式不正确', trigger: 'blur' },
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
}

async function loadRoleOptions() {
  try {
    roleOptions.value = await listRoles()
  } catch (e) {
    console.error('Failed to load roles:', e)
  }
}

async function openUserDialog(row?: any) {
  await loadRoleOptions()
  if (row) {
    isEdit.value = true
    // 拉取最新详情（包含 roleIds）
    try {
      const detail = await getUser(row.id)
      userForm.id = detail.id
      userForm.username = detail.username
      userForm.realName = detail.realName || ''
      userForm.email = detail.email || ''
      userForm.phone = detail.phone || ''
      userForm.departmentId = detail.departmentId ?? undefined
      userForm.roleIds = detail.roleIds || []
      userForm.status = detail.status ?? 1
      userForm.password = ''
      userForm.confirmPassword = ''
    } catch (e) {
      // 降级使用列表数据
      userForm.id = row.id
      userForm.username = row.username
      userForm.realName = row.realName || ''
      userForm.email = row.email || ''
      userForm.phone = row.phone || ''
      userForm.departmentId = row.departmentId ?? undefined
      userForm.roleIds = []
      userForm.status = row.status ?? 1
    }
  } else {
    isEdit.value = false
    resetUserForm()
  }
  userDialogVisible.value = true
}

function resetUserForm() {
  userForm.id = undefined
  userForm.username = ''
  userForm.realName = ''
  userForm.password = ''
  userForm.confirmPassword = ''
  userForm.email = ''
  userForm.phone = ''
  userForm.departmentId = undefined
  userForm.roleIds = []
  userForm.status = 1
  userFormRef.value?.resetFields()
}

async function onUserSubmit() {
  const valid = await userFormRef.value?.validate().catch(() => false)
  if (!valid) return
  userSubmitting.value = true
  try {
    const deptId = typeof userForm.departmentId === 'string'
      ? Number(userForm.departmentId) || undefined
      : (userForm.departmentId ?? undefined)

    if (isEdit.value && userForm.id != null) {
      await updateUser(userForm.id, {
        realName: userForm.realName,
        email: userForm.email || undefined,
        phone: userForm.phone || undefined,
        departmentId: deptId,
        status: userForm.status,
      })
      // 更新角色
      if (userForm.roleIds.length) {
        await assignUserRoles(userForm.id, userForm.roleIds)
      }
      ElMessage.success('更新成功')
    } else {
      const newId = await createUser({
        username: userForm.username,
        password: userForm.password,
        realName: userForm.realName,
        email: userForm.email || undefined,
        phone: userForm.phone || undefined,
        departmentId: deptId,
        roleIds: userForm.roleIds.length ? userForm.roleIds : undefined,
        status: userForm.status,
      })
      ElMessage.success('创建成功')
    }
    userDialogVisible.value = false
    await loadUsers()
  } finally {
    userSubmitting.value = false
  }
}

// 导入弹窗
const importDialogVisible = ref(false)
const uploadRef = ref<UploadInstance>()
const selectedFile = ref<File | null>(null)
const previewData = ref<any[]>([])
const importResult = ref<{ successCount: number; failList: any[] } | null>(null)
const importing = ref(false)

function openImportDialog() {
  importDialogVisible.value = true
  resetImport()
}

function resetImport() {
  selectedFile.value = null
  previewData.value = []
  importResult.value = null
  uploadRef.value?.clearFiles()
}

function onFileChange(file: UploadFile) {
  selectedFile.value = file.raw || null
  previewData.value = []
  importResult.value = null
  if (file.raw) {
    readExcelPreview(file.raw)
  }
}

async function readExcelPreview(file: File) {
  try {
    const data = await file.arrayBuffer()
    const workbook = XLSX.read(data)
    const worksheet = workbook.Sheets[workbook.SheetNames[0]]
    const rows = XLSX.utils.sheet_to_json(worksheet, { header: 1 }) as any[][]
    if (!rows.length) {
      previewData.value = []
      return
    }
    const headers = rows[0] as string[]
    // 映射列索引
    const colIndex: Record<string, number> = {}
    headers.forEach((h, i) => { colIndex[String(h).trim()] = i })
    // 取前10行数据（不含表头）
    const dataRows = rows.slice(1, 11)
    previewData.value = dataRows.map((row) => ({
      username: row[colIndex['用户名'] ?? colIndex['username'] ?? 0] || '',
      realName: row[colIndex['姓名'] ?? colIndex['realName'] ?? 1] || '',
      email: row[colIndex['邮箱'] ?? colIndex['email'] ?? 2] || '',
      phone: row[colIndex['手机号'] ?? colIndex['phone'] ?? 3] || '',
      departmentName: row[colIndex['部门'] ?? colIndex['departmentName'] ?? 4] || '',
      roleCode: row[colIndex['角色编码'] ?? colIndex['roleCode'] ?? 5] || '',
    }))
  } catch (e) {
    console.error('Excel preview failed:', e)
    previewData.value = [{ username: '文件解析失败，请确认是 .xlsx 格式', realName: '-', email: '-', phone: '-', departmentName: '-', roleCode: '-' }]
  }
}

async function doImport() {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  importing.value = true
  try {
    const res = await importUsersExcel(selectedFile.value)
    importResult.value = res
    if (res.failList.length === 0) {
      ElMessage.success(`成功导入 ${res.successCount} 条`)
    }
    await loadUsers()
  } catch (e: any) {
    ElMessage.error(e?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

async function downloadTemplate() {
  try {
    const blob = await downloadUserTemplate()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '用户导入模板.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
  } catch (e) {
    ElMessage.error('模板下载失败')
  }
}

function doExport() {
  // 简单导出当前页数据为 CSV
  const headers = ['用户名', '姓名', '部门', '邮箱', '手机号', '状态', '创建时间']
  const rows = users.value.map((u) => [
    u.username,
    u.realName,
    u.departmentName || '',
    u.email || '',
    u.phone || '',
    u.status === 1 ? '启用' : '禁用',
    u.createdAt || '',
  ])
  const csv = [headers, ...rows].map((r) => r.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `用户列表_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

onMounted(() => {
  loadDepartments()
  loadUsers()
})
</script>
