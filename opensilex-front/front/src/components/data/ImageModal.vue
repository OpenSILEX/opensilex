<template>
  <Modal ref="modal" modal-size="lg">
    <template #header>
      <i>
        <h4>
          <Icon icon="fa#image" />
          {{ t('component.datafile.image-modal.title') }}
        </h4>
      </i>
    </template>

    <img
      ref="image"
      :src="url || undefined"
      class="img-fluid d-block mx-auto"
      alt=""
    />

    <template #footer>
      <button
        type="button"
        class="btn greenThemeColor"
        @click="hide()"
      >{{ t('component.common.ok') }}</button>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Icon from "@/components/common/views/Icon.vue";
import Modal from "@/components/common/views/Modal.vue";

const { t } = useI18n()

const emit = defineEmits<{
  (e: 'update:fileUrl', value: string | null | undefined): void
}>()

const props = defineProps<{
  fileUrl?: string | null
}>()

const modal = ref<InstanceType<typeof Modal> | null>(null)
const image = ref<HTMLImageElement | null>(null)

const url = computed({
  get: () => props.fileUrl,
  set: (value) => emit('update:fileUrl', value)
})

function show() {
  modal.value?.show()
}

function hide() {
  modal.value?.hide()
}

defineExpose({
  show,
  hide
})
</script>

<style scoped>
.validation-confirm-container {
  color: rgb(40, 167, 69);
  font-weight: bold;
}
</style>
