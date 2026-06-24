<template>
  <div class="app-layout">
    <Sidebar :collapsed="collapsed" />
    <div class="main-area" :class="{ collapsed: collapsed }">
      <Navbar :collapsed="collapsed" @toggle="collapsed = !collapsed" />
      <div class="app-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'

const collapsed = ref(false)
</script>

<style scoped lang="scss">
.app-layout {
  display: flex;
  height: 100%;
}

.main-area {
  flex: 1;
  margin-left: 220px;
  display: flex;
  flex-direction: column;
  transition: margin-left 0.28s;
  overflow: hidden;

  &.collapsed {
    margin-left: 64px;
  }
}

.app-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
