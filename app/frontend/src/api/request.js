import axios from 'axios'
import { ElMessage } from 'element-plus'
import { auth } from '@/store/auth'
import router from '@/router'

const request = axios.create({ baseURL: '/api', timeout: 30000 })

// 自动携带 Token
request.interceptors.request.use(cfg => {
  if (auth.token) cfg.headers.Authorization = 'Bearer ' + auth.token
  return cfg
})

// 统一响应与错误处理
request.interceptors.response.use(
  res => {
    const d = res.data
    if (d && d.code === 0) return d.data
    ElMessage.error(d?.message || '请求失败')
    return Promise.reject(new Error(d?.message || '请求失败'))
  },
  err => {
    const status = err.response?.status
    const msg = err.response?.data?.message
    if (status === 401) {
      // Token 失效:清理登录态并引导重新登录
      auth.logout()
      ElMessage.warning(msg || '登录已过期，请重新登录')
      const current = router.currentRoute.value
      if (current.path !== '/login') {
        router.push({ path: '/login', query: { redirect: current.fullPath } })
      }
    } else if (!status) {
      // 完全没有 HTTP 响应:后端未启动 / 端口不通 / 请求超时
      const timeout = err.code === 'ECONNABORTED' || /timeout/i.test(err.message || '')
      ElMessage({
        type: 'error',
        duration: 6000,
        showClose: true,
        message: timeout
          ? '请求超时：后端响应过慢，请稍后重试'
          : '无法连接服务器：请双击 start-backend.bat 启动后端（端口 8091）'
      })
    } else if (status >= 500) {
      ElMessage.error(`服务器异常(${status})：请查看后端窗口日志`)
    } else {
      ElMessage.error(msg || '请求失败')
    }
    return Promise.reject(err)
  }
)

export default request
