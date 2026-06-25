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
      <div v-if="previewData.length" style="margin-top:16px">
        <h4>数据预览（前10行）</h4>
        <el-table :data="previewData" size="small" border style="margin-top:8px">
          <el-table-column prop="productId" label="产品ID" width="90" />
          <el-table-column prop="title" label="分析标题" min-width="160" />
          <el-table-column prop="costPrice" label="成本价" width="90" />
          <el-table-column prop="targetPrice" label="目标价" width="90" />
          <el-table-column prop="suggestedPrice" label="建议售价" width="90" />
        </el-table>
      </div>
      <div v-if="importResult" style="margin-top:16px">
        <el-alert
          :title="`导入完成：成功 ${importResult.successCount} 条，失败 ${importResult.failList.length} 条`"
          :type="importResult.failList.length ? 'warning' : 'success'"
          show-icon
        />
        <el-table v-if="importResult.failList.length" :data="importResult.failList" size="small" border style="margin-top:8px">
          <el-table-column prop="index" label="Excel行号" width="100" />
          <el-table-column prop="name" label="分析标题" width="160" />
          <el-table-column prop="reason" label="失败原因" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="showImportDialog = false">关闭</el-button>
        <el-button type="primary" :loading="importing" :disabled="!selectedFile" @click="doImport">确认导入</el-button>
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
  importPricingExcel, downloadPricingTemplate, type PricingVO,
} from '@/api/data'
import type { Id } from '@/api/system'
import { useUserStore } from '@/store/user'
import * as XLSX from 'xlsx'

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
const previewData = ref<any[]>([])
const importResult = ref<{ successCount: number; failList: any[] } | null>(null)
const uploadRef = ref<UploadInstance>()

function openImportDialog() { showImportDialog.value = true; resetImport() }
function resetImport() {
  selectedFile.value = null
  previewData.value = []
  importResult.value = null
  uploadRef.value?.clearFiles()
}

function onFileChange(file: UploadFile) {
  selectedFile.value = file.raw || null
  previewData.value = []
  importResult.value = null
  if (file.raw) readExcelPreview(file.raw)
}

async function readExcelPreview(file: File) {
  try {
    const data = await file.arrayBuffer()
    const workbook = XLSX.read(data)
    const worksheet = workbook.Sheets[workbook.SheetNames[0]]
    const rows = XLSX.utils.sheet_to_json(worksheet, { header: 1 }) as any[][]
    if (!rows.length) { previewData.value = []; return }
    const headers = rows[0] as string[]
    const colIndex: Record<string, number> = {}
    headers.forEach((h, i) => { colIndex[String(h).trim()] = i })
    const dataRows = rows.slice(1, 11)
    previewData.value = dataRows.map((row) => ({
      productId: row[colIndex['产品ID'] ?? colIndex['productId'] ?? 0] || '',
      title: row[colIndex['分析标题'] ?? colIndex['title'] ?? 1] || '',
      costPrice: row[colIndex['成本价'] ?? colIndex['costPrice'] ?? 2] || '',
      targetPrice: row[colIndex['目标价'] ?? colIndex['targetPrice'] ?? 3] || '',
      suggestedPrice: row[colIndex['建议售价'] ?? colIndex['suggestedPrice'] ?? 5] || '',
    }))
  } catch (e) {
    console.error('Excel preview failed:', e)
    previewData.value = [{ productId: '-', title: '文件解析失败', costPrice: '-', targetPrice: '-', suggestedPrice: '-' }]
  }
}

async function doImport() {
  if (!selectedFile.value) { ElMessage.warning('请选择文件'); return }
  importing.value = true
  try {
    const res = await importPricingExcel(selectedFile.value)
    importResult.value = res
    if (res.failList.length === 0) ElMessage.success(`成功导入 ${res.successCount} 条`)
    await fetchData()
  } catch (e: any) {
    ElMessage.error(e?.message || '导入失败')
  } finally {
    importing.value = false
  }
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
