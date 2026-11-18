<template>
  <div class="rt-page">
    <div class="rt-card" style="margin-bottom:22px">
      <el-form inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="名称/特色/城市" clearable style="width:180px" @change="search" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" placeholder="全部" clearable style="width:130px" @change="search">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="query.tagId" placeholder="全部" clearable style="width:130px" @change="search">
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="query.minPrice" :min="0" placeholder="最低" style="width:100px" @change="search" />
          <span style="margin:0 6px">-</span>
          <el-input-number v-model="query.maxPrice" :min="0" placeholder="最高" style="width:100px" @change="search" />
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="query.sort" style="width:130px" @change="search">
            <el-option label="综合口碑" value="" />
            <el-option label="价格从低到高" value="price_asc" />
            <el-option label="价格从高到低" value="price_desc" />
            <el-option label="人气优先" value="view" />
            <el-option label="最新上架" value="newest" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-loading="loading" class="rt-grid">
      <ScenicCard v-for="s in list" :key="s.id" :spot="s" />
    </div>
    <el-empty v-if="!loading && !list.length" description="没有找到符合条件的景区,换个条件试试" />
    <div style="display:flex; justify-content:center; margin-top:26px">
      <el-pagination background layout="total, prev, pager, next" :total="total"
                     :page-size="query.size" :current-page="query.current"
                     @current-change="p => { query.current = p; load() }" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { categoryApi, scenicApi, tagApi } from '@/api'
import ScenicCard from '@/components/ScenicCard.vue'

const route = useRoute()
const categories = ref([])
const tags = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(true)
const query = reactive({
  keyword: route.query.keyword || '',
  categoryId: route.query.categoryId ? Number(route.query.categoryId) : null,
  tagId: null, minPrice: null, maxPrice: null, sort: '', current: 1, size: 12
})

onMounted(async () => {
  try { categories.value = await categoryApi.list() } catch {}
  try { tags.value = await tagApi.list() } catch {}
  load()
})

async function load() {
  loading.value = true
  try {
    const res = await scenicApi.page(query)
    list.value = res.records
    total.value = res.total
  } catch {}
  loading.value = false
}
function search() { query.current = 1; load() }
function reset() {
  Object.assign(query, { keyword: '', categoryId: null, tagId: null, minPrice: null, maxPrice: null, sort: '', current: 1 })
  load()
}
</script>
