<template>
  <div class="page-container">
    <div class="page-header">
      <h2>角色管理</h2>
      <p class="page-desc">管理角色与权限分配，控制用户可访问的功能模块</p>
    </div>
    <div style="display:flex;gap:16px">
      <!-- 角色列表 -->
      <div class="role-panel">
        <div style="padding:14px 16px;border-bottom:1px solid #f0f0f0;display:flex;justify-content:space-between;align-items:center">
          <h4 style="font-size:14px;font-weight:600">角色列表</h4>
          <el-button type="primary" size="small" :icon="Plus">新增</el-button>
        </div>
        <div class="role-list">
          <div
            v-for="role in roles"
            :key="role.id"
            class="role-item"
            :class="{ active: selectedRole?.id === role.id }"
            @click="selectedRole = role"
          >
            <div class="role-name">{{ role.roleName }}</div>
            <div class="role-code">{{ role.roleCode }}</div>
            <span class="role-count">{{ role.userCount }} 人</span>
          </div>
        </div>
      </div>

      <!-- 权限配置 -->
      <div class="perm-panel">
        <div style="padding:14px 16px;border-bottom:1px solid #f0f0f0">
          <h4 style="font-size:14px;font-weight:600">权限配置 — {{ selectedRole?.roleName || '请选择角色' }}</h4>
        </div>
        <div class="perm-content" v-if="selectedRole">
          <el-tree
            :data="permTree"
            show-checkbox
            node-key="id"
            default-expand-all
            :default-checked-keys="selectedRole.permissions"
            style="padding:16px"
          />
        </div>
        <div v-else class="perm-empty">
          <el-icon :size="48" color="#dcdfe6"><Setting /></el-icon>
          <p>选择左侧角色以配置权限</p>
        </div>
        <div v-if="selectedRole" style="padding:12px 16px;border-top:1px solid #f0f0f0;text-align:right">
          <el-button>重置</el-button>
          <el-button type="primary">保存权限</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Setting } from '@element-plus/icons-vue'

const selectedRole = ref<any>(null)

const roles = [
  { id: 1, roleName: '超级管理员', roleCode: 'ROLE_ADMIN', userCount: 1, permissions: [1,2,3,4,5,6,7,8,9,10,11] },
  { id: 2, roleName: '销售经理', roleCode: 'ROLE_SALES_MGR', userCount: 2, permissions: [1,4,5] },
  { id: 3, roleName: '销售员', roleCode: 'ROLE_SALES', userCount: 5, permissions: [1,4] },
  { id: 4, roleName: '采购经理', roleCode: 'ROLE_PURCHASE_MGR', userCount: 1, permissions: [1,6] },
  { id: 5, roleName: '采购员', roleCode: 'ROLE_PURCHASE', userCount: 3, permissions: [1,6] },
  { id: 6, roleName: '财务主管', roleCode: 'ROLE_FINANCE_MGR', userCount: 1, permissions: [1,7,8,9] },
  { id: 7, roleName: '数据分析师', roleCode: 'ROLE_DATA', userCount: 2, permissions: [1,10] },
]

const permTree = [
  { id: 1, label: '仪表盘' },
  { id: 2, label: '系统管理', children: [
    { id: 3, label: '用户管理' },
    { id: 13, label: '角色管理' },
  ]},
  { id: 4, label: '订单管理', children: [
    { id: 5, label: '销售订单' },
    { id: 6, label: '采购订单' },
  ]},
  { id: 7, label: '财务管理', children: [
    { id: 8, label: '财务概览' },
    { id: 9, label: '汇率管理' },
    { id: 14, label: '资金审批' },
  ]},
  { id: 10, label: '数据中心', children: [
    { id: 11, label: '数据上传' },
    { id: 12, label: '定价分析' },
  ]},
]
</script>

<style scoped lang="scss">
.role-panel {
  width: 280px; flex-shrink: 0;
  background: #fff; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  overflow: hidden;
}
.role-list {
  max-height: 500px; overflow-y: auto;
}
.role-item {
  padding: 14px 16px; cursor: pointer;
  border-bottom: 1px solid #fafafa;
  transition: background 0.15s;
  &:hover { background: #f5f7fa; }
  &.active { background: #ecf5ff; border-left: 3px solid var(--el-color-primary); padding-left:13px; }
  .role-name { font-size: 14px; font-weight: 500; color: #303133; }
  .role-code { font-size: 12px; color: #909399; margin-top: 2px; }
  .role-count { font-size: 12px; color: #c0c4cc; }
}
.perm-panel {
  flex: 1;
  background: #fff; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  overflow: hidden;
  display: flex; flex-direction: column;
}
.perm-content { flex: 1; overflow-y: auto; }
.perm-empty {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  color: #c0c4cc; gap: 12px;
  p { font-size: 14px; }
}
</style>
