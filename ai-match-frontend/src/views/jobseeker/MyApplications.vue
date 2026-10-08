<template>
  <div class="my-applications glass-fade-in" v-loading="loading">
    <el-card>
      <template #header><span>求职记录</span></template>
      <el-tabs v-model="activeTab" @tab-change="filterByStatus">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="已申请" name="1" />
        <el-tab-pane label="面试中" name="2" />
        <el-tab-pane label="已录用" name="3" />
        <el-tab-pane label="未通过" name="4" />
      </el-tabs>

      <el-empty v-if="filteredList.length === 0" description="暂无求职记录">
        <el-button type="primary" @click="router.push('/jobseeker/recommend')">去看看推荐岗位</el-button>
      </el-empty>

      <el-table v-else :data="filteredList" stripe>
        <el-table-column prop="jobTitle" label="职位名称" />
        <el-table-column prop="company" label="公司" />
        <el-table-column prop="city" label="城市" />
        <el-table-column prop="salary" label="薪资" />
        <el-table-column label="匹配度" width="100">
          <template #default="{row}">
            <el-tag :type="row.matchScore > 80 ? 'success' : row.matchScore > 60 ? 'warning' : 'danger'">
              {{ row.matchScore }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{row}">
            <el-button size="small" type="primary" @click="viewDetail(row)">查看</el-button>
            <el-button v-if="row.status == 2" size="small" type="success" @click="goInterview(row)">面试准备</el-button>
            <el-button v-if="row.status == 1" size="small" type="danger" plain @click="withdrawApp(row)">撤回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const router = useRouter()
const loading = ref(false)
const applications = ref([])
const activeTab = ref('all')

const filteredList = computed(() => {
  if (activeTab.value === 'all') return applications.value
  return applications.value.filter(a => String(a.status) === activeTab.value)
})

const statusLabel = (s) => ['待处理', '已申请', '面试中', '已录用', '未通过'][s] || '未知'
const statusType = (s) => ['', 'warning', '', 'success', 'danger'][s] || 'info'

const filterByStatus = () => {}

const viewDetail = (row) => {
  router.push('/match/' + row.matchId)
}

const goInterview = (row) => {
  router.push('/jobseeker/interview?jobId=' + row.jobId + '&jobTitle=' + encodeURIComponent(row.jobTitle || ''))
}
const withdrawApp = async (row) => {
  try {
    await ElMessageBox.confirm('确定撤回该投递申请吗？', '确认', { type: 'warning' })
    await request.delete('/jobseeker/applications/' + row.matchId)
    ElMessage.success('已撤回')
    applications.value = applications.value.filter(a => a.matchId !== row.matchId)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('撤回失败')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await request.get('/jobseeker/applications')
    applications.value = res.data || []
  } catch (_) {} finally {
    loading.value = false
  }
})
</script>

<style scoped>
</style>