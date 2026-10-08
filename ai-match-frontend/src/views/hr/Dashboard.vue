<template>
  <div class="hr-dashboard glass-fade-in" v-loading="loading">
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

    <!-- Charts Row -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :xs="24" :md="14">
        <el-card class="glass-card">
          <template #header><span>最近发布的职位</span></template>
          <el-table :data="recentJobs" stripe size="small" v-if="recentJobs.length > 0">
            <el-table-column prop="title" label="职位" />
            <el-table-column prop="department" label="部门" />
            <el-table-column prop="city" label="城市" />
            <el-table-column prop="salary" label="薪资" />
            <el-table-column label="招聘人数" width="90"><template #default="scope">{{ scope.row.headCount || '若干' }}</template></el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="scope">
                <el-button size="small" @click="viewCandidates(scope.row)">查看候选人</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无职位" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card class="glass-card">
          <template #header><span>招聘漏斗</span></template>
          <div ref="funnelChartRef" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Quick Actions -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card class="glass-card">
          <template #header><span>快捷操作</span></template>
          <div class="quick-actions">
            <el-button type="primary" size="large" @click="router.push('/hr/jobs/upload')"><el-icon><Upload /></el-icon>上传JD</el-button>
            <el-button type="success" size="large" @click="router.push('/hr/jobs')"><el-icon><Briefcase /></el-icon>职位管理</el-button>
            <el-button type="warning" size="large" @click="router.push('/hr/talent-pool')"><el-icon><Collection /></el-icon>人才库</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { jobApi } from '@/api'
import request from '@/api/request'
import * as echarts from 'echarts'
import { Briefcase, User, Document, Check, Upload, Collection } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const jobCount = ref(0)
const candidateCount = ref(0)
const resumeCount = ref(0)
const interviewCount = ref(0)
const recentJobs = ref([])
const funnelChartRef = ref(null)
let funnelChart = null

const statCards = computed(() => [
  { label: '发布职位', value: jobCount.value, color: '#409EFF', icon: Briefcase },
  { label: '匹配候选人', value: candidateCount.value, color: '#67C23A', icon: User },
  { label: '简历总数', value: resumeCount.value, color: '#E6A23C', icon: Document },
  { label: '面试邀约', value: interviewCount.value, color: '#909399', icon: Check }
])

const viewCandidates = (row) => router.push('/hr/candidates/' + row.id)

onMounted(async () => {
  loading.value = true
  try {
    const jobsRes = await jobApi.search({ page: 1, size: 100 })
    const allJobs = jobsRes.data?.records || []
    jobCount.value = allJobs.length
    recentJobs.value = allJobs.slice(0, 5)
    try { const rRes = await request.get("/stats/resume-count"); resumeCount.value = rRes.data || 0 } catch (_) { resumeCount.value = 0 }

    let totalCandidates = 0
    for (const job of allJobs.slice(0, 10)) {
      try {
        const matchRes = await request.get('/match/recommendations/job/' + job.id + '?limit=100')
        const candidates = matchRes.data || []
        totalCandidates += candidates.length
      } catch (_) {}
    }
    candidateCount.value = totalCandidates

    await nextTick()
    initFunnelChart()
  } catch (_) {} finally { loading.value = false }
  window.addEventListener('resize', () => funnelChart?.resize())
})

const initFunnelChart = async () => {
  if (!funnelChartRef.value) return
  funnelChart = echarts.init(funnelChartRef.value)
  try {
    const res = await request.get('/stats/funnel')
    const data = res.data || {}
    const funnelData = [
      { name: '投递', value: data.applied || 0 },
      { name: '面试', value: data.interviewing || 0 },
      { name: '录用', value: data.hired || 0 }
    ]
    if (funnelData.reduce((s, i) => s + i.value, 0) === 0) {
      funnelChart.setOption({
        title: { text: '暂无招聘数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } }
      })
      return
    }
    funnelChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 人' },
      series: [{
        type: 'funnel',
        left: '15%', top: 10, bottom: 10, width: '70%',
        min: 0, max: Math.max(...funnelData.map(d => d.value), 1),
        sort: 'descending', gap: 2,
        label: { show: true, position: 'inside', formatter: '{b} {c}', color: '#fff', fontSize: 13 },
        labelLine: { show: false },
        itemStyle: { borderColor: '#1a1a2e', borderWidth: 2 },
        data: funnelData
      }]
    })
  } catch (_) {
    funnelChart.setOption({
      title: { text: '加载失败', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } }
    })
  }
}
</script>

<style scoped>
.stat-card-item { transition: all 0.3s ease; }
.stat-card-item:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.stat-card-inner { display: flex; align-items: center; gap: 16px; }
.stat-num { font-size: 28px; font-weight: bold; color: var(--text-primary); margin: 0; }
.stat-label { color: var(--text-muted); margin-top: 4px; font-size: 13px; }
.quick-actions { display: flex; gap: 16px; flex-wrap: wrap; }
</style>