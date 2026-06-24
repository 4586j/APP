<template>
  <div class="page-container">
    <div class="page-header">
      <h2>审批中心</h2>
      <p class="page-desc">查看和处理待审批事项，跟踪已发起的审批进度</p>
    </div>

    <el-tabs v-model="activeTab" style="margin-bottom:16px">
      <el-tab-pane name="pending">
        <template #label><el-badge :value="pendingCount" class="tab-badge">待我审批</el-badge></template>
      </el-tab-pane>
      <el-tab-pane label="我发起的" name="my" />
      <el-tab-pane label="全部审批" name="all" />
    </el-tabs>

    <div class="table-container">
      <el-table :data="displayList" stripe size="small" v-loading="loading">
        <el-table-column prop="requestNo" label="编号" width="150" />
        <el-table-column label="业务类型" width="110" align="center">
          <template #default="scope">{{ scope.row.bizTypeLabel }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column label="金额" width="140" align="right">
          <template #default="scope">
            <span v-if="scope.row.amount != null">¥ {{ Number(scope.row.amount).toLocaleString() }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="发起人" width="90">
          <template #default="scope">{{ scope.row.submitByLabel }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="110" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="scope">
            <template v-if="scope.row.status === 'pending' && activeTab === 'pending'">
              <el-button type="success" size="small" @click="handleApprove(scope.row)">通过</el-button>
              <el-button type="danger" size="small" @click="handleReject(scope.row)">驳回</el-button>
            </template>
            <el-tag v-else type="info" size="small">已处理</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination
          v-model:current-page="page.current"
          :page-size="page.size"
          :total="page.total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listFundApprovals,
  approveFundApproval,
  rejectFundApproval,
  type FundApprovalVO,
} from '@/api/finance'

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

interface ApprovalItem {
  id: number
  requestNo: string
  title: string
  bizTypeLabel: string
  amount: number | null
  submitByLabel: string
  status: string
  createdAt: string
}

const loading = ref(false)
const activeTab = ref('pending')
const fundList = ref<FundApprovalVO[]>([])
const page = reactive({ current: 1, size: 20, total: 0 })

function statusType(s: string): TagType {
  const m: Record<string, TagType> = { pending: 'warning', approved: 'success', rejected: 'danger', paid: 'info' }
  return m[s] || 'info'
}
function statusLabel(s: string) {
  const m: Record<string, string> = { pending: '审批中', approved: '已通过', rejected: '已驳回', paid: '已付款' }
  return m[s] || s
}

function toItem(f: FundApprovalVO): ApprovalItem {
  return {
    id: f.id,
    requestNo: f.requestNo,
    title: f.title,
    bizTypeLabel: '资金审批',
    amount: Number(f.amount),
    submitByLabel: '管理员',
    status: f.status,
    createdAt: f.createdAt,
  }
}

const allApprovals = computed<ApprovalItem[]>(() => fundList.value.map(toItem))
const pendingCount = computed(() => allApprovals.value.filter(a => a.status === 'pending').length)

const displayList = computed(() => {
  const all = allApprovals.value
  if (activeTab.value === 'pending') return all.filter(a => a.status === 'pending')
  if (activeTab.value === 'my') return all
  return all
})

async function loadData() {
  loading.value = true
  try {
    const res = await listFundApprovals({ page: page.current, size: page.size })
    fundList.value = res.records
    page.total = res.total
  } finally {
    loading.value = false
  }
}

function handleApprove(row: any) {
  ElMessageBox.confirm(`确认通过「${row.title}」？`, '提示', { type: 'info' })
    .then(async () => {
      await approveFundApproval(row.id)
      ElMessage.success('已审批通过')
      loadData()
    })
    .catch(() => {})
}

function handleReject(row: any) {
  ElMessageBox.prompt('请输入驳回原因', '驳回申请', { inputType: 'textarea' })
    .then(async ({ value }) => {
      await rejectFundApproval(row.id, value || '驳回')
      ElMessage.success('已驳回')
      loadData()
    })
    .catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.tab-badge { padding-right: 18px; }
</style>
