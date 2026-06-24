<template>
  <div class="page-container">
    <div class="page-header">
      <h2>用户管理</h2>
      <p class="page-desc">管理系统用户账号、部门归属及状态</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="用户名">
          <el-input v-model="query.keyword" placeholder="用户名/姓名" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="query.department" placeholder="全部" clearable style="width:130px">
            <el-option label="销售部" value="SALES" />
            <el-option label="采购部" value="PURCHASE" />
            <el-option label="数据部" value="DATA" />
            <el-option label="财务部" value="FINANCE" />
            <el-option label="管理部" value="MANAGEMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:100px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Plus">新增用户</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>用户列表</h3>
      </div>
      <el-table :data="users" stripe size="small">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="department" label="部门" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="deptType(row.department)">{{ deptLabel(row.department) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="110" />
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default>
            <el-button type="primary" link size="small">编辑</el-button>
            <el-button type="primary" link size="small">分配角色</el-button>
            <el-button type="danger" link size="small">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination :current-page="1" :page-size="20" :total="18" layout="total, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'

const query = reactive({ keyword: '', department: '', status: null })

function deptType(d: string) {
  const m: Record<string, string> = { SALES: 'success', PURCHASE: 'warning', DATA: '', FINANCE: 'danger', MANAGEMENT: 'primary' }
  return m[d] || 'info'
}
function deptLabel(d: string) {
  const m: Record<string, string> = { SALES: '销售部', PURCHASE: '采购部', DATA: '数据部', FINANCE: '财务部', MANAGEMENT: '管理部' }
  return m[d] || d
}

const users = [
  { username: 'admin', realName: '系统管理员', department: 'MANAGEMENT', role: '超级管理员', email: 'admin@company.com', phone: '13800000000', status: 1, createdAt: '2026-01-01' },
  { username: 'zhangsan', realName: '张三', department: 'SALES', role: '销售经理', email: 'zhangsan@company.com', phone: '13800138001', status: 1, createdAt: '2026-01-15' },
  { username: 'lisi', realName: '李四', department: 'SALES', role: '销售员', email: 'lisi@company.com', phone: '13800138002', status: 1, createdAt: '2026-01-15' },
  { username: 'wangwu', realName: '王五', department: 'PURCHASE', role: '采购经理', email: 'wangwu@company.com', phone: '13800138003', status: 1, createdAt: '2026-01-20' },
  { username: 'zhaoliu', realName: '赵六', department: 'FINANCE', role: '财务主管', email: 'zhaoliu@company.com', phone: '13800138004', status: 1, createdAt: '2026-02-01' },
  { username: 'chenqi', realName: '陈七', department: 'DATA', role: '数据分析师', email: 'chenqi@company.com', phone: '13800138005', status: 1, createdAt: '2026-02-10' },
  { username: 'sunba', realName: '孙八', department: 'PURCHASE', role: '采购员', email: 'sunba@company.com', phone: '13800138006', status: 0, createdAt: '2026-03-01' },
]
</script>
