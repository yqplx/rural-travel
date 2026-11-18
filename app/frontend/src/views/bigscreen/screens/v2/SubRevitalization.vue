<template>
  <div class="scr">
    <div class="kpis">
      <BsKpi label="AHP-TOPSIS 综合得分" :value="comp.score.toFixed(2)" tone="cyan"
             :delta="`较上期 ${comp.delta > 0 ? '+' : ''}${comp.delta}`" :up="comp.delta > 0" />
      <BsKpi label="全省排名" :value="comp.rank" :suffix="` / ${comp.of}`" tone="amber" delta="较上期上升 2 位" />
      <BsKpi label="监测指标数" :value="indicators.length" suffix="项" delta="覆盖 5 个维度" />
      <BsKpi label="达标率" :value="passRate" suffix="%" tone="cyan" :delta="`${passed} 项达标 / ${indicators.length} 项`" />
    </div>

    <BsPanel class="pa-flip" title="综合成效得分（翻牌）" en="composite score">
      <div class="flip">
        <div class="big">
          <span v-for="(c, i) in scoreChars" :key="i" :class="['cell', c.tone]">{{ c.ch }}</span>
        </div>
        <div class="lb">AHP 主观赋权 × TOPSIS 逼近理想解</div>
        <div class="bars">
          <div v-for="d in dims" :key="d.name" class="dim">
            <span class="n">{{ d.name }}</span>
            <span class="b"><i :style="{ width: d.score + '%' }" /></span>
            <span class="w">权重 {{ (d.weight * 100).toFixed(1) }}%</span>
            <span class="s">{{ d.score.toFixed(1) }}</span>
          </div>
        </div>
      </div>
    </BsPanel>

    <BsPanel class="pa-rank" title="行政村成效排名（TOPSIS 贴近度）" en="topsis ranking">
      <BsChart :option="rankOption" />
    </BsPanel>
    <BsPanel class="pa-radar" title="五维成效雷达" en="dimensions">
      <BsChart :option="radarOption" />
    </BsPanel>
    <BsPanel class="pa-wall" title="指标监测墙（20+ 项）" en="indicator wall">
      <div class="wall">
        <div v-for="it in indicators" :key="it.name" class="cell">
          <div class="top">
            <span class="nm">{{ it.name }}</span>
            <span class="yoy" :class="it.yoy.startsWith('-') ? 'down' : 'up'">{{ it.yoy }}</span>
          </div>
          <div class="val">{{ it.value.toLocaleString() }}<i>{{ it.unit }}</i></div>
          <div class="bar"><i :style="{ width: pct(it) + '%', background: pct(it) >= 100 ? '#4fe0c8' : pct(it) >= 85 ? '#4fc3f7' : '#ffc86e' }" /></div>
          <div class="tg">目标 {{ it.target.toLocaleString() }}{{ it.unit }} · {{ pct(it) }}%</div>
        </div>
      </div>
    </BsPanel>
    <BsPanel class="pa-trend" title="年度成效走势" en="yearly trend">
      <BsChart :option="trendOption" />
    </BsPanel>
  </div>
</template>

<script setup lang="ts">
/* ============ 副屏5：乡村振兴成效（AHP-TOPSIS） ============ */
import { computed } from 'vue'
import BsPanel from '../../components/BsPanel.vue'
import BsKpi from '../../components/BsKpi.vue'
import BsChart from '../../components/BsChart.vue'
import { ahpDims, topsisRank, composite, indicatorRadar, indicators, revTrend } from '../../data/mock.v2'

const dims = ahpDims
const comp = composite
const scoreChars = computed(() => {
  const arr = comp.score.toFixed(2).split('')
  return arr.map(ch => ({ ch, tone: ch === '.' ? 'dot' : 'num' }))
})
const passed = computed(() => indicators.filter(i => (i.value / i.target) * 100 >= 100).length)
const passRate = computed(() => Math.round((passed.value / indicators.length) * 100))
function pct(it: { value: number; target: number }): number {
  return Math.round((it.value / it.target) * 100)
}

const rankOption = computed(() => ({
  grid: { left: 80, right: 44, top: 10, bottom: 8 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'value', show: false },
  yAxis: {
    type: 'category', data: topsisRank.map(x => x.town).reverse(),
    axisLine: { lineStyle: { color: '#2e5578' } }, axisLabel: { color: '#b9d4ea', fontSize: 12 }
  },
  series: [{
    type: 'bar', barWidth: 12, roundCap: true,
    data: topsisRank.map(x => x.closeness).reverse(),
    itemStyle: { borderRadius: 5, color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#145a54' }, { offset: 1, color: '#4fe0c8' }] } },
    label: { show: true, position: 'right', color: '#8fb4d4', fontSize: 11 }
  }]
}))
const radarOption = computed(() => ({
  radar: {
    indicator: indicatorRadar.map(d => ({ name: d.name, max: d.max })),
    radius: '62%', center: ['50%', '52%'],
    axisName: { color: '#8fb4d4', fontSize: 12 },
    splitLine: { lineStyle: { color: 'rgba(46,85,120,.5)' } },
    splitArea: { areaStyle: { color: ['rgba(20,52,88,.25)', 'rgba(13,34,60,.15)'] } },
    axisLine: { lineStyle: { color: 'rgba(46,85,120,.6)' } }
  },
  series: [{
    type: 'radar',
    data: [{
      value: indicatorRadar.map(d => d.value),
      lineStyle: { color: '#4fe0c8', width: 2 },
      areaStyle: { color: 'rgba(79,224,200,.25)' },
      itemStyle: { color: '#4fe0c8' }
    }]
  }]
}))
const trendOption = computed(() => ({
  grid: { left: 42, right: 44, top: 26, bottom: 22 },
  tooltip: { trigger: 'axis' },
  legend: { data: ['综合得分', '文旅收入(亿元)'], textStyle: { color: '#8fb4d4', fontSize: 11 }, itemWidth: 12, itemHeight: 8 },
  xAxis: { type: 'category', boundaryGap: false, data: revTrend.map(x => x.m), axisLine: { lineStyle: { color: '#2e5578' } }, axisLabel: { color: '#7fa8c9', fontSize: 11 } },
  yAxis: [
    { type: 'value', min: 60, max: 100, splitLine: { lineStyle: { color: 'rgba(46,85,120,.35)' } }, axisLabel: { color: '#7fa8c9', fontSize: 11 } },
    { type: 'value', splitLine: { show: false }, axisLabel: { color: '#7fa8c9', fontSize: 11 } }
  ],
  series: [
    { name: '综合得分', type: 'line', smooth: true, data: revTrend.map(x => x.score), lineStyle: { color: '#4fe0c8', width: 2.2 }, itemStyle: { color: '#4fe0c8' } },
    { name: '文旅收入(亿元)', yAxisIndex: 1, type: 'line', smooth: true, data: revTrend.map(x => x.income), lineStyle: { color: '#ffc86e', width: 2 }, itemStyle: { color: '#ffc86e' } }
  ]
}))
</script>

<style scoped>
.scr {
  height: 100%;
  display: grid;
  gap: 12px;
  grid-template-columns: 1.05fr 1fr 1.15fr;
  grid-template-rows: 88px 1fr 1.25fr;
  grid-template-areas: "kpi kpi kpi" "flip rank radar" "wall wall trend";
  min-height: 0;
}
.kpis { grid-area: kpi; display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.pa-flip { grid-area: flip; }
.pa-rank { grid-area: rank; }
.pa-radar { grid-area: radar; }
.pa-wall { grid-area: wall; }
.pa-trend { grid-area: trend; }

.flip { display: flex; flex-direction: column; height: 100%; }
.flip .big { display: flex; gap: 6px; align-items: baseline; }
.flip .cell {
  display: inline-block; min-width: 38px; text-align: center;
  font-size: 46px; font-weight: 700; color: #06121f;
  background: linear-gradient(180deg, #4fe0c8, #2aa99a);
  border-radius: 4px; padding: 2px 6px;
  font-variant-numeric: tabular-nums;
  animation: flipin .5s cubic-bezier(.22, 1, .36, 1);
  box-shadow: 0 0 18px rgba(79, 224, 200, .35);
}
.flip .cell.dot { background: none; color: #4fe0c8; box-shadow: none; min-width: 14px; }
@keyframes flipin { from { transform: rotateX(-90deg); opacity: .2 } to { transform: none; opacity: 1 } }
.flip .lb { font-size: 12px; color: #5f86a8; margin: 8px 0 10px; }
.bars { flex: 1; overflow: auto; }
.dim { display: flex; align-items: center; gap: 8px; font-size: 12px; margin-bottom: 8px; }
.dim .n { flex: 0 0 58px; color: #b9d4ea; }
.dim .b { flex: 1; height: 8px; background: rgba(46, 85, 120, .5); }
.dim .b i { display: block; height: 100%; background: linear-gradient(90deg, #1d8f8a, #4fe0c8); }
.dim .w { flex: 0 0 74px; color: #5f86a8; }
.dim .s { flex: 0 0 38px; text-align: right; color: #4fe0c8; font-variant-numeric: tabular-nums; }

.wall {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px;
  height: 100%; overflow: auto; padding-right: 4px;
}
.wall .cell {
  border: 1px solid rgba(74, 144, 197, .22); background: rgba(13, 34, 60, .5);
  padding: 6px 8px; min-height: 74px;
}
.wall .top { display: flex; justify-content: space-between; align-items: baseline; gap: 4px; }
.wall .nm { font-size: 12px; color: #9fc6e8; line-height: 1.3; }
.wall .yoy { font-size: 11px; }
.wall .yoy.up { color: #58e0a8; }
.wall .yoy.down { color: #ff8f7e; }
.wall .val { font-size: 19px; font-weight: 700; color: #eaf6ff; font-variant-numeric: tabular-nums; margin: 2px 0 4px; }
.wall .val i { font-size: 11px; font-style: normal; color: #5f86a8; margin-left: 3px; }
.wall .bar { height: 5px; background: rgba(46, 85, 120, .5); }
.wall .bar i { display: block; height: 100%; }
.wall .tg { font-size: 10px; color: #4c7396; margin-top: 3px; }
</style>
