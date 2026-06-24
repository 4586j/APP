<template>
  <div class="page-container">
    <div class="page-header"><h2>销售订单</h2><p class="page-desc">管理所有销售订单，跟踪订单状态与进度</p></div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="订单编号"><el-input v-model="query.keyword" placeholder="输入订单编号" clearable style="width:180px" @clear="handleSearch" /></el-form-item>
        <el-form-item label="客户"><el-select v-model="query.customerId" placeholder="选择客户" clearable style="width:180px" @change="handleSearch"><el-option v-for="c in customers" :key="c.id" :label="c.customerName" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.status" placeholder="全部状态" clearable style="width:130px" @change="handleSearch"><el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button><el-button type="success" :icon="Plus" @click="$router.push('/order/sales/create')">新建订单</el-button></el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <el-table :data="orders" stripe size="small" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="customerName" label="客户" min-width="150" />
        <el-table-column prop="orderDate" label="下单日期" width="100" />
        <el-table-column label="金额" width="140" align="right"><template #default="{ row }">{{ row.currency }} {{ (row as any).totalAmount?.toLocaleString?.() ?? '-' }}</template></el-table-column>
        <el-table-column prop="tradeTerms" label="贸易条款" width="80" align="center" />
        <el-table-column prop="portDestination" label="目的港" width="100" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }"><el-tag :type="statusType((row as any).status) as any as any" size="small">{{ statusLabel((row as any).status) }}</el-tag></template>
        </el-table-column>
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

    <el-dialog v-model="detailVisible" :title="'订单 ' + detail?.orderNo" width="800px">
      <el-descriptions :column="3" border size="small" v-if="detail">
        <el-descriptions-item label="客户">{{ detail.customerName || detail.customerId }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(detail.status) as any" size="small">{{ statusLabel(detail.status) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="下单日期">{{ detail.orderDate }}</el-descriptions-item>
        <el-descriptions-item label="币种">{{ detail.currency }}</el-descriptions-item>
        <el-descriptions-item label="贸易条款">{{ detail.tradeTerms || '-' }}</el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ detail.paymentTerms || '-' }}</el-descriptions-item>
        <el-descriptions-item label="起运港">{{ detail.portLoading || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目的港">{{ detail.portDestination || '-' }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.currency }} {{ detail.totalAmount?.toLocaleString() }} (¥{{ detail.totalCnyAmount?.toLocaleString() }})</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.items || []" border size="small" style="margin-top:12px">
        <el-table-column label="#" width="50" type="index" />
        <el-table-column prop="productName" label="产品" min-width="150" />
        <el-table-column prop="hsCode" label="HS编码" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column prop="unitPrice" label="单价" width="100" align="right"><template #default="{ row }">{{ detail?.currency }} {{ (row as any).unitPrice?.toLocaleString?.() }}</template></el-table-column>
        <el-table-column prop="totalPrice" label="金额" width="120" align="right"><template #default="{ row }">{{ detail?.currency }} {{ (row as any).totalPrice?.toLocaleString?.() }}</template></el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detail?.status === 'draft'" type="primary" @click="handleSubmitOrder(detail!.id)">提交审批</el-button>
        <el-button v-if="detail?.status === 'submitted'" type="success" @click="handleApproveOrder(detail!.id)">审批通过</el-button>
        <el-button v-if="detail?.status === 'draft'" type="danger" plain @click="handleCancelOrder(detail!.id)">取消订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { listSalesOrders, getSalesOrder, deleteSalesOrder, updateSalesOrderStatus, type SalesOrderVO, type SalesOrderQuery } from '@/api/order'
import { listCustomers, type CustomerVO } from '@/api/customer'

const loading = ref(false); const orders = ref<SalesOrderVO[]>([])
const query = reactive<SalesOrderQuery>({ page: 1, size: 20 })
const pagination = reactive({ current: 1, size: 20, total: 0 })
const customers = ref<CustomerVO[]>([])
const detailVisible = ref(false)
const detail = ref<SalesOrderVO | null>(null)

const statusOptions = [
  { value: 'draft', label: '草稿' }, { value: 'submitted', label: '待审批' },
  { value: 'approved', label: '已审批' }, { value: 'purchasing', label: '采购中' },
  { value: 'shipping', label: '发货中' }, { value: 'delivered', label: '已交付' },
  { value: 'settled', label: '已结算' }, { value: 'cancelled', label: '已取消' },
]
const statusMap: Record<string, string> = { draft:'草稿', submitted:'待审批', approved:'已审批', purchasing:'采购中', shipping:'发货中', delivered:'已交付', settled:'已结算', cancelled:'已取消' }
const statusTypeMap: Record<string, string> = { draft:'info', submitted:'warning', approved:'success', purchasing:'', shipping:'primary', delivered:'success', settled:'success', cancelled:'danger' }
function statusLabel(s: string) { return statusMap[s] || s }
function statusType(s: string) { return statusTypeMap[s] || '' }

async function fetchOrders() { loading.value = true; try { query.page = pagination.current; query.size = pagination.size; const res = await listSalesOrders(query); orders.value = res.records ?? []; pagination.total = res.total ?? 0 } catch { orders.value = [] } finally { loading.value = false } }
function handleSearch() { pagination.current = 1; fetchOrders() }

async function fetchCustomers() { try { customers.value = (await listCustomers({})).records ?? [] } catch {} }

async function openDetail(row: SalesOrderVO) {
  try { detail.value = await getSalesOrder(row.id); detailVisible.value = true } catch { ElMessage.error('加载失败') }
}
async function handleDelete(id: number) { try { await deleteSalesOrder(id); ElMessage.success('已删除'); await fetchOrders() } catch {} }
async function handleSubmitOrder(id: number) { try { await updateSalesOrderStatus(id, 'submitted', '提交审批'); ElMessage.success('已提交'); detailVisible.value = false; await fetchOrders() } catch {} }
async function handleApproveOrder(id: number) { try { await updateSalesOrderStatus(id, 'approved', '审批通过'); ElMessage.success('已审批'); detailVisible.value = false; await fetchOrders() } catch {} }
async function handleCancelOrder(id: number) { try { await updateSalesOrderStatus(id, 'cancelled', '取消订单'); ElMessage.success('已取消'); detailVisible.value = false; await fetchOrders() } catch {} }

onMounted(() => { fetchCustomers(); fetchOrders() })
</script>
<style scoped>
.page-container { padding: 24px; }
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #1a1a2e; }
.page-desc { margin: 4px 0 0; color: #889; font-size: 13px; }
.search-bar { background: #f8f9fc; padding: 16px; border-radius: 8px; margin-bottom: 16px; }
.table-container { background: #fff; border-radius: 8px; padding: 16px; }
</style>