<template>
  <Card v-if="selected">
    <template #header>
      <h3>{{ t("component.ontology.class.detail.title") }}</h3>
    </template>
    <template #body>
      <div>
        <!-- URI -->
        <UriView :uri="selected.uri"></UriView>
        <!-- Name -->
        <StringView
            label="component.common.name"
            :value="selected.name"
        ></StringView>
        <!-- Description -->
        <TextView
            label="component.common.comment"
            :value="selected.comment"
        ></TextView>
        <!-- Icon identifier -->
        <IconView
            :label="t('component.ontology.class.detail.icon')"
            :value="selected.icon"
        ></IconView>
        <MetadataView
            v-if="selected.publisher && selected.publisher.uri"
            :publisher="selected.publisher"
            :publicationDate="selected.publicationDate"
            :lastUpdatedDate="selected.lastUpdatedDate"
        >
        </MetadataView>
      </div>
      <hr>
      <div>
        <div class="static-field row">
          <div class="col-lg-8">
            <span class="field-view-title" style="float: none">
              {{ t("component.ontology.class.detail.properties") }}
              <n-tooltip
                  trigger="hover"
              >
                <template #trigger>
                  <font-awesome-icon
                      icon="question-circle"
                  />
                </template>
                {{ t("component.ontology.class.detail.properties-help") }}
              </n-tooltip>
            </span>
          </div>

          <div class="col-lg-8">
            <Button
                v-if="user.isAdmin()"
                @click="showClassPropertyForm"
                class="greenThemeColor addPropertyButton"
                icon="ik#ik-plus"
                :small="false"
                :label="t('component.ontology.class.detail.addProperty')"
                helpMessage="component.ontology.class.detail.add-property-help"
            ></Button>
            &nbsp;
            <Button
                v-if="user.isAdmin()"
                @click="startSetPropertiesOrder"
                class="greenThemeColor"
                icon="fa#pencil-alt"
                :small="false"
                :label="t('component.ontology.class.detail.setPropertiesOrder')"
            ></Button>
          </div>
        </div>

        <!-- Add and set order buttons -->
        <div>
          <Modal
              ref="setPropertiesOrderRef"
          >
            <template #header>
              <h4>
                {{ t("component.ontology.class.detail.setPropertiesOrder") }}
              </h4>
            </template>
            <p>{{ t("component.ontology.class.detail.setPropertiesOrderInfo") }}:</p>
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
          </Modal>
        </div>

        <div>
          <n-config-provider>
            <n-data-table
                size="small"
                :data="properties"
                :columns="fields"
            >
            </n-data-table>
          </n-config-provider>
        </div>

        <OntologyClassPropertyForm
            ref="classPropertyForm"
            :createTitle="t('component.ontology.class.detail.addProperty')"
            :editTitle="t('component.ontology.class.detail.updateProperty')"
            :domain="rdfType"
            :classUri="selected.uri"
            @onCreate="emit('onDetailChange')"
            @onUpdate="emit('onDetailChange')"
        ></OntologyClassPropertyForm>
      </div>

    </template>
  </Card>
</template>

<script setup lang="ts">
import {computed, h, inject, ref, useTemplateRef, VNodeChild} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {VueJsOntologyExtensionService, VueRDFTypeDTO, VueRDFTypePropertyDTO} from "@/lib";
import {useI18n} from "vue-i18n";
import {DataTableColumns, NList, NListItem, NTooltip, NConfigProvider, NButtonGroup} from "naive-ui";
import UriLink from "@/components/common/views/UriLink.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";
import Modal from "@/components/common/views/Modal.vue";
import {VueDraggable} from "vue-draggable-plus";
import IconView from "@/components/common/views/IconView.vue";
import UriView from "@/components/common/views/UriView.vue";
import TextView from "@/components/common/views/TextView.vue";
import Card from "@/components/common/views/Card.vue";
import StringView from "@/components/common/views/StringView.vue";
import MetadataView from "@/components/common/views/MetadataView.vue";
import Button from "@/components/common/buttons/Button.vue";
import OntologyClassPropertyForm from "@/components/ontology/class/OntologyClassPropertyForm.vue";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const ontologyService = opensilex.getService<OntologyService>("opensilex-core.OntologyService");
const vueOntologyService = opensilex.getService<VueJsOntologyExtensionService>("opensilex-front.VueJsOntologyExtensionService");
const store = useStore();
const {t} = useI18n();

const user = computed(() => store.state.user);

const props = defineProps<{
  selected?: VueRDFTypeDTO,
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
    // IDE finds a type error here. This is because of vue version mismatch between the global node_modules and the
    // one in opensilex-front. I don't know how to solve it though.
    render: (data: VueRDFTypePropertyDTO, _: number) => h(UriLink, {uri: data.uri, value: data.uri})
  },
  {
    key: "is_required",
    title: t("component.ontology.class.detail.required"),
    render: (data: VueRDFTypePropertyDTO) => renderBool(data.is_required)
  },
  {
    key: "is_list",
    title: t("component.ontology.class.detail.list"),
    render: (data: VueRDFTypePropertyDTO) => renderBool(data.is_list)
  },
  {
    key: "inherited",
    title: t("component.ontology.class.detail.inherited"),
    render: (data: VueRDFTypePropertyDTO) => renderBool(data.inherited)
  },
  {
    title: t("component.common.actions"),
    key: "actions",
    render: (data: VueRDFTypePropertyDTO) =>
        h(NButtonGroup, {
              size: "small",
              className: "btn-group btn-group-sm"
            },
            h(DeleteButton, {
              onClick: () => deleteClassPropertyRestriction(data.uri),
              label: t('component.ontology.class.detail.deleteProperty'),
              small: true
            }))
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
        let message = propertyURI + " : " + t("component.ontology.class.detail.property-link-delete");
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

