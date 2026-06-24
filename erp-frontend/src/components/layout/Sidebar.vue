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
      <template v-for="item in menuItems" :key="item.path">
        <el-sub-menu v-if="item.children" :index="item.path">
          <template #title>
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </template>
          <el-menu-item
            v-for="child in item.children"
            :key="child.path"
            :index="child.path"
          >
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

defineProps<{ collapsed: boolean }>()

const route = useRoute()
const activeMenu = computed(() => route.path)

const menuItems = [
  { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
  {
    path: '/order',
    title: '订单管理',
    icon: 'Document',
    children: [
      { path: '/order/sales', title: '销售订单' },
      { path: '/order/purchase', title: '采购订单' },
    ],
  },
  { path: '/product', title: '产品管理', icon: 'Goods' },
  {
    path: '/customer',
    title: '客户管理',
    icon: 'User',
    children: [
      { path: '/customer/list', title: '客户列表' },
      { path: '/customer/supplier', title: '供应商列表' },
    ],
  },
  {
    path: '/finance',
    title: '财务管理',
    icon: 'Money',
    children: [
      { path: '/finance/overview', title: '财务概览' },
      { path: '/finance/exchange-rate', title: '汇率管理' },
      { path: '/finance/fund', title: '资金审批' },
    ],
  },
  { path: '/logistics', title: '物流管理', icon: 'Ship' },
  { path: '/document', title: '单证管理', icon: 'Files' },
  {
    path: '/data',
    title: '数据中心',
    icon: 'DataAnalysis',
    children: [
      { path: '/data/upload', title: '数据上传' },
      { path: '/data/analysis', title: '定价分析' },
    ],
  },
  { path: '/approval', title: '审批中心', icon: 'Stamp' },
  {
    path: '/system',
    title: '系统管理',
    icon: 'Setting',
    children: [
      { path: '/system/user', title: '用户管理' },
      { path: '/system/role', title: '角色管理' },
    ],
  },
]
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

  &.collapsed {
    width: 64px;
  }
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

  .logo-text {
    white-space: nowrap;
  }
}

.side-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
  overflow-x: hidden;

  &:not(.el-menu--collapse) {
    width: 220px;
  }
}

.sidebar-footer {
  padding: 12px 20px;
  color: rgba(255, 255, 255, 0.35);
  font-size: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  text-align: center;
}
</style>
