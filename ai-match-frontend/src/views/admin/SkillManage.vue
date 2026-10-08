<template>
  <div class="skill-manage glass-fade-in"><el-card>
    <template #header><div class="card-header"><span>技能标签库</span><el-button type="primary" @click="addSkill">新增标签</el-button></div></template>
    <el-table :data="skills" stripe><el-table-column prop="name" label="标签名" /><el-table-column prop="category" label="分类" /><el-table-column prop="level" label="层级" /><el-table-column prop="skillType" label="类型"><template #default="{row}"><el-tag :type="row.skillType==='hard'?'':'info'">{{ row.skillType==='hard'?'硬技能':'软技能' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="140"><template #default="{row}"><el-button size="small" @click="editSkill(row)">编辑</el-button><el-button size="small" type="danger" @click="deleteSkill(row)">删除</el-button></template></el-table-column></el-table>
    <el-dialog v-model="dialogVisible" :title="isEdit?'编辑标签':'新增标签'" width="500px"><el-form :model="form" label-width="80px">
      <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item><el-form-item label="分类"><el-input v-model="form.category" /></el-form-item><el-form-item label="层级"><el-input-number v-model="form.level" :min="1" :max="5" /></el-form-item><el-form-item label="类型"><el-select v-model="form.skillType"><el-option label="硬技能" value="hard" /><el-option label="软技能" value="soft" /></el-select></el-form-item>
    </el-form><template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="saveSkill">保存</el-button></template></el-dialog>
  </el-card></div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { skillApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
const skills = ref([]); const dialogVisible = ref(false); const isEdit = ref(false); const form = ref({ name:'', category:'', level:1, skillType:'hard' }); const editId = ref(null)
const addSkill = () => { isEdit.value = false; form.value = { name:'', category:'', level:1, skillType:'hard' }; dialogVisible.value = true }
const editSkill = (row) => { isEdit.value = true; editId.value = row.id; form.value = { ...row }; dialogVisible.value = true }
const saveSkill = async () => { try { if(isEdit.value) { await skillApi.update(editId.value, form.value) } else { await skillApi.add(form.value) }; ElMessage.success('保存成功'); dialogVisible.value = false; await loadSkills() } catch (_) {} }
const deleteSkill = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该技能标签？', '确认', { type: 'warning' })
    await skillApi.delete(row.id)
    ElMessage.success('已删除')
    await loadSkills()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}
const loadSkills = async () => { try { skills.value = (await skillApi.getSkillTree()).data || [] } catch (_) {} }
onMounted(() => loadSkills())
</script>
<style scoped>.card-header{display:flex;justify-content:space-between;align-items:center}</style>
