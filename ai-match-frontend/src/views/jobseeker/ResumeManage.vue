<template>
  <div class="resume-manage glass-fade-in" v-loading="pageLoading">
    <!-- Empty State -->
    <el-card v-if="!resumeData" class="glass-card">
      <div class="empty-state">
        <div class="empty-icon-circle">
          <svg viewBox="0 0 80 80" width="80" height="80" fill="none">
            <circle cx="40" cy="40" r="38" stroke="rgba(255,255,255,0.08)" stroke-width="2" stroke-dasharray="6 4"/>
            <path d="M28 32h24M28 42h16M28 52h20" stroke="rgba(255,255,255,0.15)" stroke-width="2.5" stroke-linecap="round"/>
          </svg>
        </div>
        <h3 class="empty-title">尚未上传简历</h3>
        <p class="empty-desc">上传简历后 AI 将自动解析并生成个人能力图谱</p>
        <div class="empty-actions">
          <el-button type="primary" size="large" @click="showUpload = true">上传简历</el-button>
          <el-button size="large" @click="showBatchUpload = true">批量导入</el-button>
        </div>
      </div>
    </el-card>

    <!-- Resume Display / Edit -->
    <el-card v-else class="glass-card">
      <template #header>
        <div class="card-header">
          <span>我的简历</span>
          <div>
            <el-button v-if="!editing" type="primary" @click="startEdit">编辑简历</el-button>
            <el-button v-else type="success" :loading="saving" @click="saveEdit">保存修改</el-button>
            <el-button v-if="editing" @click="cancelEdit">取消</el-button>
            <el-button type="warning" @click="showUpload = true" style="margin-left:8px">重新上传</el-button>
            <el-button type="success" @click="showBatchUpload = true">批量导入</el-button>
          </div>
        </div>
      </template>

      <!-- Edit Mode -->
      <el-form v-if="editing" ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="姓名"><el-input v-model="editForm.name" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="手机号" prop="phone"><el-input v-model="editForm.phone" placeholder="请输入11位手机号" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="邮箱" prop="email"><el-input v-model="editForm.email" placeholder="请输入邮箱地址" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="学历">
              <el-select v-model="editForm.education" style="width:100%">
                <el-option label="博士" value="博士" />
                <el-option label="硕士" value="硕士" />
                <el-option label="本科" value="本科" />
                <el-option label="大专" value="大专" />
                <el-option label="高中" value="高中" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="学校"><el-input v-model="editForm.school" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="专业"><el-input v-model="editForm.major" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="工作年限" prop="workYears"><el-input-number v-model="editForm.workYears" :min="0" :max="50" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="当前公司"><el-input v-model="editForm.currentCompany" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="当前职位"><el-input v-model="editForm.currentPosition" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="期望城市"><el-input v-model="editForm.expectedCity" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="期望薪资"><el-input v-model="editForm.expectedSalary" placeholder="如：15K-25K" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="求职意向"><el-input v-model="editForm.jobIntention" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="技能标签">
              <div class="skill-tag-editor">
                <el-tag
                  v-for="(s, i) in editForm.skillTags"
                  :key="i"
                  closable
                  :disable-transitions="false"
                  @close="editForm.skillTags.splice(i, 1)"
                  style="margin:4px"
                  size="large"
                >{{ s }}</el-tag>
                <el-input
                  v-if="inputVisible"
                  ref="InputRef"
                  v-model="inputValue"
                  size="small"
                  style="width:120px"
                  @keyup.enter="addTag"
                  @blur="addTag"
                />
                <el-button v-else size="small" @click="showInput">+ 添加技能</el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- View Mode -->
      <div v-else>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">{{ resumeData.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学历">{{ resumeData.education || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ resumeData.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ resumeData.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学校">{{ resumeData.school || '-' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ resumeData.major || '-' }}</el-descriptions-item>
          <el-descriptions-item label="工作年限">{{ resumeData.workYears ? resumeData.workYears + '年' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前公司">{{ resumeData.currentCompany || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前职位">{{ resumeData.currentPosition || '-' }}</el-descriptions-item>
          <el-descriptions-item label="期望城市">{{ resumeData.expectedCity || '-' }}</el-descriptions-item>
          <el-descriptions-item label="期望薪资">{{ resumeData.expectedSalary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="求职意向" :span="2">{{ resumeData.jobIntention || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="skills-section" v-if="skillList.length > 0">
          <h3>技能标签</h3>
          <el-tag v-for="s in skillList" :key="s" class="skill-tag" size="large">{{ s }}</el-tag>
        </div>
      </div>
    </el-card>

    <!-- Upload Dialog -->
    <el-dialog v-model="showUpload" title="上传简历" width="520px">
      <el-upload ref="uploadRef" drag :auto-upload="false" :on-change="handleFileChange" accept=".pdf,.doc,.docx" :limit="1">
        <el-icon size="48"><UploadFilled /></el-icon>
        <div>将简历文件拖拽到此处，或<em>点击上传</em></div>
        <template #tip><div class="el-upload__tip">支持 Word/PDF，最大20MB</div></template>
      </el-upload>
      <div v-if="uploadProgress > 0" style="margin-top:12px">
        <el-progress :percentage="uploadProgress" />
      </div>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="doUpload">确认上传并解析</el-button>
      </template>
    </el-dialog>

    <!-- Batch Upload Dialog -->
    <el-dialog v-model="showBatchUpload" title="批量导入简历" width="600px">
      <el-upload ref="batchUploadRef" drag multiple :auto-upload="false" :on-change="onBatchFileChange" :limit="20" accept=".pdf,.doc,.docx">
        <el-icon size="48"><UploadFilled /></el-icon>
        <div class="upload-text">拖拽或点击选择简历文件</div>
        <template #tip><div class="upload-tip">支持PDF/Word，单文件<=20MB，每次最多20份</div></template>
      </el-upload>
      <div v-if="batchResults.length > 0" style="margin-top:16px">
        <el-tag v-for="r in batchResults" :key="r.fileName" :type="r.status==='success'?'success':'danger'" style="margin:4px">
          {{ r.fileName }}: {{ r.status==='success' ? '成功' : '失败' }}
        </el-tag>
      </div>
      <template #footer>
        <el-button @click="showBatchUpload=false">关闭</el-button>
        <el-button type="primary" :loading="batchUploading" @click="doBatchUpload">开始批量解析</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { resumeApi } from '@/api'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import request from '@/api/request'

const showUpload = ref(false)
const uploading = ref(false)
const pageLoading = ref(false)
const resumeData = ref(null)
const fileToUpload = ref(null)
const uploadProgress = ref(0)
const uploadRef = ref(null)

// Edit mode
const editing = ref(false)
const saving = ref(false)
const editFormRef = ref(null)
const editForm = ref({})
const inputVisible = ref(false)
const inputValue = ref('')
const InputRef = ref(null)

const skillList = computed(() => {
  if (!resumeData.value?.skills) return []
  try {
    const parsed = JSON.parse(resumeData.value.skills)
    return Array.isArray(parsed) ? parsed.map(s => typeof s === 'string' ? s : s.name).filter(Boolean) : []
  } catch (_) { return String(resumeData.value.skills).split(/[,，、\n]/).filter(Boolean) }
})

const editRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确，请输入11位手机号', trigger: 'blur' }
  ],
  email: [
    { pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '邮箱格式不正确', trigger: 'blur' }
  ],
  workYears: [
    { required: true, message: '请输入工作年限', trigger: 'blur' },
    { type: 'number', min: 0, max: 50, message: '工作年限范围0-50年', trigger: 'blur' }
  ]
}



const startEdit = () => {
  editForm.value = {
    resumeId: resumeData.value.resumeId,
    name: resumeData.value.name || '',
    phone: resumeData.value.phone || '',
    email: resumeData.value.email || '',
    education: resumeData.value.education || '',
    school: resumeData.value.school || '',
    major: resumeData.value.major || '',
    workYears: resumeData.value.workYears || 0,
    currentCompany: resumeData.value.currentCompany || '',
    currentPosition: resumeData.value.currentPosition || '',
    expectedCity: resumeData.value.expectedCity || '',
    expectedSalary: resumeData.value.expectedSalary || '',
    jobIntention: resumeData.value.jobIntention || '',
    skillTags: [...skillList.value]
  }
  editing.value = true
}

const cancelEdit = () => { editing.value = false }

const saveEdit = async () => {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...editForm.value }
    payload.skills = JSON.stringify(payload.skillTags || [])
    delete payload.skillTags
    delete payload.resumeId
    await resumeApi.update(editForm.value.resumeId, payload)
    ElMessage.success('简历已更新')
    editing.value = false
    await loadResume()
  } catch (_) { ElMessage.error('保存失败') } finally { saving.value = false }
}

const showInput = async () => {
  inputVisible.value = true
  await nextTick()
  InputRef.value?.focus()
}

const addTag = () => {
  if (inputValue.value) {
    editForm.value.skillTags.push(inputValue.value)
  }
  inputVisible.value = false
  inputValue.value = ''
}

const handleFileChange = (file) => { fileToUpload.value = file.raw; uploadProgress.value = 0 }
const doUpload = async () => {
  if (!fileToUpload.value) { ElMessage.warning('请选择文件'); return }
  uploading.value = true; uploadProgress.value = 50
  try {
    const res = await resumeApi.upload(fileToUpload.value)
    uploadProgress.value = 100
    resumeData.value = res.data.parsedData || {}
    resumeData.value.resumeId = res.data.resumeId
    ElMessage.success('简历解析成功')
    showUpload.value = false; uploadRef.value?.clearFiles(); fileToUpload.value = null
  } catch (_) { uploadProgress.value = 0 } finally { uploading.value = false }
}

const loadResume = async () => {
  pageLoading.value = true
  try {
    const res = await resumeApi.getCapability()
    console.log('loadResume raw res:', JSON.stringify(res))
    console.log('loadResume res.data:', JSON.stringify(res.data))
    if (res.data?.resumeId) {
      const d = res.data
      const p = d.parsedData || {}
      resumeData.value = {
        resumeId: d.resumeId,
        name: d.name || p.name || '',
        phone: d.phone || p.phone || '',
        email: d.email || p.email || '',
        education: d.education || p.education || '',
        school: d.school || p.school || '',
        major: d.major || p.major || '',
        workYears: d.workYears ?? p.workYears ?? 0,
        currentCompany: d.currentCompany || p.currentCompany || '',
        currentPosition: d.currentPosition || p.currentPosition || '',
        expectedCity: d.expectedCity || p.expectedCity || '',
        expectedSalary: d.expectedSalary || p.expectedSalary || '',
        jobIntention: d.jobIntention || p.jobIntention || '',
        skills: d.skills || '[]',
        certificates: d.certificates || '[]',
        parsedData: p
      }
    }
  } catch(e) { console.log('loadResume error:', e) } finally { pageLoading.value = false }
}

// Batch upload
const showBatchUpload = ref(false)
const batchUploadRef = ref(null)
const batchFiles = ref([])
const batchUploading = ref(false)
const batchResults = ref([])
const onBatchFileChange = (file) => { batchFiles.value.push(file.raw) }
const doBatchUpload = async () => {
  if (batchFiles.value.length === 0) { ElMessage.warning("请选择文件"); return }
  batchUploading.value = true
  const fd = new FormData()
  batchFiles.value.forEach(f => fd.append('files', f))
  try {
    const res = await request.post('/jobseeker/resume/batch-upload', fd)
    batchResults.value = res.data || []
    ElMessage.success('批量上传完成')
    loadResume()
  } catch (_) {} finally { batchUploading.value = false; batchFiles.value = [] }
}

onMounted(() => loadResume())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.empty-state { padding: 60px 0; text-align: center; }
.empty-icon-circle { margin-bottom: 24px; opacity: 0.7; }
.empty-title { color: #e0e0e0; font-size: 20px; font-weight: 600; margin-bottom: 8px; letter-spacing: 0.5px; }
.empty-desc { color: rgba(255,255,255,0.35); font-size: 14px; margin-bottom: 32px; }
.empty-actions { display: flex; gap: 16px; justify-content: center; flex-wrap: wrap; }
.skills-section { margin-top: 20px; }
.skills-section h3 { margin-bottom: 12px; color: var(--text-primary); }
.skill-tag { margin: 4px; }
.skill-tag-editor { min-height: 40px; }

/* --- Dark theme fixes for resume content --- */
:deep(.el-descriptions) {
  --el-descriptions-item-bordered-label-background: rgba(255,255,255,0.03);
}
:deep(.el-descriptions__body) {
  background: transparent !important;
}
:deep(.el-descriptions__table) {
  background: transparent !important;
}
:deep(.el-descriptions__label) {
  background: rgba(255,255,255,0.04) !important;
  color: #b0b0c0 !important;
  border-color: rgba(255,255,255,0.06) !important;
}
:deep(.el-descriptions__content) {
  background: rgba(255,255,255,0.02) !important;
  color: #e0e0e0 !important;
  border-color: rgba(255,255,255,0.06) !important;
}
:deep(.el-form-item__label) {
  color: #b0b0c0 !important;
}
:deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.06) !important;
  border-color: rgba(255,255,255,0.08) !important;
}
:deep(.el-input__inner) {
  color: #e0e0e0 !important;
}
:deep(.el-select .el-input__wrapper) {
  background: rgba(255,255,255,0.06) !important;
}
:deep(.el-input-number .el-input__wrapper) {
  background: rgba(255,255,255,0.06) !important;
}
:deep(.el-input-number .el-input__inner) {
  color: #e0e0e0 !important;
}
:deep(.el-tag) {
  background: rgba(96,165,250,0.12);
  border-color: rgba(96,165,250,0.20);
  color: #93c5fd;
}
</style>