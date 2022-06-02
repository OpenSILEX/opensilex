<template>
  <select v-model="selected">
    <option disabled value="">Choisissez une ressource</option>
    <option>Instance locale</option>
    <option>Phenome</option>
    <option>Vitioeno</option>
  </select>
</template>

<script>
export default {
  name: "ResourcesSelector"
}
</script>

<style scoped>

</style>