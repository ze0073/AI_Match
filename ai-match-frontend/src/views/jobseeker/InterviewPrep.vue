<template>
  <div class="interview-prep glass-fade-in">
    <el-card class="glass-card" v-if="!jobId">
      <el-empty description="请从求职记录中选择一个面试岗位">
        <el-button type="primary" @click="router.push('/jobseeker/applications')">查看求职记录</el-button>
      </el-empty>
    </el-card>

    <template v-else>
      <el-card class="glass-card" style="margin-bottom:16px">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>面试准备 - {{ jobTitle }}</span>
            <el-button type="primary" :loading="loading" @click="generateQuestions">生成面试题</el-button>
          </div>
        </template>
        <p style="color:#888;font-size:13px">AI将根据职位技能要求为你生成针对性面试题，帮助你提前准备</p>
      </el-card>

      <!-- Questions List -->
      <el-card v-if="questions.length > 0" class="glass-card" style="margin-bottom:16px">
        <template #header><span>面试题目</span></template>
        <div v-for="(q, i) in questions" :key="i" class="question-item" @click="selectQuestion(q, i)">
          <el-tag size="small" :type="q.category==='技术基础'?'success':q.category==='项目经验'?'warning':'primary'">{{ q.category }}</el-tag>
          <span style="margin-left:8px;color:#e0e0e0;font-weight:500">{{ i + 1 }}. {{ q.question }}</span>
        </div>
      </el-card>

      <!-- Answer Area -->
      <el-card v-if="currentQuestion" class="glass-card">
        <template #header><span>我的回答</span></template>
        <p style="color:#aac;font-weight:500;margin-bottom:12px">{{ currentQuestion.question }}</p>
        <el-input v-model="userAnswer" type="textarea" :rows="5" placeholder="输入你的回答..." />
        <div style="margin-top:12px;display:flex;gap:8px">
          <el-button type="primary" :loading="evaluating" @click="evaluate">提交评估</el-button>
          <el-button @click="currentQuestion=null;userAnswer='';feedback=null">换一题</el-button>
        </div>

        <!-- AI Feedback -->
        <div v-if="feedback" class="feedback-panel glass-card" style="margin-top:16px;padding:16px">
          <h4 style="margin:0 0 8px;color:#e0e0e0">
            AI评分：
            <el-tag :type="feedback.score>=80?'success':feedback.score>=60?'warning':'danger'" size="large">{{ feedback.score }} 分</el-tag>
          </h4>
          <p style="color:#ccc;margin:8px 0">{{ feedback.comment }}</p>
          <div v-if="feedback.strengths?.length" style="margin-top:8px">
            <p style="color:#67C23A;font-size:13px;margin:0">优点：</p>
            <ul style="color:#aaa;margin:4px 0;padding-left:20px"><li v-for="s in feedback.strengths" :key="s">{{ s }}</li></ul>
          </div>
          <div v-if="feedback.improvements?.length" style="margin-top:8px">
            <p style="color:#E6A23C;font-size:13px;margin:0">改进建议：</p>
            <ul style="color:#aaa;margin:4px 0;padding-left:20px"><li v-for="imp in feedback.improvements" :key="imp">{{ imp }}</li></ul>
          </div>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const jobId = ref(route.query.jobId ? Number(route.query.jobId) : null)
const jobTitle = ref(route.query.jobTitle || '')
const loading = ref(false)
const evaluating = ref(false)
const questions = ref([])
const currentQuestion = ref(null)
const userAnswer = ref('')
const feedback = ref(null)

const generateQuestions = async () => {
  loading.value = true
  questions.value = []
  try {
    const res = await request.get('/interview/questions/' + jobId.value)
    const data = res.data
    if (Array.isArray(data)) {
      questions.value = data
      ElMessage.success('已生成' + data.length + '道面试题')
    } else if (typeof data === 'string') {
      try { questions.value = JSON.parse(data); ElMessage.success('已生成面试题') } catch (_) { ElMessage.error('解析失败') }
    }
  } catch (e) {
    ElMessage.error('AI生成失败，请稍后重试')
  } finally { loading.value = false }
}

const selectQuestion = (q, i) => {
  currentQuestion.value = q
  userAnswer.value = ''
  feedback.value = null
}

const evaluate = async () => {
  if (!userAnswer.value.trim()) { ElMessage.warning('请先输入回答'); return }
  evaluating.value = true
  feedback.value = null
  try {
    const res = await request.post('/interview/evaluate', {
      question: currentQuestion.value.question,
      answer: userAnswer.value,
      jobTitle: jobTitle.value
    })
    const data = res.data
    if (typeof data === 'string') {
      feedback.value = JSON.parse(data)
    } else {
      feedback.value = data
    }
    ElMessage.success('评估完成')
  } catch (e) {
    ElMessage.error('评估失败')
  } finally { evaluating.value = false }
}

onMounted(() => {
  if (jobId.value) {
    generateQuestions()
  }
})
</script>

<style scoped>
.question-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px solid rgba(255,255,255,0.04);
  display: flex;
  align-items: center;
}
.question-item:hover {
  background: rgba(255,255,255,0.04);
}
.feedback-panel {
  border: 1px solid rgba(255,255,255,0.08);
}
</style>