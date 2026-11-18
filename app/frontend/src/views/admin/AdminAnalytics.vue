<template>
  <div v-loading="loading">
    <template v-if="data">
      <el-alert type="success" :title="'运营总述:' + data.summary" :closable="false" style="margin-bottom:18px" />
      <div class="ana-grid">
        <div class="rt-card">
          <h3>近30天客流热力</h3>
          <div class="heat-grid">
            <div v-for="d in data.dailyHeat" :key="d.date" class="heat-cell" :class="'heat-l' + d.level"
                 :title="d.date + ' · ' + d.count + '次'">{{ d.date.slice(3) }}</div>
          </div>
        </div>
        <div class="rt-card">
          <h3>24小时客流分布</h3>
          <div class="hour-bars">
            <div v-for="(v, h) in data.hourDist" :key="h" class="hour-col" :title="h + '时 · ' + v + '次'">
              <div class="hour-fill" :style="{ height: hourPct(v) + '%' }"></div>
              <span v-if="h % 3 === 0" class="hour-label">{{ h }}</span>
            </div>
          </div>
          <div class="rt-muted" style="margin-top:14px; font-size:.88rem">
            周末客流占比 <b style="color:var(--rt-orange)">{{ data.weekendRatio }}%</b>
          </div>
        </div>
        <div class="rt-card">
          <h3>分类热度(浏览量 / 综合评分)</h3>
          <div v-for="c in data.categoryHeat" :key="c.name" class="bar-row">
            <span class="bar-label">{{ c.name }}</span>
            <div class="bar-track"><div class="bar-fill" :style="{ width: pct(c.browse, maxCatBrowse) + '%' }"></div></div>
            <span class="bar-val">{{ c.browse }}</span>
            <span class="rt-muted" style="width:50px">⭐{{ c.rating.toFixed(1) }}</span>
          </div>
        </div>
        <div class="rt-card">
          <h3>客源省份 Top10</h3>
          <div v-for="p in data.provinceTop" :key="p.province" class="bar-row">
            <span class="bar-label">{{ p.province }}</span>
            <div class="bar-track"><div class="bar-fill" :style="{ width: pct(p.count, maxProvince) + '%' }"></div></div>
            <span class="bar-val">{{ p.count }}</span>
          </div>
          <el-empty v-if="!data.provinceTop.length" description="暂无数据" :image-size="60" />
        </div>
        <div class="rt-card">
          <h3>价格偏好</h3>
          <div v-for="p in data.priceDist" :key="p.label" class="bar-row">
            <span class="bar-label">{{ p.label }}</span>
            <div class="bar-track"><div class="bar-fill" :style="{ width: pct(p.count, maxPrice) + '%' }"></div></div>
            <span class="bar-val">{{ p.count }}</span>
          </div>
        </div>
        <div class="rt-card">
          <h3>热门主题标签 Top10</h3>
          <div v-for="t in data.tagHeat" :key="t.name" class="bar-row">
            <span class="bar-label">{{ t.name }}</span>
            <div class="bar-track"><div class="bar-fill" :style="{ width: pct(t.count, maxTag) + '%' }"></div></div>
            <span class="bar-val">{{ t.count }}</span>
          </div>
        </div>
      </div>
      <h3 class="rt-section-title">运营建议</h3>
      <div class="sug-grid">
        <div v-for="(s, i) in data.suggestions" :key="i" class="rt-card sug">
          <b>💡 {{ s.title }}</b>
          <p class="rt-muted" style="margin-top:8px; font-size:.88rem; line-height:1.7">{{ s.detail }}</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '@/api'

const loading = ref(true)
const data = ref(null)

onMounted(async () => {
  try { data.value = await adminApi.analytics() } catch {}
  loading.value = false
})

const maxCatBrowse = computed(() => Math.max(1, ...(data.value?.categoryHeat || []).map(c => c.browse)))
const maxProvince = computed(() => Math.max(1, ...(data.value?.provinceTop || []).map(p => p.count)))
const maxPrice = computed(() => Math.max(1, ...(data.value?.priceDist || []).map(p => p.count)))
const maxTag = computed(() => Math.max(1, ...(data.value?.tagHeat || []).map(t => t.count)))
const maxHour = computed(() => Math.max(1, ...(data.value?.hourDist || [1])))

const pct = (v, max) => Math.round(v * 100 / max)
const hourPct = v => Math.max(2, Math.round(v * 100 / maxHour.value))
</script>

<style scoped>
.ana-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }
@media (max-width: 1000px) { .ana-grid { grid-template-columns: 1fr; } }
.ana-grid h3 { font-size: .98rem; margin-bottom: 14px; color: var(--rt-green-dark); }
.hour-bars { display: flex; align-items: flex-end; gap: 3px; height: 110px; }
.hour-col { flex: 1; display: flex; flex-direction: column; justify-content: flex-end; align-items: center; height: 100%; position: relative; }
.hour-fill { width: 100%; background: linear-gradient(180deg, var(--rt-green), #6FA07E); border-radius: 3px 3px 0 0; }
.hour-label { position: absolute; bottom: -18px; font-size: .65rem; color: var(--rt-muted); }
.sug-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 14px; }
.sug { border-left: 4px solid var(--rt-orange); }
</style>
