<template>
  <Modal ref="modal">
    <template #header>
      <h4>{{ title ?? t("component.annotation.list-title") }}</h4>
    </template>
    <!-- Only created while a target is set: the list loads the annotations when it is created -->
    <AnnotationList
        v-if="targetUri"
        :target="targetUri"
        :displayTargetColumn="false"
        :enableActions="false"
    ></AnnotationList>
  </Modal>
</template>

<script setup lang="ts">
import {nextTick, ref, Ref, useTemplateRef} from "vue";
import {useI18n} from "vue-i18n";
import Modal from "@/components/common/views/Modal.vue";
import AnnotationList from "@/components/annotations/list/AnnotationList.vue";

/**
 * Read only list of the annotations of an element (no edit or delete actions), in a modal. Open it with show().
 *
 * Unlike the *ModalList components, it is not used to select anything.
 */

//#region Public
defineProps<{
  /** Title of the modal, "Annotations" if undefined */
  title?: string
}>();
//#endregion

//#region Private
//#region Plugins & services
const {t} = useI18n();
//#endregion

//#region Template refs
const modal = useTemplateRef<InstanceType<typeof Modal>>("modal");
//#endregion

//#region Data & Computed
/** URI of the element whose annotations are listed */
const targetUri: Ref<string | undefined> = ref<string | undefined>(undefined);
//#endregion

//#region Methods
/**
 * Opens the modal on the annotations of the element. The list is recreated each time, so it also shows
 * annotations created since the last opening.
 */
async function show(uri: string): Promise<void> {
  targetUri.value = undefined;
  await nextTick();
  targetUri.value = uri;
  modal.value?.show();
}

function hide(): void {
  modal.value?.hide();
}
//#endregion
//#endregion

defineExpose({
  show,
  hide
});
</script>
