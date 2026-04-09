<template>
  <div class="container-fluid">
    <opensilex-PageHeader v-if="isPropertiesType" :icon="icon" :hasIcon="true" :title="propertiesTitle"
                          :description="propertiesDescription" class="detail-element-header"></opensilex-PageHeader>

    <opensilex-PageActions :returnButton="false" :tabs="true">
      <template v-slot>
        <nav class="tabs mb-3">
          <router-link
              :to="{path: typeURI}"
              :class="['tab', { active: isTypeTab }]"
          >
            {{ t("OntologyTypesView.typeTitle") }}
          </router-link>
          <router-link
              :to="{path: propertiesURI}"
              :class="['tab', { active: isPropertiesType }]"
              class="tab"
          >
            {{ t("OntologyTypesView.propertiesTitle") }}
          </router-link>
        </nav>
        <!--        <b-nav-item-->
        <!--          :active="isTypeTab"-->
        <!--          :to="{path: typeURI}"-->
        <!--          >{{ t("OntologyTypesView.typeTitle") }}-->
        <!--        </b-nav-item>-->

        <!--        <b-nav-item -->
        <!--          v-if="withProperties"-->
        <!--          :active="isPropertiesType"-->
        <!--          :to="{path: propertiesURI}"-->
        <!--          >{{ t("OntologyTypesView.propertiesTitle") }}-->
        <!--        </b-nav-item>-->
      </template>
    </opensilex-PageActions>

        <opensilex-PageContent>
          <template v-slot>
            <opensilex-OntologyClassView
              v-if="isTypeTab"
              :rdfType="rdfType"
              :icon="icon"
              :title="typeTitle"
            ></opensilex-OntologyClassView>
<!--            <opensilex-OntologyPropertyView-->
<!--              v-else-if="withProperties"-->
<!--              :rdfType="rdfType"-->
<!--              :icon="icon"-->
<!--              :title="propertiesTitle"-->
<!--            />-->
          </template>
        </opensilex-PageContent>
  </div>
</template>

<script setup lang="ts">
import {computed} from "vue";
import {useRoute} from "vue-router";
import {useI18n} from "vue-i18n";

const props = withDefaults(defineProps<{
  rdfType: string,
  icon: string,
  typeTitle: string,
  typeDescription: string,
  typeURI: string,
  propertiesTitle: string,
  propertiesDescription: string,
  propertiesURI: string,
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
