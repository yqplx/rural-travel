<template>
  <div class="scr">
    <div class="kpis">
      <BsKpi label="点击率 CTR" :value="(cur.ctr * 100).toFixed(2)" suffix="%" tone="cyan" delta="较基线 +23.7%" />
      <BsKpi label="转化率 CVR" :value="(cur.cvr * 100).toFixed(2)" suffix="%" delta="较基线 +29.7%" />
      <BsKpi label="NDCG@10" :value="cur.ndcg" tone="cyan" delta="较基线 +10.4%" />
      <BsKpi label="推荐多样性" :value="cur.diversity" delta="较基线 +20.8%" />
    </div>

    <BsPanel class="pa-trend" title="核心指标趋势（近 14 天）" en="metrics trend">
      <BsChart :option="trendOption" />
    </BsPanel>
    <BsPanel class="pa-ab" title="A/B 实验对比：基线 vs KGNN+RL" en="ab test">
      <BsChart :option="abOption" />
    </BsPanel>
    <BsPanel class="pa-rl" title="强化学习奖励收敛" en="rl reward">
      <BsChart :option="rlOption" />
    </BsPanel>
    <BsPanel class="pa-ch" title="分渠道效果" en="channels">
      <BsChart :option="chOption" />
    </BsPanel>
  </div>
</template>

<script setup lang="ts">
/* ============ 副屏2：推荐算法效果 ============ */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import BsPanel from '../../components/BsPanel.vue'
import BsKpi from '../../components/BsKpi.vue'
import BsChart from '../../components/BsChart.vue'
import { recTrend, abCompare, rlReward, recChannels } from '../../data/mock.v2'
import { poll, wobble, POLL } from '../../data/provider'

const trend = ref(recTrend.map(x => ({ ...x })))
const ch = ref(recChannels.map(x => ({ ...x })))
let stop: (() => void) | null = null
const cur = computed(() => trend.value[trend.value.length - 1])
const AX = { axisLine: { lineStyle: { color: '#2e5578' } }, axisLabel: { color: '#7fa8c9', fontSize: 11 } }
const GRID = { left: 50, right: 18, top: 26, bottom: 24 }

function tick(): void {
  const last = trend.value[trend.value.length - 1]
  const p = (v: number, d: number, max = 1) => Math.min(max, Math.max(0, +(v + (Math.random() - 0.4) * d).toFixed(4)))
  trend.value = [...trend.value.slice(1), {
    date: last.date,
    ctr: p(last.ctr, 0.004), cvr: p(last.cvr, 0.003),
    ndcg: p(last.ndcg, 0.006), diversity: p(last.diversity, 0.005)
  }]
  ch.value = wobble(ch.value, 'ctr', 0.03, 3)
}
onMounted(() => { stop = poll(tick, POLL.charts) })
onBeforeUnmount(() => stop && stop())

const trendOption = computed(() => ({
  grid: GRID,
  tooltip: { trigger: 'axis' },
  legend: { data: ['CTR', 'CVR', 'NDCG@10', '多样性'], textStyle: { color: '#8fb4d4', fontSize: 11 }, itemWidth: 12, itemHeight: 8 },
  xAxis: { type: 'category', boundaryGap: false, data: trend.value.map(x => x.date), ...AX },
  yAxis: [
    { type: 'value', max: 1, splitLine: { lineStyle: { color: 'rgba(46,85,120,.35)' } }, axisLabel: { color: '#7fa8c9', fontSize: 11 } }
  ],
  series: [
    { name: 'CTR', type: 'line', smooth: true, data: trend.value.map(x => x.ctr), lineStyle: { color: '#4fc3f7', width: 2 }, itemStyle: { color: '#4fc3f7' } },
    { name: 'CVR', type: 'line', smooth: true, data: trend.value.map(x => x.cvr), lineStyle: { color: '#4fe0c8', width: 2 }, itemStyle: { color: '#4fe0c8' } },
    { name: 'NDCG@10', type: 'line', smooth: true, data: trend.value.map(x => x.ndcg), lineStyle: { color: '#ffc86e', width: 2 }, itemStyle: { color: '#ffc86e' } },
    { name: '多样性', type: 'line', smooth: true, data: trend.value.map(x => x.diversity), lineStyle: { color: '#c79bf2', width: 2 }, itemStyle: { color: '#c79bf2' } }
  ]
}))

const abOption = computed(() => ({
  grid: { left: 60, right: 20, top: 30, bottom: 26 },
  tooltip: { trigger: 'axis' },
  legend: { data: ['基线（协同过滤）', 'KGNN + 强化学习'], textStyle: { color: '#8fb4d4', fontSize: 11 }, itemWidth: 12, itemHeight: 8 },
  xAxis: { type: 'category', data: abCompare.groups, ...AX },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(46,85,120,.35)' } }, axisLabel: { color: '#7fa8c9', fontSize: 11 } },
  series: [
    { name: '基线（协同过滤）', type: 'bar', barWidth: 16, data: abCompare.base, itemStyle: { color: 'rgba(79,195,247,.55)', borderRadius: [3, 3, 0, 0] } },
    { name: 'KGNN + 强化学习', type: 'bar', barWidth: 16, data: abCompare.kgnn, itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#4fe0c8' }, { offset: 1, color: '#1d8f8a' }] }, borderRadius: [3, 3, 0, 0] } }
  ]
}))

const rlOption = computed(() => ({
  grid: { ...GRID, left: 42 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: rlReward.map((_, i) => 'ep' + (i + 1)), ...AX, axisLabel: { show: false } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(46,85,120,.35)' } }, axisLabel: { color: '#7fa8c9', fontSize: 11 } },
  series: [{
    type: 'line', smooth: true, symbol: 'none', data: rlReward,
    lineStyle: { color: '#ffc86e', width: 2.2 },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(255,200,110,.3)' }, { offset: 1, color: 'rgba(255,200,110,0)' }] } }
  }]
}))

const chOption = computed(() => ({
  grid: { left: 110, right: 46, top: 10, bottom: 8 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'value', show: false },
  yAxis: {
    type: 'category', data: ch.value.map(x => x.name).reverse(),
    axisLine: { lineStyle: { color: '#2e5578' } }, axisLabel: { color: '#b9d4ea', fontSize: 12 }
  },
  series: [{
    type: 'bar', barWidth: 10, roundCap: true,
    data: ch.value.map(x => +(x.ctr * 100).toFixed(1)).reverse(),
    itemStyle: { borderRadius: 5, color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#1d6ea8' }, { offset: 1, color: '#4fe0c8' }] } },
    label: { show: true, position: 'right', formatter: '{c}%', color: '#8fb4d4', fontSize: 11 }
  }]
}))
</script>

<style scoped>
.scr {
  height: 100%;
  display: grid;
  gap: 12px;
  grid-template-columns: 1.25fr 1fr;
  grid-template-rows: 88px 1fr 1fr;
  grid-template-areas: "kpi kpi" "trend ab" "rl ch";
  min-height: 0;
}
.kpis { grid-area: kpi; display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.pa-trend { grid-area: trend; }
.pa-ab { grid-area: ab; }
.pa-rl { grid-area: rl; }
.pa-ch { grid-area: ch; }
</style>
