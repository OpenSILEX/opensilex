<template>
  <Modal ref="modal">
    <template #header>
      <i>
        <h4>
          <Icon icon="fa#list" />
          {{ t("component.data.export.title") }}
        </h4>
      </i>
    </template>

    <FormSelector
      :label="t('component.data.export.select-format')"
      v-model:selected="format"
      :required="true"
      :multiple="false"
      :clearable="false"
      :helpMessage="t('component.data.export.select-format-help')"
      :options="options"
    ></FormSelector>
    <FormField
      :label="t('component.data.export.check-raw-data')"
      :helpMessage="t('component.data.export.raw-data-help')"
    >
      <template #field>
        <div class="d-flex align-items-center gap-2">
          <n-switch v-model:value="withRawData" />
          {{ t("component.data.export.raw-data") }}
        </div>
      </template>
    </FormField>

    <template #footer>
      <Button
        label="component.common.close"
        class="btn-secondary"
        @click="hide()"
      />

      <Button
        label="component.data.export.export"
        class="greenThemeColor"
        @click="exportData()"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {computed, inject, ref, useTemplateRef} from "vue";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {NSwitch} from "naive-ui";
import type {DataSearchDTO} from "opensilex-core/model/dataSearchDTO";
import type OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import FormField from "@/components/common/forms/FormField.vue";
import Icon from "@/components/common/views/Icon.vue";
import Modal from "@/components/common/views/Modal.vue";
import Button from "@/components/common/buttons/Button.vue";

const props = defineProps<{
  filter: any
}>();

const {t} = useI18n();
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const store = useStore();
const lang = computed(() => store.getters.language);

const longFormat = "long";
const wideFormat = "wide";

const modalRef = useTemplateRef<InstanceType<typeof Modal>>("modal");
const withRawData = ref(false);
const format = ref(wideFormat);

const options = computed(() => [
  {id: longFormat, label: t("component.data.export.export-long")},
  {id: wideFormat, label: t("component.data.export.export-wide")}
]);

function show() {
  modalRef.value.show();
}

function hide() {
  modalRef.value.hide();
}

function exportData() {
  let path = "/core/data/export";
  let today = new Date();
  let filename =
    "export_data_" +
    today.getFullYear() +
    String(today.getMonth() + 1).padStart(2, "0") +
    String(today.getDate()).padStart(2, "0");

  let exportDto: DataSearchDTO = {
    start_date: props.filter.start_date,
    end_date: props.filter.end_date,
    targets: props.filter.scientificObjects,
    devices: props.filter.devices,
    experiments: props.filter.experiments,
    variables: props.filter.variables,
    provenances: props.filter.provenance ? [props.filter.provenance] : null,
    mode: format.value,
    with_raw_data: withRawData.value
  };

  hide();
  $opensilex.downloadFilefromPostService(path, filename, "csv", exportDto, lang.value);
}

defineExpose({
  show,
  hide
});
</script>

<style scoped>
.validation-confirm-container {
  color: rgb(40, 167, 69);
  font-weight: bold;
}
</style>
