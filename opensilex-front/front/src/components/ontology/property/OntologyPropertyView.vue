<template>
  <div class="row">
    <div class="col-md-5">
      <opensilex-Card
        noHeader
        noFooter
      >
        <template #body>
          <div class="button-zone">

            <opensilex-CreateButton
                v-if="user.isAdmin()"
                @click="showCreateForm()"
                :label="t('OntologyPropertyView.add')"
                class="createButton"
            ></opensilex-CreateButton>

            <opensilex-ModalForm
                ref="propertyForm"
                component="opensilex-OntologyPropertyForm"
                :createTitle="t('OntologyPropertyView.add')"
                :editTitle="t('OntologyPropertyView.update')"
                :initForm="initForm"
                @onCreate="refresh()"
                @onUpdate="refresh()"
                :successMessage="t('OntologyPropertyView.the-property')"
                :icon="icon"
                :data="{
                  domain: rdfType,
                  parentUri: parentURI
                }"
            ></opensilex-ModalForm>
          </div>

          <opensilex-StringFilter
              v-model:filter="nameFilter"
              @update="updateFilter()"
              :placeholder="t('OntologyPropertyView.search')"
              :debounce="300"
              :lazy="false"
          ></opensilex-StringFilter>

          <opensilex-OntologyPropertyTreeView
              ref="propertiesTree"
              :domain="rdfType"
              @selectionChange="onSelectionChanged"
              @editProperty="showEditForm($event)"
              @createChildProperty="showCreateForm($event)"
              @deleteProperty="deleteProperty($event)"
              class="scrollable-container"
          ></opensilex-OntologyPropertyTreeView>
        </template>
      </opensilex-Card>
    </div>
    <div class="col-md-7">
      <opensilex-OntologyPropertyDetail :selected="selected"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, ref, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {RDFPropertyGetDTO} from "opensilex-core/model/rDFPropertyGetDTO";
import {OntologyService} from "opensilex-core/api/ontology.service";
import OWL from "@/ontologies/OWL";
import {RDFPropertyDTO} from "opensilex-core/model/rDFPropertyDTO";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import {useI18n} from "vue-i18n";
import OntologyPropertyTreeView from "@/components/ontology/property/OntologyPropertyTreeView.vue";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const ontologyService = opensilex.getService<OntologyService>("opensilex-core.OntologyService")
const store = useStore();
const user = computed(() => store.state.user);
const {t} = useI18n();

const nameFilter = ref("");
const selected = ref<RDFPropertyGetDTO>();
const parentURI = ref("");

const propertyForm = useTemplateRef("propertyForm");
const propertiesTree = useTemplateRef<InstanceType<typeof OntologyPropertyTreeView>>("propertiesTree");

const props = defineProps<{
  rdfType: string
  title: string
  icon: string
  description: string
}>()

function initForm(form: RDFPropertyDTO): RDFPropertyDTO {
  console.log("Init form !!", parentURI.value)
  form.parent = parentURI.value;
  if (OWL.hasParent(form.parent)) {
    form.rdf_type = null;
  } else if (OWL.isDatatypeProperty(form.rdf_type)) {
    form.rdf_type = OWL.DATATYPE_PROPERTY_URI;
  } else if (OWL.isObjectTypeProperty(form.rdf_type)) {
    form.rdf_type = OWL.OBJECT_PROPERTY_URI;
  }
  form.domain = props.rdfType;
  return form;
}

function onSelectionChanged(selection: RDFPropertyDTO) {
  selected.value = selection;
}

function showCreateForm(parentTypeURI?) {
  console.log("Show create form", parentTypeURI);
  parentURI.value = parentTypeURI;
  propertyForm.value.showCreateForm();
}

function showEditForm(data) {
  ontologyService.getProperty(data.uri, data.rdf_type, this.rdfType).then(http => {
    let propertyFormComponent = propertyForm.value.getFormRef();
    propertyFormComponent.setParentPropertiesTree(
        propertiesTree.value.getTree()
    );
    let form = http.response.result;
    if (OWL.hasParent(form.parent)) {
      form.rdf_type = null;
    } else if (OWL.isDatatypeProperty(form.rdf_type)) {
      form.rdf_type = OWL.DATATYPE_PROPERTY_URI;
      form.range = opensilex.getDatatype(form.range).uri;
    } else if (OWL.isObjectTypeProperty(form.rdf_type)) {
      form.rdf_type = OWL.OBJECT_PROPERTY_URI;
      form.range = opensilex.getObjectType(form.range).uri;
    }
    propertyForm.value.showEditForm(form);
  });
}

function deleteProperty(propertyDto: ResourceTreeDTO) {
  ontologyService.deleteProperty(propertyDto.uri, propertyDto.rdf_type).then(() => {
    let message = t("OntologyPropertyView.the-property") + " " + propertyDto.uri + t("component.common.success.delete-success-message");
    opensilex.showSuccessToast(message);
    refresh();
  }).catch(opensilex.errorHandler);
}

function refresh() {
  propertiesTree.value.refresh(nameFilter.value);
}

function updateFilter() {
  refresh();
}
</script>

<style scoped lang="scss">
.createButton {
  margin-bottom: -10px;
  margin-top: -10px
}

.scrollable-container {
  width: 100%;
  height: 600px;
  overflow-y: auto; /* Enables vertical scrolling */
}
</style>


<i18n>
en:
  OntologyPropertyView:
    add: Create property
    update: Update property
    the-property: The property
    search: Search and select a property
fr:
  OntologyPropertyView:
    add: Créer une propriété
    update: Mettre à jour la propriété
    the-property: La propriété
    search: Rechercher et sélectioner une propriété
</i18n>
