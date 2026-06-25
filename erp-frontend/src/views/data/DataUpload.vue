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
          <el-button :icon="Upload" @click="openUploadDialog">上传文件</el-button>
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
            <el-popconfirm title="确定逻辑删除该上传记录？" @confirm="doDelete(row.id)">
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
    <el-dialog v-model="showUploadDialog" title="上传文件" width="520px" @closed="resetUploadForm">
      <el-form :model="uploadForm" label-width="90px">
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            drag
            action="#"
            :auto-upload="false"
            :limit="1"
            :show-file-list="true"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            :on-exceed="onFileExceed"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或 <em>点击打开资源管理器</em></div>
            <template #tip>
              <div class="el-upload__tip">当前仅保存文件及元数据，删除为逻辑删除，不会物理删除磁盘文件。</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="文件类型" required>
          <el-select v-model="uploadForm.fileType" style="width:100%">
            <el-option label="市场数据" value="market_data" />
            <el-option label="分析代码" value="analysis_code" />
            <el-option label="共享文档" value="shared_doc" />
          </el-select>
        </el-form-item>
        <el-form-item label="共享部门">
          <el-input v-model="uploadForm.department" placeholder="如：销售部、采购部" />
        </el-form-item>
        <el-form-item label="已选文件" v-if="selectedFile">
          <span>{{ selectedFile.name }}（{{ formatSize(selectedFile.size) }}）</span>
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
import { Search, Upload, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, type UploadFile, type UploadInstance, type UploadRawFile } from 'element-plus'
import { listUploads, uploadDataFile, deleteUpload, type DataUploadVO } from '@/api/data'

const loading = ref(false)
const uploading = ref(false)
const total = ref(0)
const records = ref<DataUploadVO[]>([])
const showUploadDialog = ref(false)
const selectedFile = ref<File | null>(null)
const uploadRef = ref<UploadInstance>()

const query = reactive({ pageNum: 1, pageSize: 20, keyword: '', fileType: '' })
const uploadForm = reactive({ fileType: 'market_data', department: '' })

function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
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
function openUploadDialog() { showUploadDialog.value = true }

function onFileChange(file: UploadFile) {
  selectedFile.value = file.raw || null
}

function onFileRemove() {
  selectedFile.value = null
}

function onFileExceed(files: File[]) {
  uploadRef.value?.clearFiles()
  const file = files[0] as UploadRawFile
  file.uid = Date.now()
  uploadRef.value?.handleStart(file)
  selectedFile.value = file
}

function resetUploadForm() {
  selectedFile.value = null
  uploadForm.fileType = 'market_data'
  uploadForm.department = ''
  uploadRef.value?.clearFiles()
}

async function doUpload() {
  if (!selectedFile.value) { ElMessage.warning('请选择要上传的文件'); return }
  if (!uploadForm.fileType) { ElMessage.warning('请选择文件类型'); return }
  uploading.value = true
  try {
    await uploadDataFile(selectedFile.value, uploadForm.fileType, uploadForm.department)
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    query.pageNum = 1
    await fetchData()
  } catch (e: any) {
    console.error('Upload failed:', e)
  } finally {
    uploading.value = false
  }
}

async function doDelete(id: string) {
  try {
    await deleteUpload(id)
    records.value = records.value.filter(item => item.id !== id)
    total.value = Math.max(0, total.value - 1)
    ElMessage.success('已逻辑删除')
    await fetchData()
  } catch (e: any) {
    console.error('Delete failed:', e)
    const msg = e?.response?.data?.message || e?.message || '删除失败，请稍后重试'
    ElMessage.error(msg)
  }
}

onMounted(fetchData)
</script>
