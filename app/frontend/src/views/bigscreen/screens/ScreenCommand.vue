<template>
  <div class="scr scr1">
    <!-- KPI 行 -->
    <div class="kpis">
      <BsKpi label="实时在园人数（人）" :value="live" tone="cyan" delta="较昨日 +6.4%" />
      <BsKpi label="今日累计游客（人次）" :value="8743" delta="较昨日 +9.1%" />
      <BsKpi label="智能导览服务（次）" :value="5372" tone="cyan" delta="较昨日 +12.6%" />
      <BsKpi label="游客满意度" :value="96" suffix="%" delta="连续 3 日上升" />
    </div>

    <BsPanel title="今日实时客流趋势" en="hourly flow" area="flow">
      <BsChart :option="flowOption" />
    </BsPanel>
    <BsPanel title="游客画像 · 兴趣标签" en="visitor profile" area="radar">
      <BsChart :option="radarOption" />
    </BsPanel>

    <BsPanel title="县域景点点位热力" en="heatmap map" area="map">
      <BsChart :option="mapOption" />
    </BsPanel>
    <BsPanel title="拥堵与运行告警" en="live alerts" area="alert">
      <BsLogList :seed="seed" :pool="pool" :interval="5000" :max-rows="4" />
    </BsPanel>

    <BsPanel title="全县平均拥挤度" en="crowding gauge" area="gauge">
      <BsChart :option="gaugeOption" />
    </BsPanel>
    <BsPanel title="游览路线完成率" en="route completion" area="route">
      <BsChart :option="routeOption" />
    </BsPanel>
    <BsPanel title="游客年龄结构" en="age mix" area="age">
      <BsChart :option="ageOption" />
    </BsPanel>
  </div>
</template>

<script setup>
/* ============ 屏1：总览指挥大屏（全局态势） ============ */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import BsPanel from '../components/BsPanel.vue'
import BsKpi from '../components/BsKpi.vue'
import BsChart from '../components/BsChart.vue'
import BsLogList from '../components/BsLogList.vue'
import { drift, poll, POLL } from '../data/provider'
import { flowHourly, interestRadar, mapPoints, ageMix, alertLogSeed, alertLogPool } from '../data/mock'

const live = ref(5186)
const crowdAvg = ref(62)
const flow = ref(flowHourly.map(x => ({ ...x })))
let timer = null
let stopCharts = null

/* 图表级动态刷新：末小时客流与全县拥挤度每 30 秒重算（接真实接口后换成重新拉数） */
function tickCharts() {
  const n = flow.value.length
  flow.value = flow.value.map((x, i) => (i === n - 1 ? { ...x, v: drift(x.v, 260, 600) } : x))
  crowdAvg.value = Math.max(35, Math.min(92, crowdAvg.value + Math.round((Math.random() - 0.5) * 6)))
}

onMounted(() => {
  timer = setInterval(() => { live.value = drift(live.value, 40, 3000) }, POLL.realtime)
  stopCharts = poll(tickCharts, POLL.charts)
})
onBeforeUnmount(() => { timer && clearInterval(timer); stopCharts && stopCharts() })

const seed = alertLogSeed
const pool = alertLogPool

/* 客流折线 */
const flowOption = computed(() => ({
  grid: { left: 46, right: 16, top: 26, bottom: 26 },
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category', boundaryGap: false,
    data: flow.value.map(x => x.h),
    axisLine: { lineStyle: { color: '#2e5578' } },
    axisLabel: { color: '#7fa8c9', fontSize: 12 }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(46,85,120,.35)' } },
    axisLabel: { color: '#7fa8c9', fontSize: 12 }
  },
  series: [{
    type: 'line', smooth: true, symbolSize: 6,
    data: flow.value.map(x => x.v),
    lineStyle: { color: '#3fc3e0', width: 2.5 },
    itemStyle: { color: '#4fe0c8' },
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: 'rgba(63,195,224,.4)' }, { offset: 1, color: 'rgba(63,195,224,0)' }]
      }
    }
  }]
}))

/* 画像雷达 */
const radarOption = computed(() => ({
  radar: {
    indicator: interestRadar.dims.map(n => ({ name: n, max: 100 })),
    radius: '62%', center: ['50%', '52%'],
    axisName: { color: '#8fb4d4', fontSize: 12 },
    splitLine: { lineStyle: { color: 'rgba(46,85,120,.5)' } },
    splitArea: { areaStyle: { color: ['rgba(20,52,88,.25)', 'rgba(13,34,60,.15)'] } },
    axisLine: { lineStyle: { color: 'rgba(46,85,120,.6)' } }
  },
  legend: {
    bottom: 0, textStyle: { color: '#7fa8c9', fontSize: 12 },
    data: ['今日', '上周'], itemWidth: 12, itemHeight: 8
  },
  series: [{
    type: 'radar',
    data: [
      { name: '今日', value: interestRadar.today, lineStyle: { color: '#4fe0c8', width: 2 }, areaStyle: { color: 'rgba(79,224,200,.22)' }, itemStyle: { color: '#4fe0c8' } },
      { name: '上周', value: interestRadar.lastWeek, lineStyle: { color: '#4fc3f7', width: 1.5, type: 'dashed' }, areaStyle: { color: 'rgba(79,195,247,.1)' }, itemStyle: { color: '#4fc3f7' } }
    ]
  }]
}))

/* 点位热力：effectScatter 脉冲点 + 暗网格 */
const mapOption = computed(() => ({
  grid: { left: 14, right: 14, top: 12, bottom: 14 },
  xAxis: { type: 'value', max: 100, show: false },
  yAxis: { type: 'value', max: 100, show: false },
  tooltip: {
    formatter: p => `${p.value[3]}<br/>人气热度 ${p.value[2]} · 拥挤 ${p.value[2] > 80 ? '高' : p.value[2] > 55 ? '中' : '低'}`
  },
  series: [
    {
      type: 'scatter', symbolSize: 44,
      data: mapPoints.map(p => [p.x, p.y, p.v, p.name]),
      itemStyle: { color: 'rgba(63,195,224,.14)', shadowBlur: 18, shadowColor: 'rgba(63,195,224,.35)' },
      label: { show: true, formatter: p => p.value[3], color: '#9fc6e8', fontSize: 11, position: 'right' },
      silent: true, zlevel: 1
    },
    {
      type: 'effectScatter', rippleEffect: { scale: 2.6, brushType: 'stroke' },
      data: mapPoints.map(p => ({ value: [p.x, p.y, p.v, p.name], symbolSize: 8 + p.v / 12 })),
      itemStyle: {
        color: p => p.value[2] >= 80 ? '#ff9f6e' : p.value[2] >= 55 ? '#4fe0c8' : '#4fc3f7',
        shadowBlur: 14
      },
      label: { show: true, formatter: p => p.value[2], color: '#eaf6ff', fontSize: 11, position: 'top' },
      zlevel: 2
    }
  ]
}))

/* 拥挤度仪表盘 */
const gaugeOption = computed(() => ({
  series: [{
    type: 'gauge', radius: '86%', center: ['50%', '58%'],
    startAngle: 210, endAngle: -30, min: 0, max: 100,
    progress: { show: true, width: 12, roundCap: true, itemStyle: { color: '#ffb15e' } },
    axisLine: { lineStyle: { width: 12, color: [[1, 'rgba(46,85,120,.5)']] } },
    pointer: { show: false },
    axisTick: { show: false }, splitLine: { show: false },
    axisLabel: { color: '#5f86a8', distance: 18, fontSize: 11 },
    detail: {
      valueAnimation: true, formatter: '{value}%',
      color: '#ffc86e', fontSize: 40, fontWeight: 700, offsetCenter: [0, '8%']
    },
    data: [{ value: crowdAvg.value }],
    title: { offsetCenter: [0, '42%'], color: '#8fb4d4', fontSize: 13 }
  }]
}))

/* 路线完成率 环形 */
const routeOption = computed(() => ({
  series: [{
    type: 'pie', radius: ['62%', '78%'], center: ['50%', '50%'],
    startAngle: 90, label: { show: false }, silent: true,
    data: [
      { value: 87, itemStyle: { color: '#4fe0c8' } },
      { value: 13, itemStyle: { color: 'rgba(46,85,120,.45)' } }
    ]
  }, {
    type: 'pie', radius: ['0%', '52%'], center: ['50%', '50%'],
    label: { show: false }, silent: true,
    data: [
      { value: 1114, itemStyle: { color: 'rgba(63,195,224,.55)' } },
      { value: 166, itemStyle: { color: 'rgba(46,85,120,.35)' } }
    ]
  }],
  graphic: [
    { type: 'text', left: 'center', top: '42%', style: { text: '87%', fill: '#4fe0c8', fontSize: 30, fontWeight: 700 } },
    { type: 'text', left: 'center', top: '58%', style: { text: '完成 1,114 / 规划 1,280', fill: '#8fb4d4', fontSize: 12 } }
  ]
}))

/* 年龄结构 环形 */
const ageOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c}%' },
  legend: { right: 6, top: 'middle', orient: 'vertical', textStyle: { color: '#8fb4d4', fontSize: 12 }, itemWidth: 12, itemHeight: 8 },
  series: [{
    type: 'pie', radius: ['48%', '72%'], center: ['36%', '50%'],
    label: { show: false },
    itemStyle: { borderColor: '#0a1a30', borderWidth: 2 },
    data: ageMix.map((x, i) => ({ ...x, itemStyle: { color: ['#4fc3f7', '#4fe0c8', '#ffc86e', '#c79bf2'][i] } }))
  }]
}))
</script>

<style scoped>
.scr1 {
  height: 100%;
  display: grid;
  gap: 14px;
  grid-template-columns: 470px 1fr 470px;
  grid-template-rows: 96px 1.1fr 1fr 190px;
  grid-template-areas:
    "kpi  kpi   kpi"
    "flow map   gauge"
    "radar map  route"
    "alert alert age";
  min-height: 0;
}
.kpis {
  grid-area: kpi;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
</style>
