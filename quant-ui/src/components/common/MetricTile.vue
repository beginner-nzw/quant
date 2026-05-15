<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  value: string | number
  hint?: string
  tone?: 'blue' | 'green' | 'red' | 'amber' | 'ink'
}>(), {
  hint: '',
  tone: 'ink'
})
</script>

<template>
  <div class="metric-tile" :class="`tone-${tone}`">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <small v-if="hint">{{ hint }}</small>
  </div>
</template>

<style scoped>
.metric-tile {
  position: relative;
  overflow: hidden;
  min-height: 128px;
  padding: 18px;
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.72);
  color: #142033;
  box-shadow: inset 0 0 0 1px rgba(20, 32, 51, 0.08), 0 18px 46px rgba(32, 52, 80, 0.08);
}

.metric-tile span,
.metric-tile strong,
.metric-tile small {
  position: relative;
  z-index: 1;
  display: block;
}

.metric-tile span {
  color: #6b7a8d;
  font-size: 13px;
  font-weight: 900;
}

.metric-tile strong {
  margin-top: 16px;
  font-size: 36px;
  font-weight: 950;
  letter-spacing: 0;
  line-height: 1;
}

.metric-tile small {
  margin-top: 8px;
  color: #79889a;
  font-size: 12px;
}

.tone-blue {
  color: #2653b1;
}

.tone-green {
  color: #158c73;
}

.tone-red {
  color: #cf3b38;
}

.tone-amber {
  color: #b26b11;
}

.tone-ink {
  color: #142033;
}
</style>
