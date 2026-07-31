import {computed, EmitFn, inject, ref, TemplateRef} from 'vue'
import {useI18n} from 'vue-i18n'
import Modal from "@/components/common/views/Modal.vue";
import {NForm} from "naive-ui";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";


export interface ModalFormEmits {
  onUpdate: [payload: HttpResponse<OpenSilexResponse>],
  onCreate: [payload: HttpResponse<OpenSilexResponse>],
  onSuccess: []
}

export interface ModalFormProps {
  createTitle: string,
  editTitle: string
}

type UseModalFormOptions<T> = {
  modalRef: TemplateRef<InstanceType<typeof Modal>>
  nFormRef: TemplateRef<InstanceType<typeof NForm>>
  /** called to reset form when showCreateForm is called without passing a form in it */
  getEmptyForm: () => T
  create: (form: T) => Promise<HttpResponse<OpenSilexResponse>>
  update: (form: T) => Promise<HttpResponse<OpenSilexResponse>>
  successMessage?: string
  overrideSuccessMessage?: boolean
  props: ModalFormProps
  emit: EmitFn<ModalFormEmits>
  /**called before opening modal on creation and edit mode. Use it if you need to update some interne data before showing the form. isEditMode can be safely called in the reset function.*/
  reset?: () => Promise<void> | void
  onHide?: () => void
}

/**
 * UseModalFormLogic is a composable that handles the logic of a modal form. Parametric type T is the type of the form, usually a DTO.
 */
export default function useModalFormLogic<T>(options: UseModalFormOptions<T>) {
  const opensilex: OpenSilexVuePlugin = inject('$opensilex');
  const { t } = useI18n();

  const form = ref(options.getEmptyForm());
  const isEditMode = ref(false);

  const formTitle = computed(() => t(isEditMode.value ? options.props.editTitle : options.props.createTitle));

  async function submit() {
    try {
      const ok =  await options.nFormRef.value.validate()
      if (!ok) return
    } catch (err) {
      return
    }

    const submitAction = isEditMode.value ? options.update : options.create

    try {
      showLoader()
      const result = await submitAction?.(form.value)
      if (result != null) {
        // success message
        let msg = (options.successMessage ?? t('component.common.element'))

        if (!options.overrideSuccessMessage) {
          msg += t(isEditMode.value
            ? 'component.common.success.update-success-message'
            : 'component.common.success.creation-success-message')
        }

        opensilex.showSuccessToast(msg)

        if (isEditMode.value) {
          options.emit("onUpdate", result);
        } else {
          options.emit("onCreate", result);
        }
        options.emit("onSuccess");

        options.modalRef.value.hide?.()
        options.onHide?.()
      }
    } catch (err) {
      opensilex?.errorHandler?.(err)
    } finally {
      hideLoader()
    }
  }

  function hide() {
    options.modalRef.value.hide?.()
    options.onHide?.()
  }

  function showCreateForm(passedForm?: T) {
    isEditMode.value = false
    form.value = passedForm ?? options.getEmptyForm()
    options.reset?.()
    options.nFormRef.value.restoreValidation()
    options.modalRef.value.show()
  }

  function showEditForm(editForm: T) {
    isEditMode.value = true
    form.value = editForm
    options.reset?.()
    options.nFormRef.value.restoreValidation()
    options.modalRef.value.show?.()
  }

  function showLoader(): void {
    opensilex.enableLoader();
    opensilex.showLoader();
  }

  function hideLoader(): void {
    opensilex.hideLoader();
    opensilex.disableLoader();
  }

  return {
    form,
    isEditMode,
    showCreateForm,
    showEditForm,
    hide,
    submit,
    formTitle,
  }
}
