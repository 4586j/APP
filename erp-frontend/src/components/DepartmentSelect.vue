<template>
  <el-tree-select
    v-model="selectedId"
    :disabled="disabled"
    filterable
    clearable
    check-strictly
    :data="treeData"
    :props="{ label: 'name', children: 'children' }"
    node-key="id"
    placeholder="请选择部门"
    style="width:100%"
    @change="onChange"
  />
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { getDepartmentOptions } from '@/api/system'
import type { DepartmentNode, Id } from '@/api/system'

const props = defineProps<{
  modelValue?: number | string | null
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: number | string | null): void
}>()

const selectedId = ref<number | string | null>(props.modelValue ?? null)
const treeData = ref<DepartmentNode[]>([])

async function loadOptions() {
  try {
    treeData.value = await getDepartmentOptions()
  } catch (e) {
    console.error('Failed to load department options:', e)
  }
}

function onChange(val: any) {
  emit('update:modelValue', val ?? null)
}

watch(() => props.modelValue, (val) => {
  selectedId.value = val ?? null
})

onMounted(loadOptions)
</script>
