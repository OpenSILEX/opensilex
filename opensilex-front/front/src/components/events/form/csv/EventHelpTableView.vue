<template>
  <div class="table-responsive-xl">
    <p @click="visible = !visible" style="cursor: pointer">
      <strong>{{ t('EventHelpTableView.expected-format') }} </strong>
      <opensilex-Icon
        v-if="!visible"
        icon="fa#eye"
        class="EventHelpTableViewHelpEyeIcon"
      />
      <opensilex-Icon
        v-else
        icon="fa#eye-slash"
        class="EventHelpTableViewHelpEyeIcon"
      />
    </p>

    <div v-show="visible" class="mt-2">
      <div class="table-responsive">
        <table class="table help-table">
          <thead>
            <tr>
              <th>1</th>
              <th class="required uri-field">{{ t('component.common.uri') }}</th>
              <th class="uri-field">{{ t('component.common.type') }}</th>
              <th>IsInstant</th>
              <th>{{ t('component.events.start') }}</th>
              <th>{{ t('component.events.end') }}</th>
              <th class="required uri-field">{{ t('component.events.target') }}</th>
              <th>{{ t('component.common.description') }}</th>

              <th v-if="isMove" class="uri-field">
                {{ t('component.common.geometry.from') }}
              </th>
              <th v-if="isMove" class="uri-field">
                {{ t('component.common.geometry.too') }}
              </th>
              <th v-if="isMove">
                {{ t('component.common.geometry.coordinates') }}
              </th>
              <th v-if="isMove">
                {{ t('component.common.geometry.x') }}
              </th>
              <th v-if="isMove">
                {{ t('component.common.geometry.y') }}
              </th>
              <th v-if="isMove">
                {{ t('component.common.geometry.z') }}
              </th>
            </tr>
          </thead>

          <tbody>
            <tr v-if="!isMove">
              <th>2</th>
              <td>{{ t('EventHelpTableView.uri-help') }}</td>
              <td>{{ t('EventHelpTableView.type-help') }}</td>
              <td>{{ t('EventHelpTableView.is-instant-help') }}</td>
              <td>{{ t('EventHelpTableView.start-help') }}</td>
              <td>{{ t('EventHelpTableView.end-help') }}</td>
              <td>{{ t('EventHelpTableView.target-help') }}</td>
              <td>{{ t('component.common.description') }}</td>
            </tr>

            <tr v-else>
              <th>2</th>
              <td>{{ t('MoveHelpTableView.uri-help') }}</td>
              <td>{{ t('MoveHelpTableView.type-help') }}</td>
              <td>{{ t('MoveHelpTableView.is-instant-help') }}</td>
              <td>{{ t('MoveHelpTableView.start-help') }}</td>
              <td>{{ t('MoveHelpTableView.end-help') }}</td>
              <td>{{ t('MoveHelpTableView.target-help') }}</td>
              <td>{{ t('component.common.description') }}</td>

              <td>{{ t('component.common.geometry.from-help') }}</td>
              <td>{{ t('component.common.geometry.to-help') }}</td>
              <td>{{ t('component.common.geometry.geometry-help') }}</td>
              <td>{{ t('component.common.geometry.x-help') }}</td>
              <td>{{ t('component.common.geometry.y-help') }}</td>
              <td>{{ t('component.common.geometry.z-help') }}</td>
            </tr>

            <tr class="table-info">
              <th>3</th>
              <td colspan="100%">
                <div class="help-text">
                  <div>{{ t('EventHelpTableView.text-help-line-1') }}</div>
                  <div>{{ t('EventHelpTableView.text-help-line-2') }}</div>
                  <div>{{ t('EventHelpTableView.text-help-line-3') }}</div>
                  <div>
                    {{ t('EventHelpTableView.text-help-line-4') }}
                    <strong>","</strong>
                  </div>
                  <div>
                    {{ t('EventHelpTableView.text-help-line-5', { decimalSeparator: '.' }) }}
                  </div>
                  <br />
                  <div>
                    <strong>{{ t('EventHelpTableView.text-help-line-6') }}</strong>
                  </div>
                  <div>
                    <strong>{{ t('EventHelpTableView.text-help-line-7') }}</strong>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, inject } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { useI18n } from 'vue-i18n'

const props = withDefaults(defineProps<{
  isMove?: boolean
}>(), {
  isMove: false
})

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()
const visible = ref(true)

function getDataTypeLabel(dataTypeUri: string): string | undefined {
  if (!dataTypeUri) {
    return undefined
  }

  const label = t($opensilex.getDatatype(dataTypeUri).label_key)
  return label ? label.charAt(0).toUpperCase() + label.slice(1) : undefined
}
</script>

<style scoped>
.table-responsive {
  overflow-x: auto;
}

.help-table {
  width: max-content;
  min-width: 100%;
}

.help-table th,
.help-table td {
  white-space: nowrap;
  word-break: normal;
  overflow-wrap: normal;
  vertical-align: top;
}
</style>

<i18n>
en:
  EventHelpTableView:
    expected-format: "Expected format"
    text-help-line-1: "Your can insert your data from this row."
    text-help-line-2: "First two rows of CSV content will be ignored."
    text-help-line-3: "Column orders matter."
    text-help-line-4: "CSV separator is"
    text-help-line-5: 'Decimal separator is "{decimalSeparator}"'
    text-help-line-6: "Blank values will be ignored."
    text-help-line-7: "Any unknown column identifier will be considered as an error."
    uri-help: "Event URI (autogenerated if empty)"
    type-help: "Event type URI"
    is-instant-help: "Indicate if the event is instantaneous or not"
    start-help: "Beginning of event, only if the event is not instantaneous"
    end-help: "End of event, required if the event is instantaneous"
    target-help: "Object targeted by the event (Must exist)"

  MoveHelpTableView:
    uri-help: "Move URI (autogenerated if empty)"
    type-help: "Move type URI."
    is-instant-help: "Indicate if the move is instantaneous or not"
    start-help: "Begin of the move, only if the move is not instantaneous"
    end-help: "End of the move, required if the move is instantaneous"
    target-help: "URI of the moved object (The object must exists)"

fr:
  EventHelpTableView:
    expected-format: "Format attendu"
    text-help-line-1: "Vous pouvez insérer vos données à partir de cette ligne."
    text-help-line-2: "Les deux premières lignes de contenu CSV seront ignorées."
    text-help-line-3: "L'ordre des colonnes doit être respecté."
    text-help-line-4: "Le séparateur CSV est le suivant :"
    text-help-line-5: 'Le séparateur décimal est le suivant : "{decimalSeparator}"'
    text-help-line-6: "Les valeurs vides seront ignorées."
    text-help-line-7: "Tout identifiant de colonne inconnu sera considéré comme une erreur."
    uri-help: "URI de l'évenement (auto-générée si vide)"
    type-help: "URI du type d'événement"
    is-instant-help: "Indique si l'évenement est instantané ou non"
    start-help: "Début de l'événement, uniquement si celui-ci n'est pas instantané"
    end-help: "Fin de l'événement, requis si celui-ci est instantané"
    target-help: "URI de l'objet concerné par l'évènement (Doit exister)."

  MoveHelpTableView:
    uri-help: "URI du déplacement (auto-générée si vide)"
    type-help: "URI du type de déplacement"
    is-instant-help: "Indique si le déplacement est instantané ou non"
    start-help: "Début du déplacement, uniquement si celui-ci n'est pas instantané"
    end-help: "Fin du déplacement, requis si celui-ci est instantané"
    target-help: "URI de l'objet deplacé (L'objet doit exister)"
</i18n>