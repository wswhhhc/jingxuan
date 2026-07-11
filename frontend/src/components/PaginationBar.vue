<template>
  <div class="workspace-pagination" v-if="total > 0">
    <el-pagination
      v-model:current-page="modelPage"
      v-model:page-size="modelSize"
      :total="total"
      layout="total, prev, pager, next"
      @change="$emit('change')"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 统一分页条组件
 *
 * 用法：<PaginationBar v-model:page="page" v-model:size="size" :total="total" @change="loadList" />
 *
 * 变量名匹配时可简写：
 *   <PaginationBar v-model:page v-model:size :total @change="loadList" />
 */
withDefaults(
  defineProps<{
    total: number
  }>(),
  {},
)

const modelPage = defineModel<number>('page', { default: 1 })
const modelSize = defineModel<number>('size', { default: 10 })
defineEmits<{ (e: 'change'): void }>()
</script>
