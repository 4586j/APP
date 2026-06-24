<template>
  <div class="page-container">
    <div class="page-header">
      <h2>数据上传</h2>
      <p class="page-desc">上传市场数据、分析代码或共享文件，可选择共享部门</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="文件名">
          <el-input v-model="query.keyword" placeholder="搜索" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width:140px">
            <el-option label="市场数据" value="market_data" />
            <el-option label="分析代码" value="analysis_code" />
            <el-option label="共享文档" value="shared_doc" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Upload">上传文件</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>上传记录</h3>
      </div>
      <el-table :data="records" stripe size="small">
        <el-table-column prop="fileName" label="文件名" min-width="220" />
        <el-table-column prop="type" label="类型" width="100" align="center" />
        <el-table-column prop="fileSize" label="大小" width="90" align="right">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="sharedWith" label="共享部门" width="160" />
        <el-table-column prop="uploadBy" label="上传人" width="90" />
        <el-table-column prop="uploadAt" label="上传时间" width="110" />
        <el-table-column label="操作" width="130" align="center">
          <template #default>
            <el-button type="primary" link size="small">下载</el-button>
            <el-button type="primary" link size="small">预览</el-button>
            <el-button type="danger" link size="small">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination :current-page="1" :page-size="20" :total="24" layout="total, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Search, Upload } from '@element-plus/icons-vue'

const query = reactive({ keyword: '', type: '' })

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const records = [
  { fileName: '2026年6月LED市场价格行情.xlsx', type: '市场数据', fileSize: 245760, sharedWith: '销售部、采购部', uploadBy: '陈数据', uploadAt: '2026-06-15' },
  { fileName: 'price_analysis_model_v2.py', type: '分析代码', fileSize: 15360, sharedWith: '数据部', uploadBy: '陈数据', uploadAt: '2026-06-14' },
  { fileName: 'Q2竞品分析报告.pdf', type: '共享文档', fileSize: 3145728, sharedWith: '管理部、销售部', uploadBy: '陈数据', uploadAt: '2026-06-12' },
  { fileName: '欧盟太阳能产品政策更新.docx', type: '共享文档', fileSize: 102400, sharedWith: '全部部门', uploadBy: '陈数据', uploadAt: '2026-06-10' },
  { fileName: '6月美元汇率走势预测.csv', type: '市场数据', fileSize: 8192, sharedWith: '财务部', uploadBy: '陈数据', uploadAt: '2026-06-08' },
]
</script>
