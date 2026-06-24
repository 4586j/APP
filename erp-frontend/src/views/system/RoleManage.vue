<template>
  <div class="page-container">
    <div class="page-header">
      <h2>角色管理</h2>
      <p class="page-desc">管理角色与权限分配，控制用户可访问的功能模块</p>
    </div>
    <div style="display:flex;gap:16px">
      <div class="role-panel">
        <div style="padding:14px 16px;border-bottom:1px solid #f0f0f0;display:flex;justify-content:space-between;align-items:center">
          <h4 style="font-size:14px;font-weight:600">角色列表</h4>
          <el-tag type="success" size="small">DB</el-tag>
        </div>
        <div v-loading="loading" class="role-list">
          <div
            v-for="role in roles"
            :key="role.id"
            class="role-item"
            :class="{ active: selectedRole?.id === role.id }"
            @click="selectRole(role)"
          >
            <div class="role-name">{{ role.roleName }}</div>
            <div class="role-code">{{ role.roleCode }}</div>
            <span class="role-count">{{ role.permissionIds?.length || 0 }} 项权限</span>
          </div>
        </div>
      </div>

      <div class="perm-panel">
        <div style="padding:14px 16px;border-bottom:1px solid #f0f0f0">
          <h4 style="font-size:14px;font-weight:600">权限配置 — {{ selectedRole?.roleName || '请选择角色' }}</h4>
        </div>
        <div class="perm-content" v-if="selectedRole">
          <el-tree
            ref="treeRef"
            :data="permTree"
            show-checkbox
            node-key="id"
            default-expand-all
            :props="{ label: 'name', children: 'children' }"
            style="padding:16px"
          >
            <template #default="{ data }">
              <span>
                {{ data.name }}
                <el-tag size="small" style="margin-left:6px">{{ data.code }}</el-tag>
                <el-tag v-if="data.type === 'button'" type="warning" size="small" style="margin-left:4px">按钮</el-tag>
              </span>
            </template>
          </el-tree>
        </div>
        <div v-else class="perm-empty">
          <el-icon :size="48" color="#dcdfe6"><Setting /></el-icon>
          <p>选择左侧角色以配置权限</p>
        </div>
        <div v-if="selectedRole" style="padding:12px 16px;border-top:1px solid #f0f0f0;text-align:right">
          <el-button @click="resetChecked">重置</el-button>
          <el-button type="primary" :loading="saving" @click="savePermissions">保存权限</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import { assignRolePermissions, listPermissionTree, listRoles, type PermissionNode, type Role } from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const roles = ref<Role[]>([])
const permTree = ref<PermissionNode[]>([])
const selectedRole = ref<Role | null>(null)
const treeRef = ref<any>()

async function loadData() {
  loading.value = true
  try {
    const [roleData, treeData] = await Promise.all([listRoles(), listPermissionTree()])
    roles.value = roleData
    permTree.value = treeData
    if (!selectedRole.value && roles.value.length) selectRole(roles.value[0])
  } finally {
    loading.value = false
  }
}

function selectRole(role: Role) {
  selectedRole.value = role
  nextTick(resetChecked)
}

function resetChecked() {
  if (!selectedRole.value) return
  treeRef.value?.setCheckedKeys(selectedRole.value.permissionIds || [])
}

async function savePermissions() {
  if (!selectedRole.value) return
  saving.value = true
  try {
    const checked = treeRef.value?.getCheckedKeys(false) || []
    const halfChecked = treeRef.value?.getHalfCheckedKeys?.() || []
    const ids = Array.from(new Set([...checked, ...halfChecked])).map(Number)
    await assignRolePermissions(selectedRole.value.id, ids)
    selectedRole.value.permissionIds = ids
    ElMessage.success('权限保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.role-panel {
  width: 280px; flex-shrink: 0;
  background: #fff; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  overflow: hidden;
}
.role-list { max-height: 500px; overflow-y: auto; }
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
