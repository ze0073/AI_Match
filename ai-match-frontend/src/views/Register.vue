<template>
  <div class="register-container glass-fade-in">
    <div class="register-card">
      <div class="register-header">
        <h1>创建账号</h1>
        <p>加入 AI Match 智能人才匹配平台</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" class="register-form" label-position="top">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" size="large" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱（选填）" size="large" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" size="large" />
        </el-form-item>
        <el-form-item v-if="form.userType==='HR'" label="公司名称">
          <el-input v-model="form.company" placeholder="请输入公司名称" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请设置密码（至少6位）" size="large" show-password />
        </el-form-item>
        <el-form-item label="身份" prop="userType">
          <el-radio-group v-model="form.userType" class="role-group">
            <el-radio-button value="JOBSEEKER">求职者</el-radio-button>
            <el-radio-button value="HR">企业HR</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" class="register-btn" @click="handleRegister">注 册</el-button>
        <div class="register-footer"><span>已有账号？</span><router-link to="/login">立即登录</router-link></div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
const router = useRouter(); const formRef = ref(null); const loading = ref(false)
const form = reactive({ phone: '', email: '', realName: '', company: '', password: '', userType: 'JOBSEEKER' })
const rules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  password: [{ required: true, message: '请设置密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }]
}
const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try { await authApi.register(form); ElMessage.success('注册成功，请登录'); router.push('/login') }
  catch (e) {} finally { loading.value = false }
}
</script>

<style scoped>
.register-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }
.register-card {
  width: 440px; max-width: 95vw; padding: 40px 36px 32px;
  background: var(--bg-card); backdrop-filter: var(--glass-blur); -webkit-backdrop-filter: var(--glass-blur);
  border: var(--glass-border); border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.30);
}
.register-header { text-align: center; margin-bottom: 28px; }
.register-header h1 { font-size: 30px; color: var(--accent-text); font-weight: 700; }
.register-header p { color: var(--text-muted); font-size: 13px; margin-top: 4px; }
.register-form { width: 100%; }
.register-form :deep(.el-form-item__label) { color: var(--text-secondary); font-weight: 500; }
.role-group { width: 100%; display: flex; justify-content: center; }
.register-btn { width: 100%; margin-top: 4px; height: 44px; font-size: 15px; letter-spacing: 2px; }
.register-footer { text-align: center; margin-top: 20px; font-size: 13px; color: var(--text-muted); }
.register-footer a { color: var(--accent-text); margin-left: 4px; font-weight: 500; text-decoration: none; }
.register-footer a:hover { text-decoration: underline; }
.register-form :deep(.el-input__wrapper) { background: var(--bg-input) !important; }
.register-form :deep(.el-input__inner) { color: var(--text-primary) !important; }
.register-form :deep(.el-input__inner::placeholder) { color: var(--text-placeholder) !important; }
</style>
