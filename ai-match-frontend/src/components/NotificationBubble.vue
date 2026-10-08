<template>
  <el-popover placement="bottom-end" :width="360" trigger="click" @show="loadNotifications">
    <template #reference>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
        <el-button size="small" circle><el-icon :size="18"><Bell /></el-icon></el-button>
      </el-badge>
    </template>
    <div style="max-height:400px;overflow-y:auto">
      <div style="display:flex;justify-content:space-between;align-items:center;padding:4px 0 12px;border-bottom:1px solid rgba(255,255,255,0.06)">
        <span style="font-weight:600;color:#e0e0e0">通知</span>
        <el-button size="small" text @click="readAll" v-if="unreadCount > 0">全部已读</el-button>
      </div>
      <el-empty v-if="notifications.length === 0" description="暂无通知" :image-size="40" />
      <div v-for="n in notifications" :key="n.id" class="notif-item" :class="{ unread: n.isRead === 0 }" @click="markRead(n)" style="padding:10px 8px;border-bottom:1px solid rgba(255,255,255,0.04);cursor:pointer">
        <div style="font-size:13px;color:#e0e0e0;margin-bottom:4px">{{ n.title }}</div>
        <div style="font-size:12px;color:#808090">{{ n.content }}</div>
        <div style="font-size:11px;color:#5a5a72;margin-top:4px">{{ n.createTime }}</div>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import request from '@/api/request'

const notifications = ref([])
const unreadCount = ref(0)

const loadNotifications = async () => {
  try {
    const [nRes, cRes] = await Promise.all([
      request.get('/notifications'),
      request.get('/notifications/unread-count')
    ])
    notifications.value = nRes.data || []
    unreadCount.value = cRes.data || 0
  } catch (_) {}
}

const markRead = async (n) => {
  if (n.isRead === 0) {
    try { await request.put(`/notifications/${n.id}/read`); n.isRead = 1; unreadCount.value = Math.max(0, unreadCount.value - 1) } catch (_) {}
  }
}

const readAll = async () => {
  try { await request.put('/notifications/read-all'); unreadCount.value = 0; notifications.value.forEach(n => n.isRead = 1) } catch (_) {}
}
</script>

<style scoped>
.notif-item:hover { background: rgba(255,255,255,0.03) }
.notif-item.unread { background: rgba(96,165,250,0.06) }
</style>
