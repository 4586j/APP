<template>
  <div class="page-container">
    <div class="page-header">
      <h2>汇率管理</h2>
      <p class="page-desc">维护每日汇率，用于订单金额折算与利润核算</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="日期">
          <el-date-picker v-model="query.date" type="date" style="width:160px" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="币种">
          <el-select v-model="query.currency" placeholder="全部" clearable style="width:120px">
            <el-option label="USD" value="USD" />
            <el-option label="EUR" value="EUR" />
            <el-option label="GBP" value="GBP" />
            <el-option label="JPY" value="JPY" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Plus" @click="showCreate = true">录入汇率</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>汇率记录</h3>
      </div>
      <el-table :data="rateList" stripe size="small" v-loading="loading">
        <el-table-column prop="rateDate" label="日期" width="120" sortable />
        <el-table-column prop="currencyFrom" label="币种" width="80" align="center" />
        <el-table-column prop="currencyTo" label="折算币种" width="100" align="center" />
        <el-table-column label="汇率" width="150" align="right">
          <template #default="scope">{{ Number(scope.row.rate).toFixed(6) }}</template>
        </el-table-column>
        <el-table-column prop="source" label="数据来源" width="140" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="scope">
            <el-button type="danger" link size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination
          v-model:current-page="page.current"
          :page-size="page.size"
          :total="page.total"
          layout="total, prev, pager, next"
          @current-change="loadRates"
        />
      </div>
    </div>
  </div>

  <!-- 录入汇率对话框 -->
  <el-dialog v-model="showCreate" title="录入汇率" width="480px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="日期">
        <el-date-picker v-model="form.rateDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="来源币种">
        <el-select v-model="form.currencyFrom" style="width:100%">
          <el-option label="USD" value="USD" />
          <el-option label="EUR" value="EUR" />
          <el-option label="GBP" value="GBP" />
          <el-option label="JPY" value="JPY" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标币种">
        <el-select v-model="form.currencyTo" style="width:100%">
          <el-option label="CNY" value="CNY" />
        </el-select>
      </el-form-item>
      <el-form-item label="汇率">
        <el-input-number v-model="form.rate" :precision="6" :step="0.01" :min="0" style="width:100%" />
      </el-form-item>
      <el-form-item label="来源">
        <el-input v-model="form.source" placeholder="如：中国银行" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showCreate = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleCreate">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listExchangeRates,
  createExchangeRate,
  deleteExchangeRate,
  type ExchangeRateVO,
} from '@/api/finance'

const loading = ref(false)
const submitting = ref(false)
const showCreate = ref(false)
const rateList = ref<ExchangeRateVO[]>([])
const page = reactive({ current: 1, size: 20, total: 0 })
const query = reactive({ date: '', currency: '' })
const form = reactive({ rateDate: '', currencyFrom: 'USD', currencyTo: 'CNY', rate: 7.25, source: '' })

async function loadRates() {
  loading.value = true
  try {
    const res = await listExchangeRates({
      date: query.date || undefined,
      currency: query.currency || undefined,
      page: page.current,
      size: page.size,
    })
    rateList.value = res.records
    page.total = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.current = 1
  loadRates()
}

async function handleCreate() {
  if (!form.rateDate) { ElMessage.warning('请选择日期'); return }
  submitting.value = true
  try {
    await createExchangeRate({
      currencyFrom: form.currencyFrom,
      currencyTo: form.currencyTo,
      rate: form.rate,
      rateDate: form.rateDate,
      source: form.source || undefined,
    })
    ElMessage.success('汇率录入成功')
    showCreate.value = false
    Object.assign(form, { rateDate: '', currencyFrom: 'USD', currencyTo: 'CNY', rate: 7.25, source: '' })
    loadRates()
  } finally {
    submitting.value = false
  }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`确认删除 ${row.currencyFrom}→${row.currencyTo} 汇率？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteExchangeRate(row.id)
      ElMessage.success('已删除')
      loadRates()
    })
    .catch(() => {})
}

onMounted(() => loadRates())
</script>
