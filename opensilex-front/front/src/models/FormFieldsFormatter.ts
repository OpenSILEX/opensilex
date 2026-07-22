import {useI18n} from 'vue-i18n';
import {FormItemRule} from "naive-ui";

export function required(fieldLabelKey: string): FormItemRule {
  const {t} = useI18n()
  return {
    required: true,
    trigger: ['input', 'blur'],
    message: t('validations.required_if', {_field_: t(fieldLabelKey)})
  }
}

// génère une règle "obligatoire" qui ignore les chaînes d'espaces, typiquement pour un champ "Name" par exemple
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

export function validEmail() {
  const {t} = useI18n()

  return {
    type: 'email',
    message: t('validations.emailError'),
    trigger: ['blur'],
  }
}