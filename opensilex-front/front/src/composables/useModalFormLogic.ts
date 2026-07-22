import {inject, ref, TemplateRef} from 'vue'
import {useI18n} from 'vue-i18n'
import Modal from "@/components/common/views/Modal.vue";
import {NForm} from "naive-ui";

type UseModalFormOptions = {
  modalRef: TemplateRef<InstanceType<typeof Modal>>
  nFormRef: TemplateRef<InstanceType<typeof NForm>>
  getEmptyForm: () => any
  create: (form: any) => Promise<any>
  update: (form: any) => Promise<any>
  reset: () => Promise<void> | void
  successMessage?: string | ((form: any) => string)
  overrideSuccessMessage?: boolean
  onCreate: (res: any) => void
  onUpdate: (res: any) => void
  onSuccess?: () => void
  onHide?: () => void
}

export default function useModalFormLogic(options: UseModalFormOptions) {
  const opensilex: any = inject('$opensilex')
  const { t } = useI18n()

  const form = ref(options.getEmptyForm())
  const editMode = ref(false)

  async function submit() {
    try {
      const ok =  options.nFormRef.value.validate()
      if (!ok) return
    } catch (err) {
      return
    }

    const submitAction = editMode.value ? options.update : options.create

    try {
      showLoader()
      const result = await Promise.resolve(submitAction?.(form.value))
      if (result !== false) {
        // success message
        let msg = typeof options.successMessage === 'function'
          ? options.successMessage(form.value)
          : (options.successMessage ?? t('component.common.element'))

        if (!options.overrideSuccessMessage) {
          msg += t(editMode.value
            ? 'component.common.success.update-success-message'
            : 'component.common.success.creation-success-message')
        }

        opensilex.showSuccessToast(msg)

        if (editMode.value) options.onUpdate(result)
        else options.onCreate(result)

        options.onSuccess?.()
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

  function showCreateForm(passedForm?: any) {
    editMode.value = false
    form.value = passedForm ?? options.getEmptyForm()
    options.reset()
    options.nFormRef.value.restoreValidation()
    options.modalRef.value.show()
  }

  function showEditForm(editForm: any) {
    editMode.value = true
    form.value = editForm
    options.reset()
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
    editMode,
    showCreateForm,
    showEditForm,
    hide,
    submit
  }
}
