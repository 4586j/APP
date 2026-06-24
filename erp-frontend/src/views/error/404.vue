<template>
  <div class="error-page">
    <div class="error-content">
      <h1>404</h1>
      <p>抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.error-content {
  text-align: center;
  h1 {
    font-size: 96px;
    font-weight: 700;
    color: #dcdfe6;
    line-height: 1;
    margin-bottom: 16px;
  }
  p {
    font-size: 16px;
    color: #909399;
    margin-bottom: 28px;
  }
}
</style>
