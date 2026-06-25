<template>
  <div class="workbench">
    <!-- 欢迎区 -->
    <div class="welcome-bar">
      <div class="welcome-left">
        <div class="welcome-avatar">
          <el-avatar :size="56" :icon="UserFilled" :style="{ background: '#2563eb' }" />
        </div>
        <div class="welcome-text">
          <h2>{{ greeting }}，{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</h2>
          <p class="welcome-meta">
            <el-tag size="small" type="info" effect="plain">{{ userStore.userInfo?.departmentName || userStore.userInfo?.department || '未分配部门' }}</el-tag>
            <span class="role-tags">
              <el-tag v-for="role in displayRoles" :key="role" size="small" type="primary" effect="plain" style="margin-left:6px">{{ role }}</el-tag>
            </span>
          </p>
        </div>
      </div>
      <div class="welcome-right">
        <div class="clock-time">{{ currentTime }}</div>
        <div class="clock-date">{{ currentDate }} {{ currentWeek }}</div>
      </div>
    </div>

    <!-- 快捷入口 + 待办 -->
    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :sm="24" :md="16" :lg="17">
        <div class="panel-card">
          <div class="panel-header">
            <span class="panel-title">快捷入口</span>
          </div>
          <div class="shortcut-grid">
            <div
              v-for="item in shortcuts"
              :key="item.path"
              class="shortcut-item"
              @click="$router.push(item.path)"
            >
              <div class="shortcut-icon" :style="{ background: item.bg, color: item.color }">
                <el-icon :size="22"><component :is="item.icon" /></el-icon>
              </div>
              <span class="shortcut-label">{{ item.label }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :md="8" :lg="7">
        <div class="panel-card todo-card">
          <div class="panel-header">
            <span class="panel-title">待办事项</span>
            <el-button v-if="totalTodo > 0" type="primary" link size="small" @click="$router.push('/approval')">去处理</el-button>
          </div>
          <div class="todo-list">
            <div class="todo-item" @click="$router.push('/approval')">
              <div class="todo-icon" style="background:#fff7e6;color:#fa8c16">
                <el-icon><Stamp /></el-icon>
              </div>
              <div class="todo-info">
                <span class="todo-label">待审批</span>
                <span class="todo-count" :class="{ 'is-zero': pendingApprovalCount === 0 }">{{ pendingApprovalCount }}</span>
              </div>
            </div>
            <div class="todo-item" @click="scrollToNotify">
              <div class="todo-icon" style="background:#e6f7ff;color:#1890ff">
                <el-icon><Bell /></el-icon>
              </div>
              <div class="todo-info">
                <span class="todo-label">未读通知</span>
                <span class="todo-count" :class="{ 'is-zero': unreadNotifyCount === 0 }">{{ unreadNotifyCount }}</span>
              </div>
            </div>
            <div class="todo-item" @click="$router.push('/order/sales/create')">
              <div class="todo-icon" style="background:#f6ffed;color:#52c41a">
                <el-icon><DocumentAdd /></el-icon>
              </div>
              <div class="todo-info">
                <span class="todo-label">新建订单</span>
                <span class="todo-desc">快速创建</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 通知 + 最近审批 -->
    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <div id="notify-panel" class="panel-card">
          <div class="panel-header">
            <span class="panel-title">
              消息通知
              <el-badge v-if="unreadNotifyCount > 0" :value="unreadNotifyCount" style="margin-left:8px" />
            </span>
            <el-button type="primary" link size="small" :disabled="unreadNotifyCount === 0" @click="markAllRead">全部已读</el-button>
          </div>
          <div v-loading="notifyLoading" class="notify-list">
            <div v-if="notifications.length === 0" class="empty-tip">暂无通知</div>
            <div
              v-for="n in notifications"
              :key="n.id"
              class="notify-item"
              :class="{ unread: n.isRead === 0 }"
              @click="handleNotifyClick(n)"
            >
              <div class="notify-dot" :class="{ unread: n.isRead === 0 }" />
              <div class="notify-body">
                <div class="notify-title">{{ n.title }}</div>
                <div class="notify-content">{{ n.content }}</div>
                <div class="notify-time">{{ formatTime(n.createdAt) }}</div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <div class="panel-card">
          <div class="panel-header">
            <span class="panel-title">最近审批</span>
            <el-button type="primary" link size="small" @click="$router.push('/approval')">查看更多</el-button>
          </div>
          <div v-loading="approvalLoading" class="approval-list">
            <div v-if="recentApprovals.length === 0" class="empty-tip">暂无审批记录</div>
            <div
              v-for="a in recentApprovals"
              :key="a.id"
              class="approval-item"
              @click="$router.push('/approval')"
            >
              <div class="approval-badge" :style="{ background: statusColor(a.status) }">
                <el-icon><Stamp /></el-icon>
              </div>
              <div class="approval-body">
                <div class="approval-title">{{ a.title }}</div>
                <div class="approval-meta">
                  <span>{{ a.requestNo }}</span>
                  <el-tag :type="statusType(a.status)" size="small">{{ statusLabel(a.status) }}</el-tag>
                </div>
                <div class="approval-time">{{ formatTime(a.createdAt) }}</div>
              </div>
              <div v-if="a.amount != null" class="approval-amount">
                ¥ {{ Number(a.amount).toLocaleString() }}
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 我的数据概览 -->
    <div class="panel-card">
      <div class="panel-header">
        <span class="panel-title">我的数据概览</span>
      </div>
      <div class="my-kpi-grid">
        <div v-for="k in myKpis" :key="k.key" class="my-kpi-item">
          <div class="my-kpi-value" :style="{ color: k.color }">{{ k.value }}</div>
          <div class="my-kpi-label">{{ k.label }}</div>
          <div class="my-kpi-trend" v-if="k.trend">
            <el-icon :size="12" :color="k.trend > 0 ? '#52c41a' : '#f5222d'"><component :is="k.trend > 0 ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
            <span :style="{ color: k.trend > 0 ? '#52c41a' : '#f5222d' }">{{ Math.abs(k.trend) }}%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 今日工作 -->
    <el-row :gutter="16" class="section-row">
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <div class="panel-card work-card" :class="{ 'is-warning': planStatus === '未填写' }">
          <div class="panel-header">
            <span class="panel-title">
              工作计划
              <el-tag v-if="planStatus !== '未填写'" :type="planTagType" size="small" style="margin-left:8px">{{ planStatus }}</el-tag>
            </span>
            <el-button v-if="canEditPlan" type="primary" link size="small" @click="openPlanDialog">{{ planId ? '修改' : '填写' }}</el-button>
          </div>
          <div class="work-content">
            <div v-if="planTitle || planContent">
              <div v-if="planTitle" class="work-title">{{ planTitle }}</div>
              <div class="work-desc">{{ planContent }}</div>
              <div v-if="planTimeHint" class="work-hint">{{ planTimeHint }}</div>
            </div>
            <div v-else class="empty-tip">
              <p>今日工作计划尚未填写</p>
              <p v-if="!canEditPlan" class="work-hint">当前不在填写时间（08:30-10:30）</p>
              <el-button v-if="canEditPlan" type="primary" size="small" @click="openPlanDialog">立即填写</el-button>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <div class="panel-card work-card">
          <div class="panel-header">
            <span class="panel-title">
              工作日志
              <el-tag v-if="logStatus !== '未填写'" :type="logTagType" size="small" style="margin-left:8px">{{ logStatus }}</el-tag>
            </span>
            <el-button type="primary" link size="small" @click="openLogDialog">{{ logId ? '修改' : '填写' }}</el-button>
          </div>
          <div class="work-content">
            <div v-if="logTitle || logContent">
              <div v-if="logTitle" class="work-title">{{ logTitle }}</div>
              <div class="work-desc">{{ logContent }}</div>
            </div>
            <div v-else class="empty-tip">
              <p>今日工作日志尚未填写</p>
              <el-button type="primary" size="small" @click="openLogDialog">立即填写</el-button>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 当月统计 -->
    <div class="panel-card">
      <div class="panel-header">
        <span class="panel-title">本月工作统计</span>
      </div>
      <div class="my-kpi-grid">
        <div class="my-kpi-item">
          <div class="my-kpi-value" style="color:#2563eb">{{ reportStats.planCount }}</div>
          <div class="my-kpi-label">已提交计划</div>
        </div>
        <div class="my-kpi-item">
          <div class="my-kpi-value" style="color:#16a34a">{{ reportStats.logCount }}</div>
          <div class="my-kpi-label">已提交日志</div>
        </div>
        <div class="my-kpi-item">
          <div class="my-kpi-value" style="color:#ef4444">{{ reportStats.rejectedCount }}</div>
          <div class="my-kpi-label">被驳回次数</div>
        </div>
      </div>
    </div>

    <!-- 填写弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" @closed="resetDialog">
      <el-form :model="form" label-width="80px">
        <el-form-item label="日期">
          <el-date-picker v-model="form.reportDate" type="date" value-format="YYYY-MM-DD" style="width:100%" disabled />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="选填，如：今日重点任务" maxlength="128" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入详细内容..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSave">保存</el-button>
        <el-button v-if="editId" type="success" :loading="submitting" @click="doSubmit">提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  UserFilled, Stamp, Bell, DocumentAdd, Document, Goods, User,
  Money, DataAnalysis, ArrowUp, ArrowDown,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import {
  listMyNotifications, getUnreadCount, batchMarkNotificationRead,
  type NotificationVO,
} from '@/api/notification'
import { listFundApprovals, type FundApprovalVO } from '@/api/finance'
import {
  getTodayReport, getReportStats, saveWorkPlan, saveWorkLog,
  submitWorkPlan, submitWorkLog, type WorkReportVO, type WorkReportStatsVO,
} from '@/api/report'
import type { Id } from '@/api/system'

const router = useRouter()
const userStore = useUserStore()

/* ---------- 时钟 ---------- */
const currentTime = ref('')
const currentDate = ref('')
const currentWeek = ref('')
const weekNames = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

function updateClock() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour12: false, hour: '2-digit', minute: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }).replace(/\//g, '-')
  currentWeek.value = weekNames[now.getDay()]
}
let clockTimer: ReturnType<typeof setInterval> | null = null

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const displayRoles = computed(() => {
  const roles = userStore.userInfo?.roles || []
  return roles.map(r => r.replace(/^ROLE_/, '')).slice(0, 3)
})

/* ---------- 快捷入口 ---------- */
const shortcuts = computed(() => {
  const all = [
    { label: '销售订单', icon: 'Document', path: '/order/sales', bg: '#e6f0ff', color: '#2563eb', perm: 'order' },
    { label: '采购订单', icon: 'DocumentAdd', path: '/order/purchase', bg: '#e6f7ff', color: '#0ea5e9', perm: 'order' },
    { label: '产品管理', icon: 'Goods', path: '/product', bg: '#f0fdf4', color: '#16a34a', perm: 'product' },
    { label: '客户列表', icon: 'User', path: '/customer/list', bg: '#fefce8', color: '#ca8a04', perm: 'customer' },
    { label: '资金审批', icon: 'Money', path: '/finance/fund', bg: '#fdf4ff', color: '#a855f7', perm: 'finance:fund' },
    { label: '数据上传', icon: 'DataAnalysis', path: '/data/upload', bg: '#f0f9ff', color: '#0ea5e9', perm: 'data:upload:create' },
  ]
  return all.filter(s => userStore.hasPermission(s.perm) || userStore.hasPermission(s.perm + ':view') || userStore.hasPermission(s.perm + ':create'))
})

/* ---------- 待办 ---------- */
const pendingApprovalCount = ref(0)
const unreadNotifyCount = ref(0)
const totalTodo = computed(() => pendingApprovalCount.value + unreadNotifyCount.value)

/* ---------- 通知 ---------- */
const notifyLoading = ref(false)
const notifications = ref<NotificationVO[]>([])

async function loadNotifications() {
  if (!userStore.hasPermission('notification:view')) return
  notifyLoading.value = true
  try {
    const [listRes, countRes] = await Promise.all([
      listMyNotifications({ page: 1, size: 5 }),
      getUnreadCount(),
    ])
    notifications.value = listRes.records || []
    unreadNotifyCount.value = Number(countRes) || 0
  } catch (e) {
    console.error('load notifications failed:', e)
  } finally {
    notifyLoading.value = false
  }
}

async function markAllRead() {
  try {
    await batchMarkNotificationRead()
    ElMessage.success('已全部标记为已读')
    await loadNotifications()
  } catch (e) {
    console.error('mark all read failed:', e)
  }
}

function handleNotifyClick(n: NotificationVO) {
  if (n.isRead === 0) {
    batchMarkNotificationRead([n.id]).then(() => loadNotifications()).catch(() => {})
  }
}

function scrollToNotify() {
  const el = document.getElementById('notify-panel')
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

/* ---------- 最近审批 ---------- */
const approvalLoading = ref(false)
const recentApprovals = ref<FundApprovalVO[]>([])

async function loadRecentApprovals() {
  const canView = userStore.hasPermission('finance:fund:view')
  const canApprove = userStore.hasPermission('finance:fund:approve')
  if (!canView && !canApprove) return

  approvalLoading.value = true
  try {
    // 有审批权限时优先获取我的待审批，否则获取全部列表
    let res
    if (canApprove) {
      res = await listFundApprovals({ page: 1, size: 5, status: 'pending' })
    } else {
      res = await listFundApprovals({ page: 1, size: 5 })
    }
    recentApprovals.value = (res.records || []).slice(0, 5)
    pendingApprovalCount.value = (res.records || []).filter(a => a.status === 'pending').length
  } catch (e) {
    console.error('load approvals failed:', e)
  } finally {
    approvalLoading.value = false
  }
}

/* ---------- 审批状态工具 ---------- */
function statusType(s: string) {
  const m: Record<string, any> = { pending: 'warning', approved: 'success', rejected: 'danger', paid: 'info' }
  return m[s] || 'info'
}
function statusLabel(s: string) {
  const m: Record<string, string> = { pending: '审批中', approved: '已通过', rejected: '已驳回', paid: '已付款' }
  return m[s] || s
}
function statusColor(s: string) {
  const m: Record<string, string> = { pending: '#faad14', approved: '#52c41a', rejected: '#f5222d', paid: '#1890ff' }
  return m[s] || '#bfbfbf'
}

/* ---------- 时间格式化 ---------- */
function formatTime(t?: string) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  const oneDay = 24 * 60 * 60 * 1000
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`
  if (diff < oneDay) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
  if (diff < 2 * oneDay) return '昨天'
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

/* ---------- 我的数据概览（静态演示数据，后续可接真实接口） ---------- */
const myKpis = ref([
  { key: 'myOrders', label: '我的订单', value: '12', color: '#2563eb', trend: 8 },
  { key: 'myCustomers', label: '我的客户', value: '8', color: '#16a34a', trend: 2 },
  { key: 'myApproval', label: '本月审批', value: '5', color: '#f59e0b', trend: -1 },
  { key: 'myUploads', label: '上传文件', value: '23', color: '#0ea5e9', trend: 15 },
])

/* ---------- 工作报表 ---------- */
const planId = ref<Id | undefined>()
const planTitle = ref('')
const planContent = ref('')
const planStatus = ref('未填写')
const planTagType = ref<any>('info')
const logId = ref<Id | undefined>()
const logTitle = ref('')
const logContent = ref('')
const logStatus = ref('未填写')
const logTagType = ref<any>('info')
const reportStats = ref<WorkReportStatsVO>({ planCount: 0, logCount: 0, rejectedCount: 0 })

const canEditPlan = computed(() => {
  const hour = new Date().getHours()
  const minute = new Date().getMinutes()
  const totalMinutes = hour * 60 + minute
  return totalMinutes >= 8 * 60 + 30 && totalMinutes <= 10 * 60 + 30
})

const planTimeHint = computed(() => {
  if (!canEditPlan.value) return ''
  const hour = new Date().getHours()
  const minute = new Date().getMinutes()
  const endMinutes = 10 * 60 + 30
  const currentMinutes = hour * 60 + minute
  const remaining = endMinutes - currentMinutes
  return `填写时间剩余 ${remaining} 分钟`
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogType = ref<'plan' | 'log'>('plan')
const editId = ref<Id | undefined>()
const saving = ref(false)
const submitting = ref(false)
const form = reactive({ reportDate: '', title: '', content: '' })

function openPlanDialog() {
  dialogType.value = 'plan'
  dialogTitle.value = planId.value ? '修改工作计划' : '填写工作计划'
  editId.value = planId.value
  form.reportDate = new Date().toISOString().slice(0, 10)
  form.title = planTitle.value
  form.content = planContent.value
  dialogVisible.value = true
}

function openLogDialog() {
  dialogType.value = 'log'
  dialogTitle.value = logId.value ? '修改工作日志' : '填写工作日志'
  editId.value = logId.value
  form.reportDate = new Date().toISOString().slice(0, 10)
  form.title = logTitle.value
  form.content = logContent.value
  dialogVisible.value = true
}

function resetDialog() {
  editId.value = undefined
  form.reportDate = ''
  form.title = ''
  form.content = ''
}

async function doSave() {
  if (!form.content) { ElMessage.warning('请输入内容'); return }
  saving.value = true
  try {
    if (dialogType.value === 'plan') {
      await saveWorkPlan({ id: editId.value, reportDate: form.reportDate, title: form.title, content: form.content })
      ElMessage.success('计划已保存')
    } else {
      await saveWorkLog({ id: editId.value, reportDate: form.reportDate, title: form.title, content: form.content })
      ElMessage.success('日志已保存')
    }
    dialogVisible.value = false
    await loadTodayReport()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function doSubmit() {
  if (!editId.value) { ElMessage.warning('请先保存'); return }
  submitting.value = true
  try {
    if (dialogType.value === 'plan') {
      await submitWorkPlan(editId.value)
      ElMessage.success('计划已提交审批')
    } else {
      await submitWorkLog(editId.value)
      ElMessage.success('日志已提交审批')
    }
    dialogVisible.value = false
    await loadTodayReport()
    await loadReportStats()
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function loadTodayReport() {
  try {
    const res = await getTodayReport()
    planId.value = res.planId
    planTitle.value = res.planTitle || ''
    planContent.value = res.planContent || ''
    planStatus.value = res.planStatus ? statusLabel2(res.planStatus) : '未填写'
    planTagType.value = res.planStatus ? tagType(res.planStatus) : 'info'
    logId.value = res.logId
    logTitle.value = res.logTitle || ''
    logContent.value = res.logContent || ''
    logStatus.value = res.logStatus ? statusLabel2(res.logStatus) : '未填写'
    logTagType.value = res.logStatus ? tagType(res.logStatus) : 'info'
  } catch (e) {
    console.error('load today report failed:', e)
  }
}

async function loadReportStats() {
  try {
    const res = await getReportStats()
    reportStats.value = res
  } catch (e) {
    console.error('load report stats failed:', e)
  }
}

function statusLabel2(s: string) {
  const m: Record<string, string> = { draft: '草稿', submitted: '待审批', approved: '已通过', rejected: '已驳回' }
  return m[s] || s
}

function tagType(s: string) {
  const m: Record<string, any> = { draft: 'info', submitted: 'warning', approved: 'success', rejected: 'danger' }
  return m[s] || 'info'
}

/* ---------- 生命周期 ---------- */
onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
  loadNotifications()
  loadRecentApprovals()
  loadTodayReport()
  loadReportStats()
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped lang="scss">
.workbench {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100%;
}

/* 欢迎区 */
.welcome-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.welcome-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome-text {
  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;
  }
  .welcome-meta {
    margin: 6px 0 0;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
  }
}

.welcome-right {
  text-align: right;
  .clock-time {
    font-size: 28px;
    font-weight: 700;
    color: #1f2937;
    line-height: 1.2;
    font-variant-numeric: tabular-nums;
  }
  .clock-date {
    font-size: 13px;
    color: #9ca3af;
    margin-top: 2px;
  }
}

/* 通用面板 */
.section-row {
  margin-bottom: 0;
}

.panel-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  .panel-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;
  }
}

/* 快捷入口 */
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}
@media (max-width: 1200px) {
  .shortcut-grid { grid-template-columns: repeat(4, 1fr); }
}
@media (max-width: 768px) {
  .shortcut-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 480px) {
  .shortcut-grid { grid-template-columns: repeat(2, 1fr); }
}

.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
  &:hover {
    background: #f8fafc;
  }
}

.shortcut-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.shortcut-label {
  font-size: 13px;
  color: #4b5563;
}

/* 待办 */
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  &:hover {
    background: #f8fafc;
  }
}

.todo-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.todo-info {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.todo-label {
  font-size: 14px;
  color: #374151;
}

.todo-count {
  font-size: 18px;
  font-weight: 700;
  color: #ef4444;
  &.is-zero {
    color: #9ca3af;
    font-weight: 400;
    font-size: 14px;
  }
}

.todo-desc {
  font-size: 12px;
  color: #9ca3af;
}

/* 通知列表 */
.notify-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 320px;
  overflow-y: auto;
}

.notify-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  &:hover {
    background: #f8fafc;
  }
  &.unread {
    background: #f0f9ff;
    &:hover {
      background: #e6f4ff;
    }
  }
}

.notify-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d1d5db;
  margin-top: 6px;
  flex-shrink: 0;
  &.unread {
    background: #ef4444;
  }
}

.notify-body {
  flex: 1;
  min-width: 0;
}

.notify-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notify-content {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notify-time {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

/* 审批列表 */
.approval-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 320px;
  overflow-y: auto;
}

.approval-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  &:hover {
    background: #f8fafc;
  }
}

.approval-badge {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  flex-shrink: 0;
  margin-top: 2px;
}

.approval-body {
  flex: 1;
  min-width: 0;
}

.approval-title {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.approval-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
  font-size: 12px;
  color: #6b7280;
}

.approval-time {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.approval-amount {
  font-size: 14px;
  font-weight: 600;
  color: #f59e0b;
  white-space: nowrap;
}

/* 空状态 */
.empty-tip {
  text-align: center;
  padding: 32px 0;
  color: #9ca3af;
  font-size: 13px;
}

/* 我的数据概览 */
.my-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
@media (max-width: 768px) {
  .my-kpi-grid { grid-template-columns: repeat(2, 1fr); }
}

.my-kpi-item {
  text-align: center;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
}

.my-kpi-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.my-kpi-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 4px;
}

.my-kpi-trend {
  font-size: 12px;
  margin-top: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

/* 工作卡片 */
.work-card {
  .work-content {
    min-height: 120px;
  }
  .work-title {
    font-size: 14px;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 6px;
  }
  .work-desc {
    font-size: 13px;
    color: #4b5563;
    line-height: 1.6;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .work-hint {
    font-size: 12px;
    color: #f59e0b;
    margin-top: 8px;
  }
  &.is-warning {
    border: 1px solid #fef3c7;
    background: #fffbeb;
  }
}
</style>
