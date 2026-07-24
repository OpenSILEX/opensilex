import {computed, inject, ref, TemplateRef} from 'vue'
import {useI18n} from 'vue-i18n'
import Modal from "@/components/common/views/Modal.vue";
import {NForm} from "naive-ui";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";


type UseModalFormOptions<T> = {
  modalRef: TemplateRef<InstanceType<typeof Modal>>
  nFormRef: TemplateRef<InstanceType<typeof NForm>>
  getEmptyForm: () => T
  create: (form: T) => Promise<HttpResponse<OpenSilexResponse>>
  update: (form: T) => Promise<HttpResponse<OpenSilexResponse>>
  reset: () => Promise<void> | void
  addTitle: string
  editTitle: string
  successMessage?: string
  overrideSuccessMessage?: boolean
  onCreate: (form: HttpResponse<OpenSilexResponse>) => void
  onUpdate: (form: HttpResponse<OpenSilexResponse>) => void
  onSuccess?: () => void
  onHide?: () => void
}

/**
 * UseModalFormLogic is a composable that handles the logic of a modal form. Parametric type T is the type of the form, usually a DTO.
 */
export default function useModalFormLogic<T>(options: UseModalFormOptions<T>) {
  const opensilex: any = inject('$opensilex')
  const { t } = useI18n()

  const form = ref(options.getEmptyForm())
  const editMode = ref(false)

  const formTitle = computed(() => t(editMode.value ? 'component.account.update' : 'component.account.add'))

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
        let msg = (options.successMessage ?? t('component.common.element'))

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

  function showCreateForm(passedForm?: T) {
    editMode.value = false
    form.value = passedForm ?? options.getEmptyForm()
    options.reset()
    options.nFormRef.value.restoreValidation()
    options.modalRef.value.show()
  }

  function showEditForm(editForm: T) {
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
    submit,
    formTitle,
  }
}
