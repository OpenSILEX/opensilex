<template>
  <Modal
      v-if="display"
      ref="modalRef"
      :modalSize="modalSize"
  >
    <template #header>
      <b-row class="mt-1" style="width:100%">
        <b-col cols="10" >
          <i>
            <h4>
              <slot name="icon">
                <Icon :icon="icon" class="icon-title" />
              </slot>
              <span v-if="editMode">{{ t(editTitle) }}</span>
              <span v-else>{{ t(createTitle) }}</span>
            </h4>
          </i>
        </b-col>

        <HelpButton
            v-if="tutorial && !editMode"
            label="component.tutorial.name"
            @click="getFormRef().tutorial()"
            class="helpButton"
        ></HelpButton>
      </b-row>
    </template>

    <template #footer>
      <button
          type="button"
          class="btn btn-secondary"
          @click="hide"
      >
        {{ t('component.common.cancel') }}
      </button>
      <button
          type="button"
          class="btn greenThemeColor"
          @click="validate"
      >
        {{ t('component.common.ok') }}
      </button>
    </template>

    <n-form ref="validatorRef" :model="form" @keydown.enter="validate">
      <component
          ref="componentRef"
          :is="component"
          :editMode="editMode"
          v-model:form="form"
          :data="data"
          :disableValidation="disableValidation"
          @shownSelector="disableValidation=true"
          @hideSelector="disableValidation=false"
      >
        <slot name="customFields" :form="form" :editMode="editMode"></slot>
      </component>
    </n-form>
  </Modal>
</template>

<script lang="ts">
export type ModalInnerForm<CreationDTOType, UpdateDTOType> = {
  getEmptyForm: () => CreationDTOType;
  create?: (dto: CreationDTOType) => any;
  update?: (dto: UpdateDTOType) => any;
  reset?: () => void;
  tutorial?: () => void;
  setSelectorsToFirstTimeOpenAndSetLabels?: (objectsWithLabels : Array<any>) => void;
  onShowEditForm?: () => void;
  handleSubmitError?: (error) => void;
}
</script>

<script setup lang="ts">
/**
 * Represents a modal form used to create or update a resource. The prop `component` represents the inner form and can
 * be any type of Vue component as long as it implements a `getEmptyForm` method (see {@link ModalInnerForm}). If neither
 * of the props `createAction` or `updateAction` are given, then the `component` must implement a `create` or `update`
 * method.
 */
import {computed, inject, nextTick, ref, useTemplateRef} from "vue";
import {useI18n} from "vue-i18n";
import {NForm} from "naive-ui";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import Modal from "@/components/common/views/Modal.vue";
import Icon from "@/components/common/views/Icon.vue";
import HelpButton from "@/components/common/buttons/HelpButton.vue";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const { t } = useI18n();

const modalRef = useTemplateRef<InstanceType<typeof Modal>>("modalRef");
const validatorRef = useTemplateRef<InstanceType<typeof NForm>>("validatorRef");
const componentRef = useTemplateRef<any>("componentRef");

const props = withDefaults(defineProps<{
  tutorial?: boolean;
  component?: string | object;
  editTitle?: string;
  createTitle?: string;
  icon?: string;
  modalSize?: string;
  doNotHideOnError?: boolean;
  /**
   * Arbitrary data to be passed to the inner form component
   */
  data?: any;
  /**
   * Only renders the component when either {@link showEditForm} of {@link showCreateForm} is called.
   */
  lazy?: boolean;
  initForm?: (form: any) => any;
  createAction?: (dto: any) => any;
  updateAction?: (dto: any) => any;
  successMessage?: string | Function;
  overrideSuccessMessage?: boolean;
}>(), {
  tutorial: false,
  modalSize: "md",
  doNotHideOnError: false,
  lazy: false,
  initForm: () => {},
  successMessage: "component.common.element",
  overrideSuccessMessage: false
});

const emit = defineEmits(["hide", "onCreate", "onUpdate"]);

const editMode = ref(false);

const form = ref<any>({});

/**
 * Has the modal been opened at least once ?
 */
const opened = ref(false);

const disableValidation = ref<boolean>(true);

const display = computed(() => {
  return !props.lazy || opened.value;
});

async function validate() {
  if (!disableValidation.value) {
    try {
      await validatorRef.value.validate();
    } catch (errors) {
      return;
    }

    let submitMethod: any = getFormRef().create;
    if (props.createAction) {
      submitMethod = props.createAction;
    }
    let successEvent: "onCreate" | "onUpdate" = "onCreate";
    if (editMode.value) {
      if (props.updateAction) {
        submitMethod = props.updateAction;
      } else {
        submitMethod = getFormRef().update;
      }

      successEvent = "onUpdate";
    }

    Promise.resolve(submitMethod(form.value))
        .then(result => {
          if (result !== false && result !== undefined) {
            creationOrUpdateMessage();
          }
          nextTick(() => {
            if (result !== false) {
              emit(successEvent, result);
            }
            if (result !== false || !props.doNotHideOnError) {
              hide();
            }
          });
        })
        .catch((error) => {
          getFormRef().handleSubmitError?.(error);
        });
  }
}

function creationOrUpdateMessage() {
  let successMessage;
  if (typeof props.successMessage == "function") {
    successMessage = props.successMessage(form.value);
  } else {
    successMessage = t(props.successMessage);
  }

  if (!props.overrideSuccessMessage) {
    if (editMode.value) {
      successMessage =
          successMessage +
          t("component.common.success.update-success-message");
    } else {
      successMessage =
          successMessage +
          t("component.common.success.creation-success-message");
    }
  }
  opensilex.showSuccessToast(successMessage);
}

function getFormRef(): ModalInnerForm<any, any> {
  return componentRef.value as ModalInnerForm<any, any>;
}

/**
 *
 * @param passedForm , optional passing of some predefined fields for when the user arrives on the create form
 */
function showCreateForm(passedForm?: any) {
  opened.value = true;
  editMode.value = false;
  nextTick(() => {
    //Set passed form or create an empty one
    if (passedForm) {
      form.value = passedForm;
    } else {
      form.value = getFormRef().getEmptyForm();
    }

    let initializedForm = props.initForm(form.value);
    if (initializedForm) {
      form.value = initializedForm;
    }
    modalRef.value.show();
    disableValidation.value = false;
    validatorRef.value?.restoreValidation();
    if (getFormRef().reset) {
      getFormRef().reset();
    }
  });
}

/**
 *
 * @param initiallySelectedWithLabels any object type that contains the information required to set the inner-form's initially selected items (for update modals)
 *
 * @requires  setSelectorsToFirstTimeOpenAndSetLabels function to be defined in the form ref
 */
function setSelectorsToFirstTimeOpenAndSetLabels(initiallySelectedWithLabels) {
  getFormRef().setSelectorsToFirstTimeOpenAndSetLabels(initiallySelectedWithLabels);
}

function showEditForm(editForm: any) {
  opened.value = true;

  editMode.value = true;

  nextTick(() => {
    form.value = editForm;
    modalRef.value.show();
    disableValidation.value = false;
    validatorRef.value?.restoreValidation();
    if (getFormRef().reset) {
      getFormRef().reset();
    }
    if (getFormRef().onShowEditForm) {
      getFormRef().onShowEditForm();
    }
  });
}

function hide() {
  modalRef.value.hide();
  disableValidation.value = true;
  emit("hide");
}

defineExpose({
  showCreateForm,
  showEditForm,
  setSelectorsToFirstTimeOpenAndSetLabels,
  getFormRef,
  hide
});
</script>

<style lang="scss">
.modal-dialog.modal-full {
  max-width: 95%;
}
</style>

<style scoped lang="scss">
.helpButton {
  margin-left: 25px;
  color: #00A28C;
  font-size: 1.2em;
  border: none
}

.helpButton:hover {
  background-color: #00A28C;
  color: #f1f1f1
}

@media (min-width: 200px) and (max-width: 993px) {
  .helpButton {
    margin-left: 0px;
    margin-right: 15px;
  }
}
</style>
