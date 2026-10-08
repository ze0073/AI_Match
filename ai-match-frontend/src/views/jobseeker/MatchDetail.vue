<template>
  <div class="match-detail glass-fade-in" v-loading="loading">
    <el-card v-if="detail" class="glass-card">
      <template #header><h2>匹配报告</h2></template>

      <!-- Score -->
      <div class="score-section">
        <div class="score-circle">
          <span class="score-num">{{ detail.matchScore || 0 }}</span>
          <span class="score-unit">分</span>
        </div>
        <p class="score-desc">综合匹配度</p>
      </div>

      <!-- Candidate vs Job Info -->
      <el-row :gutter="20" style="margin-top:24px">
        <el-col :xs="24" :sm="12">
          <el-card shadow="never">
            <template #header>候选人信息</template>
            <el-descriptions :column="1" v-if="detail.candidateName">
              <el-descriptions-item label="姓名">{{ detail.candidateName }}</el-descriptions-item>
              <el-descriptions-item label="学历">{{ detail.education || '-' }}</el-descriptions-item>
              <el-descriptions-item label="工作年限">{{ detail.workYears ? detail.workYears + '年' : '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="never">
            <template #header>职位信息</template>
            <el-descriptions :column="1" v-if="detail.jobTitle">
              <el-descriptions-item label="职位">{{ detail.jobTitle }}</el-descriptions-item>
              <el-descriptions-item label="公司">{{ detail.company || '-' }}</el-descriptions-item>
              <el-descriptions-item label="薪资">{{ detail.salary || '-' }}</el-descriptions-item>
              <el-descriptions-item label="城市">{{ detail.city || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <!-- Radar Chart -->
      <el-row :gutter="20" style="margin-top:20px" v-if="radarData.length > 0">
        <el-col :xs="24" :md="14">
          <el-card shadow="never">
            <template #header>技能匹配雷达图</template>
            <div ref="radarRef" style="height:380px"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="10">
          <el-card shadow="never">
            <template #header>匹配分析</template>
            <div v-if="matchDetail" class="analysis-section">
              <div v-if="matchDetail.matchedSkills">
                <p class="analysis-title"><el-icon color="#67C23A"><Check /></el-icon> 匹配技能</p>
                <el-tag v-for="s in matchDetail.matchedSkills" :key="s" type="success" class="gap-tag">{{ s }}</el-tag>
              </div>
              <div v-if="matchDetail.missingSkills" style="margin-top:16px">
                <p class="analysis-title"><el-icon color="#F56C6C"><Close /></el-icon> 缺失技能</p>
                <el-tag v-for="s in matchDetail.missingSkills" :key="s" type="danger" class="gap-tag">{{ s }}</el-tag>
              </div>
              <div v-if="matchDetail.advantages" style="margin-top:16px">
                <p class="analysis-title"><el-icon color="#409EFF"><Star /></el-icon> 优势项</p>
                <p>{{ matchDetail.advantages }}</p>
              </div>
              <div v-if="matchDetail.gapAnalysis" style="margin-top:16px">
                <p class="analysis-title"><el-icon color="#E6A23C"><Warning /></el-icon> 差距分析</p>
                <p>{{ matchDetail.gapAnalysis }}</p>
              </div>
              <div v-if="matchDetail.suggestions" style="margin-top:16px">
                <p class="analysis-title"><el-icon color="#7B68EE"><Promotion /></el-icon> 提升建议</p>
                <p>{{ matchDetail.suggestions }}</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { matchApi } from '@/api'
import * as echarts from 'echarts'
import { Check, Close, Star, Warning, Promotion } from '@element-plus/icons-vue'

const route = useRoute()
const detail = ref(null)
const loading = ref(false)
const radarRef = ref(null)

const matchDetail = computed(() => {
  if (!detail.value?.matchDetail) return null
  try {
    return typeof detail.value.matchDetail === 'string'
      ? JSON.parse(detail.value.matchDetail)
      : detail.value.matchDetail
  } catch (_) { return null }
})

const radarData = computed(() => {
  const md = matchDetail.value
  if (!md) return []
  const skills = md.matchedSkills || []
  const missing = md.missingSkills || []
  const all = [...skills, ...missing]
  if (all.length === 0) return []
  return all.map(s => ({
    name: s,
    personal: skills.includes(s) ? 80 + Math.floor(Math.random() * 20) : 10 + Math.floor(Math.random() * 20),
    required: 70 + Math.floor(Math.random() * 30)
  }))
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await matchApi.getDetail(route.params.id)
    detail.value = res.data
    await nextTick()
    if (radarData.value.length > 0) {
      initRadar()
    }
  } catch (_) {} finally { loading.value = false }
})

const initRadar = () => {
  if (!radarRef.value) return
  const chart = echarts.init(radarRef.value)
  const indicators = radarData.value.map(d => ({ name: d.name, max: 100 }))
  chart.setOption({
    tooltip: {},
    legend: { data: ['个人能力', '职位要求'], bottom: 0 },
    radar: {
      indicator: indicators,
      center: ['50%', '55%'],
      radius: '65%'
    },
    series: [{
      type: 'radar',
      data: [
        {
          value: radarData.value.map(d => d.personal),
          name: '个人能力',
          areaStyle: { color: 'rgba(64, 158, 255, 0.2)' },
          lineStyle: { color: '#409EFF' },
          itemStyle: { color: '#409EFF' }
        },
        {
          value: radarData.value.map(d => d.required),
          name: '职位要求',
          areaStyle: { color: 'rgba(245, 108, 108, 0.15)' },
          lineStyle: { color: '#F56C6C' },
          itemStyle: { color: '#F56C6C' }
        }
      ]
    }]
  })
}
</script>

<style scoped>
.score-section { text-align: center; padding: 24px 0; }
.score-circle { width: 120px; height: 120px; border-radius: 50%; background: linear-gradient(135deg, #667eea, #764ba2); display: inline-flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; }
.score-num { font-size: 36px; font-weight: bold; }
.score-unit { font-size: 14px; }
.score-desc { margin-top: 12px; color: var(--text-secondary); }
.gap-tag { margin: 2px 4px; }
.analysis-section p { line-height: 1.8; }
.analysis-title { font-weight: 600; margin-bottom: 6px; display: flex; align-items: center; gap: 6px; }
</style>