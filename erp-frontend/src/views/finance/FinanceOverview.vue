<template>
  <div class="page-container">
    <div class="page-header">
      <h2>财务概览</h2>
      <p class="page-desc">财务状况一览 · 截至 {{ today }}</p>
    </div>
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
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-title">应收应付概览</div>
        <el-table :data="arApData" stripe size="small">
          <el-table-column prop="type" label="类型" width="90" />
          <el-table-column prop="customer" label="客户/供应商" min-width="170" />
          <el-table-column prop="invoiceNo" label="发票号" width="140" />
          <el-table-column prop="amount" label="金额" width="130" align="right">
            <template #default="{ row }">{{ row.currency }} {{ row.amount.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="dueDate" label="到期日" width="110" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.overdue ? 'danger' : 'success'" size="small">{{ row.overdue ? '已逾期' : '正常' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="chart-card">
        <div class="chart-title">最近结算记录</div>
        <el-table :data="settlements" stripe size="small">
          <el-table-column prop="settlementNo" label="结算编号" width="140" />
          <el-table-column prop="orderNo" label="关联订单" width="140" />
          <el-table-column prop="type" label="类型" width="70" />
          <el-table-column prop="amount" label="金额" width="120" align="right">
            <template #default="{ row }">¥ {{ row.amount.toLocaleString() }}</template>
          </el-table-column>
          <el-table-column prop="date" label="日期" width="110" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const today = new Date().toLocaleDateString('zh-CN')

const statCards = [
  { label: '本月收入 (RMB)', value: '¥ 1,286,500', icon: 'TrendCharts', color: '#10b981' },
  { label: '本月支出 (RMB)', value: '¥ 859,200', icon: 'Money', color: '#f56c6c' },
  { label: '应收账款 (RMB)', value: '¥ 2,340,000', icon: 'Warning', color: '#f59e0b' },
  { label: '应付账款 (RMB)', value: '¥ 1,180,000', icon: 'List', color: '#2563eb' },
]

const arApData = [
  { type: '应收', customer: 'ABC Trading Inc.', invoiceNo: 'INV20260615001', amount: 48500, currency: 'USD', dueDate: '2026-07-15', overdue: false },
  { type: '应收', customer: 'Global Imports LLC', invoiceNo: 'INV20260610002', amount: 32100, currency: 'EUR', dueDate: '2026-06-30', overdue: false },
  { type: '应收', customer: 'Euro Trade GmbH', invoiceNo: 'INV20260520003', amount: 23400, currency: 'EUR', dueDate: '2026-06-10', overdue: true },
  { type: '应付', supplier: '深圳华强电子', invoiceNo: 'PINV20260615001', amount: 285000, currency: 'CNY', dueDate: '2026-07-15', overdue: false },
  { type: '应付', supplier: '浙江正泰电器', invoiceNo: 'PINV20260601002', amount: 198000, currency: 'CNY', dueDate: '2026-06-20', overdue: false },
]

const settlements = [
  { settlementNo: 'STL20260615001', orderNo: 'SO20260611005', type: '收款', amount: 646700, date: '2026-06-15' },
  { settlementNo: 'STL20260614002', orderNo: 'PO20260610004', type: '付款', amount: 85000, date: '2026-06-14' },
  { settlementNo: 'STL20260612003', orderNo: 'SO20260608002', type: '收款', amount: 382000, date: '2026-06-12' },
  { settlementNo: 'STL20260610004', orderNo: 'PO20260605005', type: '付款', amount: 310000, date: '2026-06-10' },
]
</script>
