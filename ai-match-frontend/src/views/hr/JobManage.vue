<template>
  <div class="job-manage glass-fade-in" v-loading="loading">
    <el-card class="glass-card">
      <template #header>
        <div class="card-header">
          <span>职位管理</span>
          <el-button type="primary" @click="router.push('/hr/jobs/upload')">
            <el-icon><Upload /></el-icon>上传JD
          </el-button>
        </div>
      </template>

      <el-form inline style="margin-bottom:16px">
        <el-form-item label="搜索">
          <el-input v-model="filters.keyword" placeholder="职位名称/部门" clearable @clear="doSearch" @keyup.enter="doSearch" style="width:200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="doSearch">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="jobs" stripe>
        <el-table-column prop="id" label="ID" width="50" />
        <el-table-column prop="title" label="职位名称" min-width="140" />
        <el-table-column prop="company" label="公司" width="140" />
        <el-table-column prop="department" label="部门" width="100" />
        <el-table-column prop="city" label="城市" width="80" />
        <el-table-column prop="salary" label="薪资" width="110" />
        <el-table-column prop="education" label="学历" width="80" />
        <el-table-column prop="applyCount" label="申请人数" width="80" />
        <el-table-column label="招聘人数" width="80"><template #default="scope">{{ scope.row.headCount || '若干' }}</template></el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="viewCandidates(scope.row)">候选人</el-button>
            <el-button size="small" type="primary" @click="openEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="doSearch" style="margin-top:20px;justify-content:center" />
    </el-card>

    <el-dialog v-model="editVisible" title="编辑职位" width="900px" top="2vh" destroy-on-close append-to-body :close-on-click-modal="false">
      <div class="edit-body" style="max-height: 75vh; overflow-y: auto; padding-right: 8px">
        <el-form :model="editForm" label-width="100px">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="职位名称">
                <el-input v-model="editForm.title" placeholder="如：Java高级开发工程师" size="large" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="公司名称">
                <el-input v-model="editForm.company" placeholder="如：科技有限公司" size="large" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="所属部门">
                <el-input v-model="editForm.department" placeholder="如：技术部" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="工作城市">
                <el-input v-model="editForm.city" placeholder="如：北京" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="薪资范围">
                <el-input v-model="editForm.salary" placeholder="如：15K-25K" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="学历要求">
                <el-select v-model="editForm.education" placeholder="请选择" style="width:100%">
                  <el-option label="不限" value="不限" />
                  <el-option label="大专" value="大专" />
                  <el-option label="本科" value="本科" />
                  <el-option label="硕士" value="硕士" />
                  <el-option label="博士" value="博士" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="工作年限">
                <el-input v-model="editForm.workYears" placeholder="如：3-5年" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="招聘人数">
                <el-input-number v-model="editForm.headCount" :min="1" :max="99" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="职责描述">
                <el-input v-model="editForm.responsibilities" type="textarea" :rows="5" placeholder="请描述该职位的主要职责和工作内容..." />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="技能要求">
                <div class="skill-editor">
                  <div class="skill-header-row">
                    <span class="skill-hdr-name">技能名称</span>
                    <span class="skill-hdr-rate">熟练度</span>
                    <span class="skill-hdr-rate">重要度</span>
                    <span class="skill-hdr-del"></span>
                  </div>
                  <div v-for="(skill, idx) in editForm.skills" :key="idx" class="skill-edit-row">
                    <el-input v-model="skill.name" placeholder="如：Java" style="width:200px" size="small" />
                    <div class="rate-group">
                      <el-rate v-model="skill.level" :max="5" size="small" show-score />
                    </div>
                    <div class="rate-group">
                      <el-rate v-model="skill.importance" :max="5" size="small" show-score />
                    </div>
                    <el-button type="danger" circle size="small" @click="editForm.skills.splice(idx, 1)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                  <el-button type="primary" dashed @click="editForm.skills.push({ name: '', level: 3, importance: 3 })">
                    <el-icon><Plus /></el-icon>添加技能
                  </el-button>
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>
      <template #footer>
        <el-button size="large" @click="editVisible = false">取消</el-button>
        <el-button size="large" type="primary" :loading="saving" @click="doSave">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { jobApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Delete, Plus } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const jobs = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filters = reactive({ keyword: '' })

const editVisible = ref(false)
const saving = ref(false)
const editForm = reactive({
  id: null, title: '', company: '', department: '', city: '',
  salary: '', education: '', workYears: '', headCount: 1,
  responsibilities: '', skillRequirements: '',
  skills: []
})

function parseSkills(raw) {
  if (!raw) return []
  try { return JSON.parse(raw) } catch (_) { return [] }
}

const doSearch = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.keyword) params.keyword = filters.keyword
    const res = await jobApi.search(params)
    jobs.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (_) {} finally { loading.value = false }
}

const viewCandidates = (row) => router.push('/hr/candidates/' + row.id)

const openEdit = (row) => {
  const skills = parseSkills(row.skillRequirements)
  Object.assign(editForm, {
    id: row.id, title: row.title || '', company: row.company || '',
    department: row.department || '', city: row.city || '',
    salary: row.salary || '', education: row.education || '',
    workYears: row.workYears || '', headCount: row.headCount || 1,
    responsibilities: row.responsibilities || '',
    skillRequirements: row.skillRequirements || '',
    skills: skills.length > 0 ? skills : [{ name: '', level: 3, importance: 3 }]
  })
  editVisible.value = true
}

const doSave = async () => {
  saving.value = true
  try {
    const payload = { ...editForm }
    payload.skillRequirements = JSON.stringify(editForm.skills.filter(s => s.name))
    delete payload.skills
    await jobApi.update(editForm.id, payload)
    ElMessage.success('保存成功')
    editVisible.value = false
    doSearch()
  } catch (_) { ElMessage.error('保存失败') } finally { saving.value = false }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该职位？', '确认', { type: 'warning' })
    await jobApi.delete(row.id)
    ElMessage.success('已删除')
    doSearch()
  } catch (_) {}
}

onMounted(() => doSearch())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.edit-body { padding: 0; }
.skill-editor { width: 100%; }
.skill-header-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; padding: 4px 12px; font-size: 13px; color: #1a1a1a; font-weight: 700; background: #e8e8e8; border-radius: 6px; }
.skill-hdr-name { width: 220px; flex-shrink: 0; }
.skill-hdr-rate { width: 180px; text-align: center; flex-shrink: 0; }
.skill-hdr-del { width: 32px; }
.skill-edit-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; padding: 8px 12px; background: rgba(0,0,0,0.02); border-radius: 8px; }
.rate-group { width: 160px; display: flex; justify-content: center; }
</style>