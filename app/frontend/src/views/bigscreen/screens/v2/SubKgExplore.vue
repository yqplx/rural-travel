<template>
  <div class="scr">
    <div class="kpis">
      <BsKpi label="知识图谱实体总量" :value="summary.nodes.toLocaleString()" tone="cyan" delta="7 大类" />
      <BsKpi label="关系边总量" :value="summary.edges.toLocaleString()" delta="平均连接度 8.0" />
      <BsKpi label="本月新增节点" :value="12460" tone="cyan" delta="较上月 +18.2%" />
      <BsKpi label="图谱覆盖率" :value="96.4" suffix="%" delta="县域实体已入图" />
    </div>

    <BsPanel class="pa-graph" title="知识图谱探索 · 拖拽 / 下钻 / 聚类" en="kg explore">
      <template #extra>
        <div class="ops">
          <span :class="['ly', layout === 'force' && 'on']" @click="setLayout('force')">力导向</span>
          <span :class="['ly', layout === 'circular' && 'on']" @click="setLayout('circular')">环形聚类</span>
          <span :class="['ly', layout === 'dagre' && 'on']" @click="setLayout('dagre')">层级</span>
          <span class="ly" @click="reset">复位</span>
        </div>
      </template>
      <div ref="el" class="g6" />
      <div class="tip">点击节点可下钻二级关系；当前节点 {{ graphNodes.length }} 个（全量 {{ summary.nodes.toLocaleString() }}，按需加载）</div>
    </BsPanel>

    <BsPanel class="pa-cat" title="实体类别分布" en="categories">
      <BsChart :option="catOption" />
    </BsPanel>
    <BsPanel class="pa-grow" title="图谱规模增长（近 12 月）" en="growth">
      <BsChart :option="growOption" />
    </BsPanel>
    <BsPanel class="pa-detail" :title="detailTitle" en="node detail">
      <div v-if="selected" class="detail">
        <div class="row" v-for="r in detailRows" :key="r.k">
          <span class="k">{{ r.k }}</span><span class="v">{{ r.v }}</span>
        </div>
        <div class="rels">
          <div class="sub">关联实体</div>
          <span v-for="n in relNames" :key="n" class="chip" @click="focus(n)">{{ n }}</span>
        </div>
      </div>
      <div v-else class="empty">在左侧图中点击任意节点查看详情</div>
    </BsPanel>
  </div>
</template>

<script setup lang="ts">
/* ============ 副屏1：知识图谱探索（AntV G6） ============ */
import G6 from '@antv/g6'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import BsPanel from '../../components/BsPanel.vue'
import BsKpi from '../../components/BsKpi.vue'
import BsChart from '../../components/BsChart.vue'
import { kgSummary, kgSample, kgDrill } from '../../data/mock.v2'
import { link } from '../../state/linkage'

const summary = kgSummary
const CAT_COLOR: Record<string, string> = {
  景点: '#4fc3f7', 非遗: '#4fe0c8', 美食: '#ffc86e',
  村落: '#c79bf2', 人物: '#ff9f6e', 事件: '#7fd0ff', 手艺: '#9fe6a0'
}
const el = ref<HTMLDivElement | null>(null)
const layout = ref<'force' | 'circular' | 'dagre'>('force')
const selected = ref<string | null>(null)
const graphNodes = ref<any[]>([...kgSample.nodes])
let graph: any = null

function toData(nodes: any[], edges: any[]): any {
  return {
    nodes: nodes.map(n => ({ ...n, style: { fill: CAT_COLOR[n.cat] || '#4fc3f7' } })),
    edges: edges.map((e, i) => ({ ...e, id: 'e' + i }))
  }
}

function init(): void {
  if (!el.value) return
  graph = new G6.Graph({
    container: el.value,
    width: el.value.clientWidth,
    height: el.value.clientHeight,
    fitView: true,
    layout: { type: 'force', linkDistance: 130, nodeStrength: -180, edgeStrength: 0.6, preventOverlap: true },
    modes: { default: ['drag-canvas', 'zoom-canvas', 'drag-node'] },
    defaultNode: {
      size: 30,
      style: { lineWidth: 1, stroke: 'rgba(74,144,197,.6)' },
      labelCfg: { style: { fill: '#cfe3f5', fontSize: 12 } }
    },
    defaultEdge: { style: { stroke: 'rgba(79,195,247,.45)', lineWidth: 1.2 } },
    nodeStateStyles: {
      selected: { lineWidth: 3, stroke: '#4fe0c8', shadowColor: '#4fe0c8', shadowBlur: 12 }
    }
  })
  graph.data(toData(graphNodes.value, kgSample.edges))
  graph.render()
  graph.on('node:click', (e: any) => {
    const id = e.item.getID()
    selected.value = id
    graph.setItemState(e.item, 'selected', true)
    link.selectEntity(id.startsWith('e') ? id : null)
    drill(id)
  })
  graph.on('canvas:click', () => {
    selected.value = null
    graph.getNodes().forEach((n: any) => graph.setItemState(n, 'selected', false))
  })
}

/* 下钻：按需加载二级关系 */
function drill(id: string): void {
  const d = kgDrill[id]
  if (!d) return
  const exist = new Set(graphNodes.value.map(n => n.id))
  const addN = d.nodes.filter(n => !exist.has(n.id))
  const addE = d.edges.filter(e => exist.has(e.source) && !exist.has(e.target))
  if (!addN.length) return
  graphNodes.value = [...graphNodes.value, ...addN]
  const g = graph as any
  addN.forEach((n: any) => g.addItem('node', { ...n, style: { fill: CAT_COLOR[n.cat] || '#4fc3f7' } }))
  addE.forEach((e: any, i: number) => g.addItem('edge', { ...e, id: 'drill' + i }))
  g.layout()
}

function setLayout(t: 'force' | 'circular' | 'dagre'): void {
  layout.value = t
  if (!graph) return
  if (t === 'force') graph.updateLayout({ type: 'force', linkDistance: 130, preventOverlap: true })
  else if (t === 'circular') graph.updateLayout({ type: 'circular', radius: 220 })
  else graph.updateLayout({ type: 'dagre', rankdir: 'TB', nodesep: 40, ranksep: 70 })
}
function reset(): void {
  if (!graph) return
  graphNodes.value = [...kgSample.nodes]
  graph.changeData(toData(kgSample.nodes, kgSample.edges))
  setLayout(layout.value)
}
function focus(name: string): void {
  const n = graphNodes.value.find(x => x.label === name)
  if (n && graph) {
    graph.focusItem(n.id, true)
    selected.value = n.id
  }
}

const detailTitle = computed(() => (selected.value ? '节点详情 · ' + (graphNodes.value.find(n => n.id === selected.value)?.label || '') : '节点详情'))
const relNames = computed(() => {
  if (!selected.value) return []
  const ids = new Set<string>()
  kgSample.edges.forEach(e => {
    if (e.source === selected.value) ids.add(e.target)
    if (e.target === selected.value) ids.add(e.source)
  })
  return [...ids].map(id => graphNodes.value.find(n => n.id === id)?.label).filter(Boolean) as string[]
})
const detailRows = computed(() => {
  const n = graphNodes.value.find(x => x.id === selected.value)
  if (!n) return []
  const deg = kgSample.edges.filter(e => e.source === n.id || e.target === n.id).length
  return [
    { k: '实体类别', v: n.cat },
    { k: '直接关联数', v: deg + ' 条' },
    { k: '入图时间', v: '2026-08-12' },
    { k: '来源', v: '县志 / 运营登记 / UGC 抽取' }
  ]
})

const catOption = computed(() => ({
  grid: { left: 76, right: 40, top: 10, bottom: 8 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'value', show: false },
  yAxis: {
    type: 'category', data: kgSummary.categories.map(c => c.name).reverse(),
    axisLine: { lineStyle: { color: '#2e5578' } }, axisLabel: { color: '#b9d4ea', fontSize: 12 }
  },
  series: [{
    type: 'bar', barWidth: 10, roundCap: true,
    data: kgSummary.categories.map(c => ({ value: c.n, itemStyle: { color: CAT_COLOR[c.name] || '#4fc3f7', borderRadius: 5 } })).reverse(),
    label: { show: true, position: 'right', color: '#8fb4d4', fontSize: 11 }
  }]
}))
const growOption = computed(() => ({
  grid: { left: 46, right: 16, top: 20, bottom: 22 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', boundaryGap: false, data: kgSummary.growth.map((_, i) => i + 1 + '月'), axisLine: { lineStyle: { color: '#2e5578' } }, axisLabel: { color: '#7fa8c9', fontSize: 11 } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(46,85,120,.35)' } }, axisLabel: { color: '#7fa8c9', fontSize: 11 } },
  series: [{
    type: 'line', smooth: true, data: kgSummary.growth,
    lineStyle: { color: '#4fe0c8', width: 2.4 }, itemStyle: { color: '#4fe0c8' },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(79,224,200,.35)' }, { offset: 1, color: 'rgba(79,224,200,0)' }] } }
  }]
}))

let ro: ResizeObserver | null = null
onMounted(() => {
  init()
  if (el.value) {
    ro = new ResizeObserver(() => {
      if (graph && el.value && el.value.clientWidth > 0) graph.changeSize(el.value.clientWidth, el.value.clientHeight)
    })
    ro.observe(el.value)
  }
})
onBeforeUnmount(() => { ro?.disconnect(); try { graph?.destroy() } catch { /* ignore */ } })
</script>

<style scoped>
.scr {
  height: 100%;
  display: grid;
  gap: 12px;
  grid-template-columns: 1.6fr 1fr;
  grid-template-rows: 88px 1fr 1fr;
  grid-template-areas: "kpi kpi" "graph cat" "graph grow" "graph detail";
  grid-template-rows: 88px 1.4fr 1fr 1fr;
  min-height: 0;
}
.kpis { grid-area: kpi; display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.pa-graph { grid-area: graph; }
.pa-cat { grid-area: cat; }
.pa-grow { grid-area: grow; }
.pa-detail { grid-area: detail; }
.g6 { position: absolute; inset: 0; }
.tip { position: absolute; left: 10px; bottom: 6px; font-size: 11px; color: #4c7396; }
.ops { display: flex; gap: 6px; margin-left: auto; }
.ly {
  font-size: 12px; color: #8fb4d4; border: 1px solid rgba(79, 195, 247, .35);
  padding: 2px 10px; cursor: pointer; background: rgba(14, 42, 74, .5);
}
.ly.on { background: linear-gradient(90deg, #35c8e8, #4fe0c8); color: #06121f; font-weight: 700; border-color: transparent; }
.detail { font-size: 13px; }
.detail .row { display: flex; justify-content: space-between; padding: 4px 2px; border-bottom: 1px dashed rgba(74, 144, 197, .16); }
.detail .k { color: #8fb4d4; }
.detail .v { color: #b9d4ea; }
.rels { margin-top: 8px; }
.rels .sub { font-size: 12px; color: #5f86a8; margin-bottom: 6px; }
.chip {
  display: inline-block; font-size: 12px; color: #9fc6e8; margin: 0 6px 6px 0;
  padding: 2px 8px; border: 1px solid rgba(79, 195, 247, .3); cursor: pointer; background: rgba(14, 42, 74, .4);
}
.chip:hover { color: #4fe0c8; border-color: #4fe0c8; }
.empty { font-size: 13px; color: #4c7396; padding: 12px 0; }
</style>
