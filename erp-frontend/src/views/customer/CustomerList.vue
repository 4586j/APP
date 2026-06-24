<template>
  <div class="page-container">
    <div class="page-header">
      <h2>客户列表</h2>
      <p class="page-desc">管理海外客户信息、联系人及信用额度</p>
    </div>
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="客户名称">
          <el-input v-model="query.keyword" placeholder="中/英文名称" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="国家">
          <el-select v-model="query.country" placeholder="全部国家" clearable style="width:150px">
            <el-option label="美国" value="US" />
            <el-option label="德国" value="DE" />
            <el-option label="英国" value="GB" />
            <el-option label="日本" value="JP" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部类型" clearable style="width:140px">
            <el-option label="Buyer" value="buyer" />
            <el-option label="Agent" value="agent" />
            <el-option label="Distributor" value="distributor" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search">查询</el-button>
          <el-button :icon="Plus">新增客户</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-container">
      <div class="table-header">
        <h3>客户列表</h3>
        <el-button :icon="Download">导出</el-button>
      </div>
      <el-table :data="customers" stripe size="small">
        <el-table-column prop="customerCode" label="客户编码" width="120" />
        <el-table-column prop="customerName" label="客户名称" min-width="170" />
        <el-table-column prop="customerNameEn" label="英文名称" min-width="170" />
        <el-table-column prop="country" label="国家" width="80" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactEmail" label="邮箱" min-width="180" />
        <el-table-column label="信用额度" width="140" align="right">
          <template #default="{ row }">$ {{ row.creditLimit?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="paymentTerms" label="付款方式" width="110" />
        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default>
            <el-button type="primary" link size="small">详情</el-button>
            <el-button type="primary" link size="small">编辑</el-button>
            <el-button type="danger" link size="small">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display:flex;justify-content:flex-end;padding:16px 0">
        <el-pagination :current-page="1" :page-size="20" :total="45" layout="total, prev, pager, next" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Search, Plus, Download } from '@element-plus/icons-vue'

const query = reactive({ keyword: '', country: '', type: '' })

const customers = [
  { customerCode: 'CUS001', customerName: 'ABC 贸易公司', customerNameEn: 'ABC Trading Inc.', country: '美国', contactPerson: 'John Smith', contactEmail: 'john@abctrading.com', creditLimit: 200000, paymentTerms: 'T/T 30/70' },
  { customerCode: 'CUS002', customerName: '环球进口有限公司', customerNameEn: 'Global Imports LLC', country: '美国', contactPerson: 'Sarah Johnson', contactEmail: 'sarah@globalimports.com', creditLimit: 150000, paymentTerms: 'L/C at sight' },
  { customerCode: 'CUS003', customerName: '太平洋商品公司', customerNameEn: 'Pacific Goods Co.', country: '日本', contactPerson: 'Tanaka Hiroshi', contactEmail: 'tanaka@pacificgoods.jp', creditLimit: 300000, paymentTerms: 'T/T 100%' },
  { customerCode: 'CUS004', customerName: '欧洲贸易有限公司', customerNameEn: 'Euro Trade GmbH', country: '德国', contactPerson: 'Hans Mueller', contactEmail: 'hans@eurotrade.de', creditLimit: 180000, paymentTerms: 'D/P at sight' },
  { customerCode: 'CUS005', customerName: '亚洲合作伙伴', customerNameEn: 'Asia Partners Ltd.', country: '新加坡', contactPerson: 'Lee Wei Ming', contactEmail: 'weiming@asiapartners.sg', creditLimit: 250000, paymentTerms: 'T/T 30/70' },
  { customerCode: 'CUS006', customerName: '朝阳商业公司', customerNameEn: 'Sunrise Commerce', country: '英国', contactPerson: 'James Brown', contactEmail: 'james@sunrisecommerce.co.uk', creditLimit: 120000, paymentTerms: 'L/C 60 days' },
]
</script>
