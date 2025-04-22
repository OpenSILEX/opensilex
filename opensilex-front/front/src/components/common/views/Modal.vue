<template>
 <teleport to="body">
  <div
    class="modal fade"
    :class="{ show: visible }"
    :style="{ display: visible ? 'block' : 'none', backgroundColor: visible ? 'rgba(0,0,0,0.5)' : 'transparent' }"
    tabindex="-1"
    role="dialog"
    aria-modal="true"
  >
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
      <div class="modal-content">
        <div class="modal-header">
          <slot name="header"></slot>
          <button type="button" class="btn-close" @click="hide" aria-label="Close"></button>
        </div>

        <div class="modal-body">
          <slot></slot>
        </div>

        <div class="modal-footer">
          <slot name="footer">
            <button type="button" class="btn greenThemeColor" @click="hide">
              {{ t('component.common.close') }}
            </button>
          </slot>
        </div>
      </div>
    </div>
  </div>
 </teleport>
</template>

<script setup lang="ts">
import { ref, defineExpose } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const visible = ref(false);

function show() {
  visible.value = true;
  document.body.classList.add('modal-open');
}
function hide() {
  visible.value = false;
  document.body.classList.remove('modal-open');
}

defineExpose({
  show,
  hide
});
</script>

<style scoped>
.modal.show {
  display: block;
}
</style>
