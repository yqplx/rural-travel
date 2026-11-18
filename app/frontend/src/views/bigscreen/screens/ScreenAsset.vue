<template>
  <div class="scr scr3">
    <div class="kpis">
      <BsKpi label="知识图谱实体（个）" :value="26" tone="cyan" delta="12 景点 · 6 非遗 · 8 美食" />
      <BsKpi label="知识关系边（条）" :value="90" delta="步行接驳 / 归属 / 同主题" />
      <BsKpi label="多模态内容总量（条）" :value="520" tone="cyan" delta="实体覆盖率 100%" />
      <BsKpi label="反馈驱动重生成（今日）" :value="regen" suffix="次" tone="amber" :delta="`累计 ${411 + regen} 次`" />
    </div>

    <BsPanel title="县域文旅知识图谱网络" en="knowledge graph" area="graph">
      <BsChart :option="graphOption" />
    </BsPanel>
    <BsPanel title="AI 内容资产构成" en="asset mix" area="donut">
      <BsChart :option="assetOption" />
    </BsPanel>
    <BsPanel title="内容质量五维评估" en="quality radar" area="radar">
      <BsChart :option="qualityRadarOption" />
    </BsPanel>
    <BsPanel title="实体内容访问量 TOP" en="content visits" area="visit">
      <BsChart :option="visitOption" />
    </BsPanel>
    <BsPanel title="内容质量评分分布" en="score dist" area="score">
      <BsChart :option="scoreOption" />
    </BsPanel>
    <BsPanel class="pa-log" title="AI 实时动态 · 反馈驱动重生成" en="live ai log" area="log">
      <BsLogList :seed="seed" :pool="pool" :interval="4500" :max-rows="3" />
      <div class="iter">
        <span v-for="it in iterateVersions" :key="it.name" class="iter-chip">
          {{ it.name }} <b>v{{ it.v }}</b>
        </span>
      </div>
    </BsPanel>
  </div>
</template>

<script setup>
/* ============ 屏3：AI 数字内容资产大屏 ============ */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import BsPanel from '../components/BsPanel.vue'
import BsKpi from '../components/BsKpi.vue'
import BsChart from '../components/BsChart.vue'
import BsLogList from '../components/BsLogList.vue'
import {
  kgGraph, contentAssets, qualityScoreDist, qualityRadar, entityContentVisits,
  alertLogSeed, alertLogPool
} from '../data/mock'
import { poll, wobble, POLL } from '../data/provider'

const AXIS = { axisLine: { lineStyle: { color: '#2e5578' } }, axisLabel: { color: '#7fa8c9', fontSize: 12 } }
const CAT_COLOR = ['#4fc3f7', '#4fe0c8', '#ffc86e']

/* 动态刷新：资产构成 / 实体访问量 / 今日重生成次数每 30 秒小幅重算 */
const assets = ref(contentAssets.map(x => ({ ...x })))
const visits = ref(entityContentVisits.map(x => ({ ...x })))
const regen = ref(18)
let stopCharts = null

function tickCharts() {
  assets.value = wobble(assets.value, 'value', 0.02)
  visits.value = wobble(visits.value, 'v', 0.03).sort((a, b) => b.v - a.v)
  if (Math.random() > 0.55) regen.value += 1
}
onMounted(() => { stopCharts = poll(tickCharts, POLL.charts) })
onBeforeUnmount(() => stopCharts && stopCharts())

/* 知识图谱网络 */
const graphOption = computed(() => ({
  tooltip: {},
  legend: { bottom: 4, data: kgGraph.categories, textStyle: { color: '#8fb4d4', fontSize: 12 }, itemWidth: 12, itemHeight: 8 },
  series: [{
    type: 'graph', layout: 'force', roam: false,
    force: { repulsion: 240, edgeLength: 70 },
    categories: kgGraph.categories.map((n, i) => ({ name: n, itemStyle: { color: CAT_COLOR[i] } })),
    label: { show: true, color: '#b9d4ea', fontSize: 12 },
    lineStyle: { color: 'rgba(79,195,247,.4)', width: 1.2, curveness: .18 },
    emphasis: { lineStyle: { color: '#4fe0c8', width: 2.5 } },
    data: kgGraph.nodes.map(n => ({ name: n.name, category: n.cat, symbolSize: n.size })),
    links: kgGraph.links
  }]
}))

/* 资产构成环形 */
const assetOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c} 条（{d}%）' },
  legend: { right: 4, top: 'middle', orient: 'vertical', textStyle: { color: '#8fb4d4', fontSize: 12 }, itemWidth: 12, itemHeight: 8 },
  series: [{
    type: 'pie', radius: ['50%', '74%'], center: ['38%', '50%'],
    label: { show: false }, itemStyle: { borderColor: '#0a1a30', borderWidth: 2 },
    data: assets.value.map((x, i) => ({ ...x, itemStyle: { color: ['#4fc3f7', '#4fe0c8', '#ffc86e', '#c79bf2'][i] } }))
  }],
  graphic: [
    { type: 'text', left: '30%', top: '44%', style: { text: '1,280', fill: '#4fe0c8', fontSize: 28, fontWeight: 700 } },
    { type: 'text', left: '30%', top: '56%', style: { text: '资产总条数', fill: '#8fb4d4', fontSize: 12 } }
  ]
}))

/* 质量评分分布 */
const scoreOption = computed(() => ({
  grid: { left: 66, right: 40, top: 10, bottom: 8 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'value', show: false },
  yAxis: {
    type: 'category', data: qualityScoreDist.map(x => x.name).reverse(),
    ...AXIS, axisLabel: { color: '#b9d4ea', fontSize: 12 }
  },
  series: [{
    type: 'bar', barWidth: 12, roundCap: true,
    data: qualityScoreDist.map(x => x.v).reverse(),
    itemStyle: {
      borderRadius: 6,
      color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#1d6ea8' }, { offset: 1, color: '#4fc3f7' }] }
    },
    label: { show: true, position: 'right', color: '#8fb4d4', fontSize: 12 }
  }]
}))

/* 五维评估雷达 */
const qualityRadarOption = computed(() => ({
  radar: {
    indicator: qualityRadar.dims.map(n => ({ name: n, max: 100 })),
    radius: '64%', center: ['50%', '52%'],
    axisName: { color: '#8fb4d4', fontSize: 12 },
    splitLine: { lineStyle: { color: 'rgba(46,85,120,.5)' } },
    splitArea: { areaStyle: { color: ['rgba(20,52,88,.25)', 'rgba(13,34,60,.15)'] } },
    axisLine: { lineStyle: { color: 'rgba(46,85,120,.6)' } }
  },
  series: [{
    type: 'radar',
    data: [{
      value: qualityRadar.value,
      lineStyle: { color: '#4fe0c8', width: 2 },
      areaStyle: { color: 'rgba(79,224,200,.25)' },
      itemStyle: { color: '#4fe0c8' }
    }]
  }]
}))

/* 实体内容访问量 */
const visitOption = computed(() => ({
  grid: { left: 96, right: 40, top: 10, bottom: 8 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'value', show: false },
  yAxis: {
    type: 'category', data: visits.value.map(x => x.name).reverse(),
    ...AXIS, axisLabel: { color: '#b9d4ea', fontSize: 12 }
  },
  series: [{
    type: 'bar', barWidth: 10, roundCap: true,
    data: visits.value.map(x => x.v).reverse(),
    itemStyle: {
      borderRadius: 5,
      color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#145a54' }, { offset: 1, color: '#4fe0c8' }] }
    },
    label: { show: true, position: 'right', color: '#8fb4d4', fontSize: 12 }
  }]
}))

/* AI 动态日志：取与内容相关的种子 */
const contentSeed = alertLogSeed.filter(x => x.tag === '内容' || x.tag === '问答')
const contentPool = alertLogPool.filter(x => x.tag === '内容' || x.tag === '预警')
const seed = contentSeed.length ? contentSeed : alertLogSeed
const pool = contentPool.length ? contentPool : alertLogPool
</script>

<style scoped>
.scr3 {
  height: 100%;
  display: grid;
  gap: 14px;
  grid-template-columns: 1.1fr .92fr .92fr .92fr;
  grid-template-rows: 96px 1.3fr 1fr;
  grid-template-areas:
    "kpi   kpi   kpi   kpi"
    "graph donut radar visit"
    "graph score log   log";
  min-height: 0;
}
.kpis {
  grid-area: kpi;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
.pa-log { display: flex; flex-direction: column; }
.pa-log :deep(.p-body) { display: flex; flex-direction: column; }
.iter {
  flex: 0 0 auto;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px dashed rgba(74, 144, 197, .2);
  margin-top: 6px;
}
.iter-chip {
  font-size: 12px;
  color: #8fb4d4;
  border: 1px solid rgba(79, 195, 247, .3);
  padding: 1px 8px;
  background: rgba(14, 42, 74, .4);
}
.iter-chip b { color: #4fe0c8; margin-left: 2px; }
.pa-log .bs-log { flex: 1; min-height: 0; overflow: hidden; }
</style>
