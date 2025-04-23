<template>
  <div>
    <h2>Démo Table</h2>
    <!-- Barre de recherche -->
    <n-input v-model:value="searchQuery" placeholder="Rechercher par nom..." class="mb-4" />

    <!-- Table -->
    <n-data-table 
      :columns="columns" 
      :data="paginatedData" 
      striped 
      :pagination="false"
    />

    <!-- Pagination -->
    <n-pagination 
      v-model:page="currentPage"
      :page-count="Math.ceil(filteredData.length / pageSize)" 
      class="mt-4"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { NDataTable, NInput, NPagination } from 'naive-ui';
import { sampleData } from './sampleData';


// Données et colonnes
const columns = ref([
  { title: 'ID', key: 'id' },
  { 
    title: 'Nom', 
    key: 'name',
    sorter: (a, b) => a.name.localeCompare(b.name) // Tri par nom
  },
  { title: 'Âge', key: 'age' },
  { title: 'Rôle', key: 'role' }
]);

const data = ref(sampleData);
const searchQuery = ref('');
const currentPage = ref(1);
const pageSize = 10;

// Filtrage des results selon la recherche
const filteredData = computed(() => {
  return data.value.filter(row => 
    row.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});

// Pagination des results
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filteredData.value.slice(start, start + pageSize);
});
</script>

<style>

.n-pagination {
    background-color: white;
}

</style>