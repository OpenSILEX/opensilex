<template>
  <div class="container-fluid">
    <PageContent>
        <TableView
            :items="items"
            :fields="relationsFields"
            :globalFilterField="true"
            :filterPlaceholder="t('Namespaces.placeholder')"
            sortBy="prefix">
          <template #cell(prefix)="{ data }">{{ data.item.prefix }}</template>

          <template #cell(namespaces)="{ data }">
            <UriLink
                :uri="data.item.namespaces"
                :value="data.item.namespaces"
            ></UriLink>
          </template>
        </TableView>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted, inject} from 'vue';
import PageContent from "@/components/layout/PageContent.vue";
import TableView from "@/components/common/views/TableView.vue";
import UriLink from "@/components/common/views/UriLink.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";

//#region Private
const opensilex = inject<OpenSilexVuePlugin>("$opensilex")
const { t } = useI18n()

const relationsFields = ref([
  {
    key: "prefix",
    label: t("Namespaces.prefix"),
    sortable: true,
  },
  {
    key: "namespaces",
    label: t("component.menu.namespaces"),
    sortable: true,
  }
]);

const items = ref<{ prefix: string, namespaces: string }[]>([]);

onMounted(() => {
  initNamespaces();
});

function initNamespaces() {
  for (const prefix in opensilex.namespaces) {
    items.value.push({
      prefix: prefix,
      namespaces: opensilex.namespaces[prefix]
    });
  }
}

//#endregion
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  Namespaces:
    description: Namespaces display page
    prefix: Prefix
    placeholder: Search by prefix or namespaces
fr:
  Namespaces:
    description: "Page d'affichage des espaces de nom"
    prefix: Préfixe
    placeholder: Rechercher par préfixe ou espaces de nom
</i18n>