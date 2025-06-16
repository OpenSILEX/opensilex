<template>
  <div>
    <opensilex-Button
      :label="label"
      :small="small"
      :disabled="disabled"
      variant="outline-danger"
      icon="fa#trash-alt"
      @click="openModal"
    />

    <!-- Modal Bootstrap 5 -->
    <div
      class="modal fade"
      tabindex="-1"
      ref="modalRef"
      aria-labelledby="deleteModalLabel"
      aria-hidden="true"
    >
      <div class="modal-dialog modal-sm modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-body">
            {{ t('component.common.delete-confirmation') }}
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn btn-secondary btn-sm"
              data-bs-dismiss="modal"
              @click="closeModal"
            >
              {{ t('component.common.cancel') }}
            </button>
            <button
              type="button"
              class="btn btn-danger btn-sm"
              @click="confirmDelete"
            >
              {{ t('component.common.delete') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Modal } from 'bootstrap'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  label: string
  small: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'click'): void
}>()

const { t } = useI18n()

const modalRef = ref<HTMLElement | null>(null)
let modalInstance: Modal | null = null

onMounted(() => {
  if (modalRef.value) {
    modalInstance = new Modal(modalRef.value, {
      backdrop: 'static',
      keyboard: false,
    })
  }
})

function openModal() {
  modalInstance?.show()
}

function closeModal() {
  modalInstance?.hide()
}

function confirmDelete() {
  modalInstance?.hide()
  emit('click')
}
</script>

<style scoped lang="scss">
</style>
