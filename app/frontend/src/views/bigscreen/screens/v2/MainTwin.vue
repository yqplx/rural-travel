<template>
  <div class="scr main">
    <!-- 顶栏 KPI -->
    <div class="hud-top">
      <div class="hud-cell" v-for="k in kpis" :key="k.label">
        <div class="v" :class="k.tone">{{ k.value }}<i>{{ k.unit }}</i></div>
        <div class="l">{{ k.label }} <em v-if="k.delta" :class="k.up === false ? 'down' : 'up'">{{ k.delta }}</em></div>
      </div>
    </div>

    <!-- 左：客流趋势 · 来源地 · 画像 -->
    <BsPanel class="p-left" title="客流与客群" en="flow & profile">
      <div class="blk">
        <div class="sub">近 7 日客流趋势（本期 / 上期）</div>
        <div class="mini" style="height:150px"><BsChart :option="weekOption" /></div>
      </div>
      <div class="blk">
        <div class="sub">游客来源地 TOP10</div>
        <div class="mini" style="height:210px"><BsChart :option="sourceOption" /></div>
      </div>
      <div class="blk">
        <div class="sub">游客画像</div>
        <div class="mini" style="height:205px"><BsChart :option="profileOption" /></div>
      </div>
    </BsPanel>

    <!-- 中：三维场景 -->
    <div class="stage-wrap">
      <div ref="globeEl" class="globe" />
      <div class="ops">
        <span class="op-t">图层</span>
        <span :class="['ly', show.pillar && 'on']" @click="show.pillar = !show.pillar">POI 光柱</span>
        <span :class="['ly', show.fly && 'on']" @click="show.fly = !show.fly">推荐路线</span>
        <span :class="['ly', show.particle && 'on']" @click="show.particle = !show.particle">客流粒子</span>
        <span :class="['ly', show.rain && 'on']" @click="show.rain = !show.rain">天气叠加</span>
        <span :class="['ly', show.aqi && 'on']" @click="show.aqi = !show.aqi">AQI 图层</span>
      </div>
      <div class="cam">
        <span class="op-t">视角</span>
        <span :class="['ly', mode === 'orbit' && 'on']" @click="setMode('orbit')">自由漫游</span>
        <span :class="['ly', mode === 'fpv' && 'on']" @click="setMode('fpv')">第一人称</span>
        <span :class="['ly', mode === 'top' && 'on']" @click="setMode('top')">正射俯瞰</span>
        <span class="ly" @click="resetView">复位</span>
        <span class="op-t" style="margin-left:8px">粒子</span>
        <span v-for="n in [8000, 30000, 100000]" :key="n"
              :class="['ly', budget === n && 'on']" @click="setBudget(n)">{{ n / 10000 }}万</span>
      </div>
      <div class="hint">🖱️ 左键旋转 · 右键平移 · 滚轮缩放 · 点击 POI 联动其余 5 屏</div>
      <div v-if="webglError" class="fallback">三维场景不可用（{{ webglError }}）——已降级为二维态势，不影响其余大屏。</div>

      <div class="timeline">
        <span class="ly" @click="play">{{ link.state.playing ? '⏸' : '▶' }}</span>
        <input type="range" min="0" max="29" v-model="dayIndex" @input="onSlide" />
        <span class="dt">{{ today.date }} · {{ today.weather }} · AQI {{ today.aqi }}</span>
        <span class="dt">客流 {{ today.visitors.toLocaleString() }} 人次</span>
        <span class="dt">{{ meta.full }} · {{ meta.weather }} · AQI {{ meta.aqi }} {{ meta.aqiText }}</span>
      </div>
    </div>

    <!-- 右：热门景点 · 告警 -->
    <BsPanel class="p-right" title="实时热门景点 TOP10" en="hot spots">
      <div class="hot" v-for="(h, i) in hotSpots" :key="h.name" @click="selectByName(h.name)">
        <span class="rk" :class="i < 3 && 'top'">{{ i + 1 }}</span>
        <span class="nm">{{ h.name }}</span>
        <span class="bar"><i :style="{ width: (h.v / 10000 * 100) + '%' }" /></span>
        <span class="vv">{{ h.v.toLocaleString() }}</span>
      </div>
      <div class="sub" style="margin-top:12px">实时告警（{{ twinKpi.alarms }}）</div>
      <BsLogList :seed="seed" :pool="pool" :interval="5000" :max-rows="5" />
    </BsPanel>
  </div>
</template>

<script setup lang="ts">
/* ============ 主屏：3D 数字孪生态势（3840×1080 拼接屏）============
 * Cesium 离线可用：不请求 ion 资源，县域以发光轮廓 + POI 光柱 + 飞线 +
 * GPU 粒子流呈现；倾斜摄影瓦片到位后替换 imagery / tileset 即可。
 * ================================================================= */
import 'cesium/Build/Cesium/Widgets/widgets.css'
import {
  Viewer, Color, Cartesian2, Cartesian3, Math as CMath,
  PolylineGlowMaterialProperty, ParticleSystem, CircleEmitter
} from 'cesium'
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import BsPanel from '../../components/BsPanel.vue'
import BsChart from '../../components/BsChart.vue'
import BsLogList from '../../components/BsLogList.vue'
import { link, subscribe } from '../../state/linkage'
import {
  countyMeta, countyRing, pois, routeFlows, days30, twinKpi, hotSpots,
  sourceProvinces, visitorProfile, weekTrend, alertLogSeed, alertLogPool
} from '../../data/mock.v2'
import { poll } from '../../data/provider'

const meta = countyMeta
const globeEl = ref<HTMLDivElement | null>(null)
const webglError = ref('')
const dayIndex = ref(link.state.dayIndex as number)
const budget = ref(8000)
const mode = ref<'orbit' | 'fpv' | 'top'>('orbit')
const show = reactive({ pillar: true, fly: true, particle: true, rain: false, aqi: true })

let viewer: Viewer | null = null
let particles: ParticleSystem | null = null
let rain: ParticleSystem | null = null
let flyEntities: any[] = []
let pillarEntities: any[] = []
let polyEntity: any = null
let fpvTimer: number | null = null
let stopPoll: (() => void) | null = null
let unsub: (() => void) | null = null

const today = computed(() => days30[dayIndex.value] ?? days30[days30.length - 1])
const scale = computed(() => today.value.visitors / 12580)

const kpis = computed(() => [
  { label: '今日游客总数', value: twinKpi.visitors.toLocaleString(), unit: '人', tone: '', delta: '↑18.6%' },
  { label: '今日总营收', value: twinKpi.income.toLocaleString(), unit: '元', tone: 'cyan', delta: '↑12.3%' },
  { label: '推荐转化率', value: twinKpi.conversion.toFixed(2), unit: '%', tone: '', delta: '↑3.2%' },
  { label: '当前在园人数', value: twinKpi.inPark.toLocaleString(), unit: `饱和度 ${twinKpi.saturation}%`, tone: 'cyan' },
  { label: 'KG 实体 / 边', value: '58.4k / 216.3k', unit: '', tone: '' },
  { label: '实时告警', value: twinKpi.alarms, unit: twinKpi.alarmDetail, tone: 'amber' }
])

const seed = alertLogSeed
const pool = alertLogPool

/* ---------------- 图表 ---------------- */
const AX = { axisLine: { lineStyle: { color: '#2e5578' } }, axisLabel: { color: '#7fa8c9', fontSize: 10 } }
const weekOption = computed(() => ({
  grid: { left: 40, right: 12, top: 26, bottom: 18 },
  tooltip: { trigger: 'axis' },
  legend: { data: ['本期', '上期'], textStyle: { color: '#8fb4d4', fontSize: 10 }, itemWidth: 10, itemHeight: 6 },
  xAxis: { type: 'category', boundaryGap: false, data: weekTrend.map(x => x.d), ...AX },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(46,85,120,.35)' } }, axisLabel: { color: '#7fa8c9', fontSize: 10 } },
  series: [
    { name: '本期', type: 'line', smooth: true, data: weekTrend.map(x => x.cur), lineStyle: { color: '#4fe0c8', width: 2 }, itemStyle: { color: '#4fe0c8' }, areaStyle: { color: 'rgba(79,224,200,.25)' } },
    { name: '上期', type: 'line', smooth: true, data: weekTrend.map(x => x.prev), lineStyle: { color: '#8fb4d4', width: 1.4, type: 'dashed' }, itemStyle: { color: '#8fb4d4' } }
  ]
}))
const sourceOption = computed(() => ({
  grid: { left: 62, right: 42, top: 6, bottom: 6 },
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'value', show: false },
  yAxis: {
    type: 'category', data: sourceProvinces.map(x => x.name).reverse(),
    axisLine: { lineStyle: { color: '#2e5578' } }, axisLabel: { color: '#b9d4ea', fontSize: 11 }
  },
  series: [{
    type: 'bar', barWidth: 9, roundCap: true,
    data: sourceProvinces.map(x => x.v).reverse(),
    itemStyle: { borderRadius: 4, color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#1d6ea8' }, { offset: 1, color: '#4fc3f7' }] } },
    label: { show: true, position: 'right', color: '#8fb4d4', fontSize: 10 }
  }]
}))
const profileOption = computed(() => ({
  tooltip: {},
  radar: {
    indicator: visitorProfile.interests.map(n => ({ name: n, max: 100 })),
    radius: '58%', center: ['50%', '54%'],
    axisName: { color: '#8fb4d4', fontSize: 10 },
    splitLine: { lineStyle: { color: 'rgba(46,85,120,.45)' } },
    splitArea: { areaStyle: { color: ['rgba(20,52,88,.22)', 'rgba(13,34,60,.12)'] } },
    axisLine: { lineStyle: { color: 'rgba(46,85,120,.6)' } }
  },
  series: [{
    type: 'radar',
    data: [{
      value: visitorProfile.interestValue,
      lineStyle: { color: '#4fe0c8', width: 2 },
      areaStyle: { color: 'rgba(79,224,200,.25)' },
      itemStyle: { color: '#4fe0c8' }
    }]
  }],
  graphic: visitorProfile.ages.map((a, i) => ({
    type: 'text', right: 12, top: 14 + i * 18,
    style: { text: `${a.name} (${a.v}%)`, fill: '#8fb4d4', fontSize: 11 }
  }))
}))

/* ---------------- Cesium ---------------- */
function flatRing(): number[] {
  const out: number[] = []
  countyRing.forEach(([lon, lat]) => { out.push(lon, lat) })
  return out
}
function ringCenter(): [number, number] {
  const lons = countyRing.map(p => p[0]), lats = countyRing.map(p => p[1])
  return [(Math.min(...lons) + Math.max(...lons)) / 2, (Math.min(...lats) + Math.max(...lats)) / 2]
}
function pathOf(ids: string[]): number[] {
  const out: number[] = []
  ids.forEach(id => {
    const p = pois.find(x => x.id === id)
    if (p) out.push(p.lon, p.lat, 120)
  })
  return out
}
function aqiColor(aqi: number): Color {
  if (aqi <= 50) return Color.fromCssColorString('#4fe0c8')
  if (aqi <= 80) return Color.fromCssColorString('#ffc86e')
  return Color.fromCssColorString('#ff8f7e')
}
function particleImage(): string {
  const c = document.createElement('canvas')
  c.width = 8; c.height = 8
  const ctx = c.getContext('2d')!
  const g = ctx.createRadialGradient(4, 4, 0, 4, 4, 4)
  g.addColorStop(0, 'rgba(255,255,255,1)')
  g.addColorStop(0.4, 'rgba(160,240,255,.85)')
  g.addColorStop(1, 'rgba(160,240,255,0)')
  ctx.fillStyle = g
  ctx.beginPath(); ctx.arc(4, 4, 4, 0, Math.PI * 2); ctx.fill()
  return c.toDataURL()
}

function initScene(): void {
  if (!globeEl.value) return
  try {
    viewer = new Viewer(globeEl.value, {
      baseLayer: false,
      animation: false, timeline: false, fullscreenButton: false, geocoder: false,
      homeButton: false, sceneModePicker: false, baseLayerPicker: false,
      navigationHelpButton: false, selectionIndicator: false, infoBox: false,
      scene3DOnly: true
    })
    ;(viewer as any).imageryLayers?.removeAll?.()
    viewer.scene.globe.baseColor = Color.fromCssColorString('#04101f')
    viewer.scene.globe.showGroundAtmosphere = true
    viewer.scene.fog.enabled = true
    viewer.scene.fog.density = 0.00018
    viewer.scene.skyAtmosphere.show = true
    viewer.scene.screenSpaceCameraController.minimumZoomDistance = 300

    polyEntity = viewer.entities.add({
      polygon: {
        hierarchy: Cartesian3.fromDegreesArray(flatRing()),
        material: aqiColor(meta.aqi).withAlpha(0.12),
        outline: true, outlineColor: aqiColor(meta.aqi), outlineWidth: 3
      }
    })

    pillarEntities = pois.map(p => viewer!.entities.add({
      position: Cartesian3.fromDegrees(p.lon, p.lat, p.heat * 1100 * scale.value / 2),
      cylinder: {
        length: p.heat * 1100 * scale.value,
        topRadius: 46, bottomRadius: 96,
        material: p.crowd >= 0.7
          ? Color.fromCssColorString('#ff9f6e').withAlpha(0.85)
          : Color.fromCssColorString('#4fe0c8').withAlpha(0.72)
      },
      label: {
        text: p.name, font: '13px Microsoft YaHei', style: 2,
        fillColor: Color.fromCssColorString('#d8ecff'),
        outlineColor: Color.fromCssColorString('#04101f'), outlineWidth: 3,
        pixelOffset: new Cartesian2(0, -18)
      }
    }))

    flyEntities = routeFlows.map((r, i) => viewer!.entities.add({
      polyline: {
        positions: Cartesian3.fromDegreesArrayHeights(pathOf(r.ids)),
        width: 3,
        material: new PolylineGlowMaterialProperty({
          glowPower: 0.35,
          color: [Color.fromCssColorString('#4fc3f7'), Color.fromCssColorString('#4fe0c8'),
                  Color.fromCssColorString('#ffc86e'), Color.fromCssColorString('#c79bf2')][i % 4]
        }),
        clampToGround: false
      }
    }))

    buildParticles()
    buildRain()

    const [lon, lat] = ringCenter()
    viewer.camera.setView({
      destination: Cartesian3.fromDegrees(lon, lat, 4200),
      orientation: { heading: CMath.toRadians(0), pitch: CMath.toRadians(-38), roll: 0 }
    })
  } catch (e: any) {
    webglError.value = e?.message || 'WebGL 初始化失败'
  }
}

function buildParticles(): void {
  if (!viewer) return
  particles?.destroy?.()
  const [lon, lat] = ringCenter()
  const life = 6
  particles = new ParticleSystem({
    show: show.particle,
    emitter: new CircleEmitter(2000),
    emissionRate: Math.round((budget.value * scale.value) / life),
    lifetime: life, minimumParticleLife: 3, maximumParticleLife: life,
    image: particleImage(),
    startScale: 3, endScale: 1.2,
    minimumSpeed: 8, maximumSpeed: 26,
    startColor: Color.fromCssColorString('#4fe0c8').withAlpha(0.9),
    endColor: Color.fromCssColorString('#4fe0c8').withAlpha(0.05),
    updateCallback: (p: any, dt: number) => {
      const move = new Cartesian3()
      Cartesian3.multiplyByScalar(p.velocity, dt, move)
      Cartesian3.add(p.position, move, p.position)
    }
  })
  ;(particles as any).position = Cartesian3.fromDegrees(lon, lat, 60)
  viewer.scene.primitives.add(particles)
}

function buildRain(): void {
  if (!viewer) return
  rain?.destroy?.()
  const [lon, lat] = ringCenter()
  rain = new ParticleSystem({
    show: show.rain,
    emitter: new CircleEmitter(2600), emissionRate: 2600, lifetime: 2.4,
    imageSize: new Cartesian2(1.5, 26),
    minimumSpeed: 0, maximumSpeed: 0,
    startColor: Color.fromCssColorString('#9fd6ff').withAlpha(0.25),
    endColor: Color.fromCssColorString('#9fd6ff').withAlpha(0.05),
    updateCallback: (p: any, dt: number) => { p.position.z -= 240 * dt }
  })
  ;(rain as any).position = Cartesian3.fromDegrees(lon, lat, 1400)
  viewer.scene.primitives.add(rain)
}

/* ---------------- 相机 ---------------- */
function setMode(m: 'orbit' | 'fpv' | 'top'): void {
  mode.value = m
  if (!viewer) return
  viewer.scene.screenSpaceCameraController.enableInputs = (m === 'orbit')
  stopFpv()
  const [lon, lat] = ringCenter()
  if (m === 'top') {
    viewer.camera.setView({
      destination: Cartesian3.fromDegrees(lon, lat, 9000),
      orientation: { heading: 0, pitch: CMath.toRadians(-89.9), roll: 0 }
    })
  } else if (m === 'fpv') {
    const pts = pathOf(routeFlows[0].ids)
    let i = 0
    fpvTimer = window.setInterval(() => {
      if (!viewer) return
      const n = pts.length / 3
      const cur = i % n, nxt = (i + 1) % n
      viewer.camera.setView({
        destination: Cartesian3.fromDegrees(pts[cur * 3], pts[cur * 3 + 1], 6),
        orientation: { heading: CMath.toRadians(angleOf(pts, cur, nxt)), pitch: CMath.toRadians(-6), roll: 0 }
      })
      i++
    }, 220)
  }
}
function angleOf(pts: number[], a: number, b: number): number {
  const dx = pts[b * 3] - pts[a * 3]
  const dy = pts[b * 3 + 1] - pts[a * 3 + 1]
  return (Math.atan2(dx, dy) * 180) / Math.PI
}
function stopFpv(): void { if (fpvTimer) { clearInterval(fpvTimer); fpvTimer = null } }
function resetView(): void {
  stopFpv(); mode.value = 'orbit'
  if (!viewer) return
  viewer.scene.screenSpaceCameraController.enableInputs = true
  const [lon, lat] = ringCenter()
  viewer.camera.setView({
    destination: Cartesian3.fromDegrees(lon, lat, 4200),
    orientation: { heading: 0, pitch: CMath.toRadians(-38), roll: 0 }
  })
}

/* ---------------- 交互 ---------------- */
function selectByName(name: string): void {
  const p = pois.find(x => x.name === name)
  if (p) link.selectEntity(p.id)
}
function setBudget(n: number): void { budget.value = n; buildParticles() }
function play(): void { link.setPlaying(!link.state.playing) }
function onSlide(): void { link.setDay(dayIndex.value, days30[dayIndex.value]?.date) }

function applyDay(): void {
  if (!viewer) return
  if (polyEntity) {
    ;(polyEntity.polygon as any).material = aqiColor(today.value.aqi).withAlpha(0.12)
    ;(polyEntity.polygon as any).outlineColor = aqiColor(today.value.aqi)
  }
  pillarEntities.forEach((e, i) => {
    const p = pois[i]
    ;(e.cylinder as any).length = p.heat * 1100 * scale.value
  })
  if (particles) (particles as any).emissionRate = Math.round((budget.value * scale.value) / 6)
}

onMounted(() => {
  initScene()
  stopPoll = poll(() => {
    if (link.state.playing) {
      const next = (dayIndex.value + 1) % days30.length
      dayIndex.value = next
      link.setDay(next, days30[next].date)
    }
  }, 1200)
  unsub = subscribe((m) => { if (m.type === 'day') dayIndex.value = m.payload.index })
})
watch(() => link.state.dayIndex, (v) => { dayIndex.value = v as number; applyDay() })
watch(() => link.state.entityId, (id) => {
  if (!viewer || !id) return
  const p = pois.find(x => x.id === id)
  if (p) {
    viewer.camera.flyTo({
      destination: Cartesian3.fromDegrees(p.lon, p.lat, 1600),
      orientation: { heading: 0, pitch: CMath.toRadians(-32), roll: 0 },
      duration: 1.1
    })
  }
})
watch(() => ({ ...show }), () => {
  if (particles) particles.show = show.particle
  if (rain) rain.show = show.rain
  pillarEntities.forEach(e => { e.show = show.pillar })
  flyEntities.forEach(e => { e.show = show.fly })
  applyDay()
})
onBeforeUnmount(() => {
  stopPoll && stopPoll()
  unsub && unsub()
  stopFpv()
  try { viewer?.destroy() } catch { /* ignore */ }
  viewer = null
})
</script>

<style scoped>
.main {
  height: 100%;
  display: grid;
  grid-template-columns: 520px 1fr 520px;
  grid-template-rows: 92px 1fr;
  grid-template-areas: "top top top" "left stage right";
  gap: 14px;
}
.hud-top { grid-area: top; display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; }
.hud-cell {
  background: linear-gradient(180deg, rgba(16, 44, 76, .7), rgba(9, 26, 48, .5));
  border: 1px solid rgba(74, 144, 197, .3);
  padding: 8px 14px; display: flex; flex-direction: column; justify-content: center;
}
.hud-cell .v {
  font-size: 28px; font-weight: 700; color: #4fe0c8;
  font-variant-numeric: tabular-nums; line-height: 1.15;
}
.hud-cell .v.cyan { color: #4fc3f7; }
.hud-cell .v.amber { color: #ffc86e; }
.hud-cell .v i { font-size: 12px; font-style: normal; margin-left: 5px; color: #7fa8c9; }
.hud-cell .l { font-size: 12px; color: #8fb4d4; margin-top: 2px; }
.hud-cell .l em { font-style: normal; margin-left: 6px; font-size: 11px; }
.hud-cell .l em.up { color: #58e0a8; }
.hud-cell .l em.down { color: #ff8f7e; }

.p-left { grid-area: left; }
.p-right { grid-area: right; }
.stage-wrap {
  grid-area: stage; position: relative;
  border: 1px solid rgba(74, 144, 197, .28); background: #04101f; overflow: hidden;
}
.globe { position: absolute; inset: 0; }
.ops { position: absolute; top: 10px; left: 12px; display: flex; gap: 6px; flex-wrap: wrap; z-index: 5; }
.cam { position: absolute; top: 10px; right: 12px; display: flex; gap: 6px; z-index: 5; }
.op-t { font-size: 11px; color: #4c7396; align-self: center; margin-right: 2px; }
.hint { position: absolute; bottom: 58px; left: 14px; font-size: 11px; color: #4c7396; z-index: 5; }
.fallback {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  color: #ff9f6e; font-size: 15px; background: #04101f; z-index: 4;
}
.timeline {
  position: absolute; left: 12px; right: 12px; bottom: 10px;
  display: flex; align-items: center; gap: 12px;
  background: rgba(7, 22, 40, .74); border: 1px solid rgba(74, 144, 197, .3);
  padding: 8px 14px; z-index: 5;
}
.timeline input { flex: 1; accent-color: #4fe0c8; }
.timeline .dt { font-size: 12px; color: #8fb4d4; white-space: nowrap; }
.ly {
  font-size: 12px; color: #8fb4d4; border: 1px solid rgba(79, 195, 247, .35);
  padding: 2px 10px; cursor: pointer; user-select: none; background: rgba(14, 42, 74, .5);
}
.ly.on { background: linear-gradient(90deg, #35c8e8, #4fe0c8); color: #06121f; font-weight: 700; border-color: transparent; }
.blk { margin-bottom: 10px; }
.sub { font-size: 12px; color: #5f86a8; margin-bottom: 6px; letter-spacing: 1px; }
.mini { position: relative; }
.hot {
  display: flex; align-items: center; gap: 8px; font-size: 13px;
  padding: 5px 4px; cursor: pointer; border-bottom: 1px dashed rgba(74, 144, 197, .16);
}
.hot:hover { background: rgba(20, 52, 88, .5); }
.hot .rk { flex: 0 0 20px; text-align: center; color: #4c7396; }
.hot .rk.top { color: #ffc86e; font-weight: 700; }
.hot .nm { flex: 0 0 92px; color: #cfe3f5; }
.hot .bar { flex: 1; height: 7px; background: rgba(46, 85, 120, .5); }
.hot .bar i { display: block; height: 100%; background: linear-gradient(90deg, #1d8f8a, #4fe0c8); }
.hot .vv { flex: 0 0 46px; text-align: right; color: #4fc3f7; font-variant-numeric: tabular-nums; }
</style>
