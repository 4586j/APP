<template>
  <div class="page-container">
    <div class="page-header">
      <h2>企业网盘</h2>
      <p class="page-desc">浏览、上传、管理文件，支持部门隔离和共享</p>
    </div>

    <div style="display:flex;gap:16px">
      <!-- 左侧目录树 -->
      <div class="tree-panel">
        <div style="padding:14px 16px;border-bottom:1px solid #f0f0f0;display:flex;justify-content:space-between;align-items:center">
          <h4 style="font-size:14px;font-weight:600">目录</h4>
        </div>
        <el-tree
          ref="treeRef"
          :data="deptTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          highlight-current
          :filter-node-method="filterNode"
          @node-click="onTreeClick"
        >
          <template #default="{ data }">
            <span>
              <span v-if="data.id === 'root'">📁 全部文件</span>
              <span v-else>📁 {{ data.name }}</span>
            </span>
          </template>
        </el-tree>
      </div>

      <!-- 右侧文件列表 -->
      <div class="file-panel">
        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <el-button size="small" :disabled="!currentParentId" @click="goUp">▲ 上级</el-button>
            <el-button size="small" @click="refreshList">🔄 刷新</el-button>
            <el-button v-if="hasPerm('data:upload:create')" size="small" type="primary" @click="showNewFolderDialog = true">📁 新建文件夹</el-button>
            <el-button v-if="hasPerm('data:upload:create')" size="small" type="primary" @click="showUploadDialog = true">⬆ 上传</el-button>
            <el-button v-if="hasPerm('data:upload:create')" size="small" type="primary" @click="triggerFolderUpload">📁 上传文件夹</el-button>
            <input ref="fileInputRef" type="file" multiple style="display:none" />
            <input ref="folderInputRef" type="file" webkitdirectory style="display:none" @change="onFolderSelected" />
          </div>
          <div class="toolbar-right">
            <el-input v-model="searchKeyword" placeholder="搜索当前目录" size="small" clearable style="width:200px" @keyup.enter="doSearch" />
          </div>
        </div>

        <!-- 面包屑 -->
        <div class="breadcrumb-bar">
          <el-breadcrumb>
            <el-breadcrumb-item><a href="#" @click.prevent="navigateToRoot">全部文件</a></el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumb" :key="item.id">
              <a href="#" @click.prevent="navigateTo(item.id!)">{{ item.displayName || item.name }}</a>
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <!-- 文件列表 -->
        <div class="table-container">
          <el-table :data="fileList" stripe size="small" v-loading="loading" @row-dblclick="onRowDblClick as any" empty-text="暂无文件">
            <el-table-column label="名称" min-width="300">
              <template #default="{ row }">
                <span style="cursor:pointer" @click="onRowDblClick(row as any)">
                  <span v-if="(row as any).isDirectory">📁</span>
                  <span v-else>📄</span>
                  {{ (row as any).displayName || (row as any).name }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="100" align="right">
              <template #default="{ row }">{{ (row as any).isDirectory ? '-' : formatSize((row as any).fileSize) }}</template>
            </el-table-column>
            <el-table-column prop="createdByName" label="上传人" width="100" />
            <el-table-column prop="createdAt" label="上传时间" width="170" />
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template #default="{ row }">
                <el-button v-if="!(row as any).isDirectory && hasPerm('data:upload:download')" type="primary" link size="small" @click="doDownload(row as any)">下载</el-button>
                <el-button v-if="hasPerm('data:upload:create')" type="primary" link size="small" @click="startRename(row as any)">重命名</el-button>
                <el-popconfirm v-if="hasPerm('data:upload:delete')" title="确定删除？" @confirm="doDelete(row as any)">
                  <template #reference>
                    <el-button type="danger" link size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 新建文件夹 -->
    <el-dialog v-model="showNewFolderDialog" title="新建文件夹" width="400px" @closed="newFolderName = ''">
      <el-input v-model="newFolderName" placeholder="请输入文件夹名称" @keyup.enter="confirmCreateFolder" />
      <template #footer>
        <el-button @click="showNewFolderDialog = false">取消</el-button>
        <el-button type="primary" :loading="creatingFolder" @click="confirmCreateFolder">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重命名 -->
    <el-dialog v-model="showRenameDialog" title="重命名" width="400px" @closed="renameName = ''">
      <el-input v-model="renameName" placeholder="请输入新名称" @keyup.enter="confirmRename" />
      <template #footer>
        <el-button @click="showRenameDialog = false">取消</el-button>
        <el-button type="primary" :loading="renaming" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>

    <!-- 上传进度 -->
    <el-dialog v-model="showUploadDialog" title="上传文件" width="500px" :close-on-click-modal="false">
      <div v-if="!uploading">
        <el-upload
          ref="uploadRef"
          drag
          action="#"
          :auto-upload="false"
          :limit="10"
          :show-file-list="true"
          :on-change="onUploadFileChange"
          :on-remove="onUploadFileRemove"
          multiple
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择文件</em></div>
        </el-upload>
        <el-form label-width="90px" style="margin-top:12px">
          <el-form-item label="共享给部门">
            <el-tree-select
              v-model="uploadShareDeptIds"
              :data="departmentTree"
              :props="{ label: 'name', children: 'children' }"
              node-key="id"
              multiple
              collapse-tags
              filterable
              clearable
              placeholder="选择多个部门（可选）"
              style="width:100%"
            />
          </el-form-item>
        </el-form>
      </div>
      <div v-else style="text-align:center;padding:20px">
        <el-progress :percentage="uploadPercent" :stroke-width="20" :striped="true" :striped-flow="true" />
        <p style="margin-top:10px;color:#909399">正在上传...</p>
      </div>
      <template #footer>
        <el-button v-if="!uploading" @click="showUploadDialog = false">取消</el-button>
        <el-button v-if="!uploading" type="primary" :disabled="!uploadFiles.length" @click="doUploadFiles">开始上传</el-button>
      </template>
    </el-dialog>

    <!-- 文件夹上传 -->
    <el-dialog v-model="showFolderUploadDialog" title="上传文件夹" width="520px" :close-on-click-modal="false" :close-on-press-escape="false" :show-close="false">
      <!-- 准备阶段：选择共享部门 -->
      <div v-if="!uploadingFolder">
        <div style="margin-bottom:12px;color:#606266">
          已选择 <b>{{ pendingFolderFiles.length }}</b> 个文件，请选择共享部门后开始上传
        </div>
        <el-form label-width="90px">
          <el-form-item label="共享给部门">
            <el-tree-select
              v-model="folderShareDeptIds"
              :data="departmentTree"
              :props="{ label: 'name', children: 'children' }"
              node-key="id"
              multiple
              collapse-tags
              collapse-tags-tooltip
              filterable
              clearable
              placeholder="选择多个部门（可选）"
              style="width:100%"
            />
          </el-form-item>
        </el-form>
      </div>
      <!-- 上传进度 -->
      <div v-else style="text-align:center;padding:20px">
        <div style="margin-bottom:16px;text-align:left">
          <div style="font-size:13px;color:#909399;margin-bottom:4px">当前文件进度</div>
          <el-progress :percentage="folderCurrentPercent" :stroke-width="16" :striped="true" />
        </div>
        <div style="margin-bottom:16px;text-align:left">
          <div style="font-size:13px;color:#909399;margin-bottom:4px">总体进度</div>
          <el-progress :percentage="folderUploadPercent" :stroke-width="16" :striped="true" :striped-flow="true" />
        </div>
        <p style="margin-top:10px;color:#606266;font-size:13px">{{ folderUploadProgress }}</p>
      </div>
      <template #footer>
        <el-button v-if="!uploadingFolder" @click="cancelFolderUpload">取消</el-button>
        <el-button v-if="!uploadingFolder" type="primary" @click="startFolderUpload">开始上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile, type UploadInstance } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import {
  listFiles, getBreadcrumb, createFolder, uploadFileToNetdisk,
  renameFile, deleteFile, downloadFile,
  type DatFileVO, type DatFileQuery,
} from '@/api/data'
import { getDepartmentOptions, type DepartmentNode, type Id } from '@/api/system'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const hasPerm = (perm: string) => userStore.hasPermission(perm)

const loading = ref(false)
const treeRef = ref<any>()
const deptTree = ref<any[]>([{ id: 'root', name: '全部文件', children: [] }])
const fileList = ref<DatFileVO[]>([])
const breadcrumb = ref<DatFileVO[]>([])
const currentParentId = ref<string | undefined>(undefined)
const currentDeptId = ref<string | undefined>(undefined)
const searchKeyword = ref('')

// 目录树
const departmentTree = ref<DepartmentNode[]>([])

function resolveOperationDeptId() {
  return currentDeptId.value || (
    userStore.userInfo?.departmentId != null ? String(userStore.userInfo.departmentId) : undefined
  )
}

function requireOperationDeptId() {
  const deptId = resolveOperationDeptId()
  if (!currentParentId.value && !deptId) {
    ElMessage.warning('请先在左侧选择部门后再操作')
    return undefined
  }
  return deptId
}

function filterNode(value: string, data: any) {
  if (!value) return true
  return data.name?.toLowerCase().includes(value.toLowerCase())
}

async function loadDeptTree() {
  try {
    const depts = await getDepartmentOptions()
    deptTree.value = [
      { id: 'root', name: '全部文件', children: depts || [] }
    ]
    departmentTree.value = depts || []
  } catch (e) {
    console.error('Failed to load departments:', e)
  }
}

function onTreeClick(data: any) {
  if (data.id === 'root') {
    currentDeptId.value = undefined
  } else {
    currentDeptId.value = data.id
  }
  currentParentId.value = undefined
  breadcrumb.value = []
  refreshList()
}

// 面包屑
async function loadBreadcrumb() {
  if (!currentParentId.value) {
    breadcrumb.value = []
    return
  }
  try {
    breadcrumb.value = await getBreadcrumb(currentParentId.value)
  } catch (e) {
    console.error('Failed to load breadcrumb:', e)
  }
}

function navigateToRoot() {
  currentParentId.value = undefined
  currentDeptId.value = undefined
  breadcrumb.value = []
  refreshList()
}

function navigateTo(id: string) {
  currentParentId.value = id
  loadBreadcrumb()
  refreshList()
}

function goUp() {
  if (breadcrumb.value.length > 0) {
    if (breadcrumb.value.length === 1) {
      navigateToRoot()
    } else {
      navigateTo(breadcrumb.value[breadcrumb.value.length - 2].id!)
    }
  } else {
    navigateToRoot()
  }
}

// 文件列表
async function refreshList() {
  loading.value = true
  try {
    const params: DatFileQuery = {
      parentId: currentParentId.value,
      deptId: currentDeptId.value,
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    fileList.value = await listFiles(params)
  } catch (e: any) {
    console.error('Failed to load files:', e)
  } finally {
    loading.value = false
  }
}

function doSearch() {
  refreshList()
}

function onRowDblClick(row: DatFileVO) {
  if (row.isDirectory) {
    currentParentId.value = row.id
    loadBreadcrumb()
    refreshList()
  }
}

// 新建文件夹
const showNewFolderDialog = ref(false)
const newFolderName = ref('')
const creatingFolder = ref(false)

async function confirmCreateFolder() {
  if (!newFolderName.value.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  creatingFolder.value = true
  try {
    const deptId = requireOperationDeptId()
    if (!currentParentId.value && !deptId) return
    await createFolder(currentParentId.value, newFolderName.value.trim(), deptId)
    ElMessage.success('创建成功')
    showNewFolderDialog.value = false
    await refreshList()
  } catch (e: any) {
    ElMessage.error(e?.message || '创建失败')
  } finally {
    creatingFolder.value = false
  }
}

// 重命名
const showRenameDialog = ref(false)
const renameTarget = ref<DatFileVO | null>(null)
const renameName = ref('')
const renaming = ref(false)

function startRename(row: DatFileVO) {
  renameTarget.value = row
  renameName.value = row.displayName || row.name
  showRenameDialog.value = true
}

async function confirmRename() {
  if (!renameTarget.value || !renameName.value.trim()) return
  renaming.value = true
  try {
    await renameFile(renameTarget.value.id, renameName.value.trim())
    ElMessage.success('重命名成功')
    showRenameDialog.value = false
    await refreshList()
  } catch (e: any) {
    ElMessage.error(e?.message || '重命名失败')
  } finally {
    renaming.value = false
  }
}

// 删除
async function doDelete(row: DatFileVO) {
  try {
    await deleteFile(row.id)
    ElMessage.success('删除成功')
    await refreshList()
  } catch (e: any) {
    ElMessage.error(e?.message || '删除失败')
  }
}

// 下载
async function doDownload(row: DatFileVO) {
  try {
    const blob = await downloadFile(row.id)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.displayName || row.name
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error(e?.message || '下载失败')
  }
}

// 上传
const fileInputRef = ref<HTMLInputElement>()
const folderInputRef = ref<HTMLInputElement>()
const showUploadDialog = ref(false)
const uploading = ref(false)
const uploadPercent = ref(0)
const uploadFiles = ref<File[]>([])
const uploadShareDeptIds = ref<Id[]>([])
const uploadRef = ref<UploadInstance>()

// 文件夹上传
const uploadingFolder = ref(false)
const folderUploadProgress = ref('')
const folderUploadPercent = ref(0)
const folderCurrentPercent = ref(0)
const showFolderUploadDialog = ref(false)
const pendingFolderFiles = ref<File[]>([])
const folderShareDeptIds = ref<Id[]>([])

function triggerFolderUpload() {
  folderInputRef.value?.click()
}

async function onFolderSelected(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files || !input.files.length) return
  pendingFolderFiles.value = Array.from(input.files)
  folderShareDeptIds.value = []
  folderUploadPercent.value = 0
  folderCurrentPercent.value = 0
  folderUploadProgress.value = ''
  uploadingFolder.value = false
  showFolderUploadDialog.value = true
}

function cancelFolderUpload() {
  showFolderUploadDialog.value = false
  pendingFolderFiles.value = []
  folderShareDeptIds.value = []
  if (folderInputRef.value) folderInputRef.value.value = ''
}

async function startFolderUpload() {
  const files = pendingFolderFiles.value
  if (!files.length) {
    ElMessage.warning('未选择文件')
    return
  }
  const shareIds = folderShareDeptIds.value.length > 0
    ? folderShareDeptIds.value.map(String).join(',')
    : undefined
  const deptId = requireOperationDeptId()
  if (!currentParentId.value && !deptId) return

  // 解析目录结构
  const dirMap = new Map<string, string>() // relativeDirPath -> serverFolderId
  uploadingFolder.value = true
  folderUploadPercent.value = 0
  folderCurrentPercent.value = 0
  folderUploadProgress.value = '正在创建目录结构...'

  try {
    // 收集所有唯一目录路径（按深度排序，确保父目录先创建）
    const dirSet = new Set<string>()
    for (const file of files) {
      const path = file.webkitRelativePath || file.name
      const parts = path.split('/')
      // parts[0] 是根文件夹名，逐级创建
      for (let i = 0; i < parts.length - 1; i++) {
        const dirPath = parts.slice(0, i + 1).join('/')
        dirSet.add(dirPath)
      }
    }
    // 按深度排序
    const sortedDirs = Array.from(dirSet).sort((a, b) => a.split('/').length - b.split('/').length)

    // 逐级创建目录
    for (const dirPath of sortedDirs) {
      const parts = dirPath.split('/')
      const name = parts[parts.length - 1]
      const parentRelPath = parts.slice(0, -1).join('/')
      const parentId = parentRelPath ? dirMap.get(parentRelPath) : currentParentId.value
      if (parentRelPath && !parentId) {
        throw new Error(`父目录创建失败: ${parentRelPath}`)
      }
      const id = await ensureFolder(parentId, name, deptId, shareIds)
      dirMap.set(dirPath, id)
    }

    // 上传文件
    let completed = 0
    for (const file of files) {
      const relPath = file.webkitRelativePath || file.name
      const parts = relPath.split('/')
      const dirPath = parts.slice(0, -1).join('/')
      const targetParentId = dirPath ? dirMap.get(dirPath) : currentParentId.value
      if (dirPath && !targetParentId) {
        throw new Error(`目标目录不存在: ${dirPath}`)
      }

      folderUploadProgress.value = `正在上传: ${relPath}`
      folderCurrentPercent.value = 0
      await uploadFileToNetdisk(file, targetParentId, undefined, deptId, shareIds,
        (p) => { folderCurrentPercent.value = p }
      ).catch((e: any) => {
        throw new Error(`上传失败: ${relPath}，${e?.message || '请检查权限或网络'}`)
      })
      completed++
      folderUploadPercent.value = Math.round((completed / files.length) * 100)
    }

    ElMessage.success(`文件夹上传完成，共 ${files.length} 个文件`)
    showFolderUploadDialog.value = false
    await refreshList()
  } catch (e: any) {
    ElMessage.error(e?.message || '文件夹上传失败')
  } finally {
    uploadingFolder.value = false
    pendingFolderFiles.value = []
    folderShareDeptIds.value = []
    if (folderInputRef.value) folderInputRef.value.value = '' // 清空 input 以便重复选择同一文件夹
  }
}

async function ensureFolder(parentId: string | undefined, name: string, deptId: string | undefined, shareIds: string | undefined) {
  const children = await listFiles({ parentId, deptId })
  const existing = children.find((item) =>
    item.isDirectory === 1 && (item.displayName || item.name) === name
  )
  if (existing?.id) return existing.id

  try {
    return await createFolder(parentId, name, deptId, shareIds)
  } catch (e: any) {
    const latestChildren = await listFiles({ parentId, deptId })
    const latestExisting = latestChildren.find((item) =>
      item.isDirectory === 1 && (item.displayName || item.name) === name
    )
    if (latestExisting?.id) return latestExisting.id
    throw new Error(`创建目录失败: ${name}，${e?.message || '请检查权限或是否重名'}`)
  }
}

function triggerUpload() {
  showUploadDialog.value = true
}

function onUploadFileChange(file: UploadFile) {
  if (file.raw) {
    uploadFiles.value.push(file.raw)
  }
}

function onUploadFileRemove() {
  uploadFiles.value = []
}

async function doUploadFiles() {
  if (!uploadFiles.value.length) return
  uploading.value = true
  uploadPercent.value = 0
  let success = 0
  try {
    const deptId = requireOperationDeptId()
    if (!currentParentId.value && !deptId) return
    for (const file of uploadFiles.value) {
      const shareIds = uploadShareDeptIds.value.length > 0
        ? uploadShareDeptIds.value.map(String).join(',')
        : undefined
      await uploadFileToNetdisk(
        file, currentParentId.value, undefined, deptId, shareIds,
        (p) => { uploadPercent.value = p }
      )
      success++
    }
    ElMessage.success(`上传完成，成功 ${success}/${uploadFiles.value.length} 个文件`)
    showUploadDialog.value = false
    uploadFiles.value = []
    uploadShareDeptIds.value = []
    await refreshList()
  } catch (e: any) {
    ElMessage.error(e?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

// 工具
function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

// 路由
watch(currentParentId, () => {
  loadBreadcrumb()
})

onMounted(async () => {
  await loadDeptTree()
  await refreshList()
})
</script>

<style scoped lang="scss">
.tree-panel {
  width: 260px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  overflow: hidden;
}
.file-panel {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  padding: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.toolbar-left {
  display: flex;
  gap: 8px;
}
.toolbar-right {
  display: flex;
  gap: 8px;
}
.breadcrumb-bar {
  margin-bottom: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
</style>
