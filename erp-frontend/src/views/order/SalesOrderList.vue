<template>
  <div class="page-container">
    <div class="page-header">
      <h2>销售订单</h2>
      <p class="page-desc">管理所有销售订单，跟踪订单状态与进度</p>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="订单编号">
          <el-input v-model="query.orderNo" placeholder="输入订单编号" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="客户">
          <el-select v-model="query.customerId" placeholder="选择客户" clearable style="width:180px">
            <el-option label="ABC Trading Inc." :value="1" />
            <el-option label="Global Imports LLC" :value="2" />
            <el-option label="Pacific Goods Co." :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width:130px">
            <el-option label="草稿" value="draft" />
            <el-option label="待审批" value="submitted" />
            <el-option label="已审批" value="approved" />
            <el-option label="已发货" value="shipped" />
            <el-option label="已交付" value="delivered" />
            <el-option label="已结算" value="settled" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="query.dateRange" type="daterange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" style="width:240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <div class="table-header">
        <h3>订单列表</h3>
        <div>
          <el-button :icon="Download">导出</el-button>
          <el-button type="primary" :icon="Plus" @click="$router.push('/order/sales/create')">新建订单</el-button>
        </div>
      </div>
      <el-table :data="orders" style="width:100%" stripe size="small" v-loading="false">
        <el-table-column prop="orderNo" label="订单编号" width="150" />
        <el-table-column prop="customerName" label="客户" min-width="160" />
        <el-table-column prop="currency" label="币种" width="70" align="center" />
        <el-table-column prop="amount" label="金额" width="130" align="right">
          <template #default="{ row }">{{ row.currency }} {{ row.amount.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="amountCny" label="金额 (RMB)" width="140" align="right">
          <template #default="{ row }">¥ {{ row.amountCny.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="tradeTerms" label="贸易条款" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderDate" label="下单日期" width="110" sortable />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default>
            <el-button type="primary" link size="small">详情</el-button>
            <el-button type="primary" link size="small">编辑</el-button>
            <el-button type="danger" link size="small">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination
          :current-page="1" :page-size="20" :total="128"
          :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Search, Refresh, Download, Plus } from '@element-plus/icons-vue'

const query = reactive({ orderNo: '', customerId: null, status: '', dateRange: null })

const statusMap: Record<string, { type: string; label: string }> = {
  draft: { type: 'info', label: '草稿' },
  submitted: { type: 'warning', label: '待审批' },
  approved: { type: '', label: '已审批' },
  sourcing: { type: '', label: '采购中' },
  funded: { type: 'primary', label: '已拨款' },
  shipped: { type: 'success', label: '已发货' },
  delivered: { type: 'success', label: '已交付' },
  settled: { type: 'info', label: '已结算' },
  cancelled: { type: 'danger', label: '已取消' },
}

const orders = [
  { orderNo: 'SO20260615001', customerName: 'ABC Trading Inc.', currency: 'USD', amount: 48500, amountCny: 351625, tradeTerms: 'FOB', status: 'shipped', orderDate: '2026-06-15' },
  { orderNo: 'SO20260614003', customerName: 'Global Imports LLC', currency: 'EUR', amount: 32100, amountCny: 250380, tradeTerms: 'CIF', status: 'approved', orderDate: '2026-06-14' },
  { orderNo: 'SO20260613007', customerName: 'Pacific Goods Co.', currency: 'USD', amount: 67800, amountCny: 491550, tradeTerms: 'EXW', status: 'submitted', orderDate: '2026-06-13' },
  { orderNo: 'SO20260612002', customerName: 'Euro Trade GmbH', currency: 'EUR', amount: 23400, amountCny: 182520, tradeTerms: 'DDP', status: 'delivered', orderDate: '2026-06-12' },
  { orderNo: 'SO20260611005', customerName: 'Asia Partners Ltd.', currency: 'USD', amount: 89200, amountCny: 646700, tradeTerms: 'FOB', status: 'settled', orderDate: '2026-06-11' },
  { orderNo: 'SO20260610008', customerName: 'Sunrise Commerce', currency: 'USD', amount: 15600, amountCny: 113100, tradeTerms: 'CIF', status: 'draft', orderDate: '2026-06-10' },
  { orderNo: 'SO20260609004', customerName: 'Northern Supply Co.', currency: 'GBP', amount: 41200, amountCny: 374920, tradeTerms: 'FOB', status: 'sourcing', orderDate: '2026-06-09' },
  { orderNo: 'SO20260608001', customerName: 'Delta International', currency: 'USD', amount: 28900, amountCny: 209525, tradeTerms: 'EXW', status: 'funded', orderDate: '2026-06-08' },
]
</script>
