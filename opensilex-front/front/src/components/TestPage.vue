<template>
  <h1>Hello Test Page</h1>
</template>

<script  lang="ts">
import { defineComponent, onMounted } from 'vue';



export default defineComponent({
  name: 'testPage'
});

onMounted(() => {
  console.log("TestPage MOUNTED")
})

</script>