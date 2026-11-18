<template>
  <div>
    <p class="rt-muted" style="font-size:.85rem;line-height:1.8;margin-bottom:6px;max-width:72ch">
      三个数字对应十五五规划的三条口径。它们不是装饰，是这套系统立项时要交的答卷。
    </p>

    <!-- 三大政策口径 -->
    <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:16px;margin-top:16px">
      <div class="gd-pillar" data-idx="壹">
        <div class="policy">创新数字内容消费</div>
        <div class="cap">内容生产效率提升</div>
        <div class="big">+{{ pct(m.contentEfficiencyGain) }}<small>%</small></div>
        <div class="gd-track"><i :style="{ width: Math.min(100, pct(m.contentEfficiencyGain)) + '%' }"></i></div>
        <div class="note">
          {{ m.entityTotal ?? 0 }} 个实体产出 {{ m.contentTotal ?? 0 }} 条内容，
          覆盖 {{ m.modalityCovered ?? 0 }}/4 种模态，方言版 {{ m.dialectCount ?? 0 }} 条。
          同样的人力，内容产出按这个倍数放大。
        </div>
      </div>

      <div class="gd-pillar" data-idx="贰">
        <div class="policy">乡村振兴数智赋能</div>
        <div class="cap">游客到店转化率提升</div>
        <div class="big">+{{ pct(m.conversionGain) }}<small>%</small></div>
        <div class="gd-track"><i :style="{ width: Math.min(100, pct(m.conversionGain)) + '%' }"></i></div>
        <div class="note">
          已规划 {{ m.routeCount ?? 0 }} 条路线、平均 {{ (m.avgRouteNodes ?? 0).toFixed(1) }} 站/条，
          累计 {{ m.guideSessions ?? 0 }} 次现场导览。路线按式(1)(2)个性化生成，
          把"看看而已"变成"照着走"。
        </div>
      </div>

      <div class="gd-pillar" data-idx="叁">
        <div class="policy">智慧文旅发展</div>
        <div class="cap">游客服务满意度</div>
        <div class="big">{{ pct(m.satisfactionGain) }}<small>%</small></div>
        <div class="gd-track"><i :style="{ width: Math.min(100, pct(m.satisfactionGain)) + '%' }"></i></div>
        <div class="note">
          已回收 {{ m.feedbackCount ?? 0 }} 份评价，实时满意度 {{ pct(m.satisfaction) }}%。
          每条评价都经式(4)回写热度，讲得不好的内容会自己沉下去。
        </div>
      </div>
    </div>

    <!-- 分布 -->
    <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:16px;margin-top:16px">
      <div class="rt-card">
        <span class="rt-section-title" style="margin-top:0">模态分布</span>
        <div class="gd-bars">
          <div class="bar-row" v-for="b in m.byModality || []" :key="b.name">
            <span class="bar-label">{{ b.name }}</span>
            <span class="bar-track"><span class="bar-fill" :style="{ width: ratio(b.value, m.byModality) + '%' }"></span></span>
            <span class="bar-val gd-num">{{ b.value }}</span>
          </div>
        </div>
        <p class="rt-muted" style="font-size:.78rem;line-height:1.7;margin-top:12px">
          文案、语音、画面、视频四种模态齐备，实体内容覆盖率 {{ pct(m.contentCoverage) }}%。
        </p>
      </div>

      <div class="rt-card">
        <span class="rt-section-title" style="margin-top:0">风格分布</span>
        <div class="gd-bars">
          <div class="bar-row" v-for="b in m.byStyle || []" :key="b.name">
            <span class="bar-label">{{ b.name }}</span>
            <span class="bar-track"><span class="bar-fill" :style="{ width: ratio(b.value, m.byStyle) + '%' }"></span></span>
            <span class="bar-val gd-num">{{ b.value }}</span>
          </div>
        </div>
        <p class="rt-muted" style="font-size:.78rem;line-height:1.7;margin-top:12px">
          同一种素材三种腔调，配合 {{ m.dialectCount ?? 0 }} 条方言版本，
          解决县域内容"一套文案打天下"的老问题。
        </p>
      </div>
    </div>

    <!-- 式(4) 回写样本 -->
    <div class="rt-card" style="margin-top:16px">
      <span class="rt-section-title" style="margin-top:0">式(4) 热度回写样本</span>
      <div class="gd-formula" style="margin-top:4px">
        <div class="fx">h<sub>i</sub><sup>(t+1)</sup> = (1 − η)·h<sub>i</sub><sup>(t)</sup> <span class="op">+</span> η·f<sub>i</sub><sup>(t)</sup></div>
        <div class="weights"><span>η = 0.20</span><span>f<sub>i</sub> 为评价折算到 [0,1]</span></div>
        <div class="sub">
          滑动平均而不是直接覆盖：一次差评不会把热度打下去，但方向会变。
          下面是最新的 {{ (m.emaSamples || []).length }} 条回写记录。
        </div>
      </div>
      <el-table :data="m.emaSamples || []" size="small" style="margin-top:8px">
        <el-table-column prop="entityName" label="实体" min-width="140" />
        <el-table-column label="回写前" width="110" align="right">
          <template #default="{ row }"><span class="gd-num">{{ row.before?.toFixed(3) }}</span></template>
        </el-table-column>
        <el-table-column label="评价 f" width="100" align="right">
          <template #default="{ row }"><span class="gd-num" style="color:var(--rt-orange)">{{ row.feedback?.toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="回写后" width="110" align="right">
          <template #default="{ row }"><span class="gd-num" style="color:var(--rt-green-dark);font-weight:700">{{ row.after?.toFixed(3) }}</span></template>
        </el-table-column>
        <el-table-column label="变化" width="90" align="right">
          <template #default="{ row }">
            <span :style="{ color: (row.after - row.before) >= 0 ? 'var(--rt-green-dark)' : 'var(--rt-orange)' }">
              {{ (row.after - row.before) >= 0 ? '+' : '' }}{{ (row.after - row.before).toFixed(3) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 可复制性 -->
    <div class="rt-card" style="margin-top:16px">
      <span class="rt-section-title" style="margin-top:0">轻量化可复制性</span>
      <div class="gd-stats" style="margin-bottom:0">
        <div class="gd-stat"><div class="k">已接入县域</div><div class="v">{{ m.countyCount ?? 0 }}</div></div>
        <div class="gd-stat"><div class="k">实体总数</div><div class="v">{{ m.entityTotal ?? 0 }}</div></div>
        <div class="gd-stat"><div class="k">非遗</div><div class="v">{{ m.heritageCount ?? 0 }}</div></div>
        <div class="gd-stat"><div class="k">美食</div><div class="v">{{ m.foodCount ?? 0 }}</div></div>
        <div class="gd-stat"><div class="k">AI 生成占比</div><div class="v">{{ pct(m.aiRatio) }}<small>%</small></div></div>
      </div>
      <p class="rt-muted" style="font-size:.8rem;line-height:1.8;margin-top:14px">
        换一个县，只需要三件事：导入这个县的实体与坐标、批量跑一次内容生产、把导览入口挂出去。
        算法内核不依赖任何特定县域的数据，也不依赖付费大模型——关掉 AI 开关，
        本地模板依然能跑通全链路，这让财政紧张的县也能先用起来。
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api'

const m = ref({})
const pct = v => Math.round((Number(v) || 0) * 100)
const ratio = (v, list) => {
  const max = Math.max(1, ...(list || []).map(x => x.value))
  return Math.round((v / max) * 100)
}
async function load() { m.value = await adminApi.guideMetrics().catch(() => ({})) }
onMounted(load)
</script>
