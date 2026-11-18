<template>
  <div v-loading="loading" class="rt-page">
    <template v-if="spot">
      <div class="detail-top">
        <div class="gallery rt-card" style="padding:0; overflow:hidden">
          <el-carousel height="380px" indicator-position="outside">
            <el-carousel-item v-for="(img, i) in gallery" :key="i">
              <img :src="img" style="width:100%; height:100%; object-fit:cover" @error="onImgErr" />
            </el-carousel-item>
          </el-carousel>
        </div>
        <div class="rt-card info">
          <h1 class="rt-serif" style="font-size:1.6rem">{{ spot.name }}</h1>
          <div style="margin:10px 0">
            <span class="rt-tag">{{ spot.categoryName }}</span>
            <span v-for="t in spot.tagNames" :key="t" class="rt-tag" style="background:var(--rt-orange-light); color:var(--rt-orange)">{{ t }}</span>
          </div>
          <div class="kv"><span>⭐ 综合评分</span><b>{{ spot.avgScore || '暂无' }} ({{ spot.scoreCount }}人评)</b></div>
          <div class="kv"><span>🎫 门票</span><b class="rt-price">{{ spot.ticketPrice > 0 ? '¥' + spot.ticketPrice : '免费' }}</b></div>
          <div class="kv"><span>🕘 开放时间</span><b>{{ spot.openTime || '以景区公告为准' }}</b></div>
          <div class="kv"><span>📍 地址</span><b>{{ spot.address || (spot.province + spot.city) }}</b></div>
          <div class="kv"><span>👀 人气</span><b>{{ spot.viewCount }} 人看过</b></div>
          <div class="actions">
            <el-button v-if="auth.isLogin" :type="spot.favorited ? 'warning' : 'primary'" round @click="toggleFav">
              {{ spot.favorited ? '★ 已收藏' : '☆ 收藏' }}
            </el-button>
            <el-button v-else round @click="$router.push({ path: '/login', query: { redirect: $route.fullPath } })">登录后收藏</el-button>
            <el-button round type="success" plain :loading="guideLoading" @click="toggleGuide">
              {{ playing ? '⏸ 暂停讲解' : (paused ? '▶ 继续讲解' : '🔊 语音导游') }}
            </el-button>
            <el-button v-if="playing || paused" round type="danger" plain @click="stopGuide">⏹ 停止</el-button>
          </div>
          <div v-if="guideTitle" class="guide-status rt-muted">正在讲解:{{ guideTitle }}</div>
        </div>
      </div>

      <h2 class="rt-section-title">景区介绍</h2>
      <div class="rt-card"><p style="line-height:2">{{ spot.description }}</p></div>

      <h2 class="rt-section-title">特色亮点</h2>
      <div class="rt-card">
        <span v-for="f in featureList" :key="f" class="rt-tag" style="font-size:.88rem; padding:6px 14px">✨ {{ f }}</span>
        <el-alert v-if="spot.tips" :title="'游览贴士:' + spot.tips" type="warning" :closable="false" style="margin-top:14px" />
      </div>

      <h2 class="rt-section-title">游客评价</h2>
      <div class="rt-card">
        <div v-if="auth.isLogin" style="border-bottom:1px dashed var(--rt-rule); padding-bottom:16px; margin-bottom:16px">
          <div style="display:flex; align-items:center; gap:14px; margin-bottom:10px">
            <span>我的评分</span>
            <el-rate v-model="myRating.score" />
            <el-button type="primary" size="small" :disabled="!myRating.score" @click="submitRating">
              {{ spot.myScore ? '更新评价' : '提交评价' }}
            </el-button>
          </div>
          <el-input v-model="myRating.comment" type="textarea" :rows="2" maxlength="500"
                    placeholder="说说这里给你的感觉…(选填)" />
        </div>
        <div v-for="r in ratings" :key="r.id" class="rating-item">
          <div style="display:flex; justify-content:space-between">
            <b>{{ r.nickname || r.username }}</b>
            <span><el-rate :model-value="r.score" disabled size="small" /></span>
          </div>
          <p class="rt-muted" style="margin-top:4px">{{ r.comment || '这个游客很低调,只留下了星星' }}</p>
          <div class="rt-muted" style="font-size:.75rem">{{ r.createdAt }}</div>
        </div>
        <el-empty v-if="!ratings.length" description="还没有评价,来抢沙发" />
      </div>

      <h2 class="rt-section-title">猜你喜欢</h2>
      <div class="rt-grid">
        <ScenicCard v-for="s in similar" :key="s.id" :spot="s" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { favoriteApi, ratingApi, scenicApi } from '@/api'
import { auth } from '@/store/auth'
import ScenicCard from '@/components/ScenicCard.vue'

const FALLBACK = 'data:image/svg+xml;utf8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="800" height="450"><rect width="100%" height="100%" fill="#E4EFE2"/><text x="50%" y="52%" font-size="60" text-anchor="middle">🌾</text></svg>')

const route = useRoute()
const spot = ref(null)
const ratings = ref([])
const similar = ref([])
const loading = ref(true)
const myRating = reactive({ score: 0, comment: '' })

const gallery = computed(() => {
  if (!spot.value) return []
  const imgs = (spot.value.images || []).map(i => i.imageUrl)
  return [spot.value.coverImage, ...imgs].filter(Boolean)
})
const featureList = computed(() =>
  (spot.value?.features || '').split(/[,、,]/).map(s => s.trim()).filter(Boolean))

let enterAt = Date.now()

async function loadAll(id) {
  loading.value = true
  stopGuide()
  enterAt = Date.now()
  myRating.score = 0
  myRating.comment = ''
  try {
    spot.value = await scenicApi.detail(id)
    if (spot.value.myScore) myRating.score = spot.value.myScore
  } catch {}
  try { ratings.value = await scenicApi.ratings(id) } catch {}
  try { similar.value = await scenicApi.similar(id) } catch {}
  loading.value = false
  window.scrollTo({ top: 0, behavior: 'instant' })
}

onMounted(() => loadAll(route.params.id))

// 「猜你喜欢」卡片跳转:同一路由组件复用时,监听 id 变化重新加载
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) loadAll(newId)
})

onBeforeUnmount(() => {
  stopGuide()
  // 离开时上报真实停留时长(登录用户)
  if (auth.isLogin && spot.value) {
    const duration = Math.round((Date.now() - enterAt) / 1000)
    if (duration > 3) {
      navigator.sendBeacon?.('/api/browse-history',
        new Blob([JSON.stringify({ scenicSpotId: spot.value.id, duration })], { type: 'application/json' }))
    }
  }
})

async function toggleFav() {
  const id = spot.value.id
  try {
    if (spot.value.favorited) { await favoriteApi.remove(id); spot.value.favorited = false; ElMessage.success('已取消收藏') }
    else { await favoriteApi.add(id); spot.value.favorited = true; ElMessage.success('已加入收藏') }
  } catch {}
}

async function submitRating() {
  try {
    await ratingApi.submit({ scenicSpotId: spot.value.id, score: myRating.score, comment: myRating.comment })
    ElMessage.success('评价成功,感谢分享')
    spot.value = await scenicApi.detail(spot.value.id)
    ratings.value = await scenicApi.ratings(spot.value.id)
  } catch {}
}

/* ---------- 语音导游(Web Speech API) ---------- */
const guideLoading = ref(false)
const playing = ref(false)
const paused = ref(false)
const guideTitle = ref('')
let segments = []

async function toggleGuide() {
  if (playing.value) {
    window.speechSynthesis.pause()
    playing.value = false
    paused.value = true
    return
  }
  if (paused.value) {
    window.speechSynthesis.resume()
    paused.value = false
    playing.value = true
    return
  }
  guideLoading.value = true
  try {
    const guide = await scenicApi.voiceGuide(spot.value.id)
    segments = guide.segments
    speakNext(0)
  } catch {}
  guideLoading.value = false
}

function speakNext(i) {
  if (i >= segments.length) { stopGuide(); ElMessage.success('讲解结束,祝您旅途愉快'); return }
  const seg = segments[i]
  guideTitle.value = seg.title
  const u = new SpeechSynthesisUtterance(seg.title + '。' + seg.text)
  u.lang = 'zh-CN'
  u.rate = 0.95
  u.onend = () => speakNext(i + 1)
  u.onerror = () => stopGuide()
  playing.value = true
  window.speechSynthesis.speak(u)
}

function stopGuide() {
  window.speechSynthesis.cancel()
  playing.value = false
  paused.value = false
  guideTitle.value = ''
}

function onImgErr(e) { e.target.src = FALLBACK }
</script>

<style scoped>
.detail-top { display: grid; grid-template-columns: 1.4fr 1fr; gap: 20px; }
@media (max-width: 900px) { .detail-top { grid-template-columns: 1fr; } }
.info .kv { display: flex; justify-content: space-between; padding: 9px 0; border-bottom: 1px dashed var(--rt-rule); font-size: .92rem; }
.info .kv span { color: var(--rt-muted); }
.actions { display: flex; gap: 10px; margin-top: 18px; flex-wrap: wrap; }
.guide-status { margin-top: 10px; font-size: .82rem; }
.rating-item { padding: 12px 0; border-bottom: 1px dashed var(--rt-rule); }
</style>
