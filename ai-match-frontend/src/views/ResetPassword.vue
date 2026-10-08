<template>
  <div class="reset-pwd-page">
    <div class="reset-card glass-card">
      <h2 style="text-align:center;margin-bottom:24px;color:#e0e0e0">重置密码</h2>
      <el-form :model="form" label-width="80px" style="max-width:360px;margin:0 auto">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名/手机号" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" placeholder="至少6位" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="doReset" style="width:100%">重置密码</el-button>
        </el-form-item>
      </el-form>
      <div style="text-align:center;margin-top:12px">
        <router-link to="/login" style="color:#a0a0c0;font-size:13px">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const router = useRouter()
const form = ref({ username: '', newPassword: '' })
const submitting = ref(false)

const doReset = async () => {
  if (!form.value.username) { ElMessage.warning('请输入用户名'); return }
  if (!form.value.newPassword || form.value.newPassword.length < 6) { ElMessage.warning('密码至少6位'); return }
  submitting.value = true
  try {
    await request.post('/auth/reset-password', form.value)
    ElMessage.success('密码已重置，请重新登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '重置失败')
  } finally { submitting.value = false }
}
</script>

<style scoped>
.reset-pwd-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0f0f1a;
}
.reset-card {
  padding: 40px 30px;
  border-radius: 16px;
  width: 420px;
  max-width: 90vw;
}
</style>