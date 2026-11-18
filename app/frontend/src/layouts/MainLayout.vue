<template>
  <div>
    <nav class="rt-nav">
      <div class="rt-nav-inner">
        <div class="rt-logo" @click="$router.push('/')">乡野<span>智游</span></div>
        <div class="rt-nav-links">
          <router-link to="/" exact-active-class="active">首页</router-link>
          <router-link to="/scenic" active-class="active">景区</router-link>
          <router-link to="/recognize" active-class="active">识景</router-link>
          <router-link to="/guide/graph" active-class="active">图谱</router-link>
          <router-link to="/guide/plan" active-class="active">路线</router-link>
          <router-link to="/guide/live" active-class="active">导览</router-link>
          <router-link v-if="auth.isLogin" to="/profile" active-class="active">个人中心</router-link>
        </div>
        <template v-if="!auth.isLogin">
          <el-button text @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" round @click="$router.push('/register')">注册</el-button>
        </template>
        <el-dropdown v-else @command="onCommand">
          <span style="cursor:pointer; display:flex; align-items:center; gap:6px;">
            <el-avatar :size="30" :src="auth.user?.avatar">{{ auth.user?.nickname?.[0] || '游' }}</el-avatar>
            {{ auth.user?.nickname || auth.user?.username }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item v-if="auth.isAdmin" command="admin">管理后台</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </nav>
    <router-view />
    <footer class="rt-footer">乡野智游 · 轻量化AI乡村文旅智能引流与服务系统 · 看得见山，望得见水，记得住乡愁</footer>
    <AiAssistant />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { auth } from '@/store/auth'
import AiAssistant from '@/components/AiAssistant.vue'

const router = useRouter()
function onCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'admin') router.push('/admin')
  else if (cmd === 'logout') { auth.logout(); router.push('/') }
}
</script>
