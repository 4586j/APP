import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true },
  },
  {
    path: '/',
    component: () => import('@/components/layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'Odometer' },
      },
      {
        path: 'order',
        name: 'Order',
        redirect: '/order/sales',
        meta: { title: '订单管理', icon: 'Document' },
        children: [
          {
            path: 'sales',
            name: 'SalesOrder',
            component: () => import('@/views/order/SalesOrderList.vue'),
            meta: { title: '销售订单' },
          },
          {
            path: 'sales/create',
            name: 'SalesOrderCreate',
            component: () => import('@/views/order/SalesOrderCreate.vue'),
            meta: { title: '新建销售订单', hidden: true },
          },
          {
            path: 'purchase',
            name: 'PurchaseOrder',
            component: () => import('@/views/order/PurchaseOrderList.vue'),
            meta: { title: '采购订单' },
          },
        ],
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('@/views/product/ProductList.vue'),
        meta: { title: '产品管理', icon: 'Goods' },
      },
      {
        path: 'customer',
        name: 'Customer',
        redirect: '/customer/list',
        meta: { title: '客户管理', icon: 'User' },
        children: [
          {
            path: 'list',
            name: 'CustomerList',
            component: () => import('@/views/customer/CustomerList.vue'),
            meta: { title: '客户列表' },
          },
          {
            path: 'supplier',
            name: 'SupplierList',
            component: () => import('@/views/customer/SupplierList.vue'),
            meta: { title: '供应商列表' },
          },
        ],
      },
      {
        path: 'finance',
        name: 'Finance',
        redirect: '/finance/overview',
        meta: { title: '财务管理', icon: 'Money' },
        children: [
          {
            path: 'overview',
            name: 'FinanceOverview',
            component: () => import('@/views/finance/FinanceOverview.vue'),
            meta: { title: '财务概览' },
          },
          {
            path: 'exchange-rate',
            name: 'ExchangeRate',
            component: () => import('@/views/finance/ExchangeRate.vue'),
            meta: { title: '汇率管理' },
          },
          {
            path: 'fund',
            name: 'FundApproval',
            component: () => import('@/views/finance/FundApproval.vue'),
            meta: { title: '资金审批' },
          },
        ],
      },
      {
        path: 'logistics',
        name: 'Logistics',
        component: () => import('@/views/logistics/ShipmentList.vue'),
        meta: { title: '物流管理', icon: 'Ship' },
      },
      {
        path: 'document',
        name: 'Document',
        component: () => import('@/views/document/DocumentList.vue'),
        meta: { title: '单证管理', icon: 'Files' },
      },
      {
        path: 'data',
        name: 'DataCenter',
        redirect: '/data/upload',
        meta: { title: '数据中心', icon: 'DataAnalysis' },
        children: [
          {
            path: 'upload',
            name: 'DataUpload',
            component: () => import('@/views/data/FileManage.vue'),
            meta: { title: '企业网盘' },
          },
          {
            path: 'analysis',
            name: 'PricingAnalysis',
            component: () => import('@/views/data/PricingAnalysis.vue'),
            meta: { title: '定价分析' },
          },
        ],
      },
      {
        path: 'approval',
        name: 'Approval',
        component: () => import('@/views/approval/ApprovalPending.vue'),
        meta: { title: '审批中心', icon: 'Stamp' },
      },
      {
        path: 'report',
        name: 'WorkReport',
        component: () => import('@/views/report/WorkReportManage.vue'),
        meta: { title: '工作报表', icon: 'Memo' },
      },
      {
        path: 'system',
        name: 'System',
        redirect: '/system/user',
        meta: { title: '系统管理', icon: 'Setting' },
        children: [
          {
            path: 'user',
            name: 'UserManage',
            component: () => import('@/views/system/UserManage.vue'),
            meta: { title: '用户管理' },
          },
          {
            path: 'role',
            name: 'RoleManage',
            component: () => import('@/views/system/RoleManage.vue'),
            meta: { title: '角色管理' },
          },
          {
            path: 'department',
            name: 'DepartmentManage',
            component: () => import('@/views/system/DepartmentManage.vue'),
            meta: { title: '部门管理' },
          },
          {
            path: 'permission',
            name: 'PermissionManage',
            component: () => import('@/views/system/PermissionManage.vue'),
            meta: { title: '权限管理' },
          },
        ],
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', hidden: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：未登录跳 /login，已登录访问 /login 跳首页
import { useUserStore } from '@/store/user'
import { storage, StorageKey } from '@/utils/storage'

router.beforeEach((to, _from, next) => {
  // 标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} | 外贸 ERP`
  }
  const token = storage.get<string>(StorageKey.TOKEN)
  if (to.path === '/login') {
    if (token) return next('/dashboard')
    return next()
  }
  if (!token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  // 已登录但 store 为空（刷新场景）：懒加载用户信息，失败时清 token 跳登录
  const userStore = useUserStore()
  if (!userStore.userInfo) {
    userStore
      .fetchProfile()
      .then(() => next())
      .catch(() => {
        userStore.clear()
        next({ path: '/login', query: { redirect: to.fullPath } })
      })
  } else {
    next()
  }
})

export default router
