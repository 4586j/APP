<template>
  <div class="page-container">
    <div class="page-header">
      <h2>新建销售订单</h2>
      <p class="page-desc">填写订单基本信息、添加产品明细后提交</p>
    </div>

    <el-steps :active="step" align-center style="margin-bottom:28px">
      <el-step title="基本信息" />
      <el-step title="产品明细" />
      <el-step title="确认提交" />
    </el-steps>

    <!-- Step 1: 基本信息 -->
    <div class="form-card" v-show="step === 0">
      <el-form :model="form" label-width="110px" size="default">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="客户" required>
              <el-select v-model="form.customerId" placeholder="选择客户" style="width:100%">
                <el-option label="ABC Trading Inc." :value="1" />
                <el-option label="Global Imports LLC" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="客户订单号">
              <el-input v-model="form.customerOrderNo" placeholder="客户自有PO号" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="下单日期" required>
              <el-date-picker v-model="form.orderDate" type="date" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="币种" required>
              <el-select v-model="form.currency" style="width:100%">
                <el-option label="USD - 美元" value="USD" />
                <el-option label="EUR - 欧元" value="EUR" />
                <el-option label="GBP - 英镑" value="GBP" />
                <el-option label="CNY - 人民币" value="CNY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="贸易条款" required>
              <el-select v-model="form.tradeTerms" style="width:100%">
                <el-option label="FOB" value="FOB" />
                <el-option label="CIF" value="CIF" />
                <el-option label="EXW" value="EXW" />
                <el-option label="DDP" value="DDP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="付款条款">
              <el-select v-model="form.paymentTerms" style="width:100%">
                <el-option label="T/T 30% 预付, 70% 见提单副本" value="TT_30_70" />
                <el-option label="L/C at sight" value="LC_SIGHT" />
                <el-option label="T/T 100% 预付" value="TT_100" />
                <el-option label="D/P at sight" value="DP_SIGHT" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="起运港">
              <el-input v-model="form.portLoading" placeholder="如: Shanghai" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目的港">
              <el-input v-model="form.portDestination" placeholder="如: Los Angeles" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计交期">
              <el-date-picker v-model="form.expectedDelivery" type="date" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remarks" type="textarea" :rows="2" placeholder="订单备注信息" />
        </el-form-item>
      </el-form>
      <div style="text-align:right;padding-top:8px">
        <el-button @click="$router.back()">取消</el-button>
        <el-button type="primary" @click="step = 1">下一步</el-button>
      </div>
    </div>

    <!-- Step 2: 产品明细 -->
    <div class="form-card" v-show="step === 1">
      <div style="margin-bottom:14px">
        <el-button type="primary" :icon="Plus" @click="addItem">添加产品</el-button>
      </div>
      <el-table :data="form.items" border size="small">
        <el-table-column label="产品" min-width="200">
          <template #default="{ row }">
            <el-select v-model="row.productId" placeholder="选择产品" filterable style="width:100%" size="small">
              <el-option label="LED Panel Light 600x600" :value="1" />
              <el-option label="Solar Inverter 5KW" :value="2" />
              <el-option label="Electric Motor 15HP" :value="3" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="HS编码" width="130">
          <template #default="{ row }">
            <el-input v-model="row.hsCode" size="small" placeholder="自动带出" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" size="small" controls-position="right" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.unitPrice" :min="0" :precision="2" size="small" controls-position="right" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }">
            <span class="money">{{ form.currency }} {{ (row.quantity * row.unitPrice).toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="规格" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.specification" size="small" placeholder="规格说明" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button type="danger" link size="small" @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="order-summary">
        <span>共 {{ form.items.length }} 项产品</span>
        <span class="total">合计: {{ form.currency }} {{ totalAmount.toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</span>
      </div>
      <div style="text-align:right;padding-top:8px">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" @click="step = 2">下一步</el-button>
      </div>
    </div>

    <!-- Step 3: 确认 -->
    <div class="form-card" v-show="step === 2">
      <el-descriptions title="订单信息确认" :column="3" border size="small">
        <el-descriptions-item label="客户">ABC Trading Inc.</el-descriptions-item>
        <el-descriptions-item label="币种">{{ form.currency }}</el-descriptions-item>
        <el-descriptions-item label="贸易条款">{{ form.tradeTerms }}</el-descriptions-item>
        <el-descriptions-item label="付款条款">{{ form.paymentTerms }}</el-descriptions-item>
        <el-descriptions-item label="起运港">{{ form.portLoading || '—' }}</el-descriptions-item>
        <el-descriptions-item label="目的港">{{ form.portDestination || '—' }}</el-descriptions-item>
        <el-descriptions-item label="汇率">7.2500</el-descriptions-item>
        <el-descriptions-item label="原币总额">{{ form.currency }} {{ totalAmount.toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="折合人民币">¥ {{ cnyAmount.toLocaleString() }}</el-descriptions-item>
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
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const step = ref(0)
const submitting = ref(false)

const form = reactive({
  customerId: 1,
  customerOrderNo: '',
  orderDate: new Date(),
  currency: 'USD',
  tradeTerms: 'FOB',
  paymentTerms: 'TT_30_70',
  portLoading: 'Shanghai',
  portDestination: 'Los Angeles',
  expectedDelivery: null,
  remarks: '',
  items: [
    { productId: 1, hsCode: '9405.40.9000', quantity: 1000, unitPrice: 28.5, specification: '600x600mm, 40W' },
    { productId: 2, hsCode: '8504.40.3090', quantity: 50, unitPrice: 400.0, specification: '5KW, Single Phase' },
  ],
})

const totalAmount = computed(() =>
  form.items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0)
)
const cnyAmount = computed(() => Math.round(totalAmount.value * 7.25))

function addItem() {
  form.items.push({ productId: 1, hsCode: '', quantity: 1, unitPrice: 0, specification: '' })
}
function removeItem(index: number) {
  if (form.items.length > 1) form.items.splice(index, 1)
}

function submitOrder() {
  submitting.value = true
  setTimeout(() => {
    submitting.value = false
    ElMessage.success('订单创建成功')
    router.push('/order/sales')
  }, 800)
}
</script>

<style scoped lang="scss">
.form-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.order-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0 0;
  font-size: 14px;
  color: #909399;
  .total {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a2e;
  }
}
.money {
  font-weight: 500;
  font-size: 13px;
}
</style>
