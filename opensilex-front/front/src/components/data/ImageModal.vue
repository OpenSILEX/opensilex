<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    :title="t('ImageModal.title')"
    size="huge"
    style="width: 900px; max-width: 95vw"
  >
    <template #header>
      <div class="imageModalHeader">
        <i>
          <h4 class="imageModalTitle">
            <opensilex-Icon icon="fa#image" />
            {{ t('ImageModal.title') }}
          </h4>
        </i>
      </div>
    </template>

    <div class="imageModalBody">
      <img
        ref="image"
        :src="url || undefined"
        class="imageModalImage"
        alt=""
      />
    </div>

    <template #action>
      <div class="modal-footer-right">
        <n-button class="greenThemeColor" @click="hide()">
          {{ t('component.common.ok') }}
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NModal, NButton } from 'naive-ui'

const { t } = useI18n()

const emit = defineEmits<{
  (e: 'update:fileUrl', value: string | null | undefined): void
}>()

const props = defineProps<{
  fileUrl?: string | null
}>()

const visible = ref(false)
const image = ref<HTMLImageElement | null>(null)

const url = computed({
  get: () => props.fileUrl,
  set: (value) => emit('update:fileUrl', value)
})

function show() {
  visible.value = true
}

function hide() {
  visible.value = false
}

defineExpose({
  show,
  hide
})
</script>

<style scoped>
.imageModalHeader {
  width: 100%;
}

.imageModalTitle {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.imageModalBody {
  display: flex;
  justify-content: center;
  align-items: center;
}

.imageModalImage {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.validation-confirm-container {
  color: rgb(40, 167, 69);
  font-weight: bold;
}

.modal-footer-right{
    display: flex;
    justify-content: flex-end;
}
</style>

<i18n>
en:
  ImageModal:
    title: Image

fr:
  ImageModal:
    title: Image
</i18n>