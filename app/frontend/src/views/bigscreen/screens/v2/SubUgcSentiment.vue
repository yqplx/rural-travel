<template>
  <div class="scr">
    <div class="kpis">
      <BsKpi label="UGC 总量（近 30 天）" :value="s.total.toLocaleString()" tone="cyan" delta="日增 1,620" />
      <BsKpi label="图文" :value="s.text.toLocaleString()" delta="占比 64.3%" />
      <BsKpi label="图片 / 视频" :value="(s.image + s.video).toLocaleString()" tone="cyan" delta="视觉内容 29.7%" />
      <BsKpi label="语音" :value="s.audio.toLocaleString()" delta="方言识别准确率 91%" />
    </div>

    <BsPanel class="pa-sent" title="情感倾向构成" en="sentiment">
      <BsChart :option="sentOption" />
    </BsPanel>
    <BsPanel class="pa-plat" title="平台来源分布" en="platforms">
      <BsChart :option="platOption" />
    </BsPanel>
    <BsPanel class="pa-cloud" title="高频话题词云" en="topics">
      <BsChart :option="cloudOption" />
    </BsPanel>
    <BsPanel class="pa-trend" title="24 小时情感走势" en="sentiment trend">
      <BsChart :option="trendOption" />
    </BsPanel>
    <BsPanel class="pa-hot" title="热点内容榜" en="hot ugc">
      <div class="hot" v-for="(u, i) in hot" :key="u.title">
        <span class="rk" :class="i < 3 && 'top'">{{ i + 1 }}</span>
        <span class="ti">{{ u.title }}</span>
        <span class="pf">{{ u.platform }}</span>
        <span class="st" :class="u.sentiment === '正面' ? 'ok' : u.sentiment === '负面' ? 'bad' : ''">{{ u.sentiment }}</span>
        <span class="ht">{{ u.heat }}</span>
      </div>
    </BsPanel>
  </div>
</template>

<script setup lang="ts">
/* ============ 副屏3：多模态 UGC 舆情 ============ */
import 'echarts-wordcloud'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import BsPanel from '../../components/BsPanel.vue'
import BsKpi from '../../components/BsKpi.vue'
import BsChart from '../../components/BsChart.vue'
import { ugcSummary, sentiment, ugcPlatform, ugcWords, ugcHot, ugcTrend } from '../../data/mock.v2'
import { poll, wobble, POLL } from '../../data/provider'

const s = ugcSummary
const words = ref(ugcWords.map(x => ({ ...x })))
const hot = ref(ugcHot.map(x => ({ ...x })))
let stop: (() => void) | null = null

function tick(): void {
  words.value = wobble(words.value, 'value', 0.05)
  hot.value = hot.value.map(x => ({ ...x, heat: Math.max(30, Math.min(100, x.heat + Math.round((Math.random() - 0.45) * 6))) }))
    .sort((a, b) => b.heat - a.heat)
}
onMounted(() => { stop = poll(tick, POLL.charts) })
onBeforeUnmount(() => stop && stop())

const AX = { axisLine: { lineStyle: { color: '#2e5578' } }, axisLabel: { color: '#7fa8c9', fontSize: 11 } }

const sentOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c}%' },
  legend: { bottom: 2, textStyle: { color: '#8fb4d4', fontSize: 11 }, itemWidth: 12, itemHeight: 8 },
  series: [{
    type: 'pie', radius: ['48%', '70%'], center: ['50%', '45%'],
    label: { show: false }, itemStyle: { borderColor: '#0a1a30', borderWidth: 2 },
    data: [
      { ...sentiment[0], itemStyle: { color: '#4fe0c8' } },
      { ...sentiment[1], itemStyle: { color: '#4fc3f7' } },
      { ...sentiment[2], itemStyle: { color: '#ff8f7e' } }
    ]
  }],
  graphic: [
    { type: 'text', left: 'center', top: '38%', style: { text: '68.4%', fill: '#4fe0c8', fontSize: 26, fontWeight: 700 } },
    { type: 'text', left: 'center', top: '52%', style: { text: '正面占比', fill: '#8fb4d4', fontSize: 12 } }
  ]
}))
const platOption = computed(() => ({
  grid: { left: 70, right: 40, top: 10, bottom: 8 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'value', show: false },
  yAxis: {
    type: 'category', data: ugcPlatform.map(x => x.name).reverse(),
    axisLine: { lineStyle: { color: '#2e5578' } }, axisLabel: { color: '#b9d4ea', fontSize: 12 }
  },
  series: [{
    type: 'bar', barWidth: 12, roundCap: true,
    data: ugcPlatform.map(x => x.v).reverse(),
    itemStyle: { borderRadius: 5, color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#1d6ea8' }, { offset: 1, color: '#4fc3f7' }] } },
    label: { show: true, position: 'right', color: '#8fb4d4', fontSize: 11 }
  }]
}))
const cloudOption = computed(() => ({
  series: [{
    type: 'wordCloud', shape: 'circle', width: '96%', height: '96%',
    sizeRange: [12, 46], rotationRange: [0, 0], gridSize: 6,
    textStyle: {
      fontFamily: 'Microsoft YaHei',
      color: () => ['#4fe0c8', '#4fc3f7', '#8fb4d4', '#ffc86e', '#ff9f6e'][Math.floor(Math.random() * 5)]
    },
    data: words.value
  }]
}))
const trendOption = computed(() => ({
  grid: { left: 40, right: 14, top: 24, bottom: 22 },
  tooltip: { trigger: 'axis' },
  legend: { data: ['正面', '负面'], textStyle: { color: '#8fb4d4', fontSize: 11 }, itemWidth: 12, itemHeight: 8 },
  xAxis: { type: 'category', boundaryGap: false, data: ugcTrend.map(x => x.h), ...AX },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(46,85,120,.35)' } }, axisLabel: { color: '#7fa8c9', fontSize: 11 } },
  series: [
    { name: '正面', type: 'line', smooth: true, data: ugcTrend.map(x => x.pos), lineStyle: { color: '#4fe0c8', width: 2 }, areaStyle: { color: 'rgba(79,224,200,.28)' }, itemStyle: { color: '#4fe0c8' } },
    { name: '负面', type: 'line', smooth: true, data: ugcTrend.map(x => x.neg), lineStyle: { color: '#ff8f7e', width: 2 }, areaStyle: { color: 'rgba(255,143,126,.22)' }, itemStyle: { color: '#ff8f7e' } }
  ]
}))
</script>

<style scoped>
.scr {
  height: 100%;
  display: grid;
  gap: 12px;
  grid-template-columns: 1fr 1.2fr 1fr;
  grid-template-rows: 88px 1fr 1fr;
  grid-template-areas: "kpi kpi kpi" "sent cloud plat" "trend hot hot";
  min-height: 0;
}
.kpis { grid-area: kpi; display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.pa-sent { grid-area: sent; }
.pa-plat { grid-area: plat; }
.pa-cloud { grid-area: cloud; }
.pa-trend { grid-area: trend; }
.pa-hot { grid-area: hot; }
.hot {
  display: flex; align-items: center; gap: 10px; font-size: 13px;
  padding: 6px 4px; border-bottom: 1px dashed rgba(74, 144, 197, .16);
}
.hot .rk { flex: 0 0 20px; text-align: center; color: #4c7396; }
.hot .rk.top { color: #ffc86e; font-weight: 700; }
.hot .ti { flex: 1; color: #cfe3f5; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hot .pf { flex: 0 0 auto; color: #5f86a8; font-size: 12px; }
.hot .st { flex: 0 0 auto; font-size: 12px; color: #8fb4d4; }
.hot .st.ok { color: #4fe0c8; }
.hot .st.bad { color: #ff8f7e; }
.hot .ht { flex: 0 0 34px; text-align: right; color: #4fc3f7; font-variant-numeric: tabular-nums; }
</style>
