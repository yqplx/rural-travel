import { reactive } from 'vue'

const saved = (() => {
  try { return JSON.parse(localStorage.getItem('rural_auth') || 'null') } catch { return null }
})()

export const auth = reactive({
  token: saved?.token || '',
  user: saved?.user || null,
  get isLogin() { return !!this.token },
  get isAdmin() { return this.user?.role === 'ADMIN' },
  login(data) {
    this.token = data.token
    this.user = data.user
    localStorage.setItem('rural_auth', JSON.stringify({ token: data.token, user: data.user }))
  },
  setUser(user) {
    this.user = user
    localStorage.setItem('rural_auth', JSON.stringify({ token: this.token, user }))
  },
  logout() {
    this.token = ''
    this.user = null
    localStorage.removeItem('rural_auth')
  }
})
