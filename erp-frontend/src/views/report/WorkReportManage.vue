<template>
  <div class="page-container">
    <div class="page-header">
      <h2>工作报表管理</h2>
      <p class="page-desc">查看员工工作计划与工作日志，支持批量审批</p>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="日期">
          <el-date-picker v-model="query.reportDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:150px" @change="doSearch" />
        </el-form-item>
        <el-form-item label="部门">
          <DepartmentSelect v-model="query.departmentId" clearable style="width:180px" @change="doSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px" @change="doSearch">
            <el-option label="草稿" value="draft" />
            <el-option label="待审批" value="submitted" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width:120px" @change="doSearch">
            <el-option label="仅计划" value="plan" />
            <el-option label="仅日志" value="log" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="姓名/标题/内容" clearable style="width:180px" @keyup.enter="doSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 批量操作 -->
    <div v-if="selectedRows.length > 0" class="batch-bar">
      <span>已选择 {{ selectedRows.length }} 项</span>
      <el-button type="success" size="small" @click="openBatchApprove('approved')">批量通过</el-button>
      <el-button type="danger" size="small" @click="openBatchApprove('rejected')">批量驳回</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <div class="table-header">
        <h3>报表列表</h3>
      </div>
      <el-table
        :data="records"
        stripe
        size="small"
        v-loading="loading"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="45" align="center" />
        <el-table-column label="员工" width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-name">{{ row.realName || row.username }}</div>
              <div class="user-dept">{{ row.departmentName || '-' }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="工作计划" min-width="240">
          <template #default="{ row }: any">
            <div v-if="row.planId" class="report-cell" @click="showDetail(row as WorkReportVO, 'plan')">
              <div class="report-header">
                <span class="report-title">{{ row.planTitle || '无标题' }}</span>
                <el-tag :type="tagType(row.planStatus)" size="small">{{ statusLabel(row.planStatus) }}</el-tag>
              </div>
              <div class="report-preview">{{ row.planContent }}</div>
            </div>
            <div v-else class="report-empty">未填写</div>
          </template>
        </el-table-column>

        <el-table-column label="工作日志" min-width="240">
          <template #default="{ row }: any">
            <div v-if="row.logId" class="report-cell" @click="showDetail(row as WorkReportVO, 'log')">
              <div class="report-header">
                <span class="report-title">{{ row.logTitle || '无标题' }}</span>
                <el-tag :type="tagType(row.logStatus)" size="small">{{ statusLabel(row.logStatus) }}</el-tag>
              </div>
              <div class="report-preview">{{ row.logContent }}</div>
            </div>
            <div v-else class="report-empty">未填写</div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <el-button
                v-if="row.planStatus === 'submitted'"
                type="success"
                link
                size="small"
                @click="approveOne(row.planId, 'plan', 'approved')"
              >通过(计划)</el-button>
              <el-button
                v-if="row.planStatus === 'submitted'"
                type="danger"
                link
                size="small"
                @click="approveOne(row.planId, 'plan', 'rejected')"
              >驳回(计划)</el-button>
              <el-button
                v-if="row.logStatus === 'submitted'"
                type="success"
                link
                size="small"
                @click="approveOne(row.logId, 'log', 'approved')"
              >通过(日志)</el-button>
              <el-button
                v-if="row.logStatus === 'submitted'"
                type="danger"
                link
                size="small"
                @click="approveOne(row.logId, 'log', 'rejected')"
              >驳回(日志)</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination
          :current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="onPageChange"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="详情" width="560px">
      <div v-if="detailRow">
        <div class="detail-user">
          <strong>{{ detailRow.realName || detailRow.username }}</strong>
          <el-tag size="small" type="info">{{ detailRow.departmentName || '-' }}</el-tag>
          <span class="detail-date">{{ detailRow.reportDate }}</span>
        </div>
        <el-divider />
        <div v-if="detailType === 'plan' && detailRow.planId">
          <h4>工作计划</h4>
          <div v-if="detailRow.planTitle" class="detail-title">{{ detailRow.planTitle }}</div>
          <div class="detail-content">{{ detailRow.planContent }}</div>
          <div class="detail-meta">
            <el-tag :type="tagType(detailRow.planStatus)" size="small">{{ statusLabel(detailRow.planStatus) }}</el-tag>
            <span v-if="detailRow.planApproveComment">审批意见：{{ detailRow.planApproveComment }}</span>
          </div>
        </div>
        <div v-if="detailType === 'log' && detailRow.logId">
          <h4>工作日志</h4>
          <div v-if="detailRow.logTitle" class="detail-title">{{ detailRow.logTitle }}</div>
          <div class="detail-content">{{ detailRow.logContent }}</div>
          <div class="detail-meta">
            <el-tag :type="tagType(detailRow.logStatus)" size="small">{{ statusLabel(detailRow.logStatus) }}</el-tag>
            <span v-if="detailRow.logApproveComment">审批意见：{{ detailRow.logApproveComment }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 批量审批弹窗 -->
    <el-dialog v-model="batchDialogVisible" :title="batchTitle" width="420px">
      <el-form :model="batchForm" label-width="80px">
        <el-form-item label="审批意见">
          <el-input v-model="batchForm.comment" type="textarea" :rows="3" placeholder="请输入审批意见（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="doBatchApprove">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DepartmentSelect from '@/components/DepartmentSelect.vue'
import {
  listWorkReports, batchApproveWorkReports,
  type WorkReportVO, type WorkReportQuery,
} from '@/api/report'
import type { Id } from '@/api/system'

const loading = ref(false)
const records = ref<WorkReportVO[]>([])
const total = ref(0)
const selectedRows = ref<WorkReportVO[]>([])

const query = reactive<WorkReportQuery>({
  reportDate: new Date().toISOString().slice(0, 10),
  departmentId: undefined,
  status: undefined,
  type: undefined,
  keyword: '',
  page: 1,
  size: 20,
})

function doSearch() {
  query.page = 1
  fetchData()
}

function resetQuery() {
  query.reportDate = new Date().toISOString().slice(0, 10)
  query.departmentId = undefined
  query.status = undefined
  query.type = undefined
  query.keyword = ''
  query.page = 1
  fetchData()
}

function onPageChange(p: number) {
  query.page = p
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listWorkReports({ ...query })
    records.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    console.error('load work reports failed:', e)
  } finally {
    loading.value = false
  }
}

function onSelectionChange(rows: WorkReportVO[]) {
  selectedRows.value = rows
}

/* ---------- 状态工具 ---------- */
function statusLabel(s?: string) {
  const m: Record<string, string> = { draft: '草稿', submitted: '待审批', approved: '已通过', rejected: '已驳回' }
  return s ? (m[s] || s) : '-'
}
function tagType(s?: string) {
  const m: Record<string, any> = { draft: 'info', submitted: 'warning', approved: 'success', rejected: 'danger' }
  return m[s || ''] || 'info'
}

/* ---------- 详情 ---------- */
const detailVisible = ref(false)
const detailRow = ref<WorkReportVO | null>(null)
const detailType = ref<'plan' | 'log'>('plan')

function showDetail(row: WorkReportVO, type: 'plan' | 'log') {
  detailRow.value = row
  detailType.value = type
  detailVisible.value = true
}

/* ---------- 单条审批 ---------- */
async function approveOne(id: Id, type: 'plan' | 'log', action: 'approved' | 'rejected') {
  const label = action === 'approved' ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(`确认${label}该${type === 'plan' ? '计划' : '日志'}？`, '提示', { type: action === 'approved' ? 'info' : 'warning' })
    const req = type === 'plan'
      ? { planIds: [id], action } as any
      : { logIds: [id], action } as any
    await batchApproveWorkReports(req)
    ElMessage.success(`已${label}`)
    fetchData()
  } catch (e: any) {
    if (e !== 'cancel') console.error('approve failed:', e)
  }
}

/* ---------- 批量审批 ---------- */
const batchDialogVisible = ref(false)
const batchTitle = ref('')
const batchAction = ref<'approved' | 'rejected'>('approved')
const batchLoading = ref(false)
const batchForm = reactive({ comment: '' })

function openBatchApprove(action: 'approved' | 'rejected') {
  batchAction.value = action
  batchTitle.value = action === 'approved' ? '批量通过' : '批量驳回'
  batchForm.comment = ''
  batchDialogVisible.value = true
}

async function doBatchApprove() {
  const planIds: Id[] = []
  const logIds: Id[] = []
  selectedRows.value.forEach(row => {
    if (row.planStatus === 'submitted' && row.planId) planIds.push(row.planId)
    if (row.logStatus === 'submitted' && row.logId) logIds.push(row.logId)
  })
  if (planIds.length === 0 && logIds.length === 0) {
    ElMessage.warning('没有可审批的待审批项')
    return
  }
  batchLoading.value = true
  try {
    await batchApproveWorkReports({
      planIds: planIds.length ? planIds : undefined,
      logIds: logIds.length ? logIds : undefined,
      action: batchAction.value,
      comment: batchForm.comment || undefined,
    })
    ElMessage.success('审批完成')
    batchDialogVisible.value = false
    selectedRows.value = []
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.message || '审批失败')
  } finally {
    batchLoading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.page-container {
  padding: 20px;
}
.page-header {
  margin-bottom: 16px;
  h2 { margin: 0; font-size: 20px; font-weight: 600; }
  .page-desc { margin: 4px 0 0; font-size: 13px; color: #9ca3af; }
}
.search-bar {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
}
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  padding: 10px 16px;
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid #f0f0f0;
  span { font-size: 13px; color: #4b5563; }
}
.table-container {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
}
.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  h3 { margin: 0; font-size: 15px; font-weight: 600; }
}

.user-cell {
  .user-name { font-size: 14px; font-weight: 500; color: #1f2937; }
  .user-dept { font-size: 12px; color: #9ca3af; margin-top: 2px; }
}

.report-cell {
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: background 0.2s;
  &:hover { background: #f8fafc; }
  .report-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
  }
  .report-title {
    font-size: 13px;
    font-weight: 500;
    color: #1f2937;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 160px;
  }
  .report-preview {
    font-size: 12px;
    color: #6b7280;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.5;
  }
}
.report-empty {
  font-size: 13px;
  color: #d1d5db;
  padding: 8px;
}

.op-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}

.detail-user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  .detail-date { margin-left: auto; color: #9ca3af; font-size: 13px; }
}
.detail-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 8px 0;
}
.detail-content {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.7;
  white-space: pre-wrap;
  background: #f8fafc;
  padding: 12px;
  border-radius: 6px;
  margin: 8px 0;
}
.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  font-size: 13px;
  color: #6b7280;
}
</style>
