<template>
  <el-config-provider :locale="zhCn">
    <div class="app-root">
      <router-view />
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>
