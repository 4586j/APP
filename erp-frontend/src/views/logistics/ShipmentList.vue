<template>
  <div class="page-container">
    <div class="page-header">
      <h2>物流管理</h2>
      <p class="page-desc">管理货运信息与物流轨迹跟踪</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="货运编号"><el-input v-model="query.shipmentNo" clearable style="width:160px" /></el-form-item>
        <el-form-item label="运输方式">
          <el-select v-model="query.method" placeholder="全部" clearable style="width:110px">
            <el-option label="海运" value="sea" /><el-option label="空运" value="air" />
            <el-option label="铁路" value="rail" /><el-option label="快递" value="express" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:110px">
            <el-option label="已订舱" value="booked" /><el-option label="运输中" value="in_transit" />
            <el-option label="已到港" value="arrived" /><el-option label="已交付" value="delivered" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Plus" @click="showCreate=true">新增货运</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <el-table :data="list" stripe size="small" v-loading="loading">
        <el-table-column prop="shipmentNo" label="货运编号" width="140" />
        <el-table-column prop="orderNo" label="关联订单" width="150" />
        <el-table-column prop="carrier" label="承运人" min-width="150" />
        <el-table-column label="方式" width="70" align="center">
          <template #default="scope">{{ methodLabel(scope.row.method) }}</template>
        </el-table-column>
        <el-table-column prop="blNo" label="提单号" width="140" />
        <el-table-column prop="vesselFlight" label="船名/航班" width="140" />
        <el-table-column prop="etd" label="ETD" width="100" />
        <el-table-column prop="eta" label="ETA" width="100" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="scope"><el-tag :type="statusType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="scope">
            <el-button type="primary" link size="small" @click="showTracking(scope.row as any)">轨迹</el-button>
            <el-button type="primary" link size="small" @click="showDetail(scope.row as any)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination v-model:current-page="page.current" :page-size="page.size" :total="page.total"
          layout="total, prev, pager, next" @current-change="loadData" />
      </div>
    </div>

    <!-- 新增货运对话框 -->
    <el-dialog v-model="showCreate" title="新增货运" width="650px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="运输方式"><el-select v-model="form.method" style="width:100%"><el-option label="海运" value="sea" /><el-option label="空运" value="air" /><el-option label="铁路" value="rail" /><el-option label="快递" value="express" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="承运人"><el-input v-model="form.carrier" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="关联订单"><el-input v-model="form.orderNo" placeholder="订单号" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="客户"><el-input v-model="form.customerName" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="船名/航班"><el-input v-model="form.vesselFlight" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="提单号"><el-input v-model="form.blNo" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="ETD"><el-date-picker v-model="form.etd" type="date" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="ETA"><el-date-picker v-model="form.eta" type="date" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="showCreate=false">取消</el-button><el-button type="primary" :loading="submitting" @click="handleCreate">保存</el-button></template>
    </el-dialog>

    <!-- 轨迹对话框 -->
    <el-dialog v-model="showTrackDlg" :title="'轨迹 - '+trackShipmentNo" width="550px">
      <el-timeline>
        <el-timeline-item v-for="t in trackings" :key="t.id" :timestamp="t.trackingDate" placement="top">
          <p><strong>{{ t.eventCode || t.description }}</strong></p>
          <p v-if="t.location">{{ t.location }}</p>
          <p v-if="t.operator">操作人: {{ t.operator }}</p>
        </el-timeline-item>
        <el-empty v-if="!trackings.length" description="暂无轨迹" />
      </el-timeline>
      <el-divider />
      <el-form :model="trackForm" inline>
        <el-form-item label="事件"><el-input v-model="trackForm.eventCode" placeholder="事件代码" style="width:130px" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="trackForm.description" style="width:200px" /></el-form-item>
        <el-form-item><el-button type="primary" size="small" @click="handleAddTracking">添加</el-button></el-form-item>
      </el-form>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="showDetailDlg" :title="'货运详情 - '+detail.shipmentNo" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="货运编号">{{ detail.shipmentNo }}</el-descriptions-item>
        <el-descriptions-item label="运输方式">{{ methodLabel(detail.method) }}</el-descriptions-item>
        <el-descriptions-item label="承运人">{{ detail.carrier }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="提单号">{{ detail.blNo }}</el-descriptions-item>
        <el-descriptions-item label="船名/航班">{{ detail.vesselFlight }}</el-descriptions-item>
        <el-descriptions-item label="ETD">{{ detail.etd }}</el-descriptions-item>
        <el-descriptions-item label="ETA">{{ detail.eta }}</el-descriptions-item>
        <el-descriptions-item label="关联订单">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detail.customerName }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listShipments, createShipment, addTracking, getTrackings, getShipment, type ShipmentVO, type TrackingVO } from '@/api/logistics'

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'
const loading = ref(false); const submitting = ref(false)
const showCreate = ref(false); const showTrackDlg = ref(false); const showDetailDlg = ref(false)
const list = ref<ShipmentVO[]>([]); const trackings = ref<TrackingVO[]>([])
const trackShipmentNo = ref(''); const trackShipmentId = ref(0)
const detail = ref<ShipmentVO>({} as ShipmentVO)
const page = reactive({ current: 1, size: 20, total: 0 })
const query = reactive({ shipmentNo: '', method: '', status: '' })
const form = reactive({ method: 'sea', carrier: '', orderNo: '', customerName: '', vesselFlight: '', blNo: '', etd: '', eta: '' })
const trackForm = reactive({ eventCode: '', description: '' })

function methodLabel(s: string) { const m: Record<string,string>={sea:'海运',air:'空运',rail:'铁路',express:'快递'}; return m[s]||s }
function statusType(s: string): TagType { const m: Record<string,TagType>={booked:'info',loaded:'warning',in_transit:'primary',customs:'warning',arrived:'info',delivered:'success'}; return m[s]||'info' }
function statusLabel(s: string) { const m: Record<string,string>={booked:'已订舱',loaded:'已装船',in_transit:'运输中',customs:'清关中',arrived:'已到港',delivered:'已交付'}; return m[s]||s }

async function loadData() {
  loading.value = true; try {
    const res = await listShipments({ ...query, page: page.current, size: page.size })
    list.value = res.records; page.total = res.total
  } finally { loading.value = false }
}
function handleSearch() { page.current = 1; loadData() }
async function handleCreate() {
  if (!form.carrier) { ElMessage.warning('请输入承运人'); return }
  submitting.value = true; try {
    await createShipment({ ...form })
    ElMessage.success('新增成功'); showCreate.value = false
    Object.assign(form, { method:'sea', carrier:'', orderNo:'', customerName:'', vesselFlight:'', blNo:'', etd:'', eta:'' })
    loadData()
  } finally { submitting.value = false }
}
async function showTracking(row: ShipmentVO) {
  trackShipmentNo.value = row.shipmentNo; trackShipmentId.value = row.id
  trackings.value = await getTrackings(row.id)
  showTrackDlg.value = true
}
async function handleAddTracking() {
  await addTracking({ shipmentId: trackShipmentId.value, trackingDate: new Date().toISOString(), ...trackForm })
  ElMessage.success('轨迹已添加'); trackForm.eventCode = ''; trackForm.description = ''
  trackings.value = await getTrackings(trackShipmentId.value)
}
async function showDetail(row: ShipmentVO) {
  detail.value = await getShipment(row.id)
  showDetailDlg.value = true
}
onMounted(() => loadData())
</script>
