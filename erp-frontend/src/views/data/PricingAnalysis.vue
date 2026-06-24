<template>
  <div class="page-container">
    <div class="page-header">
      <h2>定价分析</h2>
      <p class="page-desc">基于市场数据的产品定价分析与建议</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="产品">
          <el-select v-model="query.productId" placeholder="选择产品" filterable clearable style="width:200px">
            <el-option label="LED Panel Light 600x600" :value="1" />
            <el-option label="Solar Inverter 5KW" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Plus">新建分析</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>分析报告列表</h3>
      </div>
      <el-table :data="analyses" stripe size="small">
        <el-table-column prop="title" label="分析标题" min-width="220" />
        <el-table-column prop="productName" label="产品" min-width="170" />
        <el-table-column label="市场价" width="120" align="right">
          <template #default="{ row }">$ {{ row.marketPrice.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="成本价" width="120" align="right">
          <template #default="{ row }">$ {{ row.costPrice.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="建议售价" width="120" align="right">
          <template #default="{ row }">$ {{ row.suggestedPrice.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="建议利润率" width="100" align="center">
          <template #default="{ row }">{{ ((row.suggestedPrice - row.costPrice) / row.suggestedPrice * 100).toFixed(1) }}%</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'info'" size="small">{{ row.status === 'published' ? '已发布' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="analysisDate" label="日期" width="110" />
        <el-table-column label="操作" width="120" align="center">
          <template #default>
            <el-button type="primary" link size="small">查看</el-button>
            <el-button type="primary" link size="small">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination :current-page="1" :page-size="20" :total="15" layout="total, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'

const query = reactive({ productId: null, status: '' })

const analyses = [
  { title: 'LED Panel Light 北美市场定价分析', productName: 'LED Panel Light 600x600', marketPrice: 16.80, costPrice: 11.72, suggestedPrice: 14.50, status: 'published', analysisDate: '2026-06-14' },
  { title: 'Solar Inverter 欧洲市场竞争力分析', productName: 'Solar Inverter 5KW', marketPrice: 450.00, costPrice: 331.03, suggestedPrice: 400.00, status: 'published', analysisDate: '2026-06-10' },
  { title: 'Electric Motor 东南亚定价策略', productName: 'Electric Motor 15HP', marketPrice: 580.00, costPrice: 441.38, suggestedPrice: 520.00, status: 'draft', analysisDate: '2026-06-08' },
]
</script>
