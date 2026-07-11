<template>
  <NotificationList
    :api="notifyApi"
    event-name="teacher-notify-changed"
    description="把系统提醒、评审相关消息和未读状态整理成一份可持续阅读的消息册。"
  />
</template>

<script setup lang="ts">
import NotificationList from '@/components/NotificationList.vue'
import { createNotifyApi } from '@/api/notify'

const notifyApi = createNotifyApi('teacher')
</script>
