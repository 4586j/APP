<template>
  <div class="page-container">
    <div class="page-header">
      <h2>供应商列表</h2>
      <p class="page-desc">管理国内供应商信息、评分及联系方式</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="供应商名称">
          <el-input v-model="query.keyword" placeholder="名称" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="地区">
          <el-select v-model="query.region" placeholder="全部地区" clearable style="width:140px">
            <el-option label="广东" value="GD" />
            <el-option label="浙江" value="ZJ" />
            <el-option label="江苏" value="JS" />
            <el-option label="山东" value="SD" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分">
          <el-select v-model="query.rating" placeholder="全部" clearable style="width:100px">
            <el-option label="5星" :value="5" />
            <el-option label="4星" :value="4" />
            <el-option label="3星" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Plus">新增供应商</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>供应商列表</h3>
        <el-button :icon="Download">导出</el-button>
      </div>
      <el-table :data="suppliers" stripe size="small">
        <el-table-column prop="supplierCode" label="供应商编码" width="120" />
        <el-table-column prop="supplierName" label="供应商名称" min-width="200" />
        <el-table-column prop="province" label="省份" width="80" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="电话" width="130" />
        <el-table-column label="评分" width="140" align="center">
          <template #default="{ row }">
            <el-rate v-model="row.rating" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="paymentTerms" label="付款方式" width="130" />
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default>
            <el-button type="primary" link size="small">详情</el-button>
            <el-button type="primary" link size="small">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination :current-page="1" :page-size="20" :total="32" layout="total, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Search, Plus, Download } from '@element-plus/icons-vue'

const query = reactive({ keyword: '', region: '', rating: null })

const suppliers = [
  { supplierCode: 'SUP001', supplierName: '深圳华强电子有限公司', province: '广东', contactPerson: '王经理', contactPhone: '13800138001', rating: 5, paymentTerms: '月结30天' },
  { supplierCode: 'SUP002', supplierName: '浙江正泰电器股份有限公司', province: '浙江', contactPerson: '陈总监', contactPhone: '13900139002', rating: 5, paymentTerms: 'T/T 30%预付' },
  { supplierCode: 'SUP003', supplierName: '广东力特电机制造有限公司', province: '广东', contactPerson: '李工', contactPhone: '13700137003', rating: 4, paymentTerms: '月结60天' },
  { supplierCode: 'SUP004', supplierName: '江苏阳光纺织集团', province: '江苏', contactPerson: '赵总', contactPhone: '13600136004', rating: 4, paymentTerms: 'T/T 50%预付' },
  { supplierCode: 'SUP005', supplierName: '山东鲁阳机械有限公司', province: '山东', contactPerson: '刘经理', contactPhone: '13500135005', rating: 3, paymentTerms: '货到付款' },
  { supplierCode: 'SUP006', supplierName: '福建安溪茶业进出口公司', province: '福建', contactPerson: '黄女士', contactPhone: '13300133006', rating: 4, paymentTerms: 'T/T 100%预付' },
]
</script>
