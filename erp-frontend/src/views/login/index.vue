<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
      <div class="bg-shape shape-3"></div>
    </div>
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="32" color="#2563eb"><Box /></el-icon>
        <h1>外贸 ERP 系统</h1>
        <p>Foreign Trade ERP System</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>Demo 演示环境 · 任意账号密码即可登录</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: 'admin', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function handleLogin() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    loading.value = true
    setTimeout(() => {
      userStore.login(form.username, form.password)
      loading.value = false
      router.push('/dashboard')
    }, 600)
  })
}
</script>

<style scoped lang="scss">
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  .bg-shape {
    position: absolute;
    border-radius: 50%;
    opacity: 0.06;
    &.shape-1 {
      width: 600px; height: 600px;
      background: #2563eb;
      top: -200px; right: -100px;
    }
    &.shape-2 {
      width: 400px; height: 400px;
      background: #3b82f6;
      bottom: -100px; left: -80px;
    }
    &.shape-3 {
      width: 250px; height: 250px;
      background: #60a5fa;
      top: 50%; left: 60%;
      transform: translate(-50%, -50%);
    }
  }
}

.login-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 44px 40px 32px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;

  h1 {
    font-size: 22px;
    font-weight: 700;
    color: #1a1a2e;
    margin: 10px 0 6px;
  }
  p {
    font-size: 13px;
    color: #909399;
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 4px;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
