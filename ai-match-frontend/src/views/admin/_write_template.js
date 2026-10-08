const fs = require('fs');
const t = (s) => s; // identity, just for structure
const content = `<template>
  <div class="admin-dashboard glass-fade-in" v-loading="loading">
    <el-row :gutter="20">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.label">
        <el-card class="glass-card stat-card" shadow="never">
          <div class="stat-content">
            <el-icon :size="24" :color="card.color"><component :is="card.icon" /></el-icon>
            <div class="stat-info">
              <span class="stat-value">{{ card.value }}</span>
              <span class="stat-label">{{ card.label }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :xs="24" :md="12">
        <el-card class="glass-card">
          <template #header><span>\u7528\u6237\u89d2\u8272\u5206\u5e03</span></template>
          <div ref="roleChartRef" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="glass-card">
          <template #header><span>\u7cfb\u7edf\u6570\u636e\u6982\u89c8</span></template>
          <div ref="systemChartRef" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row style="margin-top:20px">
      <el-col :xs="24" :md="14">
        <el-card class="glass-card">
          <template #header><span>\u6700\u8fd1\u6ce8\u518c\u7528\u6237</span></template>
          <el-table :data="recentUsers" stripe size="small">
            <el-table-column prop="username" label="\u7528\u6237\u540d" />
            <el-table-column prop="realName" label="\u771f\u5b9e\u59d3\u540d" />
            <el-table-column label="\u7c7b\u578b" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.userType==='ADMIN'?'danger':scope.row.userType==='HR'?'warning':'success'" size="small">
                  {{ scope.row.userType==='ADMIN'?'\u7ba1\u7406\u5458':scope.row.userType==='HR'?'HR':'\u6c42\u804c\u8005' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="\u624b\u673a\u53f7" width="130" />
            <el-table-column label="\u64cd\u4f5c" width="80">
              <template #default="scope">
                <el-button type="primary" link size="small" @click="$router.push('/admin/users')">\u7ba1\u7406</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>`;
fs.writeFileSync('E:/codex/AI_Match/ai-match-frontend/src/views/admin/Dashboard_template.vue', content, 'utf-8');
console.log('Template written');