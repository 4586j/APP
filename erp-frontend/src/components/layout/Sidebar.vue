<template>
  <div class="sidebar" :class="{ collapsed }">
    <div class="logo">
      <el-icon :size="22"><Box /></el-icon>
      <span v-show="!collapsed" class="logo-text">外贸 ERP</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="collapsed"
      :router="true"
      background-color="#001529"
      text-color="#ffffffb3"
      active-text-color="#fff"
      class="side-menu"
    >
      <template v-for="item in visibleMenuItems" :key="item.path">
        <el-sub-menu v-if="item.children?.length" :index="item.path">
          <template #title>
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </template>
          <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
            <span>{{ child.title }}</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </template>
    </el-menu>
    <div class="sidebar-footer" v-show="!collapsed">
      <span>v1.0.0</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'

defineProps<{ collapsed: boolean }>()

interface MenuItem {
  path: string
  title: string
  icon?: string
  permission?: string
  alwaysShow?: boolean
  children?: MenuItem[]
}

const route = useRoute()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)

const menuItems: MenuItem[] = [
  { path: '/dashboard', title: '仪表盘', icon: 'Odometer', alwaysShow: true },
  {
    path: '/order', title: '订单管理', icon: 'Document', permission: 'order:view', children: [
      { path: '/order/sales', title: '销售订单', permission: 'order:view' },
      { path: '/order/purchase', title: '采购订单', permission: 'order:view' },
    ],
  },
  { path: '/product', title: '产品管理', icon: 'Goods', permission: 'product:view' },
  {
    path: '/customer', title: '客户管理', icon: 'User', permission: 'customer:view', children: [
      { path: '/customer/list', title: '客户列表', permission: 'customer:view' },
      { path: '/customer/supplier', title: '供应商列表', permission: 'supplier:view' },
    ],
  },
  {
    path: '/finance', title: '财务管理', icon: 'Money', permission: 'finance:view', children: [
      { path: '/finance/overview', title: '财务概览', permission: 'finance:view' },
      { path: '/finance/exchange-rate', title: '汇率管理', permission: 'exchange-rate:view' },
      { path: '/finance/fund', title: '资金审批', permission: 'fund:view' },
    ],
  },
  { path: '/logistics', title: '物流管理', icon: 'Ship', permission: 'logistics:view' },
  { path: '/document', title: '单证管理', icon: 'Files', permission: 'document:view' },
  {
    path: '/data', title: '数据中心', icon: 'DataAnalysis', permission: 'data:view', children: [
      { path: '/data/upload', title: '企业网盘', permission: 'data:view' },
      { path: '/data/analysis', title: '定价分析', permission: 'data:view' },
    ],
  },
  { path: '/approval', title: '审批中心', icon: 'Stamp', permission: 'approval:view' },
  { path: '/report', title: '工作报表', icon: 'Memo', permission: 'work:report:manage' },
  {
    path: '/system', title: '系统管理', icon: 'Setting', permission: 'system', children: [
      { path: '/system/user', title: '用户管理', permission: 'system:user' },
      { path: '/system/role', title: '角色管理', permission: 'system:role' },
      { path: '/system/department', title: '部门管理', permission: 'system:dept' },
      { path: '/system/permission', title: '权限管理', permission: 'system:perm' },
    ],
  },
]

function canShow(item: MenuItem) {
  const permissions = userStore.permissions
  return item.alwaysShow || !item.permission || permissions.includes(item.permission)
}

const visibleMenuItems = computed(() => menuItems
  .map((item) => {
    const children = item.children?.filter(canShow)
    return { ...item, children }
  })
  .filter((item) => canShow(item) || Boolean(item.children?.length)))
</script>

<style scoped lang="scss">
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 220px;
  background: #001529;
  display: flex;
  flex-direction: column;
  transition: width 0.28s;
  z-index: 1001;
  overflow-x: hidden;

  &.collapsed { width: 64px; }
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
  .logo-text { white-space: nowrap; }
}
.side-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
  overflow-x: hidden;
  &:not(.el-menu--collapse) { width: 220px; }
}
.sidebar-footer {
  padding: 12px 20px;
  color: rgba(255, 255, 255, 0.35);
  font-size: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
}
</style>
