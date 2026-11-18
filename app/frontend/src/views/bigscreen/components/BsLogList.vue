<template>
  <div class="bs-log" ref="box">
    <div class="row" v-for="(item, i) in visible" :key="item.key">
      <span class="t">{{ item.time }}</span>
      <span class="tag" :class="item.type">{{ item.tag }}</span>
      <span class="msg">{{ item.msg }}</span>
    </div>
  </div>
</template>

<script setup>
/* 滚动日志：新日志从顶部插入，最多保留 maxRows 条，旧的淡出 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { nextLog } from '../data/provider'

const props = defineProps({
  seed: { type: Array, required: true },   // 初始日志
  pool: { type: Array, default: () => [] }, // 追加素材池
  interval: { type: Number, default: 6000 },
  maxRows: { type: Number, default: 30 }
})
const box = ref(null)
const visible = ref(props.seed.map((x, i) => ({ ...x, key: 's' + i })))
let timer = null
let seq = 0

onMounted(() => {
  if (!props.pool.length) return
  timer = setInterval(() => {
    const item = nextLog(props.pool)
    visible.value.unshift({ ...item, key: 'n' + (seq++) })
    if (visible.value.length > props.maxRows) visible.value.pop()
  }, props.interval)
})
onBeforeUnmount(() => timer && clearInterval(timer))
</script>
