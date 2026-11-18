<template>
  <div class="auth-wrap">
    <div class="rt-card auth-card">
      <h1 class="rt-serif auth-title">欢迎回来</h1>
      <p class="auth-sub">登录乡野智游,继续你的田园之旅</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password
                    @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">登 录</el-button>
      </el-form>
      <div class="auth-link">还没有账号?<router-link to="/register">立即注册</router-link> · <router-link to="/">先逛逛</router-link></div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { auth } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate().catch(() => Promise.reject())
  loading.value = true
  try {
    const res = await authApi.login(form)
    auth.login(res)
    ElMessage.success('欢迎回来,' + (res.user.nickname || res.user.username))
    router.push(route.query.redirect || (res.user.role === 'ADMIN' ? '/admin' : '/'))
  } catch {} finally { loading.value = false }
}
</script>
