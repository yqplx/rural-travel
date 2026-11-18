<template>
  <!-- 后端掉线自检提示:每 20 秒探测一次,恢复后自动消失 -->
  <transition name="bar-fade">
    <div v-if="backendDown" class="backend-down">
      <span>⚠️ 后端服务未连接：请双击 <b>start-backend.bat</b> 启动后端（端口 8091），启动后本提示会自动消失</span>
    </div>
  </transition>

  <router-view />
  <!-- 开屏链路:系统加载页 → 欢迎动画 → 主页 -->
  <Loading v-if="phase === 'loading'" :ready="ready" @finish="phase = 'welcome'" />
  <Welcome v-if="phase === 'welcome'" @finish="phase = 'done'" />
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import Loading from '@/views/Loading.vue'
import Welcome from '@/views/Welcome.vue'
import { categoryApi } from '@/api'

const phase = ref('loading')
const ready = ref(false)
const backendDown = ref(false)
let pingTimer = null

// 用原生 fetch 探测,避免走 axios 拦截器反复弹错误提示
async function pingBackend() {
  try {
    const r = await fetch('/api/categories', { cache: 'no-store' })
    backendDown.value = !r.ok
  } catch {
    backendDown.value = true
  }
}

onMounted(async () => {
  try { await categoryApi.list() } catch { /* 后端未就绪也照常进入 */ }
  ready.value = true
  pingBackend()
  pingTimer = setInterval(pingBackend, 20000)
})

onBeforeUnmount(() => clearInterval(pingTimer))
</script>

<style scoped>
.backend-down {
  position: sticky;
  top: 0;
  z-index: 9999;
  background: #fff1f0;
  color: #c0392b;
  border-bottom: 1px solid #ffccc7;
  padding: 10px 16px;
  text-align: center;
  font-size: 14px;
  line-height: 1.6;
}
.backend-down b {
  background: #ffd8d0;
  padding: 1px 6px;
  border-radius: 4px;
  font-family: Consolas, monospace;
}
.bar-fade-enter-active,
.bar-fade-leave-active { transition: opacity .3s ease; }
.bar-fade-enter-from,
.bar-fade-leave-to { opacity: 0; }
</style>
