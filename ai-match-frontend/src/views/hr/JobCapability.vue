<template>
  <div class="job-graph glass-fade-in">
    <el-card class="glass-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
          <span>职位能力图谱</span>
          <el-select v-model="selectedJobId" placeholder="选择职位" @change="loadGraph" filterable style="width:280px">
            <el-option v-for="j in jobList" :key="j.id" :label="(j.title||'未知职位') + ' - ' + (j.company||'未知公司')" :value="j.id" />
          </el-select>
        </div>
      </template>
      <div v-if="!selectedJobId" style="padding:60px;text-align:center;color:#9090a0">请先选择一个职位</div>
      <div v-else-if="loading" style="padding:60px;text-align:center;color:#9090a0">加载中...</div>
      <div v-else-if="errorMsg" style="padding:20px;text-align:center;color:#f56c6c">{{ errorMsg }}</div>
      <div v-else-if="!hasData" style="padding:60px;text-align:center;color:#9090a0">该职位暂无图谱数据</div>
      <div v-else ref="chartRef" style="height:500px;width:100%"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '@/api/request'

const chartRef = ref(null)
const loading = ref(false)
const errorMsg = ref('')
const hasData = ref(false)
const selectedJobId = ref(null)
const jobList = ref([])
let chart = null

const loadJobs = async () => {
  try {
    const res = await request.get('/job/search', { params: { page: 1, size: 100 } })
    jobList.value = res.data?.records || []
  } catch (_) {}
}

const loadGraph = async () => {
  if (!selectedJobId.value) return
  loading.value = true
  errorMsg.value = ''
  hasData.value = false
  if (chart) { chart.dispose(); chart = null }
  try {
    const res = await request.post('/graph/build/job/' + selectedJobId.value)
    const data = res.data
    if (data && data.nodes && data.nodes.length > 0) {
      hasData.value = true
      await nextTick()
      await nextTick()
      setTimeout(() => renderGraph(data), 100)
    }
  } catch (e) {
    errorMsg.value = '加载失败: ' + (e.response?.data?.message || e.message || '未知错误')
  } finally {
    loading.value = false
  }
}

const renderGraph = (data) => {
  const dom = chartRef.value
  if (!dom || dom.clientHeight === 0) { setTimeout(() => renderGraph(data), 200); return }
  chart = echarts.init(dom)
  const categories = [
    { name: '职位', itemStyle: { color: '#F56C6C' } },
    { name: '技能', itemStyle: { color: '#67C23A' } },
    { name: '技能分类', itemStyle: { color: '#E6A23C' } }
  ]
  const groupMap = { job: 0, skill: 1, skill_category: 2 }
  const nodes = data.nodes.map(n => ({ ...n, name: n.label || n.name || n.id, category: groupMap[n.group] ?? 1, symbolSize: n.symbolSize || 25 }))
  const links = (data.edges || []).map(e => ({ source: e.source, target: e.target, label: { show: true, fontSize: 9, formatter: e.label }, lineStyle: { color: '#888', curveness: 0.2 } }))
  chart.setOption({
    tooltip: { formatter: p => p.dataType === 'edge' ? (p.data.label || '') : '<b>' + p.name + '</b>' },
    legend: [{ data: categories.map(c => c.name), bottom: 10, textStyle: { color: '#aaa' } }],
    series: [{
      type: 'graph', layout: 'force', force: { repulsion: 300, edgeLength: [60, 200] },
      roam: true, draggable: true, data: nodes, categories, links,
      label: { show: true, fontSize: 11, color: '#ddd' },
      emphasis: { focus: 'adjacency', lineStyle: { width: 3 } },
      lineStyle: { color: '#888', curveness: 0.3 }
    }]
  })
}

onMounted(() => loadJobs())
</script>

<style scoped>
</style>
