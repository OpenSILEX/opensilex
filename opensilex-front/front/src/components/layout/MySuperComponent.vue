<template>
<div class="superComponentContainer">
  <header>
    <h1>
      Voici mon SUPER composant ! 
      <span class="media">🦖</span>
    </h1>
    <h3> 
      <button class="btn logout" @click="logout">Logout</button>
      <button class="btn super" @click="redirectToTest"> vers Composant Test 🐶</button>
    </h3>
  </header>
  </div>
</template>

<script lang="ts">
import { defineComponent, computed } from "vue";
import { useStore } from "vuex";
import { useRouter } from "vue-router";

export default defineComponent({
  name: "mySUperComponent",
  setup() {
    const store = useStore();
    const router = useRouter();

    const isLoggedIn = computed(() => store.state.user.loggedIn);

    const logout = () => {
      console.log("logout");
      store.commit("logout");
      store.commit("refresh");
    };

    const redirectToTest = () => {
      router.push('/test');
    };

    return {
      logout, 
      redirectToTest
    };
  }
});
</script>

<style scoped>

.superComponentContainer{
  width: 100vw;
  height: 100vh;
  /* background-color: #777; */
  background: rgb(115,64,12);
  background: linear-gradient(
    183deg, rgba(115,64,12,0.7035189075630253) 16%,
    rgba(215,161,97,0.700717787114846) 40%,
    rgba(222,167,103,0.6951155462184874) 61%,
    rgba(128,86,46,0.7035189075630253) 85%
  );
  position: relative;
}

header {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  
  min-width: 500px;
  padding: 20px;
  border-radius: 15px;
  background: linear-gradient(135deg, #fbab6e, #e3e177);
  color: white;
  box-shadow: 0px 10px 30px rgba(0, 0, 0, 0.2);
  text-align: center;
}

h1 {
  font-size: 24px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.media {
  display: inline-block;
  margin-left: 10px;
  font-size: 30px;
  animation: rotateEmoji 3s linear infinite;
}

@keyframes rotateEmoji {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

h3 {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.btn {
  padding: 10px 20px;
  border: none;
  font-size: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease-in-out;
}

.logout {
  background: #ff4b5c;
  color: white;
}

.super {
  background: #4caf50;
  color: white;
}

.btn:hover {
  transform: scale(1.1);
}
</style>

