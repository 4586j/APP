<template>
  <div class="page-container">
    <div class="page-header">
      <h2>物流管理</h2>
      <p class="page-desc">管理货运信息与物流轨迹跟踪</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="货运编号">
          <el-input v-model="query.shipmentNo" placeholder="编号" clearable style="width:170px" />
        </el-form-item>
        <el-form-item label="运输方式">
          <el-select v-model="query.method" placeholder="全部" clearable style="width:120px">
            <el-option label="海运" value="sea" />
            <el-option label="空运" value="air" />
            <el-option label="铁路" value="rail" />
            <el-option label="快递" value="express" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="已订舱" value="booked" />
            <el-option label="运输中" value="in_transit" />
            <el-option label="已到港" value="arrived" />
            <el-option label="已交付" value="delivered" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Plus">新增货运</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>货运列表</h3>
      </div>
      <el-table :data="shipments" stripe size="small">
        <el-table-column prop="shipmentNo" label="货运编号" width="140" />
        <el-table-column prop="orderNo" label="关联订单" width="150" />
        <el-table-column prop="carrier" label="承运人" min-width="150" />
        <el-table-column prop="method" label="运输方式" width="80" align="center" />
        <el-table-column prop="blNo" label="提单号" width="140" />
        <el-table-column prop="vessel" label="船名/航班" width="140" />
        <el-table-column prop="etd" label="ETD" width="100" />
        <el-table-column prop="eta" label="ETA" width="100" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default>
            <el-button type="primary" link size="small">轨迹</el-button>
            <el-button type="primary" link size="small">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination :current-page="1" :page-size="20" :total="34" layout="total, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'

const query = reactive({ shipmentNo: '', method: '', status: '' })

function statusType(s: string) {
  const m: Record<string, string> = { booked: 'info', loaded: 'warning', in_transit: 'primary', customs: 'warning', arrived: '', delivered: 'success' }
  return m[s] || 'info'
}
function statusLabel(s: string) {
  const m: Record<string, string> = { booked: '已订舱', loaded: '已装船', in_transit: '运输中', customs: '清关中', arrived: '已到港', delivered: '已交付' }
  return m[s] || s
}

const shipments = [
  { shipmentNo: 'SH20260615001', orderNo: 'SO20260615001', carrier: 'Maersk Line', method: '海运', blNo: 'MAEU123456789', vessel: 'MAERSK HAVANA / 2418E', etd: '2026-06-20', eta: '2026-07-15', status: 'booked' },
  { shipmentNo: 'SH20260610002', orderNo: 'SO20260608001', carrier: 'DHL Express', method: '快递', blNo: 'DHL987654321', vessel: '—', etd: '2026-06-11', eta: '2026-06-18', status: 'in_transit' },
  { shipmentNo: 'SH20260601003', orderNo: 'SO20260525003', carrier: 'COSCO Shipping', method: '海运', blNo: 'COSU567890123', vessel: 'COSCO PRIDE / 2415W', etd: '2026-06-03', eta: '2026-06-28', status: 'arrived' },
  { shipmentNo: 'SH20260520004', orderNo: 'SO20260515004', carrier: 'Cathay Pacific', method: '空运', blNo: 'CP456789012', vessel: 'CX2880', etd: '2026-05-22', eta: '2026-05-24', status: 'delivered' },
]
</script>
