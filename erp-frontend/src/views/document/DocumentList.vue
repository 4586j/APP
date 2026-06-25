<template>
  <div class="page-container">
    <div class="page-header">
      <h2>单证管理</h2>
      <p class="page-desc">管理发票、装箱单、提单等外贸单证的生成与归档</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="单证编号"><el-input v-model="query.docNo" clearable style="width:160px" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.docType" placeholder="全部" clearable style="width:140px">
            <el-option label="商业发票" value="invoice" /><el-option label="装箱单" value="packing_list" />
            <el-option label="提单" value="bl" /><el-option label="原产地证" value="co" /><el-option label="合同" value="contract" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:100px">
            <el-option label="草稿" value="draft" /><el-option label="终版" value="final" /><el-option label="归档" value="archived" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Plus" @click="showCreate=true">生成单证</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <el-table :data="list" stripe size="small" v-loading="loading">
        <el-table-column prop="docNo" label="单证编号" width="150" />
        <el-table-column label="类型" width="100" align="center">
          <template #default="scope">{{ typeLabel(scope.row.docType) }}</template>
        </el-table-column>
        <el-table-column prop="orderNo" label="关联订单" width="150" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="scope"><el-tag :type="scope.row.status==='final'?'success':'info'" size="small">{{ scope.row.status==='final'?'终版':'草稿' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="110" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="scope">
            <el-button v-if="scope.row.status==='draft'" type="primary" link size="small" @click="handleFinalize(scope.row as any)">定稿</el-button>
            <el-button type="primary" link size="small">下载</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(scope.row as any)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination v-model:current-page="page.current" :page-size="page.size" :total="page.total"
          layout="total, prev, pager, next" @current-change="loadData" />
      </div>
    </div>

    <!-- 生成单证对话框 -->
    <el-dialog v-model="showCreate" title="生成单证" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="类型"><el-select v-model="form.docType" style="width:100%"><el-option label="商业发票" value="invoice" /><el-option label="装箱单" value="packing_list" /><el-option label="提单" value="bl" /><el-option label="原产地证" value="co" /><el-option label="合同" value="contract" /></el-select></el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="关联订单"><el-input v-model="form.orderNo" placeholder="可选" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showCreate=false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleCreate">生成</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDocuments, createDocument, finalizeDocument, deleteDocument, type DocumentVO } from '@/api/document'

const loading = ref(false); const submitting = ref(false); const showCreate = ref(false)
const list = ref<DocumentVO[]>([]); const page = reactive({ current: 1, size: 20, total: 0 })
const query = reactive({ docNo: '', docType: '', status: '' })
const form = reactive({ docType: 'invoice', title: '', orderNo: '', remark: '' })

function typeLabel(s: string) { const m: Record<string,string>={invoice:'商业发票',packing_list:'装箱单',bl:'提单',co:'原产地证',contract:'合同'}; return m[s]||s }

async function loadData() {
  loading.value = true; try {
    const res = await listDocuments({ ...query, page: page.current, size: page.size })
    list.value = res.records; page.total = res.total
  } finally { loading.value = false }
}
function handleSearch() { page.current = 1; loadData() }
async function handleCreate() {
  if (!form.title) { ElMessage.warning('请输入标题'); return }
  submitting.value = true; try {
    await createDocument({ ...form, orderId: undefined, shipmentId: undefined })
    ElMessage.success('生成成功'); showCreate.value = false
    Object.assign(form, { docType:'invoice', title:'', orderNo:'', remark:'' })
    loadData()
  } finally { submitting.value = false }
}
function handleFinalize(row: DocumentVO) {
  ElMessageBox.confirm('确认定稿？定稿后不可再编辑。', '提示', { type: 'info' }).then(async () => {
    await finalizeDocument(row.id); ElMessage.success('已定稿'); loadData()
  }).catch(() => {})
}
function handleDelete(row: DocumentVO) {
  ElMessageBox.confirm('确认删除单证？', '提示', { type: 'warning' }).then(async () => {
    await deleteDocument(row.id); ElMessage.success('已删除'); loadData()
  }).catch(() => {})
}
onMounted(() => loadData())
</script>
