<template>
  <div class="admin-dashboard glass-fade-in" v-loading="loading">
    <el-row :gutter="20">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.label">
        <el-card class="glass-card stat-card" shadow="never">
          <div class="stat-content">
            <el-icon :size="24" :color="card.color"><component :is="card.icon" /></el-icon>
            <div class="stat-info">
              <span class="stat-value">{{ card.value }}</span>
              <span class="stat-label">{{ card.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :xs="24" :md="12">
        <el-card class="glass-card">
          <template #header><span>用户角色分布</span></template>
          <div ref="roleChartRef" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="glass-card">
          <template #header><span>系统数据概览</span></template>
          <div ref="systemChartRef" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row style="margin-top:20px">
      <el-col :xs="24" :md="14">
        <el-card class="glass-card">
          <template #header><span>最近注册用户</span></template>
          <el-table :data="recentUsers" stripe size="small">
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="realName" label="真实姓名" />
            <el-table-column label="角色" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.userType==='ADMIN'?'danger':scope.row.userType==='HR'?'warning':'success'" size="small">
                  {{ scope.row.userType==='ADMIN'?'管理员':scope.row.userType==='HR'?'HR':'求职者' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column label="查看" width="80">
              <template #default="scope">
                <el-button type="primary" link size="small" @click="router.push('/admin/users')">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { UserFilled, Document, Briefcase, Connection } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api'
import request from '@/api/request'

const router = useRouter()
const loading = ref(true)
const recentUsers = ref([])
const stats = ref({ userCount: 0, resumeCount: 0, jobCount: 0, matchCount: 0 })
const systemStats = ref({ cpuCores: navigator.hardwareConcurrency || 4, usedMemoryMB: 0, totalMemoryMB: 0 })
const roleChartRef = ref(null)
const systemChartRef = ref(null)
let roleChart = null
let systemChart = null

const statCards = computed(() => [
  { label: '总用户数', value: stats.value.userCount, color: '#409EFF', icon: UserFilled },
  { label: '简历数', value: stats.value.resumeCount, color: '#67C23A', icon: Document },
  { label: '职位数', value: stats.value.jobCount, color: '#E6A23C', icon: Briefcase },
  { label: '匹配次数', value: stats.value.matchCount, color: '#F56C6C', icon: Connection }
])

function initRoleChart() {
  if (!roleChartRef.value) return
  if (roleChart) roleChart.dispose()
  roleChart = echarts.init(roleChartRef.value)
  roleChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: '0%', textStyle: { color: '#ccc' } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } },
      data: []
    }]
  })
}

function initSystemChart() {
  if (!systemChartRef.value) return
  if (systemChart) systemChart.dispose()
  systemChart = echarts.init(systemChartRef.value)
  systemChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['用户', '简历', '职位', '匹配'],
      axisLabel: { color: '#ccc' }
    },
    yAxis: { type: 'value', axisLabel: { color: '#ccc' } },
    series: [{
      type: 'bar',
      data: [0, 0, 0, 0],
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#409EFF' },
          { offset: 1, color: '#67C23A' }
        ])
      },
      barWidth: '50%'
    }]
  })
}

async function fetchData() {
  loading.value = true
  try {
    // Fetch stats
    const statsRes = await adminApi.getDashboardStats()
    if (statsRes.data) {
      stats.value = { ...statsRes.data }
    }

    // Fetch recent users
    const userRes = await adminApi.getUsers({ page: 1, size: 5 })
    if (userRes.data?.records) {
      recentUsers.value = userRes.data.records.slice(0, 5)
    } else if (userRes.data?.list) {
      recentUsers.value = userRes.data.list.slice(0, 5)
    }

    // Update role chart
    await nextTick()
    if (roleChart) {
      const allRes = await adminApi.getUsers({ page: 1, size: 1000 })
      const recs = allRes.data?.records || allRes.data?.list || []
      const jobseekerCount = recs.filter(u => u.userType === 'JOBSEEKER').length
      const hrCount = recs.filter(u => u.userType === 'HR').length
      const adminCount = recs.filter(u => u.userType === 'ADMIN').length
      roleChart.setOption({
        series: [{
          data: [
            { value: jobseekerCount, name: '求职者', itemStyle: { color: '#67C23A' } },
            { value: hrCount, name: 'HR', itemStyle: { color: '#409EFF' } },
            { value: adminCount, name: '管理员', itemStyle: { color: '#F56C6C' } }
          ].filter(d => d.value > 0)
        }]
      })
    }

        // Update system chart
    if (systemChart) {
      const s = stats.value
      systemChart.setOption({
        series: [{
          data: [
            s.userCount || 0,
            s.resumeCount || 0,
            s.jobCount || 0,
            s.matchCount || 0
          ]
        }]
      })
    }

    // Fetch system stats
    try {
      const sysRes = await request.get('/stats/system')
      if (sysRes.data) {
        systemStats.value = sysRes.data
      }
    } catch (_) {}
  } catch (e) {
    console.error('Failed to fetch admin stats:', e)
  } finally {
    loading.value = false
  }
}

function handleResize() {
  roleChart?.resize()
  systemChart?.resize()
}

onMounted(async () => {
  await nextTick()
  initRoleChart()
  initSystemChart()
  await fetchData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  roleChart?.dispose()
  systemChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.stat-card { text-align: center }
.stat-content { display: flex; align-items: center; gap: 12px; padding: 8px 0 }
.stat-info { display: flex; flex-direction: column; align-items: flex-start }
.stat-value { font-size: 28px; font-weight: 700; color: var(--text-primary) }
.stat-label { font-size: 13px; color: #666; margin-top: 2px }
</style>
