<template>
  <div>
    <p @click="visible = !visible" style="cursor: pointer">
      <strong>{{ t("component.data.dataHelp.exceptedFormat") }} </strong>
      <Icon v-if="!visible" icon="fa#eye" class="DataHelpTableViewHelpEyeIcon" />
      <Icon v-if="visible" icon="fa#eye-slash" class="DataHelpTableViewHelpEyeIcon" />
    </p>
    <div v-show="visible" class="mt-2 table-responsive">
      <table class="table">
        <thead>
          <tr>
            <th>1</th>
            <th v-if="experiment == null">experiment</th>
            <th v-if="experiment != null">scientific_object<span class="required"> *</span></th>
            <th v-else>target</th>
            <th v-if="experiment == null">device</th>
            <th>date <span class="required"> *</span></th>
            <th>uri:variable1<span class="required"> *</span></th>
            <th>uri:variable...</th>
            <th>Annotation</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <th>2</th>
            <td v-if="experiment == null">{{ t("component.data.dataHelp.experiment-help") }}</td>
            <td v-if="experiment != null">{{ t("component.data.dataHelp.objectId-help") }}</td>
            <td v-else>{{ t("component.data.dataHelp.targetId-help") }}</td>
            <td v-if="experiment == null">{{ t("component.data.dataHelp.device-help") }}</td>
            <td>{{ t("component.data.dataHelp.date-help") }}</td>
            <td>{{ t("component.data.dataHelp.variable-help") }}</td>
            <td>{{ t("component.data.dataHelp.variables-help") }}</td>
            <td>{{ t("component.data.dataTemplate.annotationHelp") }}</td>
          </tr>
          <tr>
            <th>3</th>
            <td v-if="experiment == null"
              >{{ t("component.data.dataHelp.column-type-help")
              }}<strong>{{ getDataTypeLabel("xsd:string") }}</strong></td
            >
            <td v-if="experiment == null"
              >{{ t("component.data.dataHelp.column-type-help")
              }}<strong>{{ getDataTypeLabel("xsd:string") }}</strong>
            </td>
            <td v-else
              >{{ t("component.data.dataHelp.column-type-help")
              }}<strong>{{ getDataTypeLabel("xsd:string") }}<br />{{
                  t("component.data.dataHelp.required")
                }}</strong>
            </td>
            <td v-if="experiment == null"
              >{{ t("component.data.dataHelp.column-type-help")
              }}<strong>{{ getDataTypeLabel("xsd:string") }}</strong>
            </td>
            <td>
              {{ t("component.data.dataHelp.column-type-help") }}
              <strong>
                {{ getDataTypeLabel("xsd:date") }}<br />{{
                  t("component.data.dataHelp.required")
                }}</strong
              ></td
            >
            <td
              >{{ t("component.data.dataHelp.column-type-help") }}
              <strong>{{ variableTypesLabel }}</strong></td
            >
            <td
              >{{ t("component.data.dataHelp.column-type-help") }}
              <strong>{{ variableTypesLabel }}</strong></td
            >
            <td>{{ t("component.data.dataHelp.column-type-help") }}
              <strong>{{ "String" }}</strong>
            </td>
          </tr>
          <tr>
            <td colspan="9" class="help-row-cell">
              <div class="divHelpMsg">
                <strong class="help-row-number">4</strong>
                <div class="help-text">
                  <div>{{ t("component.data.dataHelp.help-text.insert-from-row") }}</div>
                  <div>{{ t("component.data.dataHelp.help-text.ignored-rows") }}</div>
                  <template v-if="experiment != null">
                    <div>
                      <strong>{{ t("component.data.dataHelp.help-text.column-order") }}</strong>
                      {{ t("component.data.dataHelp.help-text.column-add") }}
                    </div>
                  </template>
                  <template v-else>
                    <div><strong>{{ t("component.data.dataHelp.help-text-global.optional-columns") }}</strong></div>
                    <div>{{ t("component.data.dataHelp.help-text-global.target") }}</div>
                    <div>{{ t("component.data.dataHelp.help-text-global.device") }}</div>
                    <div>{{ t("component.data.dataHelp.help-text-global.duplicate-columns") }}</div>
                    <div>{{ t("component.data.dataHelp.help-text-global.raw-data") }}</div>
                  </template>
                  <div>
                    {{ t("component.data.dataHelp.help-text.accepted-separators") }}
                    <strong>{{ t("component.common.csv-delimiters.comma") }} {{ t("component.data.dataHelp.help-text.or") }} {{ t("component.common.csv-delimiters.semicolon") }}</strong>
                  </div>
                  <div>
                    {{ t("component.data.dataHelp.help-text.decimal-separator") }} <strong>"."</strong>
                  </div>
                  <div><strong>{{ t("component.data.dataHelp.help-text.timezone") }}</strong></div>
                  <div><strong>{{ t("component.data.dataHelp.help-text.blank-values") }}</strong></div>
                  <div><strong>{{ t("component.data.dataHelp.help-text.special-values") }}</strong></div>
                  <div>{{ t("component.data.dataHelp.help-text.annotation") }}</div>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, ref} from "vue";
import {useI18n} from "vue-i18n";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import Icon from "@/components/common/views/Icon.vue";

const props = defineProps<{
  experiment?: string
}>();

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const {t} = useI18n();

const visible = ref<boolean>(true);

const variableTypesLabel = computed(() => {
  return getDataTypeLabel("xsd:string") +
    ", " +
    getDataTypeLabel("xsd:integer") +
    ", " +
    getDataTypeLabel("xsd:boolean") +
    ", " +
    getDataTypeLabel("xsd:date");
});

function getDataTypeLabel(dataTypeUri: string): string {
  if (!dataTypeUri) {
    return undefined;
  }
  let datatype = opensilex.getDatatype(dataTypeUri);
  if (!datatype) {
    return dataTypeUri;
  }
  let label = t(datatype.label_key);
  return label.charAt(0).toUpperCase() + label.slice(1);
}
</script>

<style scoped lang="scss">
.help-row-cell {
  padding-left: 0;
  border-bottom: none;

  .divHelpMsg {
    padding-left: calc(0.5rem - 1px);
  }
}

.help-row-number {
  margin-right: 1rem;
}

.help-text {
  display: flex;
  flex-direction: column;
}
</style>
