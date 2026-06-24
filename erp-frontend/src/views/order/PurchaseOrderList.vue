<template>
  <div class="page-container">
    <div class="page-header"><h2>采购订单</h2><p class="page-desc">管理供应商采购订单，跟踪采购进度与收货</p></div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="采购单号"><el-input v-model="query.keyword" placeholder="输入单号" clearable style="width:180px" @clear="handleSearch" /></el-form-item>
        <el-form-item label="供应商"><el-select v-model="query.supplierId" placeholder="选择供应商" clearable style="width:180px" @change="handleSearch"><el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.status" placeholder="全部状态" clearable style="width:130px" @change="handleSearch"><el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button></el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <el-table :data="orders" stripe size="small" v-loading="loading">
        <el-table-column prop="orderNo" label="采购单号" width="180" />
        <el-table-column prop="supplierName" label="供应商" min-width="180" />
        <el-table-column prop="orderDate" label="下单日期" width="100" />
        <el-table-column label="金额" width="130" align="right"><template #default="{ row }">{{ (row as any).currency || 'CNY' }} {{ ((row as any).totalAmount)?.toLocaleString?.() ?? '-' }}</template></el-table-column>
        <el-table-column prop="expectedDelivery" label="预计到货" width="100" />
        <el-table-column label="状态" width="110" align="center"><template #default="{ row }"><el-tag :type="statusType((row as any).status) as any" size="small">{{ statusLabel((row as any).status) }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetail(row as any)">详情</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete((row as any).id)"><template #reference><el-button type="danger" link size="small">删除</el-button></template></el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size" :total="pagination.total" layout="total, prev, pager, next" @current-change="fetchOrders" />
      </div>
    </div>

    <el-dialog v-model="detailVisible" :title="'采购单 ' + detail?.orderNo" width="750px">
      <el-descriptions :column="3" border size="small" v-if="detail">
        <el-descriptions-item label="供应商">{{ detail.supplierName || detail.supplierId }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(detail.status) as any" size="small">{{ statusLabel(detail.status) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="下单日期">{{ detail.orderDate }}</el-descriptions-item>
        <el-descriptions-item label="币种">{{ detail.currency || 'CNY' }}</el-descriptions-item>
        <el-descriptions-item label="总金额">{{ (detail.currency || 'CNY') + " " + (detail.totalAmount?.toLocaleString() ?? "-") }}</el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ detail.paymentTerms || "-" }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.items || []" border size="small" style="margin-top:12px">
        <el-table-column label="#" width="50" type="index" />
        <el-table-column prop="productName" label="产品" min-width="150" />
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column prop="unitPrice" label="单价" width="100" align="right"><template #default="{ row }">{{ (row as any).unitPrice?.toLocaleString?.() }}</template></el-table-column>
        <el-table-column prop="totalPrice" label="金额" width="120" align="right"><template #default="{ row }">{{ (row as any).totalPrice?.toLocaleString?.() }}</template></el-table-column>
      </el-table>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { listSuppliers, type SupplierVO } from "@/api/customer"
import { listPurchaseOrders, getPurchaseOrder, deletePurchaseOrder, type PurchaseOrderVO, type PurchaseOrderQuery } from "@/api/order"

const loading = ref(false); const orders = ref<PurchaseOrderVO[]>([])
const query = reactive<PurchaseOrderQuery>({ page: 1, size: 20 })
const pagination = reactive({ current: 1, size: 20, total: 0 })
const suppliers = ref<SupplierVO[]>([])
const detailVisible = ref(false); const detail = ref<PurchaseOrderVO | null>(null)
const statusOptions = [
  { value: "draft", label: "草稿" }, { value: "confirmed", label: "已确认" },
  { value: "shipping", label: "发货中" }, { value: "received", label: "已入库" }, { value: "cancelled", label: "已取消" },
]
const statusMap: Record<string, string> = { draft:"草稿", confirmed:"已确认", shipping:"发货中", received:"已入库", cancelled:"已取消" }
const statusTypeMap: Record<string, string> = { draft:"info", confirmed:"success", shipping:"primary", received:"success", cancelled:"danger" }
function statusLabel(s: string) { return statusMap[s] || s }
function statusType(s: string) { return statusTypeMap[s] || "info" }

async function fetchOrders() {
  loading.value = true; try {
    query.page = pagination.current; query.size = pagination.size
    const res = await listPurchaseOrders(query); orders.value = res.records ?? []; pagination.total = res.total ?? 0
  } catch { orders.value = [] } finally { loading.value = false }
}
function handleSearch() { pagination.current = 1; fetchOrders() }
async function fetchSuppliers() { try { const res = await listSuppliers({}); suppliers.value = res.records ?? [] } catch {} }
async function openDetail(row: PurchaseOrderVO) { try { detail.value = await getPurchaseOrder(row.id); detailVisible.value = true } catch { ElMessage.error("加载失败") } }
async function handleDelete(id: number) { try { await deletePurchaseOrder(id); ElMessage.success("已删除"); await fetchOrders() } catch {} }
onMounted(() => { fetchSuppliers(); fetchOrders() })
</script>
<style scoped>
.page-container { padding: 24px; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #1a1a2e; }
.page-desc { margin: 4px 0 0; color: #889; font-size: 13px; }
.search-bar { background: #f8f9fc; padding: 16px; border-radius: 8px; margin-bottom: 16px; }
.table-container { background: #fff; border-radius: 8px; padding: 16px; }
</style>