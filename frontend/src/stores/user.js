import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'

const api = axios.create({
  baseURL: '/api'
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  const permissions = ref([])
  const dataScope = ref(null)
  const initialized = ref(false)

  async function init() {
    if (initialized.value) return
    if (token.value) {
      await fetchUserInfo()
      await fetchPermissions()
    }
    initialized.value = true
  }

  async function login(username, password) {
    const res = await api.post('/auth/login', { username, password })
    if (res.code === 200) {
      token.value = res.data.token
      localStorage.setItem('token', res.data.token)
      await fetchUserInfo()
      await fetchPermissions()
    }
    return res
  }

  async function fetchUserInfo() {
    try {
      const res = await api.get('/auth/me')
      if (res.code === 200) {
        userInfo.value = res.data
      }
    } catch (e) {
      console.error('Failed to fetch user info:', e)
    }
  }

  async function fetchPermissions() {
    try {
      const res = await api.get('/user/permissions')
      if (res.code === 200) {
        permissions.value = res.data.menuPermissions || []
        dataScope.value = res.data.dataScope
      }
    } catch (e) {
      console.error('Failed to fetch permissions:', e)
    }
  }

  function hasPermission(permission) {
    return permissions.value.includes(permission)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    initialized.value = false
    localStorage.removeItem('token')
  }

  return {
    token,
    userInfo,
    permissions,
    dataScope,
    initialized,
    init,
    login,
    fetchUserInfo,
    fetchPermissions,
    hasPermission,
    logout
  }
})

export { api }
