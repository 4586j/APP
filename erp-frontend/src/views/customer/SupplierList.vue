<template>
  <div class="page-container">
    <div class="page-header"><h2>供应商列表</h2><p class="page-desc">管理国内供应商信息、评分及联系方式</p></div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="供应商名称"><el-input v-model="query.keyword" placeholder="名称" clearable style="width:200px" @clear="handleSearch" /></el-form-item>
        <el-form-item label="地区"><el-select v-model="query.province" placeholder="全部地区" clearable style="width:140px" @change="handleSearch"><el-option label="广东" value="广东" /><el-option label="浙江" value="浙江" /><el-option label="江苏" value="江苏" /><el-option label="山东" value="山东" /><el-option label="福建" value="福建" /></el-select></el-form-item>
        <el-form-item label="评分"><el-select v-model="query.rating" placeholder="全部" clearable style="width:100px" @change="handleSearch"><el-option label="5星" :value="5" /><el-option label="4星" :value="4" /><el-option label="3星" :value="3" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button><el-button :icon="Plus" @click="openCreate">新增供应商</el-button></el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header"><h3>供应商列表</h3><el-button :icon="Download" @click="handleExport">导出</el-button></div>
      <el-table :data="suppliers" stripe size="small" v-loading="loading">
        <el-table-column prop="supplierCode" label="供应商编码" width="120" />
        <el-table-column prop="supplierName" label="供应商名称" min-width="200" />
        <el-table-column prop="province" label="省份" width="80" />
        <el-table-column prop="contactPerson" label="联系人" width="90" />
        <el-table-column prop="contactPhone" label="电话" width="130" />
        <el-table-column label="评分" width="130" align="center"><template #default="{ row }"><el-rate v-model="(row as any).rating" disabled size="small" /></template></el-table-column>
        <el-table-column prop="mainProducts" label="主营产品" min-width="180" show-overflow-tooltip />
        <el-table-column prop="paymentTerms" label="付款方式" width="110" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEdit(row as any)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete((row as any).id)">
              <template #reference><el-button type="danger" link size="small">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size" :total="pagination.total" layout="total, prev, pager, next" @current-change="fetchSuppliers" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑供应商' : '新增供应商'" width="660px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20"><el-col :span="12"><el-form-item label="供应商编码" prop="supplierCode"><el-input v-model="form.supplierCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="供应商名称" prop="supplierName"><el-input v-model="form.supplierName" /></el-form-item></el-col></el-row>
        <el-row :gutter="20"><el-col :span="8"><el-form-item label="省份"><el-select v-model="form.province" clearable style="width:100%"><el-option label="广东" value="广东" /><el-option label="浙江" value="浙江" /><el-option label="江苏" value="江苏" /><el-option label="山东" value="山东" /><el-option label="福建" value="福建" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="城市"><el-input v-model="form.city" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="评分"><el-rate v-model="form.rating" /></el-form-item></el-col></el-row>
        <el-row :gutter="20"><el-col :span="8"><el-form-item label="联系人"><el-input v-model="form.contactPerson" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="电话"><el-input v-model="form.contactPhone" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="邮箱"><el-input v-model="form.contactEmail" /></el-form-item></el-col></el-row>
        <el-row :gutter="20"><el-col :span="12"><el-form-item label="付款方式"><el-input v-model="form.paymentTerms" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="主营产品"><el-input v-model="form.mainProducts" /></el-form-item></el-col></el-row>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Download } from '@element-plus/icons-vue'
import { listSuppliers, createSupplier, updateSupplier, deleteSupplier, type SupplierVO, type SupplierQuery, type SupplierCreateRequest } from '@/api/customer'

const loading = ref(false); const suppliers = ref<SupplierVO[]>([])
const query = reactive<SupplierQuery>({ page: 1, size: 20 })
const pagination = reactive({ current: 1, size: 20, total: 0 })
const dialogVisible = ref(false); const isEdit = ref(false); const editId = ref<number | null>(null)
const submitting = ref(false); const formRef = ref()
const form = reactive<SupplierCreateRequest>({ supplierCode: '', supplierName: '', province: '', city: '', contactPerson: '', contactPhone: '', contactEmail: '', paymentTerms: '', mainProducts: '', rating: 0 })
const rules = { supplierCode: [{ required: true, message: '请输入供应商编码', trigger: 'blur' }], supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }] }

async function fetchSuppliers() { loading.value = true; try { query.page = pagination.current; query.size = pagination.size; const res = await listSuppliers(query); suppliers.value = res.records ?? []; pagination.total = res.total ?? 0 } catch { suppliers.value = [] } finally { loading.value = false } }
function handleSearch() { pagination.current = 1; fetchSuppliers() }
function openCreate() { isEdit.value = false; editId.value = null; Object.assign(form, { supplierCode: '', supplierName: '', province: '', city: '', contactPerson: '', contactPhone: '', contactEmail: '', paymentTerms: '', mainProducts: '', rating: 0 }); dialogVisible.value = true }
function openEdit(row: SupplierVO) { isEdit.value = true; editId.value = row.id; Object.assign(form, row); dialogVisible.value = true }
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; submitting.value = true
  try { if (isEdit.value && editId.value) { await updateSupplier(editId.value, form); ElMessage.success('更新成功') } else { await createSupplier(form); ElMessage.success('创建成功') }; dialogVisible.value = false; await fetchSuppliers() } finally { submitting.value = false }
}
async function handleDelete(id: number) { try { await deleteSupplier(id); ElMessage.success('删除成功'); await fetchSuppliers() } catch {} }
function handleExport() { ElMessage.info('导出将在后续版本中实现') }
onMounted(() => fetchSuppliers())
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