<template>
  <div class="page-container">
    <div class="page-header">
      <h2>产品管理</h2>
      <p class="page-desc">管理产品目录、HS 海关编码及多币种定价</p>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="产品名称">
          <el-input v-model="query.keyword" placeholder="名称/编码" clearable style="width:200px" @clear="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width:160px" @change="handleSearch">
            <el-option v-for="c in categories" :key="c.id" :label="c.catName" :value="c.id" />
            <template v-for="c in categories" :key="'c'+c.id">
              <el-option v-for="child in c.children" :key="child.id" :label="'  └ ' + child.catName" :value="child.id" />
            </template>
          </el-select>
        </el-form-item>
        <el-form-item label="HS编码">
          <el-input v-model="query.hsCode" placeholder="海关编码" clearable style="width:150px" @clear="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Plus" @click="openCreate">新增产品</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <div class="table-header">
        <h3>产品列表</h3>
        <el-button :icon="Download" @click="handleExport">导出</el-button>
      </div>

      <el-table :data="products" stripe size="small" v-loading="loading">
        <el-table-column prop="productCode" label="产品编码" width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="240" show-overflow-tooltip />
        <el-table-column label="分类" width="110">
          <template #default="{ row }">{{ getCategoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column prop="hsCode" label="HS 编码" width="120" />
        <el-table-column prop="unit" label="单位" width="70" align="center" />
        <el-table-column label="采购价 (¥)" width="120" align="right">
          <template #default="{ row }">{{ fmtPrice(row.purchasePrice) }}</template>
        </el-table-column>
        <el-table-column label="销售价 ($)" width="120" align="right">
          <template #default="{ row }">{{ fmtPrice(row.salesPrice, '$') }}</template>
        </el-table-column>
        <el-table-column prop="originCountry" label="原产国" width="100" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row as any)">编辑</el-button>
            <el-button type="primary" link size="small" @click="openPrices(row as any)">定价</el-button>
            <el-popconfirm title="确定删除该产品？" @confirm="handleDelete((row as any).id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchProducts"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑产品' : '新增产品'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="产品编码" prop="productCode">
              <el-input v-model="form.productCode" placeholder="如 LED-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品名称" prop="productName">
              <el-input v-model="form.productName" placeholder="中文名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="英文名称">
              <el-input v-model="form.productNameEn" placeholder="English Name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width:100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.catName" :value="c.id" />
                <template v-for="c in categories" :key="'c'+c.id">
                  <el-option v-for="child in c.children" :key="child.id" :label="'  └ ' + child.catName" :value="child.id" />
                </template>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="HS编码ID">
              <el-input-number v-model="form.hsCodeId" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位">
              <el-input v-model="form.unit" placeholder="件/套/米" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="品牌">
              <el-input v-model="form.brand" placeholder="如 Opple" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="规格说明">
              <el-input v-model="form.specification" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原产国">
              <el-input v-model="form.originCountry" placeholder="如 中国" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="采购价 (¥)">
              <el-input-number v-model="form.purchasePrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="销售价 ($)">
              <el-input-number v-model="form.salesPrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="成本价 ($)">
              <el-input-number v-model="form.costPrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="重量 (kg)">
              <el-input-number v-model="form.weightKg" :min="0" :precision="4" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="体积 (m³)">
              <el-input-number v-model="form.volumeCbm" :min="0" :precision="6" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="MOQ">
              <el-input-number v-model="form.moq" :min="1" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Download } from '@element-plus/icons-vue'
import {
  listProducts, createProduct, updateProduct, deleteProduct,
  listCategoryTree,
  type ProductVO, type ProductQuery, type ProductCreateRequest, type CategoryNode,
} from '@/api/product'

/* --- 状态 --- */
const loading = ref(false)
const products = ref<ProductVO[]>([])
const categories = ref<CategoryNode[]>([])
const query = reactive<ProductQuery>({ page: 1, size: 20 })
const pagination = reactive({ current: 1, size: 20, total: 0 })

/* --- 对话框 --- */
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref()
const form = reactive<ProductCreateRequest>({
  productCode: '', productName: '', productNameEn: '',
  categoryId: undefined, unit: '件', specification: '',
  originCountry: '中国', brand: '',
  purchasePrice: 0, salesPrice: 0, costPrice: 0,
  weightKg: 0, volumeCbm: 0, moq: 1,
})

const rules = {
  productCode: [{ required: true, message: '请输入产品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
}

/* --- 工具函数 --- */
function fmtPrice(val: number | undefined | null, prefix = '¥') {
  if (val == null) return '-'
  return prefix + ' ' + Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function getCategoryName(categoryId: number | undefined | null): string {
  if (!categoryId) return '-'
  for (const c of categories.value) {
    if (c.id === categoryId) return c.catName
    if (c.children) {
      const child = c.children.find(ch => ch.id === categoryId)
      if (child) return child.catName
    }
  }
  return '-'
}

/* --- 数据加载 --- */
async function fetchCategories() {
  try {
    categories.value = await listCategoryTree()
  } catch { /* ignore */ }
}

async function fetchProducts() {
  loading.value = true
  try {
    query.page = pagination.current
    query.size = pagination.size
    const res = await listProducts(query)
    products.value = res.records ?? []
    pagination.total = res.total ?? 0
  } catch {
    products.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchProducts()
}

/* --- 新增/编辑 --- */
function openCreate() {
  isEdit.value = false
  editId.value = null
  form.productCode = ''
  form.productName = ''
  form.productNameEn = ''
  form.categoryId = undefined
  form.unit = '件'
  form.specification = ''
  form.originCountry = '中国'
  form.brand = ''
  form.purchasePrice = 0
  form.salesPrice = 0
  form.costPrice = 0
  form.weightKg = 0
  form.volumeCbm = 0
  form.moq = 1
  dialogVisible.value = true
}

async function openEdit(row: ProductVO) {
  isEdit.value = true
  editId.value = row.id
  form.productCode = row.productCode
  form.productName = row.productName
  form.productNameEn = row.productNameEn ?? ''
  form.categoryId = row.categoryId
  form.unit = row.unit || '件'
  form.specification = row.specification ?? ''
  form.originCountry = row.originCountry ?? '中国'
  form.brand = row.brand ?? ''
  form.purchasePrice = row.purchasePrice ?? 0
  form.salesPrice = row.salesPrice ?? 0
  form.costPrice = row.costPrice ?? 0
  form.weightKg = row.weightKg ?? 0
  form.volumeCbm = row.volumeCbm ?? 0
  form.moq = row.moq ?? 1
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateProduct(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createProduct(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await fetchProducts()
  } catch {
    // 错误由拦截器统一提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteProduct(id)
    ElMessage.success('删除成功')
    await fetchProducts()
  } catch { /* handled by interceptor */ }
}

function openPrices(row: ProductVO) {
  // TODO: B2.4 定价管理详情对话框
  ElMessage.info('定价管理将在后续版本中实现')
}

function handleExport() {
  ElMessage.info('导出功能将在 B2.4 中实现 (EasyExcel)')
}

/* --- 初始化 --- */
onMounted(() => {
  fetchCategories()
  fetchProducts()
})
</script>

<style scoped>
.page-container { padding: 24px; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #1a1a2e; }
.page-desc { margin: 4px 0 0; color: #889; font-size: 13px; }
.search-bar { background: #f8f9fc; padding: 16px; border-radius: 8px; margin-bottom: 16px; }
.table-container { background: #fff; border-radius: 8px; padding: 16px; }
.table-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.table-header h3 { margin: 0; font-size: 15px; color: #333; }
</style>
