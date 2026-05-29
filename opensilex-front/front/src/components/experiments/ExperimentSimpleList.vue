<template>
  <div>
    <opensilex-StringFilter
      v-model:filter="nameFilter"
      :lazy="false"
      placeholder="component.experiment.filter-label-placeholder"
    />

    <div class="table-responsive scrollable-container">
      <table class="table table-striped table-hover mb-0">
        <thead class="sticky-top bg-body">
          <tr>
            <th
              scope="col"
              role="button"
              class="user-select-none"
              @click="toggleSort('name')"
            >
              {{ t("component.common.name") }}
              <span v-if="sortBy === 'name'">
                {{ sortDir === "asc" ? "▲" : "▼" }}
              </span>
            </th>

            <th
              scope="col"
              role="button"
              class="user-select-none"
              @click="toggleSort('start_date')"
            >
              {{ t("component.common.date-time-stuff.startDate") }}
              <span v-if="sortBy === 'start_date'">
                {{ sortDir === "asc" ? "▲" : "▼" }}
              </span>
            </th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="exp in displayableExperiments" :key="exp.uri">
            <td>
              <opensilex-UriLink
                :uri="exp.uri"
                :value="exp.name"
                :to="{ path: '/experiment/details/' + encodeURIComponent(exp.uri) }"
              />
            </td>

            <td>
              <opensilex-DateView :value="exp.start_date" />
            </td>
          </tr>

          <tr v-if="!displayableExperiments || displayableExperiments.length === 0">
            <td colspan="2" class="text-muted">
              <!-- optionnel : message si liste vide -->
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import type { ExperimentGetListDTO } from "opensilex-core/model/experimentGetListDTO";

const { t } = useI18n();

const props = withDefaults(
  defineProps<{
    /**
     * Liste d'expériences à afficher
     */
    experimentList?: ExperimentGetListDTO[] | null;
  }>(),
  { experimentList: null }
);

const nameFilter = ref("");

// Tri (équivalent sort-by="name")
type SortKey = "name" | "start_date";
const sortBy = ref<SortKey>("name");
const sortDir = ref<"asc" | "desc">("asc");

function toggleSort(key: SortKey) {
  if (sortBy.value === key) {
    sortDir.value = sortDir.value === "asc" ? "desc" : "asc";
  } else {
    sortBy.value = key;
    sortDir.value = "asc";
  }
}

function compare(a: any, b: any) {
  if (a == null && b == null) return 0;
  if (a == null) return -1;
  if (b == null) return 1;

  // dates ou strings
  const av = typeof a === "string" ? a : String(a);
  const bv = typeof b === "string" ? b : String(b);
  return av.localeCompare(bv);
}

// Computed: filter + sort
const displayableExperiments = computed(() => {
  const list = props.experimentList ?? [];
  const filtered = !nameFilter.value
    ? list
    : list.filter((e) =>
        (e.name ?? "").match(new RegExp(nameFilter.value, "i"))
      );

  const key = sortBy.value;
  const dir = sortDir.value;

  return [...filtered].sort((x, y) => {
    const res = compare((x as any)[key], (y as any)[key]);
    return dir === "asc" ? res : -res;
  });
});
</script>

<style scoped lang="scss">
.help {
  font-size: 1.9em;
  background: #f1f1f1;
  color: #00A38D;
  border-radius: 50%;
  margin-left: 10px;
}

.scrollable-container {
  width: 100%;
  height: 600px;
  overflow-y: auto;
}
</style>
