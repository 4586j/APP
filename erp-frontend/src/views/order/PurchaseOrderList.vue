<template>
  <div class="page-container">
    <div class="page-header">
      <h2>采购订单</h2>
      <p class="page-desc">管理供应商采购订单，跟踪采购进度与收货</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="采购单号">
          <el-input v-model="query.poNo" placeholder="输入单号" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="query.supplierId" placeholder="选择供应商" clearable style="width:180px">
            <el-option label="深圳华强电子" :value="1" />
            <el-option label="浙江正泰电器" :value="2" />
            <el-option label="广东力特电机" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:130px">
            <el-option label="草稿" value="draft" />
            <el-option label="已发送" value="sent" />
            <el-option label="已确认" value="confirmed" />
            <el-option label="运输中" value="in_transit" />
            <el-option label="已收货" value="received" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Plus">新建采购单</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>采购订单列表</h3>
        <el-button :icon="Download">导出</el-button>
      </div>
      <el-table :data="orders" stripe size="small">
        <el-table-column prop="poNo" label="采购单号" width="150" />
        <el-table-column prop="supplierName" label="供应商" min-width="160" />
        <el-table-column prop="salesOrderNo" label="关联销售单" width="150" />
        <el-table-column prop="currency" label="币种" width="70" align="center" />
        <el-table-column prop="amount" label="金额" width="130" align="right">
          <template #default="{ row }">{{ row.currency }} {{ row.amount.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="amountCny" label="金额 (RMB)" width="140" align="right">
          <template #default="{ row }">¥ {{ row.amountCny.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="poDate" label="采购日期" width="110" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default>
            <el-button type="primary" link size="small">详情</el-button>
            <el-button type="primary" link size="small">收货</el-button>
            <el-button type="danger" link size="small">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination :current-page="1" :page-size="20" :total="56" layout="total, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
import { reactive } from 'vue'
import { Search, Plus, Download } from '@element-plus/icons-vue'

const query = reactive({ poNo: '', supplierId: null, status: '' })

function statusType(s: string): TagType {
  const m: Record<string, TagType> = { draft: 'info', sent: 'warning', confirmed: 'info', in_transit: 'primary', received: 'success' }
  return m[s] || 'info'
}
function statusLabel(s: string) {
  const m: Record<string, string> = { draft: '草稿', sent: '已发送', confirmed: '已确认', in_transit: '运输中', received: '已收货', closed: '已关闭' }
  return m[s] || s
}

const orders = [
  { poNo: 'PO20260615001', supplierName: '深圳华强电子有限公司', salesOrderNo: 'SO20260615001', currency: 'CNY', amount: 285000, amountCny: 285000, status: 'confirmed', poDate: '2026-06-15' },
  { poNo: 'PO20260614002', supplierName: '浙江正泰电器股份有限公司', salesOrderNo: 'SO20260614003', currency: 'CNY', amount: 198000, amountCny: 198000, status: 'in_transit', poDate: '2026-06-14' },
  { poNo: 'PO20260613003', supplierName: '广东力特电机制造有限公司', salesOrderNo: 'SO20260613007', currency: 'CNY', amount: 420000, amountCny: 420000, status: 'sent', poDate: '2026-06-13' },
  { poNo: 'PO20260610004', supplierName: '江苏阳光纺织集团', salesOrderNo: 'SO20260610008', currency: 'CNY', amount: 85000, amountCny: 85000, status: 'received', poDate: '2026-06-10' },
  { poNo: 'PO20260605005', supplierName: '山东鲁阳机械有限公司', salesOrderNo: 'SO20260609004', currency: 'CNY', amount: 310000, amountCny: 310000, status: 'closed', poDate: '2026-06-05' },
]
</script>
