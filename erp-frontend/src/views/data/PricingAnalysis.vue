<template>
  <div class="page-container">
    <div class="page-header">
      <h2>定价分析</h2>
      <p class="page-desc">基于市场数据的产品定价分析与建议</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px" @change="doSearch">
            <el-option label="草稿" value="draft" />
            <el-option label="审核中" value="reviewed" />
            <el-option label="已发布" value="published" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Plus" @click="openCreateDialog">新建分析</el-button>
          <el-button v-if="hasPerm('data:pricing:import')" :icon="Upload" @click="openImportDialog">批量导入</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>分析报告列表</h3>
      </div>
      <el-table :data="analyses" stripe size="small" v-loading="loading">
        <el-table-column prop="title" label="分析标题" min-width="220" />
        <el-table-column label="成本价" width="120" align="right">
          <template #default="{ row }">$ {{ row.costPrice.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="目标价" width="120" align="right">
          <template #default="{ row }">$ {{ row.targetPrice.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="建议售价" width="120" align="right">
          <template #default="{ row }">$ {{ row.suggestedPrice.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="利润率" width="100" align="center">
          <template #default="{ row }">{{ row.margin ? row.margin.toFixed(1) : '-' }}%</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="日期" width="180" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="editPricing(row)">编辑</el-button>
            <el-popconfirm title="确定删除该分析？" @confirm="doDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="onPageChange"
        />
      </div>
    </div>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑定价分析' : '新建定价分析'" width="550px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="分析标题" required>
          <el-input v-model="form.title" placeholder="输入分析标题" />
        </el-form-item>
        <el-form-item label="产品ID" required>
          <el-input v-model="form.productId" placeholder="请输入产品ID" style="width:100%" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="成本价">
              <el-input-number v-model="form.costPrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标价">
              <el-input-number v-model="form.targetPrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="竞品价">
              <el-input-number v-model="form.competitorPrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建议售价">
              <el-input-number v-model="form.suggestedPrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="利润率">
              <el-input-number v-model="form.margin" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="市场趋势">
              <el-input v-model="form.marketTrend" placeholder="如：Stable/Growing" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="草稿" value="draft" />
            <el-option label="审核中" value="reviewed" />
            <el-option label="已发布" value="published" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="showImportDialog" title="批量导入定价分析" width="720px" @closed="resetImport">
      <div style="margin-bottom:16px">
        <el-button :icon="Document" @click="downloadTemplate">下载模板</el-button>
      </div>
      <el-upload
        ref="uploadRef"
        drag
        action="#"
        :auto-upload="false"
        :limit="1"
        :show-file-list="true"
        :on-change="onFileChange"
        accept=".xlsx,.xls"
      >
        <el-icon class="el-icon--upload"><Upload /></el-icon>
        <div class="el-upload__text">拖拽 Excel 文件到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 .xlsx / .xls，文件大小不超过 10MB</div>
        </template>
      </el-upload>
      <div v-if="selectedFile" style="margin-top:16px">
        <h4>文件信息</h4>
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="文件名">{{ selectedFile.name }}</el-descriptions-item>
          <el-descriptions-item label="大小">{{ formatFileSize(selectedFile.size) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 上传进度 -->
      <div v-if="uploading" style="margin-top:16px">
        <div style="margin-bottom:8px;font-size:14px">正在上传: {{ uploadPercent }}%</div>
        <el-progress :percentage="uploadPercent" :stroke-width="16" :striped="true" :striped-flow="true" />
      </div>

      <!-- 解析/导入进度 -->
      <div v-if="importingTaskId" style="margin-top:16px">
        <div style="margin-bottom:8px;font-size:14px">
          正在解析导入: {{ importProgress.percent }}%
          <span v-if="importProgress.status === 'RUNNING'" style="color:#409EFF;margin-left:8px">
            已读 {{ importProgress.totalRows }} 行,
            成功 {{ importProgress.successCount }} 条,
            失败 {{ importProgress.failCount }} 条
          </span>
        </div>
        <el-progress
          :percentage="importProgress.percent"
          :stroke-width="16"
          :status="importProgress.status === 'FAILED' ? 'exception' : ''"
        />
        <div v-if="importProgress.message" style="margin-top:8px;color:#606266;font-size:13px">
          {{ importProgress.message }}
        </div>
      </div>

      <!-- 导入结果 -->
      <div v-if="importResult" style="margin-top:16px">
        <el-alert
          :title="importResult.message || `导入完成：成功 ${importResult.successCount} 条，失败 ${importResult.failList.length} 条`"
          :type="importResult.status === 'FAILED' ? 'error' : (importResult.failList.length ? 'warning' : 'success')"
          show-icon
        />
        <el-table v-if="importResult.failList.length" :data="importResult.failList" size="small" border style="margin-top:8px">
          <el-table-column prop="index" label="Excel行号" width="100" />
          <el-table-column prop="name" label="分析标题" width="160" />
          <el-table-column prop="reason" label="失败原因" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="showImportDialog = false" :disabled="Boolean(importingTaskId) && importProgress.status === 'RUNNING'">关闭</el-button>
        <el-button type="primary" :loading="importing" :disabled="!selectedFile || importingTaskId != null" @click="doImport">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Search, Plus, Upload, Document } from '@element-plus/icons-vue'
import { ElMessage, type TagProps, type UploadFile, type UploadInstance } from 'element-plus'
import {
  listPricings, createPricing, updatePricing, deletePricing,
  importPricingExcel, getImportProgress, downloadPricingTemplate, type PricingVO,
  type ImportTaskVO,
} from '@/api/data'
import type { Id } from '@/api/system'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const hasPerm = (perm: string) => userStore.hasPermission(perm)

const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const analyses = ref<PricingVO[]>([])
const showDialog = ref(false)
const isEdit = ref(false)
const editId = ref<Id>(0)

const query = reactive({ pageNum: 1, pageSize: 20, keyword: '', status: '' })
const form = reactive({
  productId: 1 as Id, title: '', costPrice: 0, targetPrice: 0,
  competitorPrice: 0, suggestedPrice: 0, margin: 0,
  marketTrend: '', status: 'draft', remark: ''
})

// 批量导入
const showImportDialog = ref(false)
const importing = ref(false)
const selectedFile = ref<File | null>(null)
const importResult = ref<ImportTaskVO | null>(null)
const uploadRef = ref<UploadInstance>()

// 上传进度
const uploading = ref(false)
const uploadPercent = ref(0)

// 导入进度
const importingTaskId = ref<string | null>(null)
const importProgress = ref<ImportTaskVO>({
  taskId: '', fileName: '', status: 'PENDING', message: '',
  totalRows: 0, processedRows: 0, successCount: 0, failCount: 0, percent: 0, failList: [], createdAt: '',
})
let progressTimer: ReturnType<typeof setInterval> | null = null

function openImportDialog() { showImportDialog.value = true; resetImport() }
function resetImport() {
  selectedFile.value = null
  importResult.value = null
  uploading.value = false
  uploadPercent.value = 0
  importingTaskId.value = null
  importProgress.value = {
    taskId: '', fileName: '', status: 'PENDING', message: '',
    totalRows: 0, processedRows: 0, successCount: 0, failCount: 0, percent: 0, failList: [], createdAt: '',
  }
  if (progressTimer) { clearInterval(progressTimer); progressTimer = null }
  uploadRef.value?.clearFiles()
}

function formatFileSize(bytes?: number) {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

function onFileChange(file: UploadFile) {
  selectedFile.value = file.raw || null
  importResult.value = null
}

async function doImport() {
  if (!selectedFile.value) { ElMessage.warning('请选择文件'); return }
  importing.value = true
  uploading.value = true
  uploadPercent.value = 0
  importingTaskId.value = null
  importResult.value = null
  try {
    const task = await importPricingExcel(selectedFile.value, (percent) => {
      uploadPercent.value = percent
    })
    uploading.value = false
    importingTaskId.value = task.taskId
    startPolling(task.taskId)
  } catch (e: any) {
    uploading.value = false
    importing.value = false
    ElMessage.error(e?.message || '导入失败')
  }
}

function startPolling(taskId: string) {
  if (progressTimer) clearInterval(progressTimer)
  progressTimer = setInterval(async () => {
    try {
      const progress = await getImportProgress(taskId)
      importProgress.value = progress
      if (progress.status === 'DONE' || progress.status === 'FAILED') {
        if (progressTimer) { clearInterval(progressTimer); progressTimer = null }
        importing.value = false
        importResult.value = progress
        if (progress.status === 'DONE' && progress.failCount === 0) {
          ElMessage.success(`成功导入 ${progress.successCount} 条`)
        }
        await fetchData()
      }
    } catch (e) {
      console.error('查询导入进度失败:', e)
    }
  }, 1000)
}

async function downloadTemplate() {
  try {
    const blob = await downloadPricingTemplate()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '定价分析导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error(e?.message || '下载模板失败')
  }
}

function statusType(s: string): TagProps['type'] {
  return ({ draft: 'info', reviewed: 'warning', published: 'success' } as Record<string, TagProps['type']>)[s] || 'info'
}
function statusLabel(s: string): string {
  return { draft: '草稿', reviewed: '审核中', published: '已发布' }[s] || s
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listPricings(query)
    analyses.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    console.error('Failed to load pricings:', e)
  } finally {
    loading.value = false
  }
}

function doSearch() { query.pageNum = 1; fetchData() }
function onPageChange(p: number) { query.pageNum = p; fetchData() }
function openCreateDialog() { resetForm(); showDialog.value = true }

function editPricing(row: any) {
  const pricing = row as PricingVO
  isEdit.value = true
  editId.value = pricing.id
  form.productId = pricing.productId
  form.title = pricing.title
  form.costPrice = pricing.costPrice
  form.targetPrice = pricing.targetPrice
  form.competitorPrice = pricing.competitorPrice || 0
  form.suggestedPrice = pricing.suggestedPrice || 0
  form.margin = pricing.margin || 0
  form.marketTrend = pricing.marketTrend || ''
  form.status = pricing.status
  form.remark = pricing.remark || ''
  showDialog.value = true
}

function resetForm() {
  isEdit.value = false
  editId.value = 0
  form.productId = 1
  form.title = ''
  form.costPrice = 0
  form.targetPrice = 0
  form.competitorPrice = 0
  form.suggestedPrice = 0
  form.margin = 0
  form.marketTrend = ''
  form.status = 'draft'
  form.remark = ''
}

async function doSave() {
  if (!form.title) { ElMessage.warning('请输入分析标题'); return }
  if (!form.productId) { ElMessage.warning('请输入产品ID'); return }
  saving.value = true
  try {
    if (isEdit.value) {
      await updatePricing(editId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createPricing({ ...form })
      ElMessage.success('创建成功')
    }
    showDialog.value = false
    resetForm()
    fetchData()
  } catch (e: any) {
    console.error('Save failed:', e)
  } finally {
    saving.value = false
  }
}

async function doDelete(id: Id) {
  try {
    await deletePricing(id)
    ElMessage.success('已删除')
    fetchData()
  } catch (e: any) {
    console.error('Delete failed:', e)
  }
}

onMounted(fetchData)
</script>
