<template>
  <div class="dashboard glass-fade-in" v-loading="loading">
    <!-- Stat Cards -->
    <el-row :gutter="20">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.label">
        <el-card class="glass-card stat-card-item" shadow="never">
          <div class="stat-card-inner">
            <el-icon :size="36" :color="card.color"><component :is="card.icon" /></el-icon>
            <div class="stat-info">
              <p class="stat-num">{{ card.value }}</p>
              <p class="stat-label">{{ card.label }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :xs="24" :md="16">
        <el-card class="glass-card">
          <template #header><span>岗位推荐</span></template>
          <el-table :data="recommendations" stripe size="small" v-if="recommendations.length > 0">
            <el-table-column prop="jobTitle" label="职位名称" />
            <el-table-column prop="company" label="公司" />
            <el-table-column prop="salary" label="薪资" width="110" />
            <el-table-column label="匹配度" width="120">
              <template #default="scope">
                <el-progress :percentage="scope.row.matchScore || 0" :color="scoreColor" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button type="primary" size="small" @click="viewJob(scope.row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无推荐职位，请先上传简历">
            <el-button type="primary" @click="router.push('/jobseeker/resume')">上传简历</el-button>
          </el-empty>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8">
        <el-card class="glass-card">
          <template #header><span>快捷操作</span></template>
          <div class="quick-actions">
            <el-button type="primary" size="large" class="action-btn" @click="router.push('/jobseeker/resume')"><el-icon><Upload /></el-icon>上传简历</el-button>
            <el-button type="success" size="large" class="action-btn" @click="router.push('/jobseeker/capability')"><el-icon><Connection /></el-icon>能力图谱</el-button>
            <el-button type="warning" size="large" class="action-btn" @click="router.push('/jobseeker/recommend')"><el-icon><Star /></el-icon>智能推荐</el-button>
            <el-button size="large" class="action-btn" @click="router.push('/jobseeker/jobs')"><el-icon><Search /></el-icon>搜索岗位</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Todo Section -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card class="glass-card">
          <template #header><span>待办事项</span></template>
          <div style="display:flex;gap:16px;flex-wrap:wrap">
            <div v-for="todo in todos" :key="todo.key" class="todo-item glass-card" :style="{padding:'14px 18px',flex:'1',minWidth:'200px',cursor:'pointer'}" @click="router.push(todo.link)">
              <div style="display:flex;align-items:center;gap:8px">
                <el-tag :type="todo.done ? 'success' : 'warning'" size="small">{{ todo.done ? '已完成' : '待处理' }}</el-tag>
                <span style="color:#e0e0e0;font-weight:500">{{ todo.label }}</span>
              </div>
              <p style="color:#888;font-size:13px;margin:6px 0 0">{{ todo.desc }}</p>
            </div>
            <el-empty v-if="todos.length===0" description="暂无待办" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Notifications -->
    <el-row :gutter="20" style="margin-top:20px" v-if="recentNotifications.length > 0">
      <el-col :span="24">
        <el-card class="glass-card">
          <template #header><span>最近通知</span></template>
          <div v-for="n in recentNotifications" :key="n.id" class="notif-row">
            <el-icon :size="16" color="#409EFF"><Bell /></el-icon>
            <span class="notif-title">{{ n.title }}</span>
            <span class="notif-time">{{ formatTime(n.createTime) }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { matchApi, resumeApi } from '@/api'
import request from '@/api/request'
import { Document, Star, Connection, Upload, Search, Bell } from '@element-plus/icons-vue'

const loading = ref(false)
const router = useRouter()
const resumeStatus = ref('未上传')
const recommendCount = ref(0)
const skillCount = ref(0)
const recommendations = ref([])
const recentNotifications = ref([])
const todos = ref([])

const statCards = computed(() => [
  { label: '简历状态', value: resumeStatus.value, color: '#409EFF', icon: Document },
  { label: '推荐岗位', value: recommendCount.value, color: '#67C23A', icon: Star },
  { label: '技能标签', value: skillCount.value, color: '#E6A23C', icon: Connection }
])

const scoreColor = (p) => p > 80 ? '#67C23A' : p > 60 ? '#E6A23C' : '#F56C6C'
const viewJob = (row) => router.push('/jobseeker/jobs/' + row.jobId)
const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return (d.getMonth()+1) + '/' + d.getDate() + ' ' + d.getHours() + ':' + String(d.getMinutes()).padStart(2,'0')
}

onMounted(async () => {
  loading.value = true
  try {
    const [recRes, capRes, notifRes] = await Promise.all([
      matchApi.getRecommendations(5).catch(() => null),
      resumeApi.getCapability().catch(() => null),
      request.get('/notifications').catch(() => null)
    ])

    if (recRes?.data) {
      recommendations.value = recRes.data || []
      recommendCount.value = recommendations.value.length
    }

    if (capRes?.data?.skills) {
      try {
        const skills = JSON.parse(capRes.data.skills)
        skillCount.value = Array.isArray(skills) ? skills.length : 0
      } catch (_) { skillCount.value = 0 }
      resumeStatus.value = '已完善'
    }

    if (notifRes?.data?.list) {
      recentNotifications.value = (notifRes.data.list || []).slice(0, 5)
    }

    // Build todos
    todos.value = []
    if (capRes?.data) {
      todos.value.push({ key: 'resume', label: '完善简历', desc: '查看并编辑您的简历信息', done: true, link: '/jobseeker/resume' })
    } else {
      todos.value.push({ key: 'resume', label: '上传简历', desc: '上传简历以获得精准岗位推荐', done: false, link: '/jobseeker/resume' })
    }
    todos.value.push({ key: 'recommend', label: '查看推荐', desc: '浏览系统为您智能匹配的岗位', done: recommendations.value.length > 0, link: '/jobseeker/recommend' })
    todos.value.push({ key: 'graph', label: '能力图谱', desc: '查看您的技能画像和能力分布', done: skillCount.value > 0, link: '/jobseeker/graph' })

  } catch (_) {} finally { loading.value = false }
})
</script>

<style scoped>
.stat-card-item { transition: all 0.3s ease; }
.stat-card-item:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.stat-card-inner { display: flex; align-items: center; gap: 16px; }
.stat-num { font-size: 24px; font-weight: bold; color: var(--text-primary); margin: 0; }
.stat-label { color: var(--text-muted); margin-top: 4px; font-size: 13px; }
.quick-actions { display: flex; flex-direction: column; gap: 12px; }
.action-btn { width: 100%; }
.todo-item { transition: all 0.3s ease; }
.todo-item:hover { transform: translateY(-2px); }
.notif-row { display: flex; align-items: center; gap: 8px; padding: 10px 0; border-bottom: 1px solid rgba(0,0,0,0.05); }
.notif-row:last-child { border-bottom: none; }
.notif-title { flex: 1; font-size: 14px; }
.notif-time { font-size: 12px; color: var(--text-muted); white-space: nowrap; }
</style>