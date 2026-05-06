<template>
  <div class="row">

    <!-- List and create button -->
    <div class="col-md-5">
      <Card
          noHeader
          noFooter
      >
        <template #body>
          <div class="button-zone">

            <CreateButton
                v-if="user.isAdmin()"
                @click="showCreateForm()"
                :label="t('OntologyClassView.add')"
                class="createButton">
            </CreateButton>

            <ModalForm
                ref="classForm"
                component="opensilex-OntologyClassForm"
                :createTitle="t('OntologyClassView.add')"
                :editTitle="t('OntologyClassView.update')"
                :initForm="initForm"
                @onCreate="refresh()"
                @onUpdate="refresh()"
                successMessage="OntologyClassView.the-type"
                :icon="icon"
                :data="{
                  parentUri: rdfType
                }"
            ></ModalForm>
          </div>

          <StringFilter
              v-model:filter="nameFilter"
              @update="updateFilter()"
              :placeholder="t('OntologyClassView.search')"
              :debounce="300"
              :lazy="false"
          ></StringFilter>

          <OntologyClassTreeView
              ref="classesTree"
              :rdfType="rdfType"
              @selectionChange="selected = $event"
              @editClass="showEditForm($event)"
              @createChildClass="showCreateForm($event)"
              @deleteRDFType="deleteRDFType($event)"
              class="scrollable-container"
          ></OntologyClassTreeView>

        </template>
      </Card>
    </div>

    <!-- Détails of selected element from list  -->
    <div class="col-md-7 ">
      <div>
        <OntologyClassDetail
            :rdfType="rdfType"
            :selected="selected"
            @onDetailChange="refresh()"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, onBeforeUnmount, onMounted, ref, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {VueJsOntologyExtensionService, VueRDFTypeDTO} from "@/lib";
import {useI18n} from "vue-i18n";
import OntologyClassTreeView from "@/components/ontology/class/OntologyClassTreeView.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";
import OntologyClassDetail from "@/components/ontology/class/OntologyClassDetail.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import Card from "@/components/common/views/Card.vue";

//#region Public
const props = defineProps<{
  rdfType: string,
  title: string,
  icon: string
}>();
//#endregion

//#region Private
const opensilex = inject<OpenSilexVuePlugin>("$opensilex")
const store = useStore();
const {t} = useI18n();

const service = ref<VueJsOntologyExtensionService>();
const nameFilter = ref<string>("");
const parentURI = ref<string>("");
const selected = ref<VueRDFTypeDTO | undefined>();

const user = computed(() => store.state.user);

const classForm = useTemplateRef<InstanceType<typeof ModalForm>>("classForm");
const classesTree = useTemplateRef<InstanceType<typeof OntologyClassTreeView>>("classesTree");

const unwatchLang = store.watch(
    () => store.getters.language,
    () => refresh()
);

onMounted(async () => {
  service.value = opensilex.getService("opensilex-front.VueJsOntologyExtensionService");
})

onBeforeUnmount(() => {
  unwatchLang();
})

function initForm(form): any {
  form.parent = parentURI.value;
  return form;
}

function showCreateForm(parentTypeURI?: string) {
  parentURI.value = parentTypeURI;
  classForm.value.showCreateForm();
}

function showEditForm(data) {
  service.value
      .getRDFType(data.uri, props.rdfType)
      .then(http => {
        classForm.value.showEditForm(http.response.result);
      }).catch(opensilex.errorHandler);
}

function deleteRDFType(data) {
  service.value
      .deleteRDFType(data.uri)
      .then(_ => {
        let message = t("OntologyClassView.the-type") + " " + data.name + t("component.common.success.delete-success-message");
        opensilex.showSuccessToast(message);
        selected.value = undefined;
        refresh();
      }).catch(opensilex.errorHandler);
}

function refresh() {
  classesTree.value.refresh(selected.value, nameFilter.value);
}

function updateFilter() {
  refresh();
}
//#endregion


</script>

<style scoped lang="scss">


.header-plus {
  margin-left: 90px;
}

.createButton {
  margin-bottom: -10px;
  margin-top: -10px
}

div.sticky {
  position: sticky;
  top: 0;
}

.scrollable-container {
  width: 100%;
  height: 600px;
  overflow-y: auto; /* Enables vertical scrolling */
}
</style>


<i18n>
en:
  OntologyClassView:
    the-type: The type
    add: Create type
    update: Update type
    search: Search and select a type
fr:
  OntologyClassView:
    the-type: Le type
    add: Créer un type
    update: Mettre à jour le type
    search: Rechercher et sélectionner un type

</i18n>

