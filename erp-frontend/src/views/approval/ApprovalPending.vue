<template>
  <div class="page-container">
    <div class="page-header">
      <h2>审批中心</h2>
      <p class="page-desc">查看和处理待审批事项，跟踪已发起的审批进度</p>
    </div>

    <el-tabs v-model="activeTab" style="margin-bottom:16px">
      <el-tab-pane label="待我审批" name="pending">
        <template #label><el-badge :value="3" class="tab-badge">待我审批</el-badge></template>
      </el-tab-pane>
      <el-tab-pane label="我发起的" name="my" />
      <el-tab-pane label="全部审批" name="all" />
    </el-tabs>

    <div class="search-bar" v-if="activeTab !== 'pending'">
      <el-form :model="query" inline>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width:140px">
            <el-option label="销售订单" value="sales_order" />
            <el-option label="资金申请" value="fund_approval" />
            <el-option label="采购订单" value="purchase_order" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="审批中" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-container">
      <div class="table-header">
        <h3>{{ activeTab === 'pending' ? '待审批事项' : '审批列表' }}</h3>
      </div>
      <el-table :data="filteredApprovals" stripe size="small">
        <el-table-column prop="requestNo" label="编号" width="150" />
        <el-table-column prop="bizType" label="业务类型" width="100" align="center" />
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }">
            <span v-if="row.amount">¥ {{ row.amount.toLocaleString() }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitBy" label="发起人" width="90" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" width="110" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button type="success" size="small">通过</el-button>
              <el-button type="danger" size="small">驳回</el-button>
            </template>
            <el-button type="primary" link size="small" v-else>查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination :current-page="1" :page-size="20" :total="filteredApprovals.length" layout="total, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'

const activeTab = ref('pending')
const query = reactive({ type: '', status: '' })

function statusType(s: string) {
  const m: Record<string, string> = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return m[s] || ''
}
function statusLabel(s: string) {
  const m: Record<string, string> = { pending: '审批中', approved: '已通过', rejected: '已驳回' }
  return m[s] || s
}

const allApprovals = [
  { requestNo: 'AR20260615001', bizType: '销售订单', title: '销售订单 SO20260615001 提交审批', amount: 351625, submitBy: '李销售', status: 'pending', submittedAt: '2026-06-15' },
  { requestNo: 'AR20260614002', bizType: '资金申请', title: '采购订单 PO20260614002 30%预付款申请', amount: 59400, submitBy: '王采购', status: 'pending', submittedAt: '2026-06-14' },
  { requestNo: 'AR20260613003', bizType: '采购订单', title: '采购订单 PO20260613003 提交审批', amount: 420000, submitBy: '王采购', status: 'pending', submittedAt: '2026-06-13' },
  { requestNo: 'AR20260610004', bizType: '资金申请', title: '采购订单 PO20260610004 尾款申请', amount: 42500, submitBy: '王采购', status: 'rejected', submittedAt: '2026-06-10' },
  { requestNo: 'AR20260608005', bizType: '销售订单', title: '销售订单 SO20260608001 提交审批', amount: 209525, submitBy: '李销售', status: 'approved', submittedAt: '2026-06-08' },
  { requestNo: 'AR20260605006', bizType: '资金申请', title: '展会参展费用报销', amount: 36000, submitBy: '赵经理', status: 'approved', submittedAt: '2026-06-05' },
]

const filteredApprovals = computed(() => {
  if (activeTab.value === 'pending') {
    return allApprovals.filter(a => a.status === 'pending')
  }
  if (activeTab.value === 'my') {
    return allApprovals.filter(a => a.submitBy === '李销售')
  }
  return allApprovals
})
</script>

<style scoped>
.tab-badge { padding-right: 18px; }
</style>
