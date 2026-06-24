<template>
  <div class="page-container">
    <div class="page-header">
      <h2>产品管理</h2>
      <p class="page-desc">管理产品目录、HS 海关编码及多币种定价</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="产品名称">
          <el-input v-model="query.keyword" placeholder="名称/编码" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width:160px">
            <el-option label="LED 照明" :value="1" />
            <el-option label="太阳能设备" :value="2" />
            <el-option label="电机产品" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="HS编码">
          <el-input v-model="query.hsCode" placeholder="海关编码" clearable style="width:150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Plus">新增产品</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>产品列表</h3>
        <el-button :icon="Download">导出</el-button>
      </div>
      <el-table :data="products" stripe size="small">
        <el-table-column prop="productCode" label="产品编码" width="120" />
        <el-table-column prop="productName" label="产品名称" min-width="200" />
        <el-table-column prop="categoryName" label="分类" width="110" />
        <el-table-column prop="hsCode" label="HS 编码" width="120" />
        <el-table-column prop="unit" label="单位" width="70" align="center" />
        <el-table-column label="采购价" width="130" align="right">
          <template #default="{ row }">¥ {{ row.purchasePrice?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="销售价 (USD)" width="140" align="right">
          <template #default="{ row }">$ {{ row.salesPrice?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="originCountry" label="原产国" width="100" />
        <el-table-column prop="brand" label="品牌" width="100" />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default>
            <el-button type="primary" link size="small">编辑</el-button>
            <el-button type="primary" link size="small">定价</el-button>
            <el-button type="danger" link size="small">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination :current-page="1" :page-size="20" :total="86" layout="total, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Search, Plus, Download } from '@element-plus/icons-vue'

const query = reactive({ keyword: '', categoryId: null, hsCode: '' })

const products = [
  { productCode: 'LED-001', productName: 'LED Panel Light 600x600mm 40W', categoryName: 'LED 照明', hsCode: '9405.40.9000', unit: '件', purchasePrice: 85.00, salesPrice: 14.50, originCountry: '中国', brand: 'Opple' },
  { productCode: 'SOL-002', productName: 'Solar Inverter 5KW Single Phase', categoryName: '太阳能设备', hsCode: '8504.40.3090', unit: '台', purchasePrice: 2400.00, salesPrice: 400.00, originCountry: '中国', brand: 'Growatt' },
  { productCode: 'MOT-003', productName: 'Electric Motor 15HP 3-Phase', categoryName: '电机产品', hsCode: '8501.52.0000', unit: '台', purchasePrice: 3200.00, salesPrice: 520.00, originCountry: '中国', brand: 'ABB' },
  { productCode: 'LED-004', productName: 'LED Strip Light 5050 60LED/m', categoryName: 'LED 照明', hsCode: '9405.40.9000', unit: '米', purchasePrice: 8.50, salesPrice: 1.40, originCountry: '中国', brand: 'Opple' },
  { productCode: 'SOL-005', productName: 'Mono Solar Panel 550W', categoryName: '太阳能设备', hsCode: '8541.43.0000', unit: '块', purchasePrice: 680.00, salesPrice: 110.00, originCountry: '中国', brand: 'Jinko' },
  { productCode: 'MOT-006', productName: 'Servo Motor 2KW AC Drive', categoryName: '电机产品', hsCode: '8501.51.0000', unit: '套', purchasePrice: 1800.00, salesPrice: 290.00, originCountry: '中国', brand: 'Delta' },
  { productCode: 'LED-007', productName: 'LED Flood Light 200W IP65', categoryName: 'LED 照明', hsCode: '9405.40.9000', unit: '件', purchasePrice: 195.00, salesPrice: 32.00, originCountry: '中国', brand: 'Philips' },
  { productCode: 'SOL-008', productName: 'Lithium Battery 48V 100Ah', categoryName: '太阳能设备', hsCode: '8507.60.0090', unit: '台', purchasePrice: 5800.00, salesPrice: 950.00, originCountry: '中国', brand: 'CATL' },
]
</script>
