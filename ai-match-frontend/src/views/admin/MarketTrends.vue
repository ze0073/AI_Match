<template>
  <div class="market-trends glass-fade-in" v-loading="loading">
    <!-- Stats Cards -->
    <el-row :gutter="16">
      <el-col :xs="12" :sm="4" v-for="card in statCards" :key="card.label">
        <el-card class="glass-card mini-card" shadow="never">
          <p class="mini-num">{{ card.value }}</p>
          <p class="mini-label">{{ card.label }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row 1 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :xs="24" :md="12">
        <el-card class="glass-card">
          <template #header><span>职位城市分布</span></template>
          <div ref="cityChartRef" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="glass-card">
          <template #header><span>求职者学历分布</span></template>
          <div ref="eduChartRef" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row 2 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :xs="24" :md="12">
        <el-card class="glass-card">
          <template #header><span>技能需求热度排行</span></template>
          <div ref="skillChartRef" style="height:320px"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="glass-card">
          <template #header><span>薪资范围分布</span></template>
          <div ref="salaryChartRef" style="height:320px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row 3 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card class="glass-card">
          <template #header><span>投递趋势</span></template>
          <div ref="trendChartRef" style="height:280px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const loading = ref(false)
const overview = ref({})

const cityChartRef = ref(null)
const eduChartRef = ref(null)
const skillChartRef = ref(null)
const salaryChartRef = ref(null)
const trendChartRef = ref(null)

let charts = []

const statCards = computed(() => [
  { label: '注册用户', value: overview.value.totalUsers || 0 },
  { label: '在招职位', value: overview.value.totalJobs || 0 },
  { label: '简历数', value: overview.value.totalResumes || 0 },
  { label: '匹配次数', value: overview.value.totalMatches || 0 },
  { label: '平均匹配度', value: (overview.value.avgMatchScore || 0) + '%' },
  { label: '已录用', value: overview.value.hiredCount || 0 }
])

function initChart(refEl, option) {
  if (!refEl.value) return
  const chart = echarts.init(refEl.value)
  chart.setOption(option)
  charts.push(chart)
  return chart
}

async function loadData() {
  loading.value = true
  try {
    overview.value = (await request.get('/market/overview')).data || {}

    await nextTick()

    // City distribution
    const cityData = (await request.get('/market/city-distribution')).data || []
    initChart(cityChartRef, {
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '10%', bottom: '3%', containLabel: true },
      xAxis: { type: 'value', axisLabel: { color: '#999' } },
      yAxis: { type: 'category', data: cityData.map(d => d.name).reverse(), axisLabel: { color: '#ccc', fontSize: 11 }, inverse: true },
      series: [{
        type: 'bar',
        data: cityData.map(d => d.value).reverse(),
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#764ba2' }]) },
        barWidth: '60%'
      }]
    })

    // Education distribution
    const eduData = (await request.get('/market/education-distribution')).data || []
    initChart(eduChartRef, {
      tooltip: { trigger: 'item' },
      legend: { bottom: '0%', textStyle: { color: '#ccc' } },
      series: [{
        type: 'pie',
        radius: ['45%', '72%'],
        center: ['50%', '42%'],
        data: eduData.map(d => ({ value: d.value, name: d.name })),
        label: { color: '#ddd', fontSize: 11 },
        emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' } }
      }]
    })

    // Skill ranking
    const skillData = (await request.get('/market/skill-ranking')).data || []
    initChart(skillChartRef, {
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '8%', bottom: '3%', containLabel: true },
      xAxis: { type: 'value', axisLabel: { color: '#999' } },
      yAxis: { type: 'category', data: skillData.slice(0, 12).map(d => d.name).reverse(), axisLabel: { color: '#ccc', fontSize: 11 }, inverse: true },
      series: [{
        type: 'bar',
        data: skillData.slice(0, 12).map(d => d.value).reverse(),
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#f093fb' }, { offset: 1, color: '#f5576c' }]) },
        barWidth: '60%'
      }]
    })

    // Salary distribution
    const salaryData = (await request.get('/market/salary-distribution')).data || []
    initChart(salaryChartRef, {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        roseType: 'radius',
        data: salaryData.map(d => ({ value: d.value, name: d.name })),
        label: { color: '#ddd', fontSize: 11 }
      }]
    })

    // Application trends
    const trendData = (await request.get('/market/application-trends')).data || []
    initChart(trendChartRef, {
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: trendData.map(d => d.date), axisLabel: { color: '#999', rotate: 30, fontSize: 10 } },
      yAxis: { type: 'value', axisLabel: { color: '#999' } },
      series: [{
        type: 'line',
        data: trendData.map(d => d.count),
        smooth: true,
        lineStyle: { color: '#667eea', width: 2 },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(102,126,234,0.3)' }, { offset: 1, color: 'rgba(102,126,234,0.02)' }]) },
        itemStyle: { color: '#667eea' }
      }]
    })

  } catch (_) {} finally { loading.value = false }
}

function handleResize() { charts.forEach(c => c?.resize()) }

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  charts.forEach(c => c?.dispose())
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.mini-card { text-align: center; }
.mini-num { font-size: 28px; font-weight: 700; color: var(--accent-text); margin-bottom: 4px; }
.mini-label { font-size: 12px; color: var(--text-muted); }
</style>