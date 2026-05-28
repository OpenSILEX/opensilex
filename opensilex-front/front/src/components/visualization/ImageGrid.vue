<template>
  <div class="imagesGrid">
    <opensilex-ImageSingle
      v-for="(image, index) in images"
      :key="index"
      :image="image"
      :index="index"
      @click="emit('click', $event)"
      @annotate="emit('annotate', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import type { DataFileImageDTO } from '../data/DataFileImageDTO'

withDefaults(defineProps<{
  images?: DataFileImageDTO[]
}>(), {
  images: () => []
})

const emit = defineEmits<{
  (e: 'click', value: any): void
  (e: 'annotate', value: any): void
}>()
</script>

<style scoped lang="scss">
.imagesGrid {
  display: flex;
  flex-wrap: wrap;
}
</style>