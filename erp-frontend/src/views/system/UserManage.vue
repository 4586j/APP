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
        <el-tag type="success" size="small">MySQL 实时数据</el-tag>
      </div>
      <el-table v-loading="loading" :data="users" stripe size="small">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="departmentName" label="部门" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.departmentName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="role in row.roleCodes || []" :key="role" size="small" style="margin-right:4px">{{ role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="170" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="showPerms(row)">权限</el-button>
            <el-button type="danger" link size="small" :disabled="row.username === 'admin'">删除</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { listDepartments, listUsers, type DepartmentNode, type SystemUser, type UserQuery } from '@/api/system'

const loading = ref(false)
const users = ref<SystemUser[]>([])
const total = ref(0)
type TreeSelectNode = { label: string; value: number; children?: TreeSelectNode[] }
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

function showPerms(row: any) {
  ElMessageBox.alert((row.permCodes || []).join('\n') || '暂无权限', `${row.username} 权限`, {
    customClass: 'perm-dialog',
  })
}

onMounted(() => {
  loadDepartments()
  loadUsers()
})
</script>
