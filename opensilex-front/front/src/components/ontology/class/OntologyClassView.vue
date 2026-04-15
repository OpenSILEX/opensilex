<template>
  <div class="row">

    <!-- List and create button -->
    <div class="col-md-6">
      <opensilex-Card
          noHeader
          noFooter
      >
        <template #body>
          <div class="button-zone">

            <opensilex-CreateButton
                v-if="user.isAdmin()"
                @click="showCreateForm()"
                :label="t('OntologyClassView.add')"
                class="createButton">
            </opensilex-CreateButton>

            <opensilex-ModalForm
                ref="classForm"
                component="opensilex-OntologyClassForm"
                :createTitle="t('OntologyClassView.add')"
                :editTitle="t('OntologyClassView.update')"
                :initForm="initForm"
                @onCreate="refresh()"
                @onUpdate="refresh()"
                modalSize="lg"
                successMessage="OntologyClassView.the-type"
                :icon="icon"
                :data="{
                  parentUri: rdfType
                }"
            ></opensilex-ModalForm>
          </div>

          <opensilex-StringFilter
              v-model:filter="nameFilter"
              @update="updateFilter()"
              :placeholder="t('OntologyClassView.search')"
              :debounce="300"
              :lazy="false"
          ></opensilex-StringFilter>

          <opensilex-OntologyClassTreeView
              ref="classesTree"
              :rdfType="rdfType"
              @selectionChange="selected = $event"
              @editClass="showEditForm($event)"
              @createChildClass="showCreateForm($event)"
              @deleteRDFType="deleteRDFType($event)"
              class="scrollable-container"
          ></opensilex-OntologyClassTreeView>

        </template>
      </opensilex-Card>
    </div>

    <!-- Détails of selected element from list  -->
    <div class="col-md-6 ">
      <div>
        <opensilex-OntologyClassDetail
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
import {VueJsOntologyExtensionService} from "../../../lib";
import {useI18n} from "vue-i18n";
import OntologyClassTreeView from "@/components/ontology/class/OntologyClassTreeView.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex")
const store = useStore();
const {t} = useI18n();

const user = computed(() => store.state.user);

const service = ref<VueJsOntologyExtensionService>();
const nameFilter = ref("");
const parentURI = ref("");
const selected = ref();

const classForm = useTemplateRef<InstanceType<typeof ModalForm>>("classForm");
const classesTree = useTemplateRef<InstanceType<typeof OntologyClassTreeView>>("classesTree");

const props = defineProps<{
  rdfType: string,
  title: string,
  icon: string
}>();

onMounted(async () => {
  service.value = opensilex.getService("opensilex-front.VueJsOntologyExtensionService");
})

const unwatchLang = store.watch(
    () => store.getters.language,
    () => refresh()
);

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
        let form = http.response.result;
        classForm.value.showEditForm(form);
      }).catch(opensilex.errorHandler);
}

function deleteRDFType(data) {
  service.value
      .deleteRDFType(data.uri)
      .then(http => {
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
  position: -webkit-sticky; /* Safari */
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

