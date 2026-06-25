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
          <el-button :icon="Plus" @click="showCreateDialog = true">新建分析</el-button>
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
          <el-input-number v-model="form.productId" :min="1" style="width:100%" />
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
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listPricings, createPricing, updatePricing, deletePricing, type PricingVO } from '@/api/data'

const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const analyses = ref<PricingVO[]>([])
const showDialog = ref(false)
const isEdit = ref(false)
const editId = ref(0)

const query = reactive({ pageNum: 1, pageSize: 20, keyword: '', status: '' })
const form = reactive({
  productId: 1, title: '', costPrice: 0, targetPrice: 0,
  competitorPrice: 0, suggestedPrice: 0, margin: 0,
  marketTrend: '', status: 'draft', remark: ''
})

function statusType(s: string): string {
  return { draft: 'info', reviewed: 'warning', published: 'success' }[s] || 'info'
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

function editPricing(row: PricingVO) {
  isEdit.value = true
  editId.value = row.id
  form.productId = row.productId
  form.title = row.title
  form.costPrice = row.costPrice
  form.targetPrice = row.targetPrice
  form.competitorPrice = row.competitorPrice || 0
  form.suggestedPrice = row.suggestedPrice || 0
  form.margin = row.margin || 0
  form.marketTrend = row.marketTrend || ''
  form.status = row.status
  form.remark = row.remark || ''
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

async function doDelete(id: number) {
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
