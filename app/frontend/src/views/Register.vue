<template>
  <div class="auth-wrap">
    <div class="rt-card auth-card">
      <h1 class="rt-serif auth-title">加入乡野智游</h1>
      <p class="auth-sub">注册账号,收藏心仪的乡村好去处</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
        <el-form-item prop="username"><el-input v-model="form.username" placeholder="用户名(3-20位字母/数字/下划线)" /></el-form-item>
        <el-form-item prop="nickname"><el-input v-model="form.nickname" placeholder="昵称(选填)" /></el-form-item>
        <el-form-item prop="province">
          <el-select v-model="form.province" placeholder="所在省份(选填)" style="width:100%" clearable>
            <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item prop="password"><el-input v-model="form.password" type="password" placeholder="密码(6-32位)" show-password /></el-form-item>
        <el-form-item prop="confirm"><el-input v-model="form.confirm" type="password" placeholder="确认密码" show-password @keyup.enter="submit" /></el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">注 册</el-button>
      </el-form>
      <div class="auth-link">已有账号?<router-link to="/login">直接登录</router-link></div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { auth } from '@/store/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const provinces = ['北京', '天津', '河北', '山西', '内蒙古', '辽宁', '吉林', '黑龙江', '上海', '江苏', '浙江', '安徽', '福建', '江西', '山东', '河南', '湖北', '湖南', '广东', '广西', '海南', '重庆', '四川', '贵州', '云南', '西藏', '陕西', '甘肃', '青海', '宁夏', '新疆']
const form = reactive({ username: '', nickname: '', province: '', password: '', confirm: '' })
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '3-20位字母/数字/下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为6-32位', trigger: 'blur' }
  ],
  confirm: [{
    validator: (r, v, cb) => v === form.password ? cb() : cb(new Error('两次输入的密码不一致')),
    trigger: 'blur'
  }]
}

async function submit() {
  await formRef.value.validate().catch(() => Promise.reject())
  loading.value = true
  try {
    const res = await authApi.register({
      username: form.username, password: form.password,
      nickname: form.nickname, province: form.province
    })
    auth.login(res)
    ElMessage.success('注册成功,已自动登录')
    router.push('/')
  } catch {} finally { loading.value = false }
}
</script>
