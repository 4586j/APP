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
          <el-select v-model="query.fundType" placeholder="全部类型" clearable style="width:130px">
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
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Plus" @click="showCreate = true">新建申请</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>资金申请列表</h3>
      </div>
      <el-table :data="list" stripe size="small" v-loading="loading">
        <el-table-column prop="requestNo" label="申请编号" width="150" />
        <el-table-column prop="title" label="申请标题" min-width="200" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="scope">{{ typeLabel(scope.row.fundType) }}</template>
        </el-table-column>
        <el-table-column label="金额" width="140" align="right">
          <template #default="scope">¥ {{ Number(scope.row.amount).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="supplierName" label="供应商" width="120" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="110" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="scope">
            <template v-if="scope.row.status === 'pending'">
              <el-button type="success" link size="small" @click="handleApprove(scope.row)">通过</el-button>
              <el-button type="danger" link size="small" @click="handleReject(scope.row)">驳回</el-button>
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
          @current-change="loadList"
        />
      </div>
    </div>
  </div>

  <!-- 新建申请对话框 -->
  <el-dialog v-model="showCreate" title="新建资金申请" width="520px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="申请标题">
        <el-input v-model="form.title" placeholder="如：采购订单 PO... 付款申请" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.fundType" style="width:100%">
          <el-option label="采购付款" value="purchase" />
          <el-option label="费用报销" value="expense" />
          <el-option label="预付款" value="prepay" />
        </el-select>
      </el-form-item>
      <el-form-item label="金额 (¥)">
        <el-input-number v-model="form.amount" :min="0" :step="1000" style="width:100%" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showCreate = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleCreate">提交申请</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listFundApprovals,
  createFundApproval,
  approveFundApproval,
  rejectFundApproval,
  type FundApprovalVO,
} from '@/api/finance'

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

const loading = ref(false)
const submitting = ref(false)
const showCreate = ref(false)
const list = ref<FundApprovalVO[]>([])
const page = reactive({ current: 1, size: 20, total: 0 })
const query = reactive({ requestNo: '', fundType: '', status: '' })
const form = reactive({ title: '', fundType: 'purchase', amount: 0, description: '' })

function statusType(s: string): TagType {
  const m: Record<string, TagType> = { pending: 'warning', approved: 'success', rejected: 'danger', paid: 'info' }
  return m[s] || 'info'
}
function statusLabel(s: string) {
  const m: Record<string, string> = { pending: '待审批', approved: '已通过', rejected: '已驳回', paid: '已付款' }
  return m[s] || s
}
function typeLabel(s: string) {
  const m: Record<string, string> = { purchase: '采购付款', expense: '费用报销', prepay: '预付款' }
  return m[s] || s
}

async function loadList() {
  loading.value = true
  try {
    const res = await listFundApprovals({
      requestNo: query.requestNo || undefined,
      fundType: query.fundType || undefined,
      status: query.status || undefined,
      page: page.current,
      size: page.size,
    })
    list.value = res.records
    page.total = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.current = 1
  loadList()
}

async function handleCreate() {
  if (!form.title) { ElMessage.warning('请输入申请标题'); return }
  if (form.amount <= 0) { ElMessage.warning('请输入有效金额'); return }
  submitting.value = true
  try {
    await createFundApproval({
      title: form.title,
      fundType: form.fundType,
      amount: form.amount,
      description: form.description || undefined,
    })
    ElMessage.success('申请已提交')
    showCreate.value = false
    Object.assign(form, { title: '', fundType: 'purchase', amount: 0, description: '' })
    loadList()
  } finally {
    submitting.value = false
  }
}

function handleApprove(row: any) {
  ElMessageBox.confirm(`确认通过申请「${row.title}」？`, '提示', { type: 'info' })
    .then(async () => {
      await approveFundApproval(row.id)
      ElMessage.success('已审批通过')
      loadList()
    })
    .catch(() => {})
}

function handleReject(row: any) {
  ElMessageBox.prompt('请输入驳回原因', '驳回申请', { inputType: 'textarea' })
    .then(async ({ value }) => {
      await rejectFundApproval(row.id, value || '驳回')
      ElMessage.success('已驳回')
      loadList()
    })
    .catch(() => {})
}

onMounted(() => loadList())
</script>
