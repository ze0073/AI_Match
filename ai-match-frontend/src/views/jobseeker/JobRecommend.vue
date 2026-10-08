<template>
  <div class="job-recommend glass-fade-in">
    <el-card>
      <template #header><div class="card-header"><span>智能岗位推荐</span><el-button type="primary" :loading="matching" @click="doMatch">重新匹配</el-button></div></template>
      <el-table :data="jobList" stripe v-loading="loading">
        <el-table-column prop="jobTitle" label="职位名称" />
        <el-table-column prop="company" label="公司" />
        <el-table-column prop="salary" label="薪资" width="120" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="matchScore" label="匹配度" width="120">
          <template #default="{row}"><el-tag :type="scoreType(row.matchScore)">{{ row.matchScore }}%</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{row}">
            <el-button size="small" @click="viewJob(row)">职位详情</el-button>
            <el-button size="small" type="primary" @click="applyJob(row)">投递</el-button>
            <el-button size="small" type="success" @click="viewMatch(row)">匹配报告</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { matchApi, resumeApi } from '@/api'
import request from '@/api/request'
import { ElMessage } from 'element-plus'
const router = useRouter()
const jobList = ref([])
const loading = ref(false)
const matching = ref(false)
const scoreType = (s) => s > 80 ? 'success' : s > 60 ? 'warning' : 'danger'
const viewJob = (row) => router.push(`/jobseeker/jobs/${row.jobId}`)
const viewMatch = (row) => router.push(`/match/${row.matchId}`)
const applyJob = async (row) => { try { await request.post('/jobseeker/applications', { jobId: row.jobId, matchId: row.matchId }); ElMessage.success('投递成功') } catch (_) {} }
const doMatch = async () => { matching.value = true; try { const cap = await resumeApi.getCapability(); const resumeId = cap.data?.resumeId; if (!resumeId) { ElMessage.warning('请先上传简历'); matching.value = false; return } jobList.value = (await matchApi.matchPersonToJobs(resumeId)).data?.results || [] } catch (_) {} finally { matching.value = false } }
onMounted(async () => { loading.value = true; try { jobList.value = (await matchApi.getRecommendations(10)).data || [] } catch (_) {} finally { loading.value = false } })
</script>
<style scoped>.card-header{display:flex;justify-content:space-between;align-items:center}</style>
