<template>
  <div class="bs-kpi">
    <div class="v" :class="tone">{{ display }}</div>
    <div class="l">{{ label }}</div>
    <div v-if="delta" class="d" :class="up ? 'up' : 'down'">
      {{ up ? '▲' : '▼' }} {{ delta }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [Number, String], required: true },
  suffix: { type: String, default: '' },
  tone: { type: String, default: '' }, // '' | 'cyan' | 'amber'
  delta: { type: String, default: '' },
  up: { type: Boolean, default: true }
})
const display = computed(() =>
  typeof props.value === 'number'
    ? props.value.toLocaleString('zh-CN') + (props.suffix || '')
    : props.value + (props.suffix || '')
)
</script>
