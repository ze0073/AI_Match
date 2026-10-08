<template>
  <div class="operate-log glass-fade-in" v-loading="loading">
    <el-card class="glass-card">
      <template #header><span>操作日志</span></template>
      <el-form inline style="margin-bottom:16px">
        <el-form-item label="用户">
          <el-input v-model="filters.username" placeholder="搜索用户" clearable @clear="doSearch" @keyup.enter="doSearch" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="filters.operationType" placeholder="全部" clearable @change="doSearch" style="width:140px">
            <el-option label="登录" value="登录" />
            <el-option label="注册" value="注册" />
            <el-option label="上传" value="上传" />
            <el-option label="匹配" value="匹配" />
            <el-option label="删除" value="删除" />
            <el-option label="修改" value="修改" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="doSearch">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logs" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column label="操作类型" width="100">
          <template #default="scope">
            <el-tag :type="typeColor(scope.row.operationType)" size="small">{{ scope.row.operationType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP" width="130" />
        <el-table-column prop="costTime" label="耗时(ms)" width="90" />
        <el-table-column label="结果" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.result === '成功' ? 'success' : 'danger'" size="small">{{ scope.row.result }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="160" />
      </el-table>

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
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const logs = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filters = reactive({ username: '', operationType: '' })

const typeColor = (type) => {
  const map = { '登录': '', '注册': 'success', '上传': 'warning', '匹配': 'primary', '删除': 'danger', '修改': 'info' }
  return map[type] || 'info'
}

const doSearch = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filters.username) params.username = filters.username
    if (filters.operationType) params.operationType = filters.operationType
    const res = await request.get('/admin/logs', { params })
    logs.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (_) {} finally { loading.value = false }
}

const resetFilters = () => {
  filters.username = ''
  filters.operationType = ''
  page.value = 1
  doSearch()
}

const exportLogs = () => {
  if (!logs.value.length) { ElMessage.warning('无数据可导出'); return }
  const csv = ['\\uFEFFID,用户名,操作类型,模块,描述,IP地址,耗时(ms),结果,时间'].concat(
    logs.value.map(r => [r.id||'', (r.username||'').replace(/,/g,' '), (r.operationType||'').replace(/,/g,' '), 
      (r.module||'').replace(/,/g,' '), '"'+(r.description||'').replace(/"/g,'""')+'"', r.ipAddress||'', 
      r.costTime||'', (r.result||'').replace(/,/g,' '), r.createTime||''].join(','))
  ).join('\n')
  const blob = new Blob([csv], {type:'text/csv;charset=UTF-8'})
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'operation_logs.csv'; a.click()
  ElMessage.success('导出成功')
}
onMounted(() => doSearch())
</script>