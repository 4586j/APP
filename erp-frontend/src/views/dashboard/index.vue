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
import { ref, onMounted } from 'vue'

const today = new Date().toLocaleDateString('zh-CN')

const statCards = [
  { label: '今日订单数', value: '28', icon: 'Document', color: '#2563eb' },
  { label: '本月销售额 (USD)', value: '$ 486,200', icon: 'Money', color: '#10b981' },
  { label: '待审批事项', value: '12', icon: 'Stamp', color: '#f59e0b' },
  { label: '本月利润 (RMB)', value: '¥ 326,800', icon: 'TrendCharts', color: '#8b5cf6' },
]

const recentOrders = [
  { orderNo: 'SO20260615001', customer: 'ABC Trading Inc.', amount: 48500, status: 'shipped', date: '2026-06-14' },
  { orderNo: 'SO20260614003', customer: 'Global Imports LLC', amount: 32100, status: 'approved', date: '2026-06-14' },
  { orderNo: 'SO20260613007', customer: 'Pacific Goods Co.', amount: 67800, status: 'submitted', date: '2026-06-13' },
  { orderNo: 'SO20260612002', customer: 'Euro Trade GmbH', amount: 23400, status: 'delivered', date: '2026-06-12' },
  { orderNo: 'SO20260611005', customer: 'Asia Partners Ltd.', amount: 89200, status: 'settled', date: '2026-06-11' },
]

const todos = [
  { text: '销售订单 SO20260615001 等待审批', color: '#e6a23c', tag: '审批', tagType: 'warning' as const },
  { text: '资金申请 FA20260614002 需要财务审批', color: '#e6a23c', tag: '审批', tagType: 'warning' as const },
  { text: '采购订单 PO20260613001 已到港待提货', color: '#2563eb', tag: '物流', tagType: 'primary' as const },
  { text: '汇率 USD/CNY 已更新至 7.2500', color: '#10b981', tag: '系统', tagType: 'success' as const },
  { text: '客户 DEF Corp 信用额度即将超限', color: '#f56c6c', tag: '预警', tagType: 'danger' as const },
]

function statusType(status: string): string {
  const map: Record<string, string> = { submitted: 'warning', approved: '', shipped: 'success', delivered: 'success', settled: 'info' }
  return map[status] || 'info'
}
function statusLabel(status: string): string {
  const map: Record<string, string> = { submitted: '待审批', approved: '已审批', shipped: '已发货', delivered: '已交付', settled: '已结算' }
  return map[status] || status
}

const trendChartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()

onMounted(async () => {
  try {
    const echarts = await import('echarts')

    if (trendChartRef.value) {
      const chart = echarts.init(trendChartRef.value)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 40, right: 20, top: 10, bottom: 24 },
        xAxis: {
          type: 'category',
          data: ['7月','8月','9月','10月','11月','12月','1月','2月','3月','4月','5月','6月'],
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
            name: '销售额 (万美元)',
            type: 'bar',
            data: [42, 38, 45, 55, 48, 52, 40, 36, 50, 58, 46, 48],
            itemStyle: { borderRadius: [4, 4, 0, 0], color: '#2563eb' },
            barWidth: 22,
          },
          {
            name: '利润 (万美元)',
            type: 'line',
            data: [14, 12, 15, 20, 16, 18, 13, 11, 17, 22, 15, 16],
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
          data: [
            { value: 35, name: '已交付', itemStyle: { color: '#10b981' } },
            { value: 22, name: '已发货', itemStyle: { color: '#2563eb' } },
            { value: 18, name: '已审批', itemStyle: { color: '#60a5fa' } },
            { value: 12, name: '待审批', itemStyle: { color: '#f59e0b' } },
            { value: 8, name: '已结算', itemStyle: { color: '#8b5cf6' } },
            { value: 5, name: '已取消', itemStyle: { color: '#f56c6c' } },
          ],
        }],
      })
    }
  } catch (e) {
    console.error('ECharts init error:', e)
  }
})
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
