<template>
  <div class="system-config glass-fade-in" v-loading="loading">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="glass-card">
          <template #header><span>系统状态</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="数据存储">JSON 文件存储</el-descriptions-item>
            <el-descriptions-item label="AI模型">{{ config.aiModel || '未配置' }}</el-descriptions-item>
            <el-descriptions-item label="后端端口">8080</el-descriptions-item>
            <el-descriptions-item label="数据目录">./data/</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="glass-card">
          <template #header><span>数据统计</span></template>
          <el-row :gutter="16">
            <el-col :span="12"><div class="mini-stat"><p class="mini-num">{{ config.userCount }}</p><p class="mini-label">用户</p></div></el-col>
            <el-col :span="12"><div class="mini-stat"><p class="mini-num">{{ config.resumeCount }}</p><p class="mini-label">简历</p></div></el-col>
            <el-col :span="12"><div class="mini-stat"><p class="mini-num">{{ config.jobCount }}</p><p class="mini-label">职位</p></div></el-col>
            <el-col :span="12"><div class="mini-stat"><p class="mini-num">{{ config.matchCount }}</p><p class="mini-label">匹配</p></div></el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card class="glass-card">
          <template #header><span>服务器资源</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="CPU 核数">{{ systemStats.cpuCores }}</el-descriptions-item>
            <el-descriptions-item label="已用内存">{{ systemStats.usedMemoryMB }} MB</el-descriptions-item>
            <el-descriptions-item label="总内存">{{ systemStats.totalMemoryMB }} MB</el-descriptions-item>
            <el-descriptions-item label="在线用户">{{ systemStats.onlineUsers }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="glass-card">
          <template #header><span>数据文件</span></template>
          <el-table :data="dataFiles" stripe size="small">
            <el-table-column prop="name" label="文件名" />
            <el-table-column prop="records" label="记录数" width="100" />
            <el-table-column prop="size" label="大小" width="100" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'
import { adminApi } from '@/api'

const loading = ref(false)
const config = ref({ aiModel: '', userCount: 0, resumeCount: 0, jobCount: 0, matchCount: 0 })
const systemStats = ref({ cpuCores: 0, usedMemoryMB: 0, totalMemoryMB: 0, onlineUsers: 0 })
const dataFiles = ref([])

const loadConfig = async () => {
  loading.value = true
  try {
    const [statRes, sysRes] = await Promise.all([
      adminApi.getDashboardStats(),
      request.get('/stats/system')
    ])
    if (statRes.data) {
      config.value = { ...config.value, ...statRes.data }
    }
    if (sysRes.data) {
      systemStats.value = sysRes.data
    }
    dataFiles.value = [
      { name: 'users.json', records: statRes.data?.userCount || 0, size: '-' },
      { name: 'resumes.json', records: statRes.data?.resumeCount || 0, size: '-' },
      { name: 'jobs.json', records: statRes.data?.jobCount || 0, size: '-' },
      { name: 'skills.json', records: '-', size: '-' },
      { name: 'match_records.json', records: statRes.data?.matchCount || 0, size: '-' },
      { name: 'notifications.json', records: '-', size: '-' },
      { name: 'operation_logs.json', records: '-', size: '-' }
    ]
  } catch (_) {} finally { loading.value = false }
}

onMounted(() => loadConfig())
</script>

<style scoped>
.mini-stat { text-align: center; padding: 16px 8px; }
.mini-num { font-size: 32px; font-weight: bold; color: var(--accent-text); margin-bottom: 4px; }
.mini-label { font-size: 13px; color: var(--text-muted); }
</style>
