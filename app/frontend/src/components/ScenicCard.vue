<template>
  <div class="rt-card scenic-card" @click="$router.push(`/scenic/${spot.id}`)">
    <img class="cover" :src="spot.coverImage" :alt="spot.name" loading="lazy"
         @error="e => e.target.src = FALLBACK" />
    <div class="body">
      <div class="name">{{ spot.name }}</div>
      <div>
        <span class="rt-tag">{{ spot.categoryName }}</span>
        <span v-for="t in (spot.tagNames || []).slice(0, 2)" :key="t" class="rt-tag" style="background:var(--rt-orange-light); color:var(--rt-orange)">{{ t }}</span>
      </div>
      <div v-if="spot.reason" style="margin-top:8px"><span class="rt-reason">🌿 {{ spot.reason }}</span></div>
      <div class="meta">
        <span class="rt-price">{{ spot.ticketPrice > 0 ? '¥' + spot.ticketPrice : '免费' }}</span>
        <span class="rt-muted" style="font-size:.85rem">⭐ {{ spot.avgScore || '暂无' }} · {{ spot.viewCount }}人看过</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ spot: { type: Object, required: true } })
const FALLBACK = 'data:image/svg+xml;utf8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="240"><rect width="100%" height="100%" fill="#E4EFE2"/><text x="50%" y="52%" font-size="40" text-anchor="middle">🌾</text></svg>')
</script>
