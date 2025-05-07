<template>
  <div class="modal fade" :class="{ show: isVisible }" tabindex="-1" style="display: block;" v-if="isVisible" @click.self="hide">
    <div class="modal-dialog" :class="modalSizeClass">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">
            <slot name="icon">
              <opensilex-Icon :icon="icon" class="icon-title" />
            </slot>
            {{ editMode ? $t(editTitle) : $t(createTitle) }}
          </h5>
          <button type="button" class="btn-close" @click="hide" aria-label="Close"></button>
        </div>
        <div class="modal-body">
            
            <!-- vee-validate remplacé ?  -->

          <!-- <ValidationObserver ref="validatorRef">
            <component
              :is="component"
              ref="componentRef"
              v-model:form="form"
              :editMode="editMode"
              :data="data"
              :disableValidation="disableValidation"
              @shownSelector="disableValidation = true"
              @hideSelector="disableValidation = false"
            >
              <slot name="customFields" :form="form" :editMode="editMode"></slot>
            </component>
          </ValidationObserver> -->
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="hide">
            {{ $t('component.common.cancel') }}
          </button>
          <button type="button" class="btn btn-primary" @click="validate">
            {{ $t('component.common.ok') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
// import { ValidationObserver } from '@vee-validate/components';

export default defineComponent({
  name: 'ModalForm',
  props: {
    component: { type: String, required: true },
    editTitle: { type: String, required: true },
    createTitle: { type: String, required: true },
    icon: { type: String, required: false },
    modalSize: { type: String, default: 'md' },
    data: { type: Object, default: () => ({}) },
    tutorial: { type: Boolean, default: false },
    static: { type: Boolean, default: true },
    doNotHideOnError: { type: Boolean, default: false },
    lazy: { type: Boolean, default: false },
    initForm: { type: Function, default: (form) => form },
    createAction: { type: Function },
    updateAction: { type: Function },
    successMessage: { type: [String, Function], default: 'component.common.element' },
    overrideSuccessMessage: { type: Boolean, default: false },
  },
  setup(props, { emit }) {
    const { t } = useI18n();
    const isVisible = ref(false);
    const editMode = ref(false);
    const form = ref({});
    const disableValidation = ref(true);
    const validatorRef = ref(null);
    const componentRef = ref(null);

    const modalSizeClass = computed(() => {
      return props.modalSize === 'full' ? 'modal-fullscreen' : `modal-${props.modalSize}`;
    });

    const showCreateForm = (passedForm = null) => {
      editMode.value = false;
      isVisible.value = true;
      form.value = passedForm || componentRef.value?.getEmptyForm() || {};
      form.value = props.initForm(form.value);
      validatorRef.value?.reset();
      componentRef.value?.reset?.();
    };

    const showEditForm = (editForm) => {
      editMode.value = true;
      isVisible.value = true;
      form.value = editForm;
      validatorRef.value?.reset();
      componentRef.value?.reset?.();
      componentRef.value?.onShowEditForm?.();
    };

    const hide = () => {
      isVisible.value = false;
      emit('hide');
    };

    const validate = () => {
      if (!disableValidation.value) {
        validatorRef.value?.validate().then((isValid) => {
          if (isValid) {
            let submitMethod = editMode.value ? props.updateAction || componentRef.value?.update : props.createAction || componentRef.value?.create;
            let successEvent = editMode.value ? 'onUpdate' : 'onCreate';

            let submitResult = submitMethod(form.value);
            if (!(submitResult instanceof Promise)) {
              submitResult = Promise.resolve(submitResult);
            }
            submitResult
              .then((result) => {
                if (result !== false && result !== undefined) {
                  creationOrUpdateMessage();
                }
                if (result !== false || !props.doNotHideOnError) {
                  hide();
                }
                emit(successEvent, result);
              })
              .catch((error) => {
                componentRef.value?.handleSubmitError?.(error);
              });
          }
        });
      }
    };

    const creationOrUpdateMessage = () => {
      let successMsg = typeof props.successMessage === 'function' ? props.successMessage(form.value) : t(props.successMessage);
      if (!props.overrideSuccessMessage) {
        successMsg += editMode.value ? t('component.common.success.update-success-message') : t('component.common.success.creation-success-message');
      }
      // Remplacer par le toaster 
      console.log(successMsg);
    };

    return {
      isVisible,
      editMode,
      form,
      disableValidation,
      validatorRef,
      componentRef,
      modalSizeClass,
      showCreateForm,
      showEditForm,
      hide,
      validate,
      t,
    };
  },
});
</script>
