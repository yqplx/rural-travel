<template>
  <div class="scr scr2">
    <div class="kpis">
      <BsKpi label="平均游览时长（小时）" :value="3.6" tone="cyan" delta="较昨日 +0.2" />
      <BsKpi label="语音讲解播放（次）" :value="voice.played" :delta="`完读率 ${voice.finishRate}%`" />
      <BsKpi label="语音问答量（次）" :value="qa.total" tone="cyan"
             :delta="`溯源占比 ${Math.round(qa.grounded / qa.total * 100)}%`" />
      <BsKpi label="路线重规划（今日）" :value="42" suffix="次" delta="全部自动触发" up />
    </div>

    <BsPanel title="景点访问排行 TOP" en="visit ranking" area="rank">
      <BsChart :option="rankOption" />
    </BsPanel>
    <BsPanel title="满意度评分分布" en="rating dist" area="dist">
      <BsChart :option="distOption" />
    </BsPanel>

    <BsPanel title="高频问答词云" en="qa word cloud" area="cloud">
      <BsChart :option="cloudOption" />
    </BsPanel>
    <BsPanel title="游客满意度 7 日趋势" en="satisfaction trend" area="trend">
      <BsChart :option="trendOption" />
    </BsPanel>

    <BsPanel title="语音问答 · 回答构成" en="qa composition" area="qa">
      <BsChart :option="qaOption" />
    </BsPanel>
    <BsPanel title="语音讲解播放" en="voice playback" area="voice">
      <BsChart :option="voiceOption" />
    </BsPanel>
  </div>
</template>

<script setup>
/* ============ 屏2：游客体验与导览效果大屏 ============ */
import 'echarts-wordcloud'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import BsPanel from '../components/BsPanel.vue'
import BsKpi from '../components/BsKpi.vue'
import BsChart from '../components/BsChart.vue'
import { visitRank, satisfactionDist, satisfactionTrend, qaStats, qaWordCloud, voiceStats } from '../data/mock'
import { drift, poll, wobble, POLL } from '../data/provider'

const AXIS = { axisLine: { lineStyle: { color: '#2e5578' } }, axisLabel: { color: '#7fa8c9', fontSize: 12 } }

/* 动态刷新：排行 / 播放 / 问答 / 满意度趋势每 30 秒小幅重算 */
const rank = ref(visitRank.map(x => ({ ...x })))
const voice = ref({ ...voiceStats })
const qa = ref({ ...qaStats })
const trend = ref(satisfactionTrend.map(x => ({ ...x })))
let stopCharts = null

function tickCharts() {
  rank.value = wobble(rank.value, 'v', 0.03).sort((a, b) => b.v - a.v)
  voice.value = {
    ...voice.value,
    played: voice.value.played + Math.floor(Math.random() * 9),
    finished: voice.value.finished + Math.floor(Math.random() * 8)
  }
  voice.value.finishRate = Math.min(99, Math.round(voice.value.finished / voice.value.played * 100))
  const add = Math.floor(Math.random() * 6)
  qa.value = {
    total: qa.value.total + add,
    grounded: qa.value.grounded + Math.max(0, add - 1),
    llm: qa.value.llm + (add > 3 ? 1 : 0),
    hallucination: qa.value.hallucination + (Math.random() > 0.7 ? 1 : 0)
  }
  const n = trend.value.length
  trend.value = trend.value.map((x, i) =>
    i === n - 1 ? { ...x, v: Math.max(88, Math.min(100, x.v + Math.round((Math.random() - 0.5) * 2))) } : x)
}

onMounted(() => { stopCharts = poll(tickCharts, POLL.charts) })
onBeforeUnmount(() => stopCharts && stopCharts())

/* 景点访问排行（横向条） */
const rankOption = computed(() => ({
  grid: { left: 96, right: 40, top: 10, bottom: 8 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'value', show: false },
  yAxis: {
    type: 'category',
    data: rank.value.map(x => x.name).reverse(),
    ...AXIS, axisLabel: { color: '#b9d4ea', fontSize: 13 }
  },
  series: [{
    type: 'bar', barWidth: 12, roundCap: true,
    data: rank.value.map(x => x.v).reverse(),
    itemStyle: {
      borderRadius: 6,
      color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#1d6ea8' }, { offset: 1, color: '#4fe0c8' }] }
    },
    label: { show: true, position: 'right', color: '#8fb4d4', fontSize: 12 }
  }]
}))

/* 满意度评分分布 */
const distOption = computed(() => ({
  grid: { left: 40, right: 14, top: 26, bottom: 26 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: satisfactionDist.map(x => x.name), ...AXIS },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(46,85,120,.35)' } }, axisLabel: { color: '#7fa8c9' } },
  series: [{
    type: 'bar', barWidth: 26,
    data: satisfactionDist.map(x => ({ value: x.v, itemStyle: { color: x.name === '5星' ? '#4fe0c8' : x.name === '1星' ? '#ff8f7e' : '#4fc3f7', borderRadius: [4, 4, 0, 0] } })),
    label: { show: true, position: 'top', color: '#8fb4d4', formatter: '{c}%' }
  }]
}))

/* 词云 */
const cloudOption = computed(() => ({
  series: [{
    type: 'wordCloud',
    shape: 'circle', width: '96%', height: '96%',
    sizeRange: [14, 52], rotationRange: [0, 0], gridSize: 8,
    textStyle: {
      fontFamily: 'Microsoft YaHei',
      color: () => ['#4fe0c8', '#4fc3f7', '#8fb4d4', '#ffc86e'][Math.floor(Math.random() * 4)]
    },
    data: qaWordCloud
  }]
}))

/* 满意度 7 日趋势 */
const trendOption = computed(() => ({
  grid: { left: 40, right: 18, top: 26, bottom: 26 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', boundaryGap: false, data: trend.value.map(x => x.d), ...AXIS },
  yAxis: { type: 'value', min: 85, max: 100, splitLine: { lineStyle: { color: 'rgba(46,85,120,.35)' } }, axisLabel: { color: '#7fa8c9' } },
  series: [{
    type: 'line', smooth: true, symbolSize: 6,
    data: trend.value.map(x => x.v),
    lineStyle: { color: '#4fe0c8', width: 2.5 }, itemStyle: { color: '#4fe0c8' },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(79,224,200,.35)' }, { offset: 1, color: 'rgba(79,224,200,0)' }] } }
  }]
}))

/* 问答构成：溯源 / 大模型 / 幻觉 */
const qaOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c} 次（{d}%）' },
  legend: { bottom: 4, textStyle: { color: '#8fb4d4', fontSize: 12 }, itemWidth: 12, itemHeight: 8 },
  series: [{
    type: 'pie', radius: ['46%', '70%'], center: ['50%', '44%'],
    label: { show: false }, itemStyle: { borderColor: '#0a1a30', borderWidth: 2 },
    data: [
      { name: '知识图谱溯源', value: qa.value.grounded, itemStyle: { color: '#4fe0c8' } },
      { name: '大模型生成', value: qa.value.llm, itemStyle: { color: '#4fc3f7' } },
      { name: '幻觉拦截', value: qa.value.hallucination, itemStyle: { color: '#ff8f7e' } }
    ]
  }],
  graphic: [
    { type: 'text', left: 'center', top: '38%', style: { text: qa.value.grounded + '', fill: '#4fe0c8', fontSize: 28, fontWeight: 700 } },
    { type: 'text', left: 'center', top: '50%', style: { text: '次命中溯源', fill: '#8fb4d4', fontSize: 12 } }
  ]
}))

/* 语音播放：完读率环 + 数字 */
const voiceOption = computed(() => ({
  series: [{
    type: 'gauge', radius: '88%', center: ['50%', '56%'],
    startAngle: 210, endAngle: -30, min: 0, max: 100,
    progress: { show: true, width: 12, roundCap: true, itemStyle: { color: '#4fc3f7' } },
    axisLine: { lineStyle: { width: 12, color: [[1, 'rgba(46,85,120,.5)']] } },
    pointer: { show: false }, axisTick: { show: false }, splitLine: { show: false },
    axisLabel: { color: '#5f86a8', distance: 16, fontSize: 11 },
    detail: { valueAnimation: true, formatter: '{value}%', color: '#4fc3f7', fontSize: 38, fontWeight: 700, offsetCenter: [0, '6%'] },
    data: [{ value: voice.value.finishRate }],
    title: { offsetCenter: [0, '44%'], color: '#8fb4d4', fontSize: 13, formatter: () => `完读 ${voice.value.finished} / 播放 ${voice.value.played}` }
  }]
}))
</script>

<style scoped>
.scr2 {
  height: 100%;
  display: grid;
  gap: 14px;
  grid-template-columns: 1fr 1.2fr 1fr;
  grid-template-rows: 96px 1fr 1fr;
  grid-template-areas:
    "kpi   kpi   kpi"
    "rank  cloud qa"
    "dist  trend voice";
  min-height: 0;
}
.kpis {
  grid-area: kpi;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
</style>
