<template>
  todo germplasm list
</template>

<script setup lang="ts">
//#region Public
defineEmits<{
  onEdit: []
  onDelete: []
}>()

defineExpose({
  refresh,
  updateSelectedGermplasm
})
//#endregion

//#region Private
function refresh() {
  throw Error('Not implemented');
}

function updateSelectedGermplasm() {
  throw Error('Not implemented');
}
//#endregion
</script>

<style scoped lang="scss">

</style>