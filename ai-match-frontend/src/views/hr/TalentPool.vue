<template>
  <div class="talent-pool glass-fade-in">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="全部候选人" name="all" />
      <el-tab-pane label="我的收藏" name="favorites" />
      <el-tab-pane label="跨职位推荐" name="cross" />
    </el-tabs>

    <!-- Tab 1: All Candidates -->
    <div v-show="activeTab === 'all'">
      <el-card class="glass-card">
        <el-form inline class="filter-form">
          <el-form-item label="关键词">
            <el-input v-model="filters.keyword" placeholder="姓名/技能/职位" clearable @clear="doSearch" />
          </el-form-item>
          <el-form-item label="学历">
            <el-select v-model="filters.education" placeholder="不限" clearable @change="doSearch">
              <el-option label="博士" value="博士" />
              <el-option label="硕士" value="硕士" />
              <el-option label="本科" value="本科" />
              <el-option label="大专" value="大专" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="doSearch">搜索</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="candidates" stripe v-loading="loading">
          <el-table-column prop="name" label="姓名" width="80" />
          <el-table-column prop="education" label="学历" width="70" />
          <el-table-column prop="school" label="学校" width="140" />
          <el-table-column prop="workYears" label="经验" width="70">
            <template #default="scope">{{ scope.row.workYears }}年</template>
          </el-table-column>
          <el-table-column prop="currentPosition" label="当前职位" width="140" />
          <el-table-column label="技能" min-width="180">
            <template #default="scope">
              <el-tag
                v-for="(s, i) in parseSkills(scope.row.skills)"
                :key="i"
                size="small"
                class="skill-tag"
              >{{ s }}</el-tag>
              <span v-if="!scope.row.skills" style="color:var(--text-muted)">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="scope">
              <el-button v-if="favResumeIds.has(scope.row.resumeId)" size="small" type="info" disabled>已收藏</el-button>
              <el-button v-else size="small" type="warning" @click="addFavorite(scope.row)">收藏</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="selectedCandidates.length > 0" style="position:sticky;bottom:16px;background:rgba(30,30,50,0.95);backdrop-filter:blur(10px);padding:12px 16px;border-radius:12px;display:flex;align-items:center;gap:12px;margin-top:16px;z-index:10">
          <span style="color:#eee">已选择 {{ selectedCandidates.length }} 位</span>
          <el-button type="primary" size="small" @click="showCompare">对比选中</el-button>
          <el-button size="small" @click="clearSelection">取消选择</el-button>
        </div>
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="doSearch"
          style="margin-top:20px;justify-content:center"
        />
      </el-card>
    </div>

    <!-- Tab 2: Favorites -->
    <div v-show="activeTab === 'favorites'">
      <el-card class="glass-card">
        <el-radio-group v-model="favFilter" @change="loadFavorites" style="margin-bottom:16px">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="意向">意向</el-radio-button>
          <el-radio-button label="面试">面试</el-radio-button>
          <el-radio-button label="录用">录用</el-radio-button>
        </el-radio-group>

        <el-table :data="favorites" stripe v-loading="loading">
          <el-table-column prop="name" label="姓名" width="80" />
          <el-table-column prop="education" label="学历" width="70" />
          <el-table-column prop="workYears" label="经验" width="70">
            <template #default="scope">{{ scope.row.workYears }}年</template>
          </el-table-column>
          <el-table-column prop="currentPosition" label="当前职位" width="140" />
          <el-table-column label="分类" width="100">
            <template #default="scope">
              <el-select v-model="scope.row.category" size="small" @change="updateCategory(scope.row)">
                <el-option label="意向" value="意向" />
                <el-option label="面试" value="面试" />
                <el-option label="录用" value="录用" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="140">
            <template #default="scope">
              <el-input
                v-model="scope.row.notes"
                size="small"
                placeholder="添加备注..."
                @blur="updateNotes(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="scope">
              <el-button size="small" type="danger" @click="removeFavorite(scope.row)">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && favorites.length === 0" description="暂无收藏" />
      </el-card>
    </div>

    <!-- Tab 3: Cross-Position Recommend -->
    <div v-show="activeTab === 'cross'">
      <el-card class="glass-card">
        <el-form inline style="margin-bottom:16px">
          <el-form-item label="选择职位">
            <el-select
              v-model="selectedJobId"
              placeholder="选择职位查看推荐候选人"
              @change="loadCrossRecommend"
              filterable
              style="width:340px"
            >
              <el-option
                v-for="j in jobList"
                :key="j.id"
                :label="(j.title || '') + ' - ' + (j.company || '')"
                :value="j.id"
              />
            </el-select>
          </el-form-item>
        </el-form>

        <div v-if="selectedJobId">
          <el-table :data="crossCandidates" stripe v-loading="loading">
            <el-table-column prop="name" label="姓名" width="80" />
            <el-table-column prop="education" label="学历" width="70" />
            <el-table-column prop="workYears" label="经验" width="70" />
            <el-table-column prop="currentPosition" label="当前职位" width="140" />
            <el-table-column prop="expectedCity" label="期望城市" width="100" />
            <el-table-column label="技能" min-width="160">
              <template #default="scope">
                <el-tag
                  v-for="(s, i) in (scope.row.skills || [])"
                  :key="i"
                  size="small"
                  class="skill-tag"
                >{{ s }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="匹配度" width="100">
              <template #default="scope">
                <el-tag :type="(scope.row.matchScore || 0) >= 80 ? 'success' : (scope.row.matchScore || 0) >= 60 ? 'warning' : 'danger'">
                  {{ scope.row.matchScore || 0 }}%
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="category" label="分类" width="80" />
          </el-table>
          <el-empty v-if="!loading && crossCandidates.length === 0" description="暂无匹配候选人" />
        </div>
        <el-empty v-if="!selectedJobId" description="请先选择一个职位" />
      </el-card>
    </div>
  </div>

    <!-- Compare Dialog -->
    <el-dialog v-model="compareVisible" title="候选人对比" width="90%" top="5vh">
      <div style="overflow-x:auto">
        <table style="width:100%;border-collapse:collapse;color:#e0e0e0">
          <thead><tr style="background:rgba(255,255,255,0.08)">
              <th style="padding:10px;text-align:left;min-width:80px">字段</th>
            <th v-for="(c,i) in compareList" :key="i" style="padding:10px;text-align:center;min-width:140px">{{ c.name }}</th>
          </tr></thead>
          <tbody>
            <tr v-for="item in compareFields" :key="item.key" style="border-bottom:1px solid rgba(255,255,255,0.06)">
              <td style="padding:10px;font-weight:600">{{ item.label }}</td>
              <td v-for="(c,i) in compareList" :key="i" style="padding:10px;text-align:center">{{ c[item.key] || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const activeTab = ref('all')
const loading = ref(false)
const candidates = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filters = reactive({ keyword: '', education: '' })

const favorites = ref([])
const favResumeIds = ref(new Set())
const favFilter = ref('')

const selectedJobId = ref(null)
const crossCandidates = ref([])
const tableRef = ref(null)
const selectedCandidates = ref([])
const compareVisible = ref(false)
const compareList = ref([])
const compareFields = [
  { key: 'name', label: '姓名' },
  { key: 'education', label: '学历' },
  { key: 'school', label: '学校' },
  { key: 'workYears', label: '工作年限(年)' },
  { key: 'currentPosition', label: '当前职位' },
  { key: 'expectedCity', label: '期望城市' },
  { key: 'skills', label: '技能' }
]
const jobList = ref([])

function parseSkills(skills) {
  if (!skills) return []
  if (Array.isArray(skills)) return skills
  try {
    const parsed = JSON.parse(skills)
    return Array.isArray(parsed) ? parsed : []
  } catch (_) {
    return String(skills).split(/[,，、\n]/).map(s => s.trim()).filter(Boolean)
  }
}

async function doSearch() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: size.value,
      keyword: filters.keyword || undefined,
      education: filters.education || undefined
    }
    const res = await request.get('/hr/talent-pool/search', { params })
    candidates.value = res.data?.records || []
    total.value = res.data?.total || 0
    // Sync favorites
    try {
      const favRes = await request.get('/hr/favorites')
      favResumeIds.value = new Set((favRes.data || []).map(f => f.resumeId))
    } catch (_) {}
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.education = ''
  page.value = 1
  doSearch()
}

async function loadFavorites() {
  loading.value = true
  try {
    const params = favFilter.value ? { category: favFilter.value } : {}
    const res = await request.get('/hr/favorites', { params })
    favResumeIds.value = new Set((res.data || []).map(f => f.resumeId))
favorites.value = (res.data || []).map(f => ({
      ...f,
      category: f.category || '意向',
      notes: f.notes || ''
    }))
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function addFavorite(row) {
  try {
    await request.post('/hr/favorites/' + row.resumeId)
    ElMessage.success('已收藏')
  } catch (e) {
    ElMessage.error('收藏失败')
  }
}

async function removeFavorite(row) {
  try {
    await request.delete('/hr/favorites/' + row.resumeId)
    ElMessage.success('已取消')
    loadFavorites()
  } catch (e) {
    ElMessage.error('取消失败')
  }
}

async function updateCategory(row) {
  try {
    await request.put('/hr/favorites/' + row.id + '/category', null, {
      params: { category: row.category }
    })
    ElMessage.success('分类已更新')
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

async function updateNotes(row) {
  try {
    await request.put('/hr/favorites/' + row.id + '/notes', { notes: row.notes || '' })
  } catch (e) {
    // silent fail for notes
  }
}

async function loadJobs() {
  try {
    const r = await request.get('/job/search', { params: { page: 1, size: 100 } })
    jobList.value = r.data?.records || []
  } catch (e) {
    console.error(e)
  }
}

async function loadCrossRecommend() {
  if (!selectedJobId.value) return
  loading.value = true
  try {
    const r = await request.get('/hr/cross-recommend/' + selectedJobId.value)
    crossCandidates.value = (r.data || []).map(c => ({
      ...c,
      skills: c.skills || [],
      matchScore: c.matchScore || 0,
      category: c.category || '意向'
    }))
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function onSelectChange(rows) { selectedCandidates.value = rows }
function clearSelection() { tableRef.value?.clearSelection(); selectedCandidates.value = [] }
function showCompare() {
  compareList.value = selectedCandidates.value.slice(0, 5).map(c => ({
    ...c, skills: Array.isArray(c.skills) ? c.skills.slice(0,8).join(', ') : (c.skills || '-')
  }))
  compareVisible.value = true
}

function onTabChange(tab) {
  if (tab === 'all') { doSearch(); loadFavorites() }
  else if (tab === 'favorites') loadFavorites()
  else if (tab === 'cross') loadFavorites()
}

onMounted(() => {
  doSearch()
  loadJobs()
})
</script>

<style scoped>
.talent-pool {
  padding: 0;
}
.filter-form {
  margin-bottom: 16px;
}
.skill-tag {
  margin: 2px;
}
</style>