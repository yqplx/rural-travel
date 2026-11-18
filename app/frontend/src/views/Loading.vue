<template>
  <transition name="fade">
    <div class="loading-mask">
      <div class="loading-box">
        <div class="plant">🌱</div>
        <div class="title rt-serif">乡野智游</div>
        <div class="sub">轻量化AI乡村文旅智能引流与服务系统</div>
        <div class="bar"><div class="fill" :style="{ width: progress + '%' }"></div></div>
        <div class="tip">{{ tip }}</div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({ ready: Boolean })
const emit = defineEmits(['finish'])
const progress = ref(0)
const tip = ref('正在走进田野…')
const tips = ['正在走进田野…', '采摘最新鲜的灵感…', '为你铺开乡间小路…', '马上就好…']
let timer = null

onMounted(() => {
  let i = 0
  timer = setInterval(() => {
    progress.value = Math.min(progress.value + Math.random() * 12, props.ready ? 100 : 88)
    if (progress.value >= 100) {
      clearInterval(timer)
      setTimeout(() => emit('finish'), 350)
    }
    tip.value = tips[++i % tips.length]
  }, 220)
})
onBeforeUnmount(() => clearInterval(timer))
watch(() => props.ready, v => { if (v) progress.value = Math.max(progress.value, 92) })
</script>

<style scoped>
.loading-mask { position: fixed; inset: 0; z-index: 2000; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(160deg, #EDF3E7 0%, #FAF8F2 50%, #F6E7D8 100%); }
.loading-box { text-align: center; width: 300px; }
.plant { font-size: 3rem; animation: grow 1.6s ease-in-out infinite; }
@keyframes grow { 0%,100% { transform: scale(1) rotate(-4deg); } 50% { transform: scale(1.18) rotate(4deg); } }
.title { font-size: 1.8rem; font-weight: 700; color: #2F6B3D; margin-top: 10px; }
.sub { color: #67715F; font-size: .85rem; margin: 6px 0 26px; }
.bar { height: 8px; background: #E2DCCB; border-radius: 6px; overflow: hidden; }
.fill { height: 100%; border-radius: 6px; background: linear-gradient(90deg, #6FA07E, #3E7C4F); transition: width .25s; }
.tip { margin-top: 12px; color: #67715F; font-size: .8rem; }
.fade-leave-active { transition: opacity .5s; }
.fade-leave-to { opacity: 0; }
</style>
