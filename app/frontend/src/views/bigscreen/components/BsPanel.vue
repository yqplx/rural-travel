<template>
  <div class="bs-panel" :style="{ gridArea: area }">
    <div class="p-title">
      {{ title }} <span class="en" v-if="en">{{ en }}</span>
      <slot name="extra" style="margin-left:auto" />
    </div>
    <div class="p-body">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  en: { type: String, default: '' },
  area: { type: String, default: '' }
})
</script>
