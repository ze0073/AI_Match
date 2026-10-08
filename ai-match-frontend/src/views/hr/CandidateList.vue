<template>
  <div class="candidates glass-fade-in">
    <el-card class="glass-card">
      <template #header>
        <div class="card-header">
          <span>候选人管理 - {{ jobTitle }}</span>
          <el-button type="primary" :loading="matching" @click="doMatch">重新AI匹配</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="AI匹配结果" name="ai" />
        <el-tab-pane label="主动投递" name="apply" />
      </el-tabs>

      <div v-show="activeTab === 'ai'">
        <el-table :data="candidates" stripe v-loading="loading" @selection-change="onSelectChange">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="userName" label="姓名" width="90" />
          <el-table-column prop="education" label="学历" width="70" />
          <el-table-column prop="workYears" label="经验" width="70" />
          <el-table-column prop="currentPosition" label="当前职位" min-width="130" />
          <el-table-column label="匹配度" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.matchScore>80?'success':scope.row.matchScore>60?'warning':'danger'">
                {{ scope.row.matchScore }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <el-button size="small" @click="viewDetail(scope.row)">查看报告</el-button>
              <el-button size="small" type="success" :loading="scope.row._inviting" @click="doInvite(scope.row)">邀约面试</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && candidates.length===0" description="暂无AI匹配结果" />
      </div>

      <div v-show="activeTab === 'apply'">
        <div style="margin-bottom:10px">
          <el-select v-model="appStatusFilter" placeholder="筛选状态" clearable size="small" style="width:120px">
            <el-option label="全部" value="" />
            <el-option label="已申请" value="1" />
            <el-option label="面试中" value="2" />
            <el-option label="已录用" value="3" />
            <el-option label="已拒绝" value="4" />
          </el-select>
        </div>
        <el-table :data="filteredApplications" stripe v-loading="appLoading">
          <el-table-column prop="name" label="姓名" width="90" />
          <el-table-column prop="education" label="学历" width="70" />
          <el-table-column prop="workYears" label="经验" width="70">
            <template #default="scope">{{ scope.row.workYears }}年</template>
          </el-table-column>
          <el-table-column prop="currentPosition" label="当前职位" min-width="120" />
          <el-table-column label="技能" min-width="150">
            <template #default="scope">
              <el-tag v-for="(s,i) in parseSkills(scope.row.skills)" :key="i" size="small" class="skill-tag">{{ s }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="scope">
              <el-select v-model="scope.row.status" size="small" @change="(val) => updateAppStatus(scope.row, val)">
                <el-option :value="1" label="已申请" />
                <el-option :value="2" label="面试中" />
                <el-option :value="3" label="已录用" />
                <el-option :value="4" label="已拒绝" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="scope">
              <el-button size="small" type="danger" plain @click="deleteApp(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!appLoading && applications.length===0" description="暂无主动投递" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { matchApi, jobApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const route = useRoute(); const router = useRouter()
const jobTitle = ref('')
const candidates = ref([])
const applications = ref([])
const loading = ref(false)
const appLoading = ref(false)
const matching = ref(false)
const activeTab = ref('ai')
const selectedCandidates = ref([])
const appStatusFilter = ref('')

function parseSkills(skills) {
  if (!skills) return []
  if (Array.isArray(skills)) return skills
  try { const p = JSON.parse(skills); return Array.isArray(p) ? p : [] } catch (_) { return String(skills).split(/[,,]/).filter(Boolean) }
}

const doMatch = async () => {
  matching.value = true
  try {
    const res = await matchApi.matchJobToCandidates(route.params.jobId)
    candidates.value = res.data?.results || []
    ElMessage.success('匹配完成')
  } catch (_) {} finally { matching.value = false }
}

const filteredApplications = computed(() => {
  if (!appStatusFilter.value) return applications.value
  return applications.value.filter(a => String(a.status) === appStatusFilter.value)
})

const loadApplications = async () => {
  appLoading.value = true
  try {
    const res = await request.get('/hr/applications')
    applications.value = (res.data || []).filter(a =>
      String(a.jobId) === String(route.params.jobId)
    )
  } catch (_) {} finally { appLoading.value = false }
}

const updateAppStatus = async (row, status) => {
  try {
    await request.put('/hr/applications/' + row.matchId + '/status', { status: status })
    ElMessage.success('状态已更新')
  } catch (_) { ElMessage.error('更新失败') }
}

const doInvite = async (row) => {
  row._inviting = true
  try {
    await request.post('/hr/invite', {
      resumeId: row.resumeId,
      jobId: Number(route.params.jobId),
      matchScore: row.matchScore || 0
    })
    ElMessage.success('邀约已发送')
  } catch (_) { ElMessage.error('邀约失败') } finally { row._inviting = false }
}

function onSelectChange(rows) { selectedCandidates.value = rows }
function clearSelection() { selectedCandidates.value = [] }
async function batchInvite() {
  try {
    await ElMessageBox.confirm(`确定批量邀约 ${selectedCandidates.value.length} 位候选人？`, '确认', { type: 'warning' })
    for (const row of selectedCandidates.value) {
      try { await request.post('/hr/invite', { resumeId: row.resumeId, jobId: Number(route.params.jobId), matchScore: row.matchScore || 0 }) } catch (_) {}
    }
    ElMessage.success('批量邀约已发送')
    clearSelection()
  } catch (_) {}
}
async function exportCandidates() {
  const items = activeTab.value === 'ai' ? candidates.value : applications.value
  if (!items.length) { ElMessage.warning('无数据可导出'); return }
  const csv = ['姓名,学历,经验,当前职位,匹配度,状态'].concat(
    items.map(r => [r.userName||r.name||'', r.education||'', r.workYears||'', r.currentPosition||'', r.matchScore||'', 
      ['','待处理','已邀约','已录用','已拒绝'][r.status]||''].join(','))
  ).join('\n')
  const blob = new Blob(['\uFEFF'+csv], {type:'text/csv;charset=UTF-8'})
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'candidates.csv'; a.click()
  ElMessage.success('导出成功')
}
const deleteApp = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该投递记录？', '提示', { type: 'warning' })
    await request.delete('/hr/applications/' + row.matchId)
    ElMessage.success('已删除')
    loadApplications()
  } catch (_) {}
}
const viewDetail = (row) => router.push('/match/' + row.matchId)

onMounted(async () => {
  loading.value = true
  try {
    const jobRes = await jobApi.getDetail(route.params.jobId)
    jobTitle.value = jobRes.data?.title || ''
    const matchRes = await matchApi.getRecommendationsForJob(route.params.jobId, 10)
    candidates.value = matchRes.data || []
  } catch (_) {} finally { loading.value = false }
  loadApplications()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.skill-tag { margin: 2px; }
</style>