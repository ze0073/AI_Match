<template>
  <div class="job-detail glass-fade-in" v-loading="loading">
    <el-card v-if="job" class="glass-card">
      <template #header>
        <div class="card-header">
          <div>
            <h2 style="margin:0">{{ job.title }}</h2>
            <span style="color:var(--text-muted);font-size:14px">{{ job.company }} · {{ job.department || '' }} · {{ job.city }}</span>
          </div>
          <div style="display:flex;align-items:center;gap:10px">
            <el-tag size="large" :type="(matchScore||0)>=80?'success':(matchScore||0)>=60?'warning':'danger'">
              匹配度 {{ matchScore || '--' }}%
            </el-tag>
            <el-button v-if="!hasApplied" type="primary" @click="applyForJob" :loading="applying">立即申请</el-button>
            <el-tag v-else type="info">已申请</el-tag>
            <el-button type="success" @click="showRoadmap">学习路线</el-button>
          </div>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="薪资">{{ job.salary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学历要求">{{ job.education || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工作年限">{{ job.workYears ? job.workYears + '年' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="招聘人数">{{ job.headCount || '-' }}人</el-descriptions-item>
        <el-descriptions-item label="所属行业">{{ job.industry || '-' }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ job.department || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="section" v-if="job.description">
        <h3>职位描述</h3>
        <p style="white-space:pre-wrap;line-height:1.8">{{ job.description }}</p>
      </div>

      <div class="section" v-if="job.requirement">
        <h3>任职要求</h3>
        <p style="white-space:pre-wrap;line-height:1.8">{{ job.requirement }}</p>
      </div>

      <div class="section" v-if="skillList.length > 0">
        <h3>技能要求</h3>
        <div class="skill-cloud">
          <el-tag
            v-for="(skill, i) in skillList"
            :key="i"
            size="large"
            :type="skill.importance >= 4 ? 'danger' : skill.importance >= 3 ? 'warning' : ''"
            class="skill-tag-item"
          >
            <span class="skill-name">{{ skill.name }}</span>
            <span class="skill-level">
              <el-rate :model-value="skill.level || 1" disabled show-score text-color="#999" size="small" />
            </span>
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- Similar Jobs -->
    <el-card v-if="job" class="glass-card" style="margin-top:20px">
      <template #header><span>相似职位推荐</span></template>
      <div v-if="loadingSimilar">加载中...</div>
      <div v-else-if="similarJobs.length===0" style="color:#999;text-align:center;padding:20px">暂无相似职位</div>
      <el-row v-else :gutter="16">
        <el-col :xs="24" :sm="12" :md="8" v-for="sj in similarJobs" :key="sj.id" style="margin-bottom:12px">
          <div class="glass-card sim-job-card" @click="router.push('/jobseeker/jobs/'+sj.id)" style="padding:14px;cursor:pointer">
            <p style="font-weight:600;margin:0">{{ sj.title }}</p>
            <p style="font-size:13px;color:#aaa;margin:4px 0">{{ sj.company }} · {{ sj.city }} · {{ sj.salary }}</p>
            <el-progress :percentage="sj.matchScore||0" :stroke-width="6" :color="'#67C23A'" />
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- Learning Roadmap Dialog -->
    <el-dialog v-model="roadmapVisible" title="学习路线" width="700px" top="5vh">
      <div v-if="roadmapLoading" style="text-align:center;padding:40px">AI正在生成学习路线...</div>
      <div v-else-if="roadmap">
        <el-alert v-if="roadmap.gapSkills?.length" :title="'待提升技能：' + roadmap.gapSkills.join('、')" type="warning" :closable="false" style="margin-bottom:12px" />
        <el-alert v-if="roadmap.matchedSkills?.length" :title="'已掌握技能：' + roadmap.matchedSkills.join('、')" type="success" :closable="false" style="margin-bottom:16px" />
        <el-timeline v-if="roadmap.roadmap?.length">
          <el-timeline-item v-for="step in roadmap.roadmap" :key="step.step" :timestamp="step.duration" placement="top">
            <el-card shadow="hover">
              <p style="font-weight:600;margin:0">{{ step.step }}. {{ step.title }}</p>
              <p style="font-size:13px;color:#aaa;margin:4px 0">技能：{{ (step.skills||[]).join('、') }}</p>
              <p style="font-size:12px;color:#888;margin:0">资源：{{ (step.resources||[]).slice(0,3).join('、') }}</p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <p v-if="roadmap.totalDuration" style="text-align:center;color:#aaa;margin-top:12px">预计总时长：{{ roadmap.totalDuration }}</p>
      </div>
      <el-empty v-else description="暂无学习路线" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { jobApi, resumeApi } from '@/api'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const applying = ref(false)
const job = ref(null)
const matchScore = ref(0)
const hasApplied = ref(false)
const loadingSimilar = ref(false)
const similarJobs = ref([])
const roadmapVisible = ref(false)
const roadmapLoading = ref(false)
const roadmap = ref(null)

const skillList = computed(() => {
  if (!job.value?.skillRequirements) return []
  try {
    const raw = job.value.skillRequirements
    if (typeof raw === 'string') {
      return JSON.parse(raw)
    }
    if (Array.isArray(raw)) return raw
    return []
  } catch (_) { return [] }
})

const applyForJob = async () => {
  applying.value = true
  try {
    const capRes = await resumeApi.getCapability()
    const resumeId = capRes?.data?.resumeId
    if (!resumeId) { ElMessage.warning('请先上传简历'); applying.value = false; return }
    await request.post('/jobseeker/applications', { jobId: job.value.id, resumeId })
    hasApplied.value = true
    ElMessage.success('申请成功')
  } catch (_) {} finally { applying.value = false }
}

const showRoadmap = async () => {
  roadmapVisible.value = true
  roadmapLoading.value = true
  roadmap.value = null
  try {
    const res = await request.get('/interview/roadmap/' + route.params.id)
    roadmap.value = res.data
  } catch (_) { ElMessage.error('AI请求失败') }
  finally { roadmapLoading.value = false }
}

onMounted(async () => {
  loading.value = true
  const jobId = route.params.id
  try {
    const jobRes = await jobApi.getDetail(jobId)
    job.value = jobRes.data

    loadingSimilar.value = true
    try { const sr = await request.get('/job/' + jobId + '/similar'); similarJobs.value = sr.data || [] } catch (_) {} finally { loadingSimilar.value = false }

    const appsRes = await request.get('/jobseeker/applications').catch(() => null)
    if (appsRes?.data) {
      hasApplied.value = appsRes.data.some(a => a.jobId === Number(jobId))
    }
  } catch (_) {} finally { loading.value = false }
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.section { margin-top: 24px; }
.section h3 { margin-bottom: 12px; color: var(--text-primary); font-size: 16px; }
.skill-cloud { display: flex; flex-wrap: wrap; gap: 10px; }
.skill-tag-item { padding: 8px 14px; border-radius: 12px; display: flex; flex-direction: column; align-items: center; gap: 4px; min-width: 100px; }
.skill-name { font-weight: 600; font-size: 14px; }
.sim-job-card { transition: transform 0.2s; }
.sim-job-card:hover { transform: translateY(-2px); }
</style>