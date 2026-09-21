<template>
  <div v-if="show" class="overlay">
    <div class="lds-ripple">
      <div></div>
      <div></div>
    </div>
  </div>
  <slot></slot>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  show: boolean
}>(), {
  show: false
});

</script>

<style scoped lang="scss">
.overlay {
  z-index: 32000;
  position: absolute;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, .5);
}

.lds-ripple {
  display: inline-block;
  position: relative;
  width: 80px;
  height: 80px;
  margin: auto;
}

.lds-ripple div {
  position: absolute;
  border: 4px solid #000;
  opacity: 1;
  border-radius: 50%;
  animation: lds-ripple 1s cubic-bezier(0, 0.2, 0.8, 1) infinite;
}

.lds-ripple div:nth-child(2) {
  animation-delay: -0.5s;
}

@keyframes lds-ripple {
  0% {
    top: 40px;
    left: 40px;
    width: 0;
    height: 0;
    opacity: 1;
  }
  100% {
    top: 0px;
    left: 0px;
    width: 80px;
    height: 80px;
    opacity: 0;
  }
}
</style>
