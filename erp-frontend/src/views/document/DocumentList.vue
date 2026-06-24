<template>
  <div class="page-container">
    <div class="page-header">
      <h2>单证管理</h2>
      <p class="page-desc">管理发票、装箱单、提单等外贸单证的生成与归档</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="单证编号">
          <el-input v-model="query.docNo" placeholder="编号" clearable style="width:170px" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width:140px">
            <el-option label="商业发票" value="invoice" />
            <el-option label="装箱单" value="packing_list" />
            <el-option label="提单" value="bl" />
            <el-option label="原产地证" value="co" />
            <el-option label="合同" value="contract" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Plus">生成单证</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>单证列表</h3>
      </div>
      <el-table :data="documents" stripe size="small">
        <el-table-column prop="docNo" label="单证编号" width="150" />
        <el-table-column prop="type" label="类型" width="100" align="center" />
        <el-table-column prop="orderNo" label="关联订单" width="150" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="version" label="版本" width="70" align="center" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'final' ? 'success' : 'info'" size="small">{{ row.status === 'final' ? '终版' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="110" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default>
            <el-button type="primary" link size="small">下载</el-button>
            <el-button type="primary" link size="small">预览</el-button>
            <el-button type="primary" link size="small">版本</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination :current-page="1" :page-size="20" :total="68" layout="total, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'

const query = reactive({ docNo: '', type: '' })

const documents = [
  { docNo: 'DOC20260615001', type: '商业发票', orderNo: 'SO20260615001', title: 'Invoice - ABC Trading Inc. - LED Panel Light', version: 2, status: 'final', createdAt: '2026-06-15' },
  { docNo: 'DOC20260615002', type: '装箱单', orderNo: 'SO20260615001', title: 'Packing List - ABC Trading Inc. - 1000pcs', version: 1, status: 'final', createdAt: '2026-06-15' },
  { docNo: 'DOC20260614003', type: '提单', orderNo: 'SO20260612002', title: 'B/L - Euro Trade GmbH - COSU567890123', version: 1, status: 'final', createdAt: '2026-06-14' },
  { docNo: 'DOC20260610004', type: '原产地证', orderNo: 'SO20260608001', title: 'C/O - Delta International - Certificate of Origin', version: 1, status: 'draft', createdAt: '2026-06-10' },
  { docNo: 'DOC20260605005', type: '合同', orderNo: 'SO20260611005', title: 'Sales Contract - Asia Partners Ltd.', version: 3, status: 'final', createdAt: '2026-06-05' },
]
</script>
