<template>
  <div>
    <!-- 主视觉:乡村风景轮播 + 宣传语叠加 -->
    <section class="hero-carousel" @mouseenter="pauseAuto" @mouseleave="resumeAuto">
      <div v-for="(s, i) in slides" :key="s.id" class="hero-slide" :class="{ active: i === cur }"
           :aria-hidden="i !== cur">
        <img class="hero-bg" :src="heroImg(s)" :alt="s.name" :loading="i === 0 ? 'eager' : 'lazy'"
             @error="e => e.target.style.opacity = 0" />
      </div>
      <div class="hero-scrim"></div>

      <div class="hero-slogan">
        <span class="slogan-badge">✨ 去乡间，把日子过成诗</span>
      </div>

      <div class="hero-content">
        <transition name="hero-text" mode="out-in">
          <div :key="cur" class="hero-text">
            <h1 class="rt-serif">{{ slides[cur]?.name }}</h1>
            <p>{{ slides[cur]?.description }}</p>
            <el-button round type="warning" size="large" @click="goSpot(slides[cur])">
              去看看 <span style="margin-left:4px">→</span>
            </el-button>
          </div>
        </transition>
      </div>

      <button class="hero-arrow left" aria-label="上一张" @click="prev">‹</button>
      <button class="hero-arrow right" aria-label="下一张" @click="next">›</button>

      <div class="hero-dots">
        <span v-for="(s, i) in slides" :key="i" class="dot" :class="{ on: i === cur }"
              @click="go(i)"></span>
      </div>
    </section>

    <div class="rt-page">
      <!-- 搜索工具条 -->
      <div class="rt-card search-bar">
        <el-input v-model="keyword" size="large" placeholder="搜索景区、特色、城市…"
                  clearable @keyup.enter="goSearch">
          <template #append><el-button @click="goSearch">搜索</el-button></template>
        </el-input>
      </div>

      <!-- 分类快捷入口 -->
      <h2 class="rt-section-title">按心情出发</h2>
      <div class="cat-grid">
        <div v-for="c in categories" :key="c.id" class="cat-card rt-card" @click="goCategory(c.id)">
          <div class="cat-icon">{{ c.icon || '🌾' }}</div>
          <div>{{ c.name }}</div>
        </div>
      </div>

      <!-- 为你推荐 / 热门推荐 (分页) -->
      <div class="rec-head">
        <h2 class="rt-section-title" style="margin-bottom:0">{{ auth.isLogin ? '为你推荐' : '热门推荐' }}</h2>
        <div class="rec-meta">
          <span class="rec-count">共 {{ total }} 个景区 · 第 {{ page }}/{{ pages }} 页</span>
          <el-button v-if="auth.isLogin" round plain type="primary" size="small" @click="prefOpen = true">
            ⚙️ 调整偏好
          </el-button>
        </div>
      </div>
      <div v-loading="loading" class="rt-grid">
        <ScenicCard v-for="s in spots" :key="s.id" :spot="s" />
      </div>
      <el-empty v-if="!loading && !spots.length" description="暂无推荐,去景区页逛逛吧" />

      <!-- 分页导航 -->
      <div v-if="total > size" class="pager">
        <el-pagination
          background
          layout="prev, pager, next, jumper"
          :current-page="page"
          :page-size="size"
          :total="total"
          :pager-count="7"
          @current-change="onPage"
        />
      </div>
    </div>

    <!-- 用户偏好弹窗(登录用户首次进入自动弹出) -->
    <PreferenceModal v-model="prefOpen" @saved="loadPage(1)" />
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { categoryApi, scenicApi } from '@/api'
import { auth } from '@/store/auth'
import ScenicCard from '@/components/ScenicCard.vue'
import PreferenceModal from '@/components/PreferenceModal.vue'

const router = useRouter()
const keyword = ref('')
const categories = ref([])
const spots = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)
const pages = ref(1)
const loading = ref(true)
const slides = ref([])
const cur = ref(0)
const prefOpen = ref(false)
let timer = null

onMounted(async () => {
  try { categories.value = await categoryApi.list() } catch {}
  loadPage(1)
  try {
    // 轮播取综合评分 Top5 景区作为主视觉
    const res = await scenicApi.page({ size: 5, sort: '' })
    slides.value = (res.records || []).filter(s => s.coverImage)
    if (slides.value.length) startAuto()
  } catch {}
  // 登录用户且本次浏览器未设置过偏好 → 自动弹出偏好弹窗
  if (auth.isLogin && !localStorage.getItem('rural_pref_done')) {
    setTimeout(() => { prefOpen.value = true }, 700)
  }
})

onBeforeUnmount(stopAuto)

function loadPage(p) {
  page.value = p
  loading.value = true
  scenicApi.page({ current: p, size: size.value, sort: '' })
    .then(res => {
      spots.value = res.records || []
      total.value = res.total || 0
      // 后端 PageResult 不返回 pages,这里自行计算
      pages.value = Math.ceil((res.total || 0) / size.value) || 1
    })
    .catch(() => {})
    .finally(() => { loading.value = false })
  // 滚到推荐区上方
  if (p > 1) {
    const el = document.querySelector('.rec-head')
    el && el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}
function onPage(p) { loadPage(p) }

/* ---- 轮播 ---- */
const heroImg = s => s.coverImage.replace('/800/450', '/1600/700')
function startAuto() {
  stopAuto()
  timer = setInterval(() => next(), 5000)
}
function stopAuto() { if (timer) { clearInterval(timer); timer = null } }
function pauseAuto() { stopAuto() }
function resumeAuto() { if (slides.value.length > 1) startAuto() }
function next() { cur.value = (cur.value + 1) % slides.value.length }
function prev() { cur.value = (cur.value - 1 + slides.value.length) % slides.value.length }
function go(i) { cur.value = i }

function goSpot(s) { if (s) router.push(`/scenic/${s.id}`) }
function goSearch() { router.push({ path: '/scenic', query: { keyword: keyword.value } }) }
function goCategory(id) { router.push({ path: '/scenic', query: { categoryId: id } }) }
</script>

<style scoped>
/* ---- 轮播主视觉 ---- */
.hero-carousel {
  position: relative; height: 460px; overflow: hidden; background: #26302A;
}
.hero-slide { position: absolute; inset: 0; opacity: 0; transition: opacity .9s ease; }
.hero-slide.active { opacity: 1; }
.hero-bg {
  width: 100%; height: 100%; object-fit: cover; display: block;
  animation: kenburns 9s ease-out forwards;
}
@keyframes kenburns { from { transform: scale(1); } to { transform: scale(1.06); } }
.hero-scrim {
  position: absolute; inset: 0; pointer-events: none;
  background: linear-gradient(12deg, rgba(15, 25, 18, .68) 0%, rgba(15, 25, 18, .32) 42%, rgba(15, 25, 18, .08) 70%);
}
.hero-slogan { position: absolute; top: 22px; left: 26px; z-index: 3; }
.slogan-badge {
  display: inline-block; padding: 7px 16px; border-radius: 22px;
  background: rgba(250, 248, 242, .16); backdrop-filter: blur(6px);
  color: #FAF8F2; font-size: .88rem; letter-spacing: .12em; border: 1px solid rgba(250,248,242,.28);
}
.hero-content { position: absolute; left: 0; right: 0; bottom: 54px; z-index: 3; }
.hero-text {
  max-width: 1200px; margin: 0 auto; padding: 0 26px; color: #fff;
}
.hero-text h1 {
  font-size: clamp(1.7rem, 4vw, 2.7rem); font-weight: 700;
  text-shadow: 0 2px 18px rgba(0, 0, 0, .45); margin-bottom: 10px;
}
.hero-text p {
  max-width: 620px; font-size: .95rem; line-height: 1.8; color: rgba(255, 255, 255, .94);
  text-shadow: 0 1px 8px rgba(0, 0, 0, .5);
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  margin-bottom: 18px;
}
.hero-text :deep(.el-button) { background: var(--rt-orange); border-color: var(--rt-orange); }
.hero-text :deep(.el-button:hover) { background: #a8551f; border-color: #a8551f; }

.hero-arrow {
  position: absolute; top: 50%; transform: translateY(-50%); z-index: 4;
  width: 44px; height: 44px; border-radius: 50%; border: none; cursor: pointer;
  background: rgba(250, 248, 242, .22); color: #fff; font-size: 1.6rem; line-height: 1;
  backdrop-filter: blur(4px); opacity: 0; transition: all .25s;
  display: flex; align-items: center; justify-content: center; padding-bottom: 4px;
}
.hero-carousel:hover .hero-arrow { opacity: 1; }
.hero-arrow:hover { background: rgba(250, 248, 242, .42); }
.hero-arrow.left { left: 18px; }
.hero-arrow.right { right: 18px; }

.hero-dots {
  position: absolute; bottom: 20px; left: 0; right: 0; z-index: 4;
  display: flex; justify-content: center; gap: 10px;
}
.dot {
  width: 26px; height: 4px; border-radius: 3px; cursor: pointer;
  background: rgba(250, 248, 242, .38); transition: all .3s;
}
.dot.on { background: #FAF8F2; width: 40px; }

.hero-text-enter-active, .hero-text-leave-active { transition: all .45s ease; }
.hero-text-enter-from { opacity: 0; transform: translateY(16px); }
.hero-text-leave-to { opacity: 0; transform: translateY(-10px); }

/* ---- 搜索条 ---- */
.search-bar { margin-top: -34px; position: relative; z-index: 5; padding: 14px 18px; }

/* ---- 分类 ---- */
.cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 14px; }
.cat-card { text-align: center; cursor: pointer; padding: 22px 10px; font-weight: 600; }
.cat-icon { font-size: 2rem; margin-bottom: 8px; }
.rec-head { display: flex; align-items: center; justify-content: space-between; margin-top: 36px; flex-wrap: wrap; gap: 10px; }
.rec-meta { display: flex; align-items: center; gap: 14px; }
.rec-count { color: var(--rt-muted); font-size: .88rem; }
.pager { display: flex; justify-content: center; margin: 28px 0 14px; }

@media (max-width: 768px) {
  .hero-carousel { height: 380px; }
  .hero-content { bottom: 48px; }
  .hero-arrow { display: none; }
  .search-bar { margin-top: -26px; }
}
</style>
