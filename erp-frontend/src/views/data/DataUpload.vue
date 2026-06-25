<template>
  <div class="page-container">
    <div class="page-header">
      <h2>数据上传</h2>
      <p class="page-desc">上传市场数据、分析代码或共享文件，可选择共享部门</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="文件名">
          <el-input v-model="query.keyword" placeholder="搜索" clearable style="width:200px" @clear="doSearch" @keyup.enter="doSearch" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.fileType" placeholder="全部" clearable style="width:140px" @change="doSearch">
            <el-option label="市场数据" value="market_data" />
            <el-option label="分析代码" value="analysis_code" />
            <el-option label="共享文档" value="shared_doc" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Upload" @click="showUploadDialog = true">上传文件</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>上传记录</h3>
      </div>
      <el-table :data="records" stripe size="small" v-loading="loading">
        <el-table-column prop="fileName" label="文件名" min-width="220" />
        <el-table-column prop="fileType" label="类型" width="120" align="center" />
        <el-table-column prop="fileSize" label="大小" width="90" align="right">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="department" label="共享部门" width="160" />
        <el-table-column prop="createdBy" label="上传人" width="90" />
        <el-table-column prop="createdAt" label="上传时间" width="180" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-popconfirm title="确定删除该记录？" @confirm="doDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="onPageChange"
        />
      </div>
    </div>

    <!-- 上传对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文件" width="450px">
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="文件名" required>
          <el-input v-model="uploadForm.fileName" placeholder="输入文件名（含扩展名）" />
        </el-form-item>
        <el-form-item label="文件类型" required>
          <el-select v-model="uploadForm.fileType" style="width:100%">
            <el-option label="市场数据" value="market_data" />
            <el-option label="分析代码" value="analysis_code" />
            <el-option label="共享文档" value="shared_doc" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件大小">
          <el-input-number v-model="uploadForm.fileSize" :min="0" :step="1024" style="width:100%" />
        </el-form-item>
        <el-form-item label="共享部门">
          <el-input v-model="uploadForm.department" placeholder="如：销售部、采购部" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="doUpload">确认上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Search, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listUploads, createUpload, deleteUpload, type DataUploadVO } from '@/api/data'

const loading = ref(false)
const uploading = ref(false)
const total = ref(0)
const records = ref<DataUploadVO[]>([])
const showUploadDialog = ref(false)

const query = reactive({ pageNum: 1, pageSize: 20, keyword: '', fileType: '' })
const uploadForm = reactive({ fileName: '', fileType: 'market_data', fileSize: 1024, department: '' })

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listUploads(query)
    records.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    console.error('Failed to load uploads:', e)
  } finally {
    loading.value = false
  }
}

function doSearch() { query.pageNum = 1; fetchData() }
function onPageChange(p: number) { query.pageNum = p; fetchData() }

async function doUpload() {
  if (!uploadForm.fileName) { ElMessage.warning('请输入文件名'); return }
  if (!uploadForm.fileType) { ElMessage.warning('请选择文件类型'); return }
  uploading.value = true
  try {
    await createUpload(uploadForm.fileName, uploadForm.fileType, uploadForm.fileSize, uploadForm.department)
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    uploadForm.fileName = ''
    uploadForm.fileSize = 1024
    uploadForm.department = ''
    fetchData()
  } catch (e: any) {
    console.error('Upload failed:', e)
  } finally {
    uploading.value = false
  }
}

async function doDelete(id: number) {
  try {
    await deleteUpload(id)
    ElMessage.success('已删除')
    fetchData()
  } catch (e: any) {
    console.error('Delete failed:', e)
  }
}

onMounted(fetchData)
</script>
