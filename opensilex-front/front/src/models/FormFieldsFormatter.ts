/**
 * Handy functions to pass as form validation rules for n-form
 */
import {useI18n} from 'vue-i18n';
import {FormItemRule} from "naive-ui";

/**
 * Returns a validation rule that requires a non-empty string. Typically used for a "Name" field, for example.
 * @param fieldLabelKey is used to personalize the error message saying which field is required.
 */
export function requiredTrimmed(fieldLabelKey: string): FormItemRule {
  const {t} = useI18n()
  return {
    validator: (_rule: any, value: string) => {
      if (typeof value === 'string' && value.trim().length > 0) {
        return true
      }
      return new Error(
          t('validations.required_if', {_field_: t(fieldLabelKey)})
      )
    },
    trigger: ['input', 'blur']
  }
}

/**
 * Returns a validation rule that requires a value to be non-null (and non-empty if the value is an array). Useful for every selector field and object fields.
 * For string values, use requiredTrimmed instead.
 * @param fieldLabelKey is used to personalize the error message saying which field is required.
 */
export function requiredObjectOrLists(fieldLabelKey: string): FormItemRule {
  const {t} = useI18n()
  return {
    required: true,
    message: t("validations.required_if", {
      _field_: t(fieldLabelKey),
    }),
    trigger: ["change", "blur"],
  }
}

export function validEmail(): FormItemRule {
  const {t} = useI18n()

  return {
    type: 'email',
    message: t('validations.emailError'),
    trigger: ['blur'],
  }
}