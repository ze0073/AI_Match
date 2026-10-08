<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="sidebar glass-panel">
      <div class="logo"><h2>AI Match</h2><p>求职者中心</p></div>
      <el-menu :default-active="activeMenu" router class="side-menu">
        <el-menu-item index="/jobseeker/dashboard"><el-icon><HomeFilled /></el-icon><span>首页</span></el-menu-item>
        <el-menu-item index="/jobseeker/resume"><el-icon><Document /></el-icon><span>我的简历</span></el-menu-item>
        <el-menu-item index="/jobseeker/capability"><el-icon><Connection /></el-icon><span>能力图谱</span></el-menu-item>
        <el-menu-item index="/jobseeker/recommend"><el-icon><Star /></el-icon><span>岗位推荐</span></el-menu-item>
        <el-menu-item index="/jobseeker/jobs"><el-icon><Search /></el-icon><span>岗位搜索</span></el-menu-item>
        <el-menu-item index="/jobseeker/applications"><el-icon><List /></el-icon><span>求职记录</span></el-menu-item>
        <el-menu-item index="/jobseeker/profile"><el-icon><User /></el-icon><span>个人设置</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="navbar">
        <div class="navbar-right">
          <el-badge :value="unreadCount" :hidden="unreadCount===0" :max="99">
            <el-popover placement="bottom" :width="360" trigger="click" @show="loadNotifications">
              <template #reference><el-button circle class="notif-btn"><el-icon :size="20"><Bell /></el-icon></el-button></template>
              <div class="notif-panel">
                <div class="notif-header"><span>消息通知</span><el-button size="small" text @click="markAllRead" v-if="unreadCount>0">全部已读</el-button></div>
                <div v-if="notifications.length===0" style="text-align:center;padding:20px;color:var(--text-muted)">暂无通知</div>
                <div v-for="n in notifications" :key="n.id" class="notif-item" :class="{unread:n.isRead===0}" @click="readNotif(n)">
                  <p class="notif-title">{{ n.title }}</p><p class="notif-content">{{ n.content }}</p><span class="notif-time">{{ fmtTime(n.createTime) }}</span>
                </div>
              </div>
            </el-popover>
          </el-badge>
          <el-dropdown @command="handleCommand">
            <span class="user-info">{{ userStore.realName || userStore.username }}<el-icon><ArrowDown /></el-icon></span>
            <template #dropdown><el-dropdown-menu><el-dropdown-item command="profile">个人设置</el-dropdown-item><el-dropdown-item divided command="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
          </el-dropdown>
        </div>
              <NotificationBubble style="margin-left:12px" />
      </el-header>
      <el-main class="main-content"><router-view /></el-main>
    </el-container>
  </el-container>
</template>
<script setup>
import NotificationBubble from '@/components/NotificationBubble.vue'
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const route = useRoute(); const router = useRouter(); const userStore = useUserStore()
const activeMenu = computed(() => route.path)
const handleCommand = (c) => { if(c==='logout'){userStore.logout();router.push('/login')} else if(c==='profile')router.push('/jobseeker/profile') }

const unreadCount = ref(0); const notifications = ref([])
const loadNotifications = async () => { try { const r=await request.get('/notifications'); notifications.value=r.data?.list||[]; unreadCount.value=r.data?.unreadCount||0 } catch (_) {} }
const readNotif = async (n) => { if(n.isRead===0){ try { await request.put('/notifications/'+n.id+'/read'); n.isRead=1; unreadCount.value-- } catch (_) {} } }
const markAllRead = async () => { try { await request.put('/notifications/read-all'); notifications.value.forEach(n=>n.isRead=1); unreadCount.value=0 } catch (_) {} }
const fmtTime = (t) => { if(!t) return ''; const d=new Date(t); return (d.getMonth()+1)+'/'+d.getDate()+' '+d.getHours()+':'+String(d.getMinutes()).padStart(2,'0') }
setInterval(async () => { try { const r=await request.get('/notifications/unread-count'); unreadCount.value=r.data||0 } catch (_) {} }, 60000)
onMounted(async () => { try { const r=await request.get('/notifications/unread-count'); unreadCount.value=r.data||0 } catch (_) {} })
</script>
<style scoped>
.layout-container { height: 100vh; } .sidebar { overflow-y: auto; padding-top: 12px; }
.logo { text-align: center; padding: 24px 0 20px; } .logo h2 { font-size: 22px; margin-bottom: 2px; color: var(--accent-text); font-weight: 700; }
.logo p { font-size: 12px; color: var(--text-muted); } .side-menu { background: transparent !important; border-right: none !important; padding: 0 8px; }
.navbar { background: var(--bg-sidebar); backdrop-filter: var(--glass-blur); -webkit-backdrop-filter: var(--glass-blur); border-bottom: 1px solid rgba(255,255,255,0.25); display: flex; align-items: center; justify-content: flex-end; padding: 0 24px; height: 56px; }
.navbar-right { display: flex; align-items: center; gap: 16px; }
.notif-btn { background: transparent; border: none; box-shadow: none; color: var(--text-secondary); }
.notif-btn:hover { color: var(--accent-text); }
.user-info { cursor: pointer; display: flex; align-items: center; gap: 6px; color: var(--text-primary); font-weight: 500; }
.main-content { background: transparent; padding: 20px; }
.notif-panel { max-height: 400px; overflow-y: auto; }
.notif-header { display: flex; justify-content: space-between; align-items: center; padding: 8px 0 12px; border-bottom: 1px solid rgba(0,0,0,0.06); margin-bottom: 8px; font-weight: 600; }
.notif-item { padding: 10px 4px; border-radius: 8px; cursor: pointer; transition: background 0.2s; }
.notif-item:hover { background: rgba(0,0,0,0.03); } .notif-item.unread { background: rgba(108,130,180,0.06); }
.notif-title { font-size: 14px; font-weight: 500; margin-bottom: 2px; color: var(--text-primary); }
.notif-content { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }
.notif-time { font-size: 11px; color: var(--text-muted); }
@media (max-width: 768px) { .sidebar { width: 64px !important; } .logo h2, .logo p, .side-menu span { display: none; } }
</style>