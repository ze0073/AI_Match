<template>
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
          <template #header><span>用户角色分布</span></template>
          <div ref="roleChartRef" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="glass-card">
          <template #header><span>系统数据概览</span></template>
          <div ref="systemChartRef" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row style="margin-top:20px">
      <el-col :xs="24" :md="14">
        <el-card class="glass-card">
          <template #header><span>最近注册用户</span></template>
          <el-table :data="recentUsers" stripe size="small">
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="realName" label="真实姓名" />
            <el-table-column label="角色" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.userType==='ADMIN'?'danger':scope.row.userType==='HR'?'warning':'success'" size="small">
                  {{ scope.row.userType==='ADMIN'?'管理员':scope.row.userType==='HR'?'HR':'求职者' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="电话" width="130" />
            <el-table-column label="操作" width="80">
              <template #default="scope">
                <el-button type="primary" link size="small" @click="router.push('/admin/users')">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card class="glass-card">
          <template #header><span>系统状态</span></template>
          <div class="system-info">
            <div class="info-row"><span class="info-label">CPU 核数:</span><span>{{ systemStats.cpuCores }}</span></div>
            <div class="info-row"><span class="info-label">内存使用:</span><span>{{ systemStats.usedMemoryMB }} / {{ systemStats.totalMemoryMB }} MB</span></div>
            <div class="info-row"><span class="info-label">在线用户:</span><span>{{ stats.userCount }}</span></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
