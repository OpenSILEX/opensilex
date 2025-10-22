<template>
  <opensilex-ModalForm
    ref="modalForm"
    modal-size="lg"
    :tutorial="false"
    :component="formComponent"
    :createTitle="t('AnnotationModalForm.add')"
    :editTitle="t('AnnotationModalForm.edit')"
    icon="fa#vials"
    :create-action="create"
    :update-action="update"
  />
</template>

<script setup lang="ts">
import { ref, inject, withDefaults, defineProps, defineEmits, nextTick } from 'vue';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';

import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin';
import AnnotationForm from './AnnotationForm.vue';



import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse';
import type {
  AnnotationsService
} from 'opensilex-core/api/annotations.service';
import type {
  AnnotationCreationDTO,
  AnnotationUpdateDTO
} from 'opensilex-core/index';

// ----- props / emits
const props = withDefaults(defineProps<{
  editMode?: boolean;
}>(), {
  editMode: false
});

const emit = defineEmits<{
  (e:'onCreate', uri: string): void;
  (e:'onUpdate', uri: string): void;
}>();

// ----- services / env
const store = useStore();
const { t } = useI18n();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const service = opensilex.getService<AnnotationsService>('opensilex.AnnotationsService');

// ----- refs
const modalForm = ref<any>(null);
const formComponent = AnnotationForm;
const targets = ref<string[]>([]);

function showCreateForm(targetsArg: string[] = []) {
  targets.value = targetsArg ?? [];
  modalForm.value?.showCreateForm?.();
}

function showEditForm(form: AnnotationUpdateDTO) {
  modalForm.value?.showEditForm?.(form);
}

// ----- actions
async function create(annotation: AnnotationCreationDTO) {
  try {
    annotation.targets = targets.value;

    const http = await service.createAnnotation(annotation) as HttpResponse<OpenSilexResponse<string>>;
    const createdUri = http.response.result?.toString?.() ?? (http.response.result as any);

    const msg = `${t('Annotation.name')} ${createdUri} ${t('component.common.success.creation-success-message')}`;
    opensilex.showSuccessToast(msg);

    emit('onCreate', String(createdUri));
    return annotation; // renvoyer truthy au ModalForm
  } catch (error: any) {
    if (error?.status === 409) {
      opensilex.errorHandler(error, `Annotation ${annotation.uri} : ${t('Annotation.already-exist')}`);
    } else {
      opensilex.errorHandler(error);
    }
    return false;
  }
}

async function update(annotation: AnnotationUpdateDTO) {
  try {
    await service.updateAnnotation(annotation);
    const msg = `${t('Annotation.name')} ${annotation.uri} ${t('component.common.success.update-success-message')}`;
    opensilex.showSuccessToast(msg);
    emit('onUpdate', String(annotation.uri));
    return annotation; // truthy
  } catch (error: any) {
    opensilex.errorHandler(error);
    return false;
  }
}

// Expose pour le parent (AnnotationList.vue)
defineExpose({
  showCreateForm,
  showEditForm
});
</script>

<i18n>
en:
    AnnotationModalForm:
        add: Add annotation
        edit: Edit annotation

fr:
    AnnotationModalForm:
        add: Ajouter une annotation
        edit: Éditer l'annotation
</i18n>
