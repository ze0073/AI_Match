<template>
  <div class="job-upload glass-fade-in"><el-card><template #header>上传职位JD</template>
    <el-upload drag :auto-upload="false" :on-change="handleFile" accept=".pdf,.doc,.docx" :limit="1">
      <el-icon size="48"><UploadFilled /></el-icon><div>拖拽或<em>点击上传</em>JD文件</div>
    </el-upload>
    <div style="margin-top:16px"><el-button type="primary" :loading="uploading" @click="doUpload">解析JD</el-button></div>
    <el-card v-if="parsedData" style="margin-top:16px"><template #header>解析结果</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="职位">{{ parsedData.title }}</el-descriptions-item><el-descriptions-item label="部门">{{ parsedData.department }}</el-descriptions-item>
        <el-descriptions-item label="城市">{{ parsedData.city }}</el-descriptions-item><el-descriptions-item label="薪资">{{ parsedData.salary }}</el-descriptions-item>
        <el-descriptions-item label="学历">{{ parsedData.education }}</el-descriptions-item><el-descriptions-item label="经验">{{ parsedData.workYears }}年</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </el-card></div>
</template>
<script setup>
import { ref } from 'vue'
import { jobApi } from '@/api'
import { ElMessage } from 'element-plus'
const uploading = ref(false); const parsedData = ref(null); const fileToUpload = ref(null)
const handleFile = (file) => fileToUpload.value = file.raw
const doUpload = async () => {
  if(!fileToUpload.value){ElMessage.warning('请选择文件');return}
  uploading.value = true
  try { const res = await jobApi.upload(fileToUpload.value); parsedData.value = res.data.parsedData; ElMessage.success('JD解析成功') } catch (_) {} finally { uploading.value = false }
}
</script>
