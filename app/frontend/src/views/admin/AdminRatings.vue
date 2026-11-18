<template>
  <div>
    <div class="rt-card" style="margin-bottom:16px">
      <el-form inline @submit.prevent>
        <el-form-item label="景区">
          <el-select v-model="spotId" clearable filterable placeholder="全部景区" style="width:220px" @change="load(1)">
            <el-option v-for="s in spots" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </el-form>
    </div>
    <div class="rt-card">
      <el-table v-loading="loading" :data="list">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="scenicName" label="景区" min-width="150" />
        <el-table-column label="用户" width="130">
          <template #default="{ row }">{{ row.nickname || row.username }}</template>
        </el-table-column>
        <el-table-column label="评分" width="150">
          <template #default="{ row }"><el-rate :model-value="row.score" disabled size="small" /></template>
        </el-table-column>
        <el-table-column prop="comment" label="评价" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column width="90" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="删除该评分?" @confirm="remove(row.id)">
              <template #reference><el-button size="small" text type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="10"
                     style="justify-content:center; margin-top:14px" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi, scenicApi } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const spotId = ref(null)
const spots = ref([])

onMounted(async () => {
  try { spots.value = (await scenicApi.page({ size: 100 })).records } catch {}
  load(1)
})
async function load(p) {
  loading.value = true
  try {
    const res = await adminApi.ratings({ current: p, size: 10, scenicSpotId: spotId.value })
    list.value = res.records; total.value = res.total
  } catch {}
  loading.value = false
}
async function remove(id) {
  try { await adminApi.deleteRating(id); ElMessage.success('已删除,景区均分已刷新'); load(1) } catch {}
}
</script>
