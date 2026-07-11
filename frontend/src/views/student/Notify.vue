<template>
  <NotificationList
    :api="notifyApi"
    event-name="student-notify-changed"
    description="把审核结果、发布动态、评论回复与排行榜提醒整理成一份可持续阅读的消息册。"
  />
</template>

<script setup lang="ts">
import NotificationList from '@/components/NotificationList.vue'
import { createNotifyApi } from '@/api/notify'

const notifyApi = createNotifyApi('student')
</script>
