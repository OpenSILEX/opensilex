<template>
  <div class="row">
    <vue-easy-lightbox
      :visible="visible"
      :imgs="imgs"
      :index="index"
      @hide="visible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(defineProps<{
  images?: Array<{
    url: string
    [key: string]: any
  }>
}>(), {
  images: () => []
})

const visible = ref(false)
const index = ref(0)

const imgs = computed(() => {
  return props.images.map(image => image.url)
})

function openOnIndex(selectedIndex: number) {
  index.value = selectedIndex
  visible.value = true
}

defineExpose({
  openOnIndex
})
</script>

<style scoped lang="scss">
</style>