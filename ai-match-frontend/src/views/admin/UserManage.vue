<template>
  <div class="user-manage glass-fade-in" v-loading="loading">
    <el-card class="glass-card">
      <template #header><span>用户管理</span></template>

      <el-form inline style="margin-bottom:16px">
        <el-form-item label="搜索">
          <el-input v-model="filters.keyword" placeholder="用户名/姓名/手机号" clearable @clear="doSearch" @keyup.enter="doSearch" style="width:220px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filters.userType" placeholder="全部" clearable @change="doSearch" style="width:120px">
            <el-option label="求职者" value="JOBSEEKER" />
            <el-option label="HR" value="HR" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="doSearch">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="users" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="角色" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.userType==='ADMIN'?'danger':scope.row.userType==='HR'?'warning':'success'" size="small">
              {{ scope.row.userType==='JOBSEEKER'?'求职者':scope.row.userType==='HR'?'HR':'管理员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70">
          <template #default="scope">
            <el-switch
              :model-value="scope.row.status === 1"
              @change="(v) => toggleStatus(scope.row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openEdit(scope.row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="editVisible" title="编辑用户" width="500px">
        <el-form :model="editForm" label-width="80px">
          <el-form-item label="姓名">
            <el-input v-model="editForm.realName" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="editForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="editForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="公司" v-if="editForm.userType === 'HR'">
            <el-input v-model="editForm.company" placeholder="请输入公司名称" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingEdit" @click="saveEdit">保存</el-button>
        </template>
      </el-dialog>

      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        @current-change="doSearch"
        @size-change="(s) => { size = s; doSearch() }"
        style="margin-top:20px;justify-content:center"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '@/api'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const users = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filters = reactive({ keyword: '', userType: '' })

const editVisible = ref(false)
const savingEdit = ref(false)
const editForm = reactive({ id: null, realName: '', phone: '', email: '', company: '', userType: '' })

const openEdit = (row) => {
  editForm.id = row.id
  editForm.realName = row.realName || ''
  editForm.phone = row.phone || ''
  editForm.email = row.email || ''
  editForm.company = row.company || ''
  editForm.userType = row.userType || ''
  editVisible.value = true
}

const saveEdit = async () => {
  savingEdit.value = true
  try {
    const body = { realName: editForm.realName, phone: editForm.phone, email: editForm.email }
    if (editForm.userType === 'HR') body.company = editForm.company
    await request.put('/admin/users/' + editForm.id, body)
    ElMessage.success('用户信息已更新')
    editVisible.value = false
    doSearch()
  } catch (_) { ElMessage.error('保存失败') }
  finally { savingEdit.value = false }
}

const doSearch = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.userType) params.userType = filters.userType
    const res = await adminApi.getUsers(params)
    users.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (_) {} finally { loading.value = false }
}

const resetFilters = () => {
  filters.keyword = ''
  filters.userType = ''
  page.value = 1
  doSearch()
}

const toggleStatus = async (row, v) => {
  try {
    await adminApi.updateStatus(row.id, v ? 1 : 0)
    row.status = v ? 1 : 0
    ElMessage.success(v ? '已启用' : '已禁用')
  } catch (_) {}
}

onMounted(() => doSearch())
</script>
