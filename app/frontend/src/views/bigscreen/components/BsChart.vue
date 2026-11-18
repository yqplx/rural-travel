<template>
  <div ref="el" class="bs-chart"></div>
</template>

<script setup>
/* echarts 通用包装：初始化 / 自适应 / 卸载回收，视图层只管给 option */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true }
})
const el = ref(null)
let chart = null
let ro = null

function render() {
  if (chart && el.value) chart.setOption(props.option, true)
}

onMounted(() => {
  chart = echarts.init(el.value)
  chart.setOption(props.option)
  ro = new ResizeObserver(() => chart && chart.resize())
  ro.observe(el.value)
})
watch(() => props.option, render, { deep: true })
onBeforeUnmount(() => {
  ro && ro.disconnect()
  chart && chart.dispose()
  chart = null
})
</script>
