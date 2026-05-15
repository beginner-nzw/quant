<script setup lang="ts">
withDefaults(defineProps<{
  title?: string
  description?: string
  actionLabel?: string
}>(), {
  title: '暂无数据',
  description: '当前条件下没有可展示的记录。',
  actionLabel: ''
})

const emit = defineEmits<{
  action: []
}>()
</script>

<template>
  <div class="empty-state">
    <div class="empty-mark" aria-hidden="true" />
    <strong>{{ title }}</strong>
    <p>{{ description }}</p>
    <button v-if="actionLabel" type="button" @click="emit('action')">
      {{ actionLabel }}
    </button>
  </div>
</template>

<style scoped>
.empty-state {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 42px 24px;
  border: 1px dashed rgba(20, 32, 51, 0.16);
  border-radius: var(--qa-radius-lg);
  background: rgba(255, 255, 255, 0.56);
  color: #142033;
  text-align: center;
}

.empty-mark {
  width: 48px;
  height: 4px;
  border-radius: 2px;
  background: #158c73;
}

.empty-state strong {
  font-size: 20px;
  font-weight: 950;
}

.empty-state p {
  max-width: 420px;
  margin: 0;
  color: #6b7a8d;
}

.empty-state button {
  margin-top: 8px;
  padding: 12px 18px;
  border: 0;
  border-radius: var(--qa-radius-md);
  background: #142033;
  color: #fff;
  font: inherit;
  font-weight: 900;
  cursor: pointer;
}
</style>
