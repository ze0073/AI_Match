import { createRouter, createWebHistory } from 'vue-router'

function parseJwtPayload(token) {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch (_) {
    return null
  }
}

const routes = [
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/ResetPassword.vue'),
    meta: { title: 'Reset Password' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: 'Login' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: 'Register' }
  },
  {
    path: '/match/:id',
    name: 'SharedMatchDetail',
    component: () => import('@/views/jobseeker/MatchDetail.vue'),
    meta: { requiresAuth: true, title: 'Match Detail' }
  },
  {
    path: '/jobseeker',
    component: () => import('@/views/jobseeker/Layout.vue'),
    meta: { requiresAuth: true, role: 'JOBSEEKER' },
    children: [
      { path: '', redirect: '/jobseeker/dashboard' },
      { path: 'dashboard', name: 'JSDashboard', component: () => import('@/views/jobseeker/Dashboard.vue'), meta: { title: 'Dashboard' } },
      { path: 'resume', name: 'Resume', component: () => import('@/views/jobseeker/ResumeManage.vue'), meta: { title: 'My Resume' } },
      { path: 'capability', name: 'CapabilityGraph', component: () => import('@/views/jobseeker/CapabilityGraph.vue'), meta: { title: 'Capability Graph' } },
      { path: 'recommend', name: 'JobRecommend', component: () => import('@/views/jobseeker/JobRecommend.vue'), meta: { title: 'Job Recommend' } },
      { path: 'jobs', name: 'JobSearch', component: () => import('@/views/jobseeker/JobSearch.vue'), meta: { title: 'Job Search' } },
      { path: 'jobs/:id', name: 'JobDetail', component: () => import('@/views/jobseeker/JobDetail.vue'), meta: { title: 'Job Detail' } },
      { path: 'match/:id', name: 'MatchDetail', component: () => import('@/views/jobseeker/MatchDetail.vue'), meta: { title: 'Match Detail' } },
      { path: 'interview', name: 'InterviewPrep', component: () => import('@/views/jobseeker/InterviewPrep.vue'), meta: { title: 'AI Interview' } },
      { path: 'applications', name: 'MyApplications', component: () => import('@/views/jobseeker/MyApplications.vue'), meta: { title: 'My Applications' } },
      { path: 'profile', name: 'JSProfile', component: () => import('@/views/jobseeker/Profile.vue'), meta: { title: 'Profile' } }
    ]
  },
  {
    path: '/hr',
    component: () => import('@/views/hr/Layout.vue'),
    meta: { requiresAuth: true, role: 'HR' },
    children: [
      { path: '', redirect: '/hr/dashboard' },
      { path: 'dashboard', name: 'HRDashboard', component: () => import('@/views/hr/Dashboard.vue'), meta: { title: 'Dashboard' } },
      { path: 'jobs', name: 'ManageJobs', component: () => import('@/views/hr/JobManage.vue'), meta: { title: 'Job Manage' } },
      { path: 'jobs/upload', name: 'UploadJob', component: () => import('@/views/hr/JobUpload.vue'), meta: { title: 'Upload JD' } },
      { path: 'candidates/:jobId', name: 'Candidates', component: () => import('@/views/hr/CandidateList.vue'), meta: { title: 'Candidates' } },
      { path: 'job-capability', name: 'JobCapability', component: () => import('@/views/hr/JobCapability.vue'), meta: { title: 'Job Capability' } },
      { path: 'talent-pool', name: 'TalentPool', component: () => import('@/views/hr/TalentPool.vue'), meta: { title: 'Talent Pool' } },
      { path: 'profile', name: 'HRProfile', component: () => import('@/views/hr/Profile.vue'), meta: { title: 'Profile' } }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/Layout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: 'Dashboard' } },
      { path: 'users', name: 'UserManage', component: () => import('@/views/admin/UserManage.vue'), meta: { title: 'Users' } },
      { path: 'skills', name: 'SkillManage', component: () => import('@/views/admin/SkillManage.vue'), meta: { title: 'Skills' } },
      { path: 'config', name: 'SystemConfig', component: () => import('@/views/admin/SystemConfig.vue'), meta: { title: 'Config' } },
      { path: 'market', name: 'MarketTrends', component: () => import('@/views/admin/MarketTrends.vue'), meta: { title: 'Market' } },
      { path: 'logs', name: 'OperateLog', component: () => import('@/views/admin/OperateLog.vue'), meta: { title: 'Logs' } },
      { path: 'profile', name: 'AdminProfile', component: () => import('@/views/admin/Profile.vue'), meta: { title: 'Profile' } }
    ]
  },
  { path: '/', redirect: '/login' },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? 'AI Match - ' + to.meta.title : 'AI Match'

  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth) {
    if (!token) {
      next('/login')
      return
    }

    const payload = parseJwtPayload(token)
    if (!payload) {
      localStorage.clear()
      next('/login')
      return
    }

    const userType = payload.userType || payload.role
    if (to.meta.role && to.meta.role.toUpperCase() !== userType.toUpperCase()) {
      next('/login')
      return
    }
  }
  next()
})

export default router