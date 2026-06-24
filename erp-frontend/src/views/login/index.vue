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

      <el-form :model="form" :rules="rules" ref="formRef" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" autocomplete="username" />
        </el-form-item>

        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock"
                    show-password autocomplete="current-password" />
        </el-form-item>

        <el-form-item v-if="captchaEnabled" prop="captchaCode" class="captcha-row">
          <el-input v-model="form.captchaCode" placeholder="验证码" maxlength="4" class="captcha-input" />
          <div class="captcha-img" @click="loadCaptcha" :title="'点击刷新'">
            <img v-if="captcha.imageBase64" :src="captcha.imageBase64" alt="captcha" />
            <span v-else class="captcha-placeholder">加载中...</span>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>外贸 ERP · 默认账号 admin / admin123</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { User, Lock, Box } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getCaptcha } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

// 验证码开关（后端 app.security.captcha.enabled 控制；前端先默认开启，加载失败自动隐藏）
const captchaEnabled = ref(true)
const captcha = reactive({ uuid: '', imageBase64: '' })

const form = reactive({
  username: 'admin',
  password: 'admin123',
  captchaCode: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [
    {
      validator: (_: any, val: string, cb: any) => {
        if (!captchaEnabled.value) return cb()
        if (!val) return cb(new Error('请输入验证码'))
        if (val.length < 1) return cb(new Error('验证码格式错误'))
        cb()
      },
      trigger: 'blur',
    },
  ],
}

async function loadCaptcha() {
  try {
    const resp = await getCaptcha()
    captcha.uuid = resp.uuid
    captcha.imageBase64 = resp.imageBase64
    form.captchaCode = ''
  } catch (e: any) {
    // 后端关掉了 captcha：禁用前端字段
    captchaEnabled.value = false
    captcha.uuid = ''
    captcha.imageBase64 = ''
  }
}

async function handleLogin() {
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) return

  loading.value = true
  try {
    await userStore.login({
      username: form.username.trim(),
      password: form.password,
      captchaUuid: captchaEnabled.value ? captcha.uuid : undefined,
      captchaCode: captchaEnabled.value ? form.captchaCode : undefined,
    })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } catch (e: any) {
    // 拦截器已 toast；登录失败刷新验证码
    if (captchaEnabled.value) loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCaptcha()
})
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

.captcha-row :deep(.el-form-item__content) {
  display: flex;
  gap: 12px;
  align-items: stretch;
}
.captcha-input {
  flex: 1;
}
.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}
.captcha-placeholder {
  font-size: 12px;
  color: #909399;
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
