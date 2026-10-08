<template>
  <div class="login-container glass-fade-in">
    <div class="login-card">
      <div class="login-header">
        <h1>AI Match</h1>
        <p>智能人才匹配与能力图谱系统</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="手机号/邮箱" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password :prefix-icon="Lock" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item prop="userType">
          <el-radio-group v-model="form.userType" class="role-group">
            <el-radio-button value="JOBSEEKER">求职者</el-radio-button>
            <el-radio-button value="HR">企业HR</el-radio-button>
            <el-radio-button value="ADMIN">管理员</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div style="text-align:right;margin-bottom:8px"><router-link to="/reset-password" style="color:#888;font-size:13px">忘记密码？</router-link></div>
        <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
          登 录
        </el-button>
        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  userType: 'JOBSEEKER'
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await authApi.login(form)
    userStore.setAuth(res.data)
    const routeMap = {
      JOBSEEKER: '/jobseeker/dashboard',
      HR: '/hr/dashboard',
      ADMIN: '/admin/dashboard'
    }
    ElMessage.success('登录成功')
    router.push(routeMap[form.userType] || '/')
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.login-card {
  width: 420px;
  max-width: 95vw;
  padding: 44px 36px 36px;
  background: var(--bg-card);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: var(--glass-border);
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.30);
}
.login-header { text-align: center; margin-bottom: 36px; }
.login-header h1 { font-size: 34px; color: var(--accent-text); font-weight: 700; letter-spacing: 2px; }
.login-header p { color: var(--text-muted); font-size: 13px; margin-top: 6px; }
.login-form { width: 100%; }
.role-group { width: 100%; display: flex; justify-content: center; }
.login-btn { width: 100%; margin-top: 8px; height: 44px; font-size: 15px; letter-spacing: 2px; }
.login-footer { text-align: center; margin-top: 24px; font-size: 13px; color: var(--text-muted); }
.login-footer a { color: var(--accent-text); margin-left: 4px; font-weight: 500; text-decoration: none; }
.login-footer a:hover { text-decoration: underline; }
.login-form :deep(.el-input__wrapper) { background: var(--bg-input) !important; }
.login-form :deep(.el-input__inner) { color: var(--text-primary) !important; }
.login-form :deep(.el-input__inner::placeholder) { color: var(--text-placeholder) !important; }
@media (max-width: 480px) {
  .login-card { padding: 32px 20px 24px; }
  .login-header h1 { font-size: 28px; }
}
</style>
