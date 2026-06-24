<template>
  <div class="page-container">
    <div class="page-header">
      <h2>资金审批</h2>
      <p class="page-desc">管理采购付款、费用报销等资金申请与审批</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="申请编号">
          <el-input v-model="query.requestNo" placeholder="编号" clearable style="width:170px" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部类型" clearable style="width:130px">
            <el-option label="采购付款" value="purchase" />
            <el-option label="费用报销" value="expense" />
            <el-option label="预付款" value="prepay" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="待审批" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
            <el-option label="已付款" value="paid" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Plus">新建申请</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>资金申请列表</h3>
      </div>
      <el-table :data="approvals" stripe size="small">
        <el-table-column prop="requestNo" label="申请编号" width="150" />
        <el-table-column prop="title" label="申请标题" min-width="200" />
        <el-table-column prop="type" label="类型" width="90" align="center" />
        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }">¥ {{ row.amount.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="requestBy" label="申请人" width="90" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestDate" label="申请日期" width="110" />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button type="success" link size="small">通过</el-button>
              <el-button type="danger" link size="small">驳回</el-button>
            </template>
            <el-button type="primary" link size="small" v-else>详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination :current-page="1" :page-size="20" :total="42" layout="total, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'

const query = reactive({ requestNo: '', type: '', status: '' })

function statusType(s: string) {
  const m: Record<string, string> = { pending: 'warning', approved: 'success', rejected: 'danger', paid: 'info' }
  return m[s] || ''
}
function statusLabel(s: string) {
  const m: Record<string, string> = { pending: '待审批', approved: '已通过', rejected: '已驳回', paid: '已付款' }
  return m[s] || s
}

const approvals = [
  { requestNo: 'FA20260615001', title: '采购订单 PO20260615001 付款申请', type: '采购付款', amount: 285000, requestBy: '王采购', status: 'pending', requestDate: '2026-06-15' },
  { requestNo: 'FA20260614002', title: '采购订单 PO20260614002 30%预付款', type: '预付款', amount: 59400, requestBy: '王采购', status: 'approved', requestDate: '2026-06-14' },
  { requestNo: 'FA20260613003', title: '6月份差旅费用报销', type: '费用报销', amount: 8500, requestBy: '李销售', status: 'paid', requestDate: '2026-06-13' },
  { requestNo: 'FA20260610004', title: '采购订单 PO20260610004 尾款', type: '采购付款', amount: 42500, requestBy: '王采购', status: 'rejected', requestDate: '2026-06-10' },
  { requestNo: 'FA20260605005', title: '展会参展费用', type: '费用报销', amount: 36000, requestBy: '赵经理', status: 'paid', requestDate: '2026-06-05' },
]
</script>
