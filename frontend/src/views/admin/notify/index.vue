<template>
  <NotificationList
    :api="notifyApi"
    event-name="admin-notify-changed"
    description="管理工作流中产生的审核、发布、评分批次等系统通知。"
  />
</template>

<script setup lang="ts">
import NotificationList from '@/components/NotificationList.vue'
import { createNotifyApi } from '@/api/notify'

const notifyApi = createNotifyApi('admin')
</script>
