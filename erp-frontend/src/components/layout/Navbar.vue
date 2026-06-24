<template>
  <div class="navbar">
    <div class="navbar-left">
      <el-icon class="toggle-btn" @click="$emit('toggle')" :size="20">
        <Fold v-if="!collapsed" />
        <Expand v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="navbar-right">
      <el-badge :value="5" :max="99" class="notice-badge">
        <el-icon :size="20"><Bell /></el-icon>
      </el-badge>
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" icon="UserFilled" />
          <span class="username">张三</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>
              <el-icon><User /></el-icon>个人信息
            </el-dropdown-item>
            <el-dropdown-item>
              <el-icon><Key /></el-icon>修改密码
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

defineProps<{ collapsed: boolean }>()
defineEmits<{ toggle: [] }>()

const route = useRoute()
const router = useRouter()

const currentTitle = computed(() => {
  const matched = route.matched
  return matched.length > 0 ? matched[matched.length - 1].meta?.title as string : ''
})

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.navbar {
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
  z-index: 1000;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toggle-btn {
  cursor: pointer;
  color: #606266;
  &:hover { color: var(--el-color-primary); }
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notice-badge {
  cursor: pointer;
  color: #606266;
  &:hover { color: var(--el-color-primary); }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;

  &:hover {
    background: #f5f7fa;
  }

  .username {
    font-size: 14px;
    color: #303133;
  }
}
</style>
