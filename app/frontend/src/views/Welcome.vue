<template>
  <transition name="welcome-fade">
    <div class="welcome-mask" @click="done">
      <div class="welcome-box">
        <div class="welcome-leaves"><span>🌾</span><span>🍃</span><span>🌸</span></div>
        <h1 class="welcome-title rt-serif">
          <span v-for="(ch, i) in chars" :key="i" class="wch" :style="{ animationDelay: 0.15 + i * 0.09 + 's' }">{{ ch }}</span>
        </h1>
        <p class="welcome-sub">去乡间 · 把日子过成诗</p>
        <div class="welcome-line"></div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

const emit = defineEmits(['finish'])
const leaving = ref(false)
const chars = computed(() => '欢迎来到乡野智游'.split(''))
let t1 = null, t2 = null

onMounted(() => {
  // 展示约 2.2s 后自动淡出,过渡到主页
  t1 = setTimeout(() => {
    leaving.value = true
    t2 = setTimeout(() => emit('finish'), 650)
  }, 2200)
})
function done() {
  if (leaving.value) return
  clearTimeout(t1)
  leaving.value = true
  t2 = setTimeout(() => emit('finish'), 650)
}
</script>

<style scoped>
.welcome-mask {
  position: fixed; inset: 0; z-index: 3000;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(160deg, #E4EFE2 0%, #FAF8F2 48%, #F6E7D8 100%);
  cursor: pointer;
}
.welcome-box { text-align: center; padding: 0 20px; }
.welcome-leaves { font-size: 1.6rem; letter-spacing: 18px; margin-bottom: 18px; }
.welcome-leaves span { display: inline-block; animation: float 2.4s ease-in-out infinite; }
.welcome-leaves span:nth-child(2) { animation-delay: .3s; }
.welcome-leaves span:nth-child(3) { animation-delay: .6s; }
@keyframes float { 0%,100% { transform: translateY(0) rotate(-6deg); } 50% { transform: translateY(-10px) rotate(6deg); } }

.welcome-title { font-size: clamp(2rem, 7vw, 3.6rem); font-weight: 700; color: #2F6B3D; letter-spacing: .12em; }
.wch { display: inline-block; opacity: 0; transform: translateY(26px); animation: rise .7s cubic-bezier(.2, .7, .3, 1) forwards; }
@keyframes rise { to { opacity: 1; transform: translateY(0); } }

.welcome-sub {
  margin-top: 18px; color: #67715F; font-size: .95rem; letter-spacing: .35em;
  opacity: 0; animation: rise .8s .95s forwards;
}
.welcome-line {
  width: 0; height: 3px; margin: 22px auto 0; border-radius: 2px;
  background: linear-gradient(90deg, #3E7C4F, #C2662B);
  animation: grow 1s 1.1s forwards;
}
@keyframes grow { to { width: 160px; } }

.welcome-fade-leave-active { transition: opacity .6s ease, transform .6s ease; }
.welcome-fade-leave-to { opacity: 0; transform: scale(1.04); }
</style>
