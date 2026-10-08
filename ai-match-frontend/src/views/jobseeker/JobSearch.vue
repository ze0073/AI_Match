<template>
  <div class="job-search glass-fade-in">
    <el-card class="glass-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item><el-input v-model="searchForm.keyword" placeholder="职位/公司关键词" clearable /></el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.city" placeholder="城市" clearable>
            <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.education" placeholder="学历" clearable>
            <el-option label="本科" value="本科" /><el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" /><el-option label="大专" value="大专" />
          </el-select>
        </el-form-item>
        <el-form-item label="薪资(K)">
          <el-slider v-model="salaryRange" range :min="0" :max="50" :step="2" style="width:180px" />
          <span style="margin-left:8px;font-size:13px;color:#aaa">{{ salaryRange[0] }}K - {{ salaryRange[1] }}K</span>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">搜索</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card class="glass-card" style="margin-top:16px">
      <el-table :data="jobList" stripe v-loading="loading">
        <el-table-column prop="title" label="职位名称" />
        <el-table-column prop="company" label="公司" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="salary" label="薪资" width="120" />
        <el-table-column prop="education" label="学历" width="80" />
        <el-table-column label="匹配度" width="90">
          <template #default="{row}">
            <el-progress :percentage="row.matchScore||0" :color="scoreColor" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{row}">
            <el-button size="small" type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button size="small" type="success" @click="applyJob(row)">投递</el-button>
            <el-button size="small" :type="row._fav ? 'warning' : ''" @click="toggleFav(row)" circle>
              {{ row._fav ? '★' : '☆' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;display:flex;justify-content:center">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total,prev,pager,next" @current-change="search" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { jobApi, resumeApi } from '@/api'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const searchForm = ref({ keyword: "", city: "", education: "" })
const salaryRange = ref([0, 50])
const cities = ['北京','上海','深圳','杭州','广州','成都','武汉','南京','西安','苏州']
const jobList = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const scoreColor = (p) => p > 70 ? '#67C23A' : p > 40 ? '#E6A23C' : '#F56C6C'

const search = async () => {
  loading.value = true
  try {
    const params = { ...searchForm.value, page: page.value, size: size.value }; if (salaryRange.value[0] > 0) params.salaryMin = salaryRange.value[0]; if (salaryRange.value[1] < 50) params.salaryMax = salaryRange.value[1]; const res = await jobApi.searchWithScore(params)
    jobList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (_) {} finally { loading.value = false }
}

const viewDetail = (row) => router.push('/jobseeker/jobs/' + row.id)

const toggleFav = (row) => {
  const favs = JSON.parse(localStorage.getItem('js_fav_jobs') || '[]')
  if (row._fav) {
    const idx = favs.indexOf(row.id)
    if (idx > -1) favs.splice(idx, 1)
    row._fav = false
    ElMessage.success('已取消收藏')
  } else {
    favs.push(row.id)
    row._fav = true
    ElMessage.success('已收藏')
  }
  localStorage.setItem('js_fav_jobs', JSON.stringify(favs))
}

const loadFavs = () => { 
  const favs = JSON.parse(localStorage.getItem('js_fav_jobs') || '[]')
  jobList.value.forEach(j => { j._fav = favs.includes(j.id) })
}

const goRoadmap = (row) => {
  router.push('/jobseeker/jobs/' + row.id + '?showRoadmap=1')
}

const applyJob = async (row) => {
  try {
    const capRes = await resumeApi.getCapability()
    const resumeId = capRes?.data?.resumeId
    if (!resumeId) { ElMessage.warning('请先上传简历'); return }
    await request.post('/jobseeker/applications/apply', null, { params: { resumeId, jobId: row.id } })
    ElMessage.success('投递成功！')
  } catch (e) { ElMessage.error(e.response?.data?.message || '投递失败') }
}

onMounted(async () => { await search(); await loadFavs(); })
</script>

<style scoped>
:deep(.el-input__wrapper) { background: rgba(255,255,255,0.06) !important; border-color: rgba(255,255,255,0.08) !important; }
:deep(.el-input__inner) { color: #e0e0e0 !important; }
:deep(.el-input__inner::placeholder) { color: rgba(255,255,255,0.30) !important; }
:deep(.el-select .el-input__wrapper) { background: rgba(255,255,255,0.06) !important; }
:deep(.el-select .el-input__inner) { color: #e0e0e0 !important; }
</style>