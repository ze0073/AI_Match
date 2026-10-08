import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

request.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code !== 200) {
      if (data.code === 401) {
        localStorage.clear()
        window.location.href = '/login'
        return Promise.reject(new Error('登录已过期'))
      }
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  error => {
    const serverMsg = error.response?.data?.message
    if (error.response?.status === 401) {
      const url = error.config?.url || ''
      if (!url.includes('/auth/login') && !url.includes('/auth/register')) {
        localStorage.clear()
        window.location.href = '/login'
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(error)
      }
    }
    if (serverMsg) {
      ElMessage.error(serverMsg)
      return Promise.reject(error)
    }
    if (error.response?.status === 429) {
      ElMessage.error('请求过于频繁，请稍后再试')
      return Promise.reject(error)
    }
    if (error.response?.status === 503) {
      ElMessage.error('AI服务暂时不可用，请稍后重试')
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request