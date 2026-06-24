<template>
  <div class="page-container">
    <div class="page-header">
      <h2>汇率管理</h2>
      <p class="page-desc">维护每日汇率，用于订单金额折算与利润核算</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="日期">
          <el-date-picker v-model="query.date" type="date" style="width:160px" />
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
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Plus">录入汇率</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>汇率记录</h3>
      </div>
      <el-table :data="rates" stripe size="small">
        <el-table-column prop="date" label="日期" width="120" sortable />
        <el-table-column prop="currencyFrom" label="币种" width="80" align="center" />
        <el-table-column prop="currencyTo" label="折算币种" width="100" align="center" />
        <el-table-column prop="rate" label="汇率" width="130" align="right">
          <template #default="{ row }">{{ row.rate.toFixed(6) }}</template>
        </el-table-column>
        <el-table-column prop="source" label="数据来源" width="120" />
        <el-table-column label="操作" width="100" align="center">
          <template #default>
            <el-button type="primary" link size="small">编辑</el-button>
            <el-button type="danger" link size="small">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination :current-page="1" :page-size="20" :total="180" layout="total, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'

const query = reactive({ date: new Date(), currency: '' })

const rates = [
  { date: '2026-06-15', currencyFrom: 'USD', currencyTo: 'CNY', rate: 7.250000, source: '中国银行' },
  { date: '2026-06-15', currencyFrom: 'EUR', currencyTo: 'CNY', rate: 7.800000, source: '中国银行' },
  { date: '2026-06-15', currencyFrom: 'GBP', currencyTo: 'CNY', rate: 9.100000, source: '中国银行' },
  { date: '2026-06-15', currencyFrom: 'JPY', currencyTo: 'CNY', rate: 0.048500, source: '中国银行' },
  { date: '2026-06-14', currencyFrom: 'USD', currencyTo: 'CNY', rate: 7.248500, source: '中国银行' },
  { date: '2026-06-14', currencyFrom: 'EUR', currencyTo: 'CNY', rate: 7.795000, source: '中国银行' },
]
</script>
