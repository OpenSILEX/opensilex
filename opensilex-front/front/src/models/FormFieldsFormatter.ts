/**
 * Handy functions to pass as form validation rules for n-form
 */
import {useI18n} from 'vue-i18n';

/**
 * Returns a validation rule that requires a non-empty string. Typically used for a "Name" field, for example.
 * @param fieldLabelKey is used to personalize the error message saying which field is required.
 */
export function requiredTrimmed(fieldLabelKey: string) {
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
 * Returns a validation rule that requires at list one non-null object in an array. Useful for every selector field.
 * @param fieldLabelKey is used to personalize the error message saying which field is required.
 */
export function requiredArray(fieldLabelKey: string) {
  const {t} = useI18n()
  return {
    validator: (_rule: any, value: Array<any>) => {
      if (Array.isArray(value) && value.length > 0) {
        return true
      }
      return new Error(
          t('validations.required_if', {_field_: t(fieldLabelKey)})
      )
    },
    trigger: ['input', 'blur']
  }
}

export function validEmail() {
  const {t} = useI18n()

  return {
    type: 'email',
    message: t('validations.emailError'),
    trigger: ['blur'],
  }
}