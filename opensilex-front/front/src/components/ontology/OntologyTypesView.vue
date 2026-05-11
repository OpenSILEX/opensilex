<template>
  <div class="container-fluid">
    <PageHeader v-if="isPropertiesType" :icon="icon" :hasIcon="true" :title="propertiesTitle"
                          :description="propertiesDescription" class="detail-element-header"></PageHeader>

    <PageActions :returnButton="false">
      <template v-slot>
        <nav class="tabs mb-3">
          <router-link
              :to="{path: typeURI}"
              :class="['tab', { active: isTypeTab }]"
          >
            {{ t("OntologyTypesView.typeTitle") }}
          </router-link>
          <router-link
              v-if="withProperties"
              :to="{path: propertiesURI}"
              :class="['tab', { active: isPropertiesType }]"
              class="tab"
          >
            {{ t("OntologyTypesView.propertiesTitle") }}
          </router-link>
        </nav>
      </template>
    </PageActions>

    <PageContent>
      <template v-slot>
        <OntologyClassView
            v-if="isTypeTab"
            :rdfType="rdfType"
            :icon="icon"
            :title="typeTitle"
        ></OntologyClassView>
        <OntologyPropertyView
            v-else-if="withProperties"
            :rdfType="rdfType"
            :icon="icon"
            :title="propertiesTitle"
        />
      </template>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import {computed} from "vue";
import {useRoute} from "vue-router";
import {useI18n} from "vue-i18n";
import PageHeader from "@/components/layout/PageHeader.vue";
import PageActions from "@/components/layout/PageActions.vue";
import PageContent from "@/components/layout/PageContent.vue";
import OntologyClassView from "@/components/ontology/class/OntologyClassView.vue";
import OntologyPropertyView from "@/components/ontology/property/OntologyPropertyView.vue";

const props = withDefaults(defineProps<{
  rdfType: string,
  icon: string,
  typeTitle: string,
  typeDescription: string,
  typeURI: string,
  propertiesTitle?: string,
  propertiesDescription?: string,
  propertiesURI?: string,
  withProperties?: boolean,
}>(), {
  withProperties: true
});

const route = useRoute();
const {t} = useI18n();

const isPropertiesType = computed(() => route.path.startsWith(props.propertiesURI));
const isTypeTab = computed(() => route.path.startsWith(props.typeURI) && !isPropertiesType.value);
</script>

<style scoped lang="scss">
.tab {
  text-decoration-line: none;
}

.active {
  color: #007bff;
}
</style>


<i18n>
en:
  OntologyTypesView:
    typeTitle: Types
    propertiesTitle: Properties

fr:
  OntologyTypesView:
    typeTitle: Types
    propertiesTitle: Propriétés
</i18n>
