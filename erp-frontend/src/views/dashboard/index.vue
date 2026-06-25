<template>
  <div class="page-container">
    <div class="page-header">
      <h2>仪表盘</h2>
      <p class="page-desc">公司运营数据概览 · 更新时间：{{ today }}</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card" v-for="card in statCards" :key="card.label">
        <div class="stat-icon" :style="{ background: card.color }">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <!-- 图表行 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-title">近12个月销售趋势</div>
        <div class="chart-body" ref="trendChartRef"></div>
      </div>
      <div class="chart-card">
        <div class="chart-title">订单状态分布</div>
        <div class="chart-body" ref="pieChartRef"></div>
      </div>
    </div>

    <!-- 最近订单 + 待办 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-title">最近销售订单</div>
        <el-table :data="recentOrders" style="width: 100%" size="small" stripe>
          <el-table-column prop="orderNo" label="订单编号" width="150" />
          <el-table-column prop="customer" label="客户" />
          <el-table-column prop="amount" label="金额" width="130" align="right">
            <template #default="{ row }">$ {{ row.amount.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="date" label="日期" width="110" />
        </el-table>
      </div>
      <div class="chart-card">
        <div class="chart-title">待办事项</div>
        <div class="todo-list">
          <div v-for="(todo, i) in todos" :key="i" class="todo-item">
            <el-icon :color="todo.color" :size="16"><WarningFilled /></el-icon>
            <span>{{ todo.text }}</span>
            <el-tag size="small" :type="todo.tagType">{{ todo.tag }}</el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
import { ref, onMounted } from 'vue'
import { getDashboardStats, type DashboardStatsVO } from '@/api/data'

const today = new Date().toLocaleDateString('zh-CN')
const stats = ref<DashboardStatsVO | null>(null)

const statCards = ref([
  { label: '客户数', value: '-', icon: 'User', color: '#2563eb' },
  { label: '产品数', value: '-', icon: 'Goods', color: '#10b981' },
  { label: '总订单数', value: '-', icon: 'Document', color: '#f59e0b' },
  { label: '本月利润 (USD)', value: '-', icon: 'TrendCharts', color: '#8b5cf6' },
])

onMounted(async () => {
  try {
    const s = await getDashboardStats()
    stats.value = s
    statCards.value = [
      { label: '客户数', value: String(s.customerCount), icon: 'User', color: '#2563eb' },
      { label: '产品数', value: String(s.productCount), icon: 'Goods', color: '#10b981' },
      { label: '总订单数', value: String(s.orderCount), icon: 'Document', color: '#f59e0b' },
      { label: '本月利润 (USD)', value: `$ ${s.monthlyProfit.toLocaleString()}`, icon: 'TrendCharts', color: '#8b5cf6' },
    ]
    // 图表
    await renderCharts(s)
  } catch (e) {
    console.error('Dashboard load failed:', e)
  }
})

async function renderCharts(s: DashboardStatsVO) {
  try {
    const echarts = await import('echarts')

    if (trendChartRef.value) {
      const chart = echarts.init(trendChartRef.value)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 20, top: 10, bottom: 24 },
        xAxis: {
          type: 'category',
          data: s.trend.length ? s.trend.map(t => t.month) : [],
          axisLine: { lineStyle: { color: '#e0e0e0' } },
          axisLabel: { color: '#999', fontSize: 11 },
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#f0f0f0' } },
          axisLabel: { color: '#999', fontSize: 11 },
        },
        series: [
          {
            name: '销售额',
            type: 'bar',
            data: s.trend.length ? s.trend.map(t => t.revenue) : [],
            itemStyle: { borderRadius: [4, 4, 0, 0], color: '#2563eb' },
            barWidth: 22,
          },
          {
            name: '利润',
            type: 'line',
            data: s.trend.length ? s.trend.map(t => t.profit) : [],
            smooth: true,
            lineStyle: { color: '#10b981', width: 2 },
            itemStyle: { color: '#10b981' },
            symbol: 'circle',
            symbolSize: 6,
          },
        ],
      })
    }

    if (pieChartRef.value) {
      const chart = echarts.init(pieChartRef.value)
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0, textStyle: { fontSize: 12 } },
        series: [{
          type: 'pie',
          radius: ['55%', '78%'],
          center: ['50%', '45%'],
          label: { show: false },
          data: s.orderStatusDist.length
            ? s.orderStatusDist.map(d => ({ value: d.value, name: d.name, itemStyle: { color: pickColor(d.name) } }))
            : [{ value: 1, name: '暂无数据', itemStyle: { color: '#e0e0e0' } }],
        }],
      })
    }
  } catch (e) {
    console.error('ECharts init error:', e)
  }
}

function pickColor(name: string): string {
  const map: Record<string, string> = {
    '已交付': '#10b981', '已发货': '#2563eb', '已审批': '#60a5fa',
    '待审批': '#f59e0b', '已结算': '#8b5cf6', '已取消': '#f56c6c',
  }
  return map[name] || '#909399'
}

const recentOrders = [
  { orderNo: 'SO20260615001', customer: 'ABC Trading Inc.', amount: 48500, status: 'shipped', date: '2026-06-14' },
  { orderNo: 'SO20260614003', customer: 'Global Imports LLC', amount: 32100, status: 'approved', date: '2026-06-14' },
  { orderNo: 'SO20260613007', customer: 'Pacific Goods Co.', amount: 67800, status: 'submitted', date: '2026-06-13' },
  { orderNo: 'SO20260612002', customer: 'Euro Trade GmbH', amount: 23400, status: 'delivered', date: '2026-06-12' },
  { orderNo: 'SO20260611005', customer: 'Asia Partners Ltd.', amount: 89200, status: 'settled', date: '2026-06-11' },
]

const todos: Array<{ text: string; tag: string; tagType: TagType; color: string }> = [
  { text: '销售订单 SO20260615001 等待审批', color: '#e6a23c', tag: '审批', tagType: 'warning' as const },
  { text: '资金申请 FA20260614002 需要财务审批', color: '#e6a23c', tag: '审批', tagType: 'warning' as const },
  { text: '采购订单 PO20260613001 已到港待提货', color: '#2563eb', tag: '物流', tagType: 'primary' as const },
  { text: '汇率 USD/CNY 已更新至 7.2500', color: '#10b981', tag: '系统', tagType: 'success' as const },
  { text: '客户 DEF Corp 信用额度即将超限', color: '#f56c6c', tag: '预警', tagType: 'danger' as const },
]

function statusType(status: string): TagType {
  const map: Record<string, TagType> = { submitted: 'warning', approved: 'info', shipped: 'success', delivered: 'success', settled: 'info' }
  return map[status] || 'info'
}
function statusLabel(status: string): string {
  const map: Record<string, string> = { submitted: '待审批', approved: '已审批', shipped: '已发货', delivered: '已交付', settled: '已结算' }
  return map[status] || status
}

const trendChartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()
</script>

<style scoped lang="scss">
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
  span { flex: 1; }
  &:hover { background: #f0f2f5; }
}
</style>
