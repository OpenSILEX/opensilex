<template>
    <div class="container-fluid">
        <opensilex-PageContent>
            <template v-slot>
                <opensilex-TableView
                    :items="items"
                    :fields="relationsFields"
                    :globalFilterField="true"
                    filterPlaceholder="Namespaces.placeholder"
                    sortBy="prefix">

                    <template v-slot:cell(prefix)="{ data }">{{data.item.prefix}}</template>

                    <template v-slot:cell(namespaces)="{ data }">
                        <opensilex-UriLink
                            :uri="data.item.namespaces"
                            :value="data.item.namespaces"
                        ></opensilex-UriLink>
                    </template>
                </opensilex-TableView>
            </template>
        </opensilex-PageContent>
    </div>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class NamespacesView extends Vue {
    $opensilex: any;

    relationsFields: any[] = [
        {
            key: "prefix",
            label: "Namespaces.prefix",
            sortable: true,
        },
        {
            key: "namespaces",
            label: "component.menu.namespaces",
            sortable: true,
        }
    ];

    items: any[] = [];
    
    created() {
        this.initNamespaces();
    }

    initNamespaces() {
        for (let prefix in this.$opensilex.namespaces) {
            this.items.push({
                "prefix": prefix,
                "namespaces": this.$opensilex.namespaces[prefix]
            });
        }
    }
}
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