<template>
  <div class="rt-card">
    <el-form inline style="margin-bottom:10px" @submit.prevent>
      <el-form-item label="关键词"><el-input v-model="keyword" clearable placeholder="操作/用户名" style="width:200px" @change="load(1)" /></el-form-item>
      <el-button type="primary" @click="load(1)">查询</el-button>
    </el-form>
    <el-table v-loading="loading" :data="list">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="操作人" width="130" />
      <el-table-column prop="action" label="操作" width="120" />
      <el-table-column prop="detail" label="详情" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="时间" width="180" />
    </el-table>
    <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="15"
                   style="justify-content:center; margin-top:14px" @current-change="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminApi } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')

onMounted(() => load(1))
async function load(p) {
  loading.value = true
  try {
    const res = await adminApi.logs({ current: p, size: 15, keyword: keyword.value })
    list.value = res.records; total.value = res.total
  } catch {}
  loading.value = false
}
</script>
