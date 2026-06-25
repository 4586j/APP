<template>
  <div class="finance">
    <!-- 页头 -->
    <div class="finance-header">
      <h2>财务概览</h2>
      <p class="finance-desc">财务状况一览 · 截至 {{ today }}</p>
    </div>

    <!-- KPI 卡片 -->
    <div class="kpi-grid">
      <div
        v-for="card in statCards"
        :key="card.label"
        class="kpi-card"
        :style="{ '--kpi-color': card.color, '--kpi-color-soft': card.colorSoft }"
      >
        <div class="kpi-icon">
          <el-icon :size="24"><component :is="card.icon" /></el-icon>
        </div>
        <div class="kpi-body">
          <div class="kpi-value">{{ card.value }}</div>
          <div class="kpi-label">{{ card.label }}</div>
        </div>
        <el-icon class="kpi-watermark" :size="64"><component :is="card.icon" /></el-icon>
      </div>
    </div>

    <!-- 表格区 -->
    <el-row :gutter="20" class="card-row">
      <el-col :xs="24" :sm="24" :md="24" :lg="14">
        <div class="panel-card">
          <div class="panel-title">应收应付概览</div>
          <el-table :data="arApData" stripe size="small" :header-cell-style="headerCellStyle">
            <el-table-column prop="type" label="类型" width="80">
              <template #default="{ row }">
                <el-tag :type="row.type === '应收' ? 'success' : 'warning'" size="small" effect="light">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="客户/供应商" min-width="170">
              <template #default="{ row }">{{ row.customer || row.supplier || '-' }}</template>
            </el-table-column>
            <el-table-column prop="invoiceNo" label="发票号" width="150" />
            <el-table-column prop="amount" label="金额" width="140" align="right">
              <template #default="{ row }">{{ row.currency }} {{ row.amount.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="dueDate" label="到期日" width="110" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.overdue ? 'danger' : 'success'" size="small">
                  {{ row.overdue ? '已逾期' : '正常' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :md="24" :lg="10">
        <div class="panel-card">
          <div class="panel-title">最近结算记录</div>
          <el-table :data="settlements" stripe size="small" :header-cell-style="headerCellStyle">
            <el-table-column prop="settlementNo" label="结算编号" min-width="140" />
            <el-table-column prop="orderNo" label="关联订单" min-width="140" />
            <el-table-column prop="type" label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.type === '收款' ? 'success' : 'info'" size="small" effect="light">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="130" align="right">
              <template #default="{ row }">¥ {{ row.amount.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="date" label="日期" width="110" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
const today = new Date().toLocaleDateString('zh-CN')

const headerCellStyle = {
  background: '#f8fafc',
  color: '#475569',
  fontWeight: 600,
}

const statCards = [
  { label: '本月收入 (RMB)', value: '¥ 1,286,500', icon: 'TrendCharts', color: '#16a34a', colorSoft: '#dcfce7' },
  { label: '本月支出 (RMB)', value: '¥ 859,200', icon: 'Money', color: '#ef4444', colorSoft: '#fee2e2' },
  { label: '应收账款 (RMB)', value: '¥ 2,340,000', icon: 'Warning', color: '#f59e0b', colorSoft: '#fef3c7' },
  { label: '应付账款 (RMB)', value: '¥ 1,180,000', icon: 'List', color: '#2563eb', colorSoft: '#dbeafe' },
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

<style scoped lang="scss">
.finance {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100%;
}

/* 页头 */
.finance-header {
  margin-bottom: 20px;
  h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: #1f2937;
  }
  .finance-desc {
    margin: 6px 0 0;
    font-size: 13px;
    color: #9ca3af;
  }
}

/* KPI 栅格：响应式自动换行（4 / 2 / 1 列） */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
@media (max-width: 992px) {
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 480px) {
  .kpi-grid { grid-template-columns: 1fr; }
}

.kpi-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  cursor: default;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }

  .kpi-icon {
    flex-shrink: 0;
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 12px;
    color: var(--kpi-color);
    background: var(--kpi-color-soft);
  }

  .kpi-body {
    min-width: 0;
  }
  .kpi-value {
    font-size: 22px;
    font-weight: 700;
    line-height: 1.2;
    color: #1f2937;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .kpi-label {
    margin-top: 4px;
    font-size: 13px;
    color: #6b7280;
  }

  .kpi-watermark {
    position: absolute;
    right: -8px;
    bottom: -10px;
    color: var(--kpi-color);
    opacity: 0.06;
    pointer-events: none;
  }
}

/* 表格卡片 */
.card-row {
  margin-bottom: 0;
}
.panel-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  padding: 16px 20px 20px;
  margin-bottom: 20px;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}
</style>
