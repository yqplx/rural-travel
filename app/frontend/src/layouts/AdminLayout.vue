<template>
  <div class="admin-shell">
    <aside class="admin-aside">
      <div class="brand">🌾 <span>乡野智游·管理端</span></div>
      <el-menu :default-active="$route.path" router @select="onMenuSelect">
        <el-menu-item index="/admin/analytics">📊 <span>数据分析</span></el-menu-item>
        <el-menu-item index="/admin/scenic">🏞️ <span>景区管理</span></el-menu-item>
        <el-menu-item index="/admin/dict">🏷️ <span>分类标签</span></el-menu-item>
        <el-menu-item index="/admin/users">👥 <span>用户管理</span></el-menu-item>
        <el-menu-item index="/admin/ratings">⭐ <span>评分管理</span></el-menu-item>
        <el-menu-item index="/admin/virtual">🧪 <span>虚拟样本</span></el-menu-item>
        <el-menu-item index="/admin/knowledge">📚 <span>AI知识库</span></el-menu-item>
        <el-menu-item index="/admin/content">✍️ <span>内容工作台</span></el-menu-item>
        <el-menu-item index="/admin/logs">📜 <span>系统日志</span></el-menu-item>
        <el-menu-item index="/admin/guide-kg">🕸️ <span>县域知识图谱</span></el-menu-item>
        <el-menu-item index="/admin/guide-content">🎬 <span>多模态量产</span></el-menu-item>
        <el-menu-item index="/admin/guide-metrics">🏛️ <span>政策契合看板</span></el-menu-item>
        <!-- 独立全屏页，新窗口打开 -->
        <el-menu-item index="__bigscreen">📡 <span>可视化大屏</span></el-menu-item>
      </el-menu>
    </aside>
    <main class="admin-main">
      <div class="admin-head">
        <h2>{{ title }}</h2>
        <div>
          <el-button text @click="$router.push('/')">返回前台</el-button>
          <el-button text type="danger" @click="logout">退出登录</el-button>
        </div>
      </div>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auth } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const titles = {
  '/admin/analytics': '数据分析', '/admin/scenic': '景区管理', '/admin/dict': '分类与标签',
  '/admin/users': '用户管理', '/admin/ratings': '评分管理', '/admin/virtual': '虚拟样本',
  '/admin/knowledge': 'AI知识库', '/admin/content': '内容工作台', '/admin/logs': '系统日志',
  '/admin/guide-kg': '县域知识图谱', '/admin/guide-content': '多模态内容量产工作台',
  '/admin/guide-metrics': '政策契合度看板'
}
const title = computed(() => titles[route.path] || '管理端')
function logout() { auth.logout(); router.push('/') }

/* 可视化大屏是独立全屏页，不在管理端框架内嵌套，新窗口打开 */
function onMenuSelect(index) {
  if (index === '__bigscreen') window.open('/bigscreen', '_blank')
}
</script>
