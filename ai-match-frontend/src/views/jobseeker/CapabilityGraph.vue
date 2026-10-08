<template>
  <div class="capability-graph glass-fade-in">
    <el-card class="glass-card">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px">
          <span>个人能力图谱</span>
          <div style="display:flex;gap:6px;align-items:center;flex-wrap:wrap">
            <el-select v-model="layoutMode" size="small" style="width:100px" @change="reloadGraph">
              <el-option label="力导向" value="force"/>
              <el-option label="环形" value="circular"/>
              <el-option label="树形" value="tree"/>
            </el-select>
            <el-select v-model="theme" size="small" style="width:80px" @change="reloadGraph">
              <el-option label="默认" value="default"/>
              <el-option label="暖色" value="warm"/>
              <el-option label="冷色" value="cool"/>
              <el-option label="赛博" value="cyber"/>
              <el-option label="自然" value="nature"/>
            </el-select>
            <el-select v-model="nodeShape" size="small" style="width:80px" @change="reloadGraph">
              <el-option label="圆形" value="circle"/>
              <el-option label="圆角" value="roundRect"/>
              <el-option label="菱形" value="diamond"/>
            </el-select>
            <el-select v-model="chartMode" size="small" style="width:80px" @change="reloadGraph">
              <el-option label="图谱" value="graph"/>
              <el-option label="饼图" value="pie"/>
            </el-select>
            <el-tooltip content="流动粒子效果">
              <el-switch v-model="showParticles" size="small" @change="reloadGraph" v-if="chartMode==='graph'"/>
            </el-tooltip>
            <el-button size="small" :icon="FullScreen" circle @click="toggleFullscreen"/>
            <el-button size="small" @click="loadGraph" :loading="loading">刷新</el-button>
          </div>
        </div>
      </template>
      <div v-if="errorMsg" style="padding:20px;text-align:center;color:#f56c6c">{{ errorMsg }}</div>
      <div v-else-if="loading" style="padding:60px;text-align:center;color:#9090a0">加载中...</div>
      <div v-else-if="!hasData" style="padding:60px;text-align:center">
        <p style="color:#9090a0;font-size:15px;margin-bottom:16px">暂无能力图谱数据，请先上传简历</p>
        <el-button type="primary" @click="$router.push('/jobseeker/resume')">去上传简历</el-button>
      </div>
      <div v-else ref="graphWrap" :class="['graph-wrap', { fullscreen: isFullscreen }]">
        <div v-show="chartMode==='graph'" ref="chartRef" class="graph-inner"></div>
        <div v-show="chartMode==='pie'" ref="pieRef" class="graph-inner"></div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { FullScreen } from '@element-plus/icons-vue'
import { graphApi } from '@/api'

const chartRef = ref(null)
const graphWrap = ref(null)
const loading = ref(false)
const errorMsg = ref('')
const hasData = ref(false)
const isFullscreen = ref(false)
const layoutMode = ref('force')
const theme = ref('default')
const nodeShape = ref('circle')
const showParticles = ref(false)
const chartMode = ref('graph')
const pieRef = ref(null)
let chart = null
let cachedData = null

const THEMES = {
  default: { person:'#409EFF', skill:'#67C23A', skill_category:'#E6A23C', job:'#F56C6C', missing_skill:'#F56C6C', edge:'#555', bg:'#1a1a2e' },
  warm:    { person:'#E85D75', skill:'#F4A261', skill_category:'#E76F51', job:'#D62828', missing_skill:'#C1121F', edge:'#B5838D', bg:'#2E1A1A' },
  cool:    { person:'#4A90D9', skill:'#6C5CE7', skill_category:'#A78BFA', job:'#00B4D8', missing_skill:'#0096C7', edge:'#6B7B8D', bg:'#1A1A2E' },
  cyber:   { person:'#00FFFF', skill:'#FF00FF', skill_category:'#FFFF00', job:'#00FF00', missing_skill:'#FF3333', edge:'#0FF', bg:'#0D0D1A' },
  nature:  { person:'#2D6A4F', skill:'#52B788', skill_category:'#95D5B2', job:'#D4A373', missing_skill:'#BC4749', edge:'#8C9B7D', bg:'#1B2A1B' }
}

const loadGraph = async () => {
  loading.value = true; errorMsg.value = ''; hasData.value = false
  if (chart) { chart.dispose(); chart = null }
  try {
    const res = await graphApi.getMyGraph()
    const data = res.data
    if (data && data.nodes && data.nodes.length > 0) {
      cachedData = data; hasData.value = true
      await nextTick(); await nextTick()
      setTimeout(() => renderGraph(data), 100)
    }
  } catch (e) { errorMsg.value = '加载失败: ' + (e.message || '') }
  finally { loading.value = false }
}


const toggleFullscreen = () => { isFullscreen.value = !isFullscreen.value; setTimeout(() => { chart?.resize() }, 300) }

const renderGraph = (data) => {
  const dom = chartRef.value
  if (!dom || dom.clientHeight === 0) { setTimeout(() => renderGraph(data), 200); return }
  chart = echarts.init(dom)
  const t = THEMES[theme.value] || THEMES.default

  const categories = [
    { name: '个人', itemStyle: { color: t.person } },
    { name: '技能', itemStyle: { color: t.skill } },
    { name: '技能分类', itemStyle: { color: t.skill_category } },
    { name: '职位', itemStyle: { color: t.job } },
    { name: '缺失技能', itemStyle: { color: t.missing_skill } }
  ]
  const groupMap = { person: 0, skill: 1, skill_category: 2, job: 3, missing_skill: 4 }
  const shapeMap = { circle: 'circle', roundRect: 'roundRect', diamond: 'diamond' }

  const nodes = data.nodes.map(n => ({
    name: n.label || n.name || n.id,
    id: n.id,
    category: groupMap[n.group] ?? 1,
    symbolSize: n.symbolSize || 25,
    symbol: shapeMap[nodeShape.value] || 'circle'
  }))
  const links = (data.edges || []).map(e => ({
    source: e.source, target: e.target,
    lineStyle: { color: t.edge, curveness: 0.2, width: showParticles.value ? 1.5 : 1 }
  }))

  const isTree = layoutMode.value === 'tree'
  const isCircular = layoutMode.value === 'circular'

  let series = {
    type: 'graph',
    layout: isTree ? 'none' : (isCircular ? 'circular' : 'force'),
    circular: isCircular ? { rotateLabel: true } : undefined,
    force: (!isTree && !isCircular) ? { repulsion: 300, edgeLength: [80, 200], gravity: 0.1 } : undefined,
    roam: true, draggable: true,
    data: nodes, categories, links,
    label: { show: true, fontSize: 11, color: t.person === '#00FFFF' ? '#fff' : '#ddd' },
    emphasis: { focus: 'adjacency', lineStyle: { width: 3 } },
    lineStyle: { color: t.edge, curveness: 0.3, opacity: 0.6 },
    scaleLimit: { min: 0.2, max: 5 },
    animation: true, animationDuration: 1000, animationEasingUpdate: 'cubicInOut'
  }

  if (showParticles.value) {
    series = {
      ...series,
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: 8,
      lineStyle: { ...series.lineStyle, width: 1.5 },
      edges: links
    }
  }

  if (isTree) {
    const rootNode = nodes.find(n => n.category === 0)
    const childLinks = []
    const visited = new Set()
    const edges = data.edges || []
    function buildTree(nodeId) {
      visited.add(nodeId)
      edges.forEach(e => {
        const targetId = e.source === nodeId ? e.target : (e.target === nodeId ? e.source : null)
        if (targetId && !visited.has(targetId)) {
          childLinks.push({ source: nodeId, target: targetId, lineStyle: { color: t.edge } })
          buildTree(targetId)
        }
      })
    }
    if (rootNode) buildTree(rootNode.id)
    series.links = childLinks
    series.layout = 'force'
    series.force = { repulsion: 200, edgeLength: [60, 160], gravity: 0.05, center: [200, 250] }
  }

  chart.setOption({
    backgroundColor: t.bg,
    tooltip: { formatter: p => p.dataType === 'edge' ? '' : '<b>' + p.name + '</b>', backgroundColor: 'rgba(0,0,0,0.7)', borderColor: t.edge, textStyle: { color: '#ddd' } },
    legend: { data: categories.map(c => c.name), bottom: 10, textStyle: { color: '#aaa' } },
    series: [series]
  })
}

const renderPie = (data) => {
  const dom = pieRef.value
  if (!dom || dom.clientHeight === 0) { setTimeout(() => renderPie(data), 200); return }
  if (chart) { chart.dispose(); chart = null }
  chart = echarts.init(dom)
  const t = THEMES[theme.value] || THEMES.default
  // Group skills by category or just show as list
  const cnMap = { programming: '编程语言', framework: '框架', database: '数据库', devops: '运维工具', middleware: '中间件', soft: '软技能', skill: '技能', skill_category: '技能分类' }
  const skillNodes = data.nodes.filter(n => n.group === 'skill' || n.group === 'skill_category')
  const catCount = {}
  skillNodes.forEach(n => {
    const cat = cnMap[n.category] || cnMap[n.group] || n.category || '其他'
    catCount[cat] = (catCount[cat] || 0) + 1
  })
  let pieData
  if (Object.keys(catCount).length <= 1) {
    pieData = skillNodes.map(n => ({ name: n.label || n.name, value: 1 }))
  } else {
    pieData = Object.entries(catCount).map(([k, v]) => ({ name: k, value: v }))
  }
  // 3D-like stacked pie slices
  const depth = 12
  const pieSeries = []
  for (let d = 0; d < depth; d++) {
    pieSeries.push({
      type: 'pie',
      radius: [(38 + d * 0.1) + '%', (68 + d * 0.1) + '%'],
      center: ['50%', (48 - d * 0.3) + '%'],
      roseType: 'radius',
      silent: d > 0,
      label: { show: d === 0, color: '#fff', fontSize: 12, formatter: '{b}\n{d}%' },
      labelLine: { show: d === 0 },
      emphasis: { disabled: d > 0, label: { fontSize: 18, fontWeight: 'bold' }, scaleSize: 10 },
      itemStyle: {
        borderRadius: 6,
        borderColor: d === 0 ? t.bg : 'transparent',
        borderWidth: 2,
        opacity: d === 0 ? 1 : 0.04
      },
      data: pieData,
      z: depth - d
    })
  }
  chart.setOption({
    backgroundColor: t.bg,
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 10, textStyle: { color: '#aaa' } },
    series: pieSeries
  })
}

const reloadGraph = () => {
  if (!cachedData) return
  if (chart) { chart.dispose(); chart = null }
  setTimeout(() => {
    if (chartMode.value === 'pie') renderPie(cachedData)
    else renderGraph(cachedData)
  }, 50)
}

onMounted(() => { loadGraph() })
onUnmounted(() => { chart?.dispose() })
</script>

<style scoped>
.graph-wrap { width: 100%; height: 520px; transition: all 0.3s; }
.graph-wrap.fullscreen { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; z-index: 9999; background: #1a1a2e; padding: 20px; }
.graph-inner { width: 100%; height: 100%; }
</style>
