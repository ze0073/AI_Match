import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const username = ref(localStorage.getItem('username') || '')
  const userType = ref(localStorage.getItem('userType') || '')
  const realName = ref(localStorage.getItem('realName') || '')
  const userInfo = ref(null)

  const setAuth = (data) => {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    userType.value = data.userType
    realName.value = data.realName || ''

    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('userType', data.userType)
    localStorage.setItem('realName', data.realName || '')
  }

  const logout = () => {
    token.value = ''
    userId.value = ''
    username.value = ''
    userType.value = ''
    realName.value = ''
    userInfo.value = null

    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('userType')
    localStorage.removeItem('realName')
  }

  const fetchUserInfo = async () => {
    try {
      const res = await authApi.getUserInfo()
      userInfo.value = res.data
    } catch (e) {
      console.error('获取用户信息失败', e)
    }
  }

  return { token, userId, username, userType, realName, userInfo, setAuth, logout, fetchUserInfo }
})
