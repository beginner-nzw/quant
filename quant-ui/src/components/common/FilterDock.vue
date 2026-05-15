<script setup lang="ts">
withDefaults(defineProps<{
  title?: string
  description?: string
}>(), {
  title: '筛选条件',
  description: ''
})
</script>

<template>
  <section class="filter-dock">
    <div class="filter-copy">
      <span>Filter Dock</span>
      <strong>{{ title }}</strong>
      <p v-if="description">{{ description }}</p>
    </div>
    <div class="filter-content">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.filter-dock {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
  border: 1px solid rgba(20, 32, 51, 0.08);
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 18px 46px rgba(32, 52, 80, 0.08);
}

.filter-copy span {
  display: block;
  color: #527086;
  font-size: 11px;
  font-weight: 950;
  letter-spacing: 0;
  text-transform: uppercase;
}

.filter-copy strong {
  display: block;
  margin-top: 8px;
  color: #142033;
  font-size: 20px;
  font-weight: 950;
  letter-spacing: 0;
}

.filter-copy p {
  margin: 8px 0 0;
  color: #6b7a8d;
  font-size: 13px;
  line-height: 1.7;
}

.filter-content {
  min-width: 0;
}

@media (max-width: 920px) {
  .filter-dock {
    grid-template-columns: 1fr;
  }
}
</style>
