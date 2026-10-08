<template>
  <div class="profile glass-fade-in" v-loading="loading">
    <el-card class="glass-card">
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
          <el-button v-if="!editing" type="primary" size="small" @click="startEdit">编辑资料</el-button>
        </div>
      </template>
      <el-descriptions v-if="!editing" :column="2" border>
        <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ userInfo.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ userInfo.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userInfo.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公司">
          <span style="color:#93c5fd;font-weight:600">{{ userInfo.company || '未填写' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag size="small" type="warning">HR</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ userInfo.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-form v-if="editing" :model="editForm" label-width="80px" style="max-width:400px">
        <el-form-item label="姓名">
          <el-input v-model="editForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="公司">
          <el-input v-model="editForm.company" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
          <el-button @click="cancelEdit">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="glass-card" style="margin-top:16px">
      <template #header>修改密码</template>
      <el-form :model="pwd" label-width="100px" style="max-width:400px">
        <el-form-item label="原密码">
          <el-input v-model="pwd.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwd.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="changingPwd" @click="changePwd">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const userStore = useUserStore()
const loading = ref(false)
const editing = ref(false)
const saving = ref(false)
const editForm = reactive({ phone: '', email: '', realName: '', company: '' })
const changingPwd = ref(false)
const userInfo = ref({
  username: userStore.username || '',
  realName: userStore.realName || '',
  phone: '',
  email: '',
  userType: '',
  company: '',
  createTime: ''
})
const pwd = ref({ oldPassword: '', newPassword: '' })

const changePwd = async () => {
  if (!pwd.value.oldPassword || !pwd.value.newPassword) {
    ElMessage.warning('请填写密码')
    return
  }
  changingPwd.value = true
  try {
    await authApi.updatePassword(pwd.value.oldPassword, pwd.value.newPassword)
    ElMessage.success('密码修改成功')
    pwd.value = { oldPassword: '', newPassword: '' }
  } catch (_) {} finally { changingPwd.value = false }
}

const startEdit = () => {
  editForm.phone = userInfo.value.phone || ''
  editForm.email = userInfo.value.email || ''
  editForm.realName = userInfo.value.realName || ''
  editForm.company = userInfo.value.company || ''
  editing.value = true
}
const cancelEdit = () => { editing.value = false }
const saveProfile = async () => {
  saving.value = true
  try {
    await request.put('/auth/userinfo', editForm)
    userInfo.value = { ...userInfo.value, ...editForm }
    editing.value = false
    ElMessage.success('\u4e2a\u4eba\u4fe1\u606f\u5df2\u66f4\u65b0')
  } catch (_) { ElMessage.error('\u4fdd\u5b58\u5931\u8d25') }
  finally { saving.value = false }
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await request.get('/auth/userinfo')
    if (res.data) {
      userInfo.value = { ...userInfo.value, ...res.data }
    }
  } catch (_) {} finally { loading.value = false }
})
</script>
<style scoped>
:deep(.el-descriptions__label) { background: rgba(255,255,255,0.04) !important; color: #b0b0c0 !important; border-color: rgba(255,255,255,0.06) !important; }
:deep(.el-descriptions__content) { background: rgba(255,255,255,0.02) !important; color: #e0e0e0 !important; border-color: rgba(255,255,255,0.06) !important; }
:deep(.el-input__wrapper) { background: rgba(255,255,255,0.06) !important; border-color: rgba(255,255,255,0.08) !important; }
:deep(.el-input__inner) { color: #e0e0e0 !important; }
:deep(.el-form-item__label) { color: #b0b0c0 !important; }
:deep(.el-tag) { background: rgba(96,165,250,0.12); border-color: rgba(96,165,250,0.20); color: #93c5fd; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>