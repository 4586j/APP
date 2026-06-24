<template>
  <div class="page-container">
    <div class="page-header"><h2>新建销售订单</h2><p class="page-desc">填写订单基本信息、添加产品明细后提交</p></div>

    <el-steps :active="step" align-center style="margin-bottom:28px">
      <el-step title="基本信息" /><el-step title="产品明细" /><el-step title="确认提交" />
    </el-steps>

    <!-- Step 1: 基本信息 -->
    <div class="form-card" v-show="step === 0">
      <el-form :model="form" label-width="110px" size="default">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="客户" required>
              <el-select v-model="form.customerId" placeholder="选择客户" style="width:100%" filterable>
                <el-option v-for="c in customers" :key="c.id" :label="c.customerName + (c.customerNameEn ? ' / ' + c.customerNameEn : '')" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="客户订单号"><el-input v-model="form.customerOrderNo" placeholder="客户自有PO号" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="下单日期" required><el-date-picker v-model="form.orderDate" type="date" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="币种" required>
              <el-select v-model="form.currency" style="width:100%"><el-option label="USD - 美元" value="USD" /><el-option label="EUR - 欧元" value="EUR" /><el-option label="GBP - 英镑" value="GBP" /><el-option label="CNY - 人民币" value="CNY" /></el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="贸易条款" required>
              <el-select v-model="form.tradeTerms" style="width:100%"><el-option label="FOB" value="FOB" /><el-option label="CIF" value="CIF" /><el-option label="EXW" value="EXW" /><el-option label="DDP" value="DDP" /></el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="付款条款">
              <el-select v-model="form.paymentTerms" style="width:100%"><el-option label="T/T 30%预付, 70%见提单副本" value="TT_30_70" /><el-option label="L/C at sight" value="LC_SIGHT" /><el-option label="T/T 100%预付" value="TT_100" /><el-option label="D/P at sight" value="DP_SIGHT" /></el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8"><el-form-item label="起运港"><el-input v-model="form.portLoading" placeholder="如: Shanghai" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="目的港"><el-input v-model="form.portDestination" placeholder="如: Los Angeles" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="预计交期"><el-date-picker v-model="form.expectedDelivery" type="date" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="订单备注信息" /></el-form-item>
      </el-form>
      <div style="text-align:right;padding-top:8px"><el-button @click="$router.back()">取消</el-button><el-button type="primary" @click="step = 1">下一步</el-button></div>
    </div>

    <!-- Step 2: 产品明细 -->
    <div class="form-card" v-show="step === 1">
      <div style="margin-bottom:14px"><el-button type="primary" :icon="Plus" @click="addItem">添加产品</el-button></div>
      <el-table :data="form.items" border size="small">
        <el-table-column label="产品" min-width="220">
          <template #default="{ row, $index }">
            <el-select v-model="row.productId" placeholder="搜索产品名称/编码" filterable style="width:100%" size="small" @change="onProductChange($index, row.productId)">
              <el-option v-for="p in products" :key="p.id" :label="p.productCode + ' - ' + p.productName" :value="p.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="HS编码" width="120"><template #default="{ row }"><el-input v-model="row.hsCode" size="small" /></template></el-table-column>
        <el-table-column label="数量" width="110"><template #default="{ row }"><el-input-number v-model="row.quantity" :min="1" size="small" controls-position="right" style="width:100%" /></template></el-table-column>
        <el-table-column label="单价" width="120"><template #default="{ row }"><el-input-number v-model="row.unitPrice" :min="0" :precision="2" size="small" controls-position="right" style="width:100%" /></template></el-table-column>
        <el-table-column label="金额" width="130" align="right"><template #default="{ row }"><span class="money">{{ form.currency }} {{ (row.quantity * row.unitPrice).toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</span></template></el-table-column>
        <el-table-column label="规格" min-width="120"><template #default="{ row }"><el-input v-model="row.specification" size="small" placeholder="规格说明" /></template></el-table-column>
        <el-table-column label="操作" width="70" align="center"><template #default="{ $index }"><el-button type="danger" link size="small" @click="removeItem($index)">删除</el-button></template></el-table-column>
      </el-table>
      <div class="order-summary"><span>共 {{ form.items.length }} 项产品</span><span class="total">合计: {{ form.currency }} {{ totalAmount.toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</span></div>
      <div style="text-align:right;padding-top:8px"><el-button @click="step = 0">上一步</el-button><el-button type="primary" @click="step = 2">下一步</el-button></div>
    </div>

    <!-- Step 3: 确认 -->
    <div class="form-card" v-show="step === 2">
      <el-descriptions title="订单信息确认" :column="3" border size="small">
        <el-descriptions-item label="客户">{{ selectedCustomerName }}</el-descriptions-item>
        <el-descriptions-item label="币种">{{ form.currency }}</el-descriptions-item>
        <el-descriptions-item label="贸易条款">{{ form.tradeTerms }}</el-descriptions-item>
        <el-descriptions-item label="付款条款">{{ form.paymentTerms }}</el-descriptions-item>
        <el-descriptions-item label="起运港">{{ form.portLoading || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目的港">{{ form.portDestination || '-' }}</el-descriptions-item>
        <el-descriptions-item label="原币总额">{{ form.currency }} {{ totalAmount.toLocaleString() }}</el-descriptions-item>
      </el-descriptions>
      <div style="text-align:right;padding-top:20px">
        <el-button @click="step = 1">上一步</el-button>
        <el-button @click="$router.back()">取消</el-button>
        <el-button type="primary" @click="submitOrder" :loading="submitting">提交订单</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listCustomers, type CustomerVO } from '@/api/customer'
import { listProducts, type ProductVO } from '@/api/product'
import { createSalesOrder } from '@/api/order'

const router = useRouter()
const step = ref(0)
const submitting = ref(false)
const customers = ref<CustomerVO[]>([])
const products = ref<ProductVO[]>([])
const productsMap = ref<Record<number, ProductVO>>({})

const form = reactive({
  customerId: undefined as number | undefined,
  customerOrderNo: '',
  orderDate: new Date(),
  currency: 'USD',
  tradeTerms: 'FOB',
  paymentTerms: 'TT_30_70',
  portLoading: 'Shanghai',
  portDestination: 'Los Angeles',
  expectedDelivery: null as Date | null,
  remarks: '',
  exchangeRate: 7.25,
  items: [] as { productId: number; productCode?: string; productName?: string; hsCode?: string; specification?: string; quantity: number; unit?: string; unitPrice: number }[],
})

const totalAmount = computed(() => form.items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0))

const selectedCustomerName = computed(() => {
  if (!form.customerId) return '-'
  const c = customers.value.find(x => x.id === form.customerId)
  return c ? (c.customerName + (c.customerNameEn ? ' / ' + c.customerNameEn : '')) : String(form.customerId)
})

function onProductChange(index: number, productId: number) {
  const p = productsMap.value[productId]
  if (!p) return
  const item = form.items[index]
  item.productCode = p.productCode
  item.productName = p.productName
  item.hsCode = p.hsCode || ''
  item.specification = p.specification || ''
  item.unit = p.unit || '件'
  if (!item.unitPrice) item.unitPrice = p.salesPrice || 0
}

function addItem() {
  form.items.push({ productId: 0, quantity: 1, unitPrice: 0 })
}
function removeItem(index: number) {
  if (form.items.length > 1) form.items.splice(index, 1)
}

async function submitOrder() {
  if (!form.customerId) { ElMessage.warning('请选择客户'); step.value = 0; return }
  if (form.items.length === 0) { ElMessage.warning('请添加至少一个产品'); step.value = 1; return }

  submitting.value = true
  try {
    const result = await createSalesOrder({
      customerId: form.customerId,
      customerOrderNo: form.customerOrderNo || undefined,
      orderDate: form.orderDate.toISOString().split('T')[0],
      currency: form.currency,
      tradeTerms: form.tradeTerms,
      paymentTerms: form.paymentTerms,
      portLoading: form.portLoading || undefined,
      portDestination: form.portDestination || undefined,
      expectedDelivery: form.expectedDelivery?.toISOString().split('T')[0] || undefined,
      exchangeRate: form.exchangeRate || undefined,
      remarks: form.remarks || undefined,
      items: form.items.map(i => ({
        productId: i.productId,
        productCode: i.productCode,
        productName: i.productName,
        hsCode: i.hsCode,
        specification: i.specification,
        quantity: i.quantity,
        unit: i.unit || '件',
        unitPrice: i.unitPrice,
      })),
    })
    ElMessage.success('订单创建成功: ' + result.orderNo)
    router.push('/order/sales')
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try { customers.value = (await listCustomers({ page: 1, size: 200 })).records ?? [] } catch {}
  try { const res = await listProducts({ page: 1, size: 200 }); products.value = res.records ?? []; productsMap.value = Object.fromEntries(products.value.map(p => [p.id, p])) } catch {}
})
</script>

<style scoped lang="scss">
.form-card { background: #fff; border-radius: 8px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.order-summary { display: flex; justify-content: space-between; align-items: center; padding: 14px 0 0; font-size: 14px; color: #909399; }
.order-summary .total { font-size: 16px; font-weight: 600; color: #1a1a2e; }
.money { font-weight: 500; font-size: 13px; }
</style>
