<template>
  <div class="page-container">
    <div class="page-header">
      <h2>客户列表</h2>
      <p class="page-desc">管理海外客户信息、联系人及信用额度</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="客户名称"><el-input v-model="query.keyword" placeholder="中/英文名称" clearable style="width:200px" @clear="handleSearch" /></el-form-item>
        <el-form-item label="国家"><el-select v-model="query.country" placeholder="全部国家" clearable style="width:150px" @change="handleSearch"><el-option label="美国" value="美国" /><el-option label="德国" value="德国" /><el-option label="英国" value="英国" /><el-option label="日本" value="日本" /><el-option label="新加坡" value="新加坡" /></el-select></el-form-item>
        <el-form-item label="类型"><el-select v-model="query.customerType" placeholder="全部类型" clearable style="width:140px" @change="handleSearch"><el-option label="Buyer" value="buyer" /><el-option label="Agent" value="agent" /><el-option label="Distributor" value="distributor" /></el-select></el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Plus" @click="openCreate">新增客户</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header"><h3>客户列表</h3><el-button :icon="Download" @click="handleExport">导出</el-button></div>
      <el-table :data="customers" stripe size="small" v-loading="loading">
        <el-table-column prop="customerCode" label="客户编码" width="120" />
        <el-table-column prop="customerName" label="客户名称" min-width="150" />
        <el-table-column prop="customerNameEn" label="英文名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="country" label="国家" width="80" />
        <el-table-column prop="contactPerson" label="联系人" width="90" />
        <el-table-column prop="contactEmail" label="邮箱" min-width="160" />
        <el-table-column label="信用额度" width="130" align="right"><template #default="{ row }">$ {{ (row as any).creditLimit?.toLocaleString?.() ?? '-' }}</template></el-table-column>
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
        <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size" :total="pagination.total" layout="total, prev, pager, next" @current-change="fetchCustomers" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑客户' : '新增客户'" width="660px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20"><el-col :span="12"><el-form-item label="客户编码" prop="customerCode"><el-input v-model="form.customerCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="客户名称" prop="customerName"><el-input v-model="form.customerName" /></el-form-item></el-col></el-row>
        <el-row :gutter="20"><el-col :span="12"><el-form-item label="英文名称"><el-input v-model="form.customerNameEn" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="客户类型"><el-select v-model="form.customerType" clearable style="width:100%"><el-option label="Buyer" value="buyer" /><el-option label="Agent" value="agent" /><el-option label="Distributor" value="distributor" /></el-select></el-form-item></el-col></el-row>
        <el-row :gutter="20"><el-col :span="8"><el-form-item label="国家"><el-input v-model="form.country" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="联系人"><el-input v-model="form.contactPerson" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="邮箱"><el-input v-model="form.contactEmail" /></el-form-item></el-col></el-row>
        <el-row :gutter="20"><el-col :span="8"><el-form-item label="电话"><el-input v-model="form.contactPhone" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="付款方式"><el-input v-model="form.paymentTerms" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="信用额度($)"><el-input-number v-model="form.creditLimit" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col></el-row>
        <el-row :gutter="20"><el-col :span="12"><el-form-item label="税号/VAT"><el-input v-model="form.taxId" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="SWIFT Code"><el-input v-model="form.swiftCode" /></el-form-item></el-col></el-row>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Download } from '@element-plus/icons-vue'
import { listCustomers, createCustomer, updateCustomer, deleteCustomer, type CustomerVO, type CustomerQuery, type CustomerCreateRequest } from '@/api/customer'

const loading = ref(false); const customers = ref<CustomerVO[]>([])
const query = reactive<CustomerQuery>({ page: 1, size: 20 })
const pagination = reactive({ current: 1, size: 20, total: 0 })
const dialogVisible = ref(false); const isEdit = ref(false); const editId = ref<number | null>(null)
const submitting = ref(false); const formRef = ref()
const form = reactive<CustomerCreateRequest>({ customerCode: '', customerName: '', customerNameEn: '', customerType: '', country: '', contactPerson: '', contactEmail: '', contactPhone: '', paymentTerms: '', creditLimit: 0, taxId: '', swiftCode: '' })
const rules = { customerCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }], customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }] }

async function fetchCustomers() { loading.value = true; try { query.page = pagination.current; query.size = pagination.size; const res = await listCustomers(query); customers.value = res.records ?? []; pagination.total = res.total ?? 0 } catch { customers.value = [] } finally { loading.value = false } }
function handleSearch() { pagination.current = 1; fetchCustomers() }

function openCreate() { isEdit.value = false; editId.value = null; Object.assign(form, { customerCode: '', customerName: '', customerNameEn: '', customerType: '', country: '', contactPerson: '', contactEmail: '', contactPhone: '', paymentTerms: '', creditLimit: 0, taxId: '', swiftCode: '' }); dialogVisible.value = true }
function openEdit(row: CustomerVO) { isEdit.value = true; editId.value = row.id; Object.assign(form, row); dialogVisible.value = true }

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; submitting.value = true
  try { if (isEdit.value && editId.value) { await updateCustomer(editId.value, form); ElMessage.success('更新成功') } else { await createCustomer(form); ElMessage.success('创建成功') }; dialogVisible.value = false; await fetchCustomers() } finally { submitting.value = false }
}
async function handleDelete(id: number) { try { await deleteCustomer(id); ElMessage.success('删除成功'); await fetchCustomers() } catch {} }
function handleExport() { ElMessage.info('导出将在后续版本中实现') }
onMounted(() => fetchCustomers())
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