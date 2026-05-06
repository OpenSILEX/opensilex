<template>
  <opensilex-Card v-if="selected">
    <template #header>
      <h3>{{ t("OntologyClassDetail.title") }}</h3>
    </template>
    <template #body>
      <div>
        <!-- URI -->
        <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
        <!-- Name -->
        <opensilex-StringView
            label="component.common.name"
            :value="selected.name"
        ></opensilex-StringView>
        <!-- Description -->
        <opensilex-TextView
            label="component.common.comment"
            :value="selected.comment"
        ></opensilex-TextView>
        <!-- Abstract type -->
        <!-- <opensilex-BooleanView label="OntologyClassForm.abstract-type" :value="selected.is_abstract"></opensilex-BooleanView> -->
        <!-- Icon identifier -->
        <IconView
            label="OntologyClassForm.icon"
            :value="selected.icon"
        ></IconView>
        <opensilex-MetadataView
            v-if="selected.publisher && selected.publisher.uri"
            :publisher="selected.publisher"
            :publicationDate="selected.publicationDate"
            :lastUpdatedDate="selected.lastUpdatedDate"
        >
        </opensilex-MetadataView>
      </div>
      <hr>
      <div>
        <div class="static-field row">
          <div class="col-lg-8">
            <span class="field-view-title" style="float: none">
              {{ t("OntologyClassDetail.properties") }}
              <n-tooltip
                  trigger="hover"
              >
                <template #trigger>
                  <font-awesome-icon
                      icon="question-circle"
                  />
                </template>
                {{ t("OntologyClassDetail.properties-help") }}
              </n-tooltip>
            </span>
          </div>

          <div class="col-lg-8">
            <opensilex-Button
                v-if="user.isAdmin()"
                @click="showClassPropertyForm"
                class="greenThemeColor addPropertyButton"
                icon="ik#ik-plus"
                :small="false"
                :label="t('OntologyClassDetail.addProperty')"
                helpMessage="OntologyClassDetail.add-property-help"
            ></opensilex-Button>
            &nbsp;
            <opensilex-Button
                v-if="user.isAdmin()"
                @click="startSetPropertiesOrder"
                class="greenThemeColor"
                icon="fa#pencil-alt"
                :small="false"
                :label="t('OntologyClassDetail.setPropertiesOrder')"
            ></opensilex-Button>
          </div>
        </div>

        <!-- Add and set order buttons -->
        <div>
          <opensilex-Modal
              ref="setPropertiesOrderRef"
          >
            <template #header>
              <h4>
                {{ t("OntologyClassDetail.setPropertiesOrder") }}
              </h4>
            </template>
            <p>{{ t("OntologyClassDetail.setPropertiesOrderInfo") }}:</p>
            <n-list bordered>
              <VueDraggable v-model="customPropertyOrder">
                <n-list-item
                    v-for="element in customPropertyOrder"
                    :key="element.uri"
                    style="cursor: grab"
                >{{ element.name }}
                </n-list-item>
              </VueDraggable>
            </n-list>
            <template #footer>
              <button type="button" class="btn btn-secondary" @click="setPropertiesOrderRef.hide()">
                {{ t('component.common.close') }}
              </button>

              <button type="button" class="btn greenThemeColor" @click="setPropertiesOrder()">
                {{ t("component.common.validateSelection") }}
              </button>
            </template>
          </opensilex-Modal>
        </div>

        <div>
          <n-data-table
              size="small"
              :data="properties"
              :columns="fields"
          >
          </n-data-table>
        </div>

        <opensilex-ModalForm
            ref="classPropertyForm"
            component="opensilex-OntologyClassPropertyForm"
            :createTitle="t('OntologyClassDetail.addProperty')"
            :editTitle="t('OntologyClassDetail.updateProperty')"
            @onCreate="$emit('onDetailChange')"
            @onUpdate="$emit('onDetailChange')"
            :successMessage="t('OntologyClassView.the-type')"
            :data="{
              domain: rdfType,
              classUri: selected.uri
            }"
        ></opensilex-ModalForm>
      </div>

    </template>
  </opensilex-Card>
</template>

<script setup lang="ts">
import {computed, h, inject, ref, useTemplateRef, VNodeChild} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {VueJsOntologyExtensionService, VueRDFTypePropertyDTO} from "@/lib";
import {useI18n} from "vue-i18n";
import {DataTableColumns, NList, NListItem, NTooltip} from "naive-ui";
import UriLink from "@/components/common/views/UriLink.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";
import Modal from "@/components/common/views/Modal.vue";
import {VueDraggable} from "vue-draggable-plus";
import IconView from "@/components/common/views/IconView.vue";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const ontologyService = opensilex.getService<OntologyService>("opensilex-core.OntologyService");
const vueOntologyService = opensilex.getService<VueJsOntologyExtensionService>("opensilex-front.VueJsOntologyExtensionService");
const store = useStore();
const {t} = useI18n();

const user = computed(() => store.state.user);

const props = defineProps<{
  selected: any,
  rdfType: string,
}>();

const emit = defineEmits<{
  onDetailChange: []
}>()

const classPropertyForm = useTemplateRef<InstanceType<typeof ModalForm>>('classPropertyForm');
const setPropertiesOrderRef = useTemplateRef<InstanceType<typeof Modal>>('setPropertiesOrderRef');

const fields: DataTableColumns<VueRDFTypePropertyDTO> = [
  {
    key: "name",
    title: t("component.common.name"),
  },
  {
    key: "uri",
    title: t("component.common.uri"),
    render: (data: VueRDFTypePropertyDTO) => h(UriLink, {uri: data.uri, value: data.uri})
  },
  {
    key: "is_required",
    title: t("OntologyClassDetail.required"),
    render: (data: VueRDFTypePropertyDTO) => renderBool(data.is_required)
  },
  {
    key: "is_list",
    title: t("OntologyClassDetail.list"),
    render: (data: VueRDFTypePropertyDTO) => renderBool(data.is_list)
  },
  {
    key: "inherited",
    title: t("OntologyClassDetail.inherited"),
    render: (data: VueRDFTypePropertyDTO) => renderBool(data.inherited)
  },
  {
    title: t("component.common.actions"),
    key: "actions",
    render: (data: VueRDFTypePropertyDTO) => h(DeleteButton, {
      onClick: () => deleteClassPropertyRestriction(data.uri),
      label: t('OntologyClassDetail.deleteProperty'),
      small: true
    })
  },
];
const customPropertyOrder = ref<Array<VueRDFTypePropertyDTO>>([]);

const properties = computed<VueRDFTypePropertyDTO[]>(() => {
  let allProps: VueRDFTypePropertyDTO[] = props.selected.data_properties.concat(
      props.selected.object_properties
  );
  let pOrder = props.selected.properties_order;

  allProps.sort((a, b) => {
    if (a.uri == b.uri) {
      return 0;
    }

    if (a.uri == "rdfs:label") {
      return -1;
    }

    if (b.uri == "rdfs:label") {
      return 1;
    }

    let aIndex = pOrder.indexOf(a.uri);
    let bIndex = pOrder.indexOf(b.uri);
    if (aIndex == -1) {
      if (bIndex == -1) {
        return a.uri.localeCompare(b.uri);
      } else {
        return -1;
      }
    } else {
      if (bIndex == -1) {
        return 1;
      } else {
        return aIndex - bIndex;
      }
    }
  });

  return allProps;
});

function renderBool(value: boolean): VNodeChild {
  return h('span', value ? t("component.common.yes") : t("component.common.no"));
}

function showClassPropertyForm() {
  classPropertyForm.value.showCreateForm();
}

function deleteClassPropertyRestriction(propertyURI) {
  ontologyService.deleteClassPropertyRestriction(props.selected.uri, propertyURI)
      .then(() => {
        let message = propertyURI + " : " + t("OntologyClassDetail.property-link-delete");
        opensilex.showSuccessToast(message);
        emit("onDetailChange");
      })
      .catch(opensilex.errorHandler);
}


function setPropertiesOrder() {
  let propertiesOrder = ["rdfs:label"];
  for (let p of customPropertyOrder.value) {
    propertiesOrder.push(p.uri);
  }

  vueOntologyService.setRDFTypePropertiesOrder(props.selected.uri, propertiesOrder)
      .then(() => {
        setPropertiesOrderRef.value.hide();
        emit("onDetailChange");
      });
}


function startSetPropertiesOrder() {
  customPropertyOrder.value = [];
  for (let p of properties.value) {
    if (p.uri != "rdfs:label") {
      customPropertyOrder.value.push(p);
    }
  }
  setPropertiesOrderRef.value.show();
}
</script>

<style scoped lang="scss">
.align-right {
  float: right;
}

::v-deep td > span {
  white-space: nowrap;
}

@media (min-width: 769px) and (max-width: 1540px) {
  .addPropertyButton {
    margin-bottom: 5px;
  }
}

.greenThemeColor {
  color: #f1f1f1
}
</style>


<i18n>
en:
  OntologyClassDetail:
    title: Object type detail
    required: Required
    list: List of values
    inherited: Inherited
    properties: Properties
    setPropertiesOrder: Set properties order
    objectProperties: Object properties
    addProperty: Add property to type
    add-property-help: Link existing property to the type
    deleteProperty: Delete property
    setPropertiesOrderInfo: You can define properties display order by drag & drop them in the list below
    properties-help: "List of all properties which can apply on the selected type. Including inherited properties and properties which are not specific to the type (ex: name or description)"
    property-link-delete: "The property has been deleted from type"
fr:
  OntologyClassDetail:
    title: Détail du type d'objet
    required: Obligatoire
    list: Liste de valeurs
    inherited: Héritée
    properties: Propriétés
    setPropertiesOrder: Définir l'ordre des propriétés
    objectProperties: Relations vers des objets
    addProperty: Ajouter une propriété au type
    add-property-help: Ajouter une propriété existante pour ce type
    deleteProperty: Supprimer la propriété
    property-link-delete: "La propriété a été supprimée du type"
    setPropertiesOrderInfo: Vous pouvez définir l'ordre d'affichage des propriétés par glisser-déposer dans la liste ci-dessous
    properties-help: "Liste de toutes les propriétés qui peuvent s'appliquer au type selectionné. Y compris les propriétés héritées et les propriétés qui ne sont pas spécifiques au type (ex: nom ou description)"
</i18n>

