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
 * Returns a validation rule that requires a value to be non-null. Useful for single selector field and object fields.
 * For string values, use {@link requiredTrimmed} instead.
 * For arrays (for example, in multiple selectors), use {@link requiredNotEmpty} instead.
 *
 * @param fieldLabelKey is used to personalize the error message saying which field is required.
 */
export function required(fieldLabelKey: string): FormItemRule {
  const {t} = useI18n()
  return {
    required: true,
    message: t("validations.required_if", {
      _field_: t(fieldLabelKey),
    }),
    trigger: ["input", "blur"],
  }
}

/**
 * Creates a validation rule that requires an array to be non-empty
 *
 * @param fieldLabelKey
 */
export function requiredNotEmpty(fieldLabelKey: string): FormItemRule {
  const {t} = useI18n();
  return {
    validator: (_rule, value: Array<unknown>) => {
      return value.length > 0;
    },
    message: t("validations.required_if", {
      _field_: t(fieldLabelKey),
    }),
    trigger: ["input", "blur"]
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