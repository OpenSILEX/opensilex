<template>
  <div v-if="(lang === 'fr')">
    <h1 id="comment-ajouter-des-ressources-g-n-tiques-">Comment ajouter des ressources génétiques ?</h1>
    <h2 id="introduction">Introduction</h2>
    <p>Vous pouvez rentrer ici toutes les ressources génétiques qui sont utilisées dans vos expérimentations.
    Vous devez insérer les ressources génétiques avant d&#39;insérer vos objets scientifiques.</p>
    <p>Voici les différents types de ressources génétiques que vous pouvez insérer, de la moins fine à la plus fine : </p>
    <ul>
    <li>Espèce</li>
    <li>Variété</li>
    <li>Accession (ou clone)</li>
    <li>PlantMaterialLot (la partie physique de l&#39;accession qui a été utilisée. Cela peut-être un lot de semence, ou un lot de vitroplants par exemple.)</li>
    </ul>
    <p>Selon le type de ressource sélectionnée, le tableau d&#39;insertion sera différent.</p>
    <h2 id="r-gles-">Règles :</h2>
    <p><strong>Globales</strong> :</p>
    <ul>
    <li>Le label est obligatoire pour tout type de ressource</li>
    <li>Si l&#39;URI n&#39;est pas renseigné, alors il sera généré automatiquement. C&#39;est l&#39;URI qui sert d&#39;identifiant unique dans la base.</li>
    <li>Tous les autres champs exceptés URI de l&#39;espèce, URI de la variété, URI de l&#39;accession sont facultatifs</li>
    <li>Pour ajouter plusieurs synonyms, utilisez | comme séparateur</li>
    <li>Vous pouvez ajouter des attributs en cliquant sur le bouton <strong>Add Column</strong>. (Ces attributs sont là à titre d&#39;information, ils ne pourront pas être utilisés pour filtrer par exemple.)</li>
    </ul>
    <p><strong>Création d&#39;espèce</strong><br>Les espèces issues de l&#39;ontologie Agrovoc ont été automatiquement entrées dans la base. Si une espèce est manquante, vous pouvez l&#39;ajouter en utilisant cette fonctionnalité mais celle-ci ne sera pas traduite.</p>
    <p><strong>Création de variété</strong> </p>
    <ul>
    <li>l&#39;URI de l&#39;espèce est obligatoire et doit exister dans la base. Si celle-ci n&#39;existe pas, il faudra alors créer cette espèce.</li>
    </ul>
    <p><strong>Création d&#39;accession</strong> </p>
    <ul>
    <li>Il est obligatoire de renseigner l&#39;URI de la variété ou l&#39;URI de l&#39;espèce.</li>
    <li>Si l&#39;URI de la variété est renseignée mais pas l&#39;espèce, le système ira automatiquement rechercher l&#39;espèce liée à cette variété pour la renseigner sur l&#39;accession.</li>
    <li>Si vous renseignez à la fois l&#39;URI de l&#39;espèce et de la variété, alors le système vérifiera que la variété appartient bien à cette espèce.</li>
    </ul>
    <p><strong>Création de lot (lot de semence ou autre)</strong> </p>
    <ul>
    <li>Il est obligatoire de renseigner l&#39;URI de l&#39;accession, l&#39;URI de la variété ou l&#39;URI de l&#39;espèce.</li>
    <li>Si l&#39;URI de l&#39;accession est renseignée mais ni la variété, ni l&#39;espèce, alors le système ira automatiquement rechercher l&#39;espèce et la variété liée à cette accession pour les renseigner sur le lot inséré.</li>
    <li>Si vous renseignez à la fois l&#39;URI de l&#39;espèce, de la variété et de l&#39;accession, alors le système vérifiera que l&#39;accession appartient bien à la variété et l&#39;espèce renseignées.</li>
    </ul>
    <p>##Exemple de scénario : 
    Je souhaite insérer des plantes de maïs sur lesquels j&#39;ai les informations de variété et de lot de semences.</p>
    <ol>
    <li>Je vérifie que l&#39;espèce maïs existe et je récupère son URI.</li>
    <li>Je crée les différentes variétés en renseignant l&#39;URI de l&#39;espèce.</li>
    <li>Je récupère les URI des variétés et je crée les différents lot en renseignant les URI de variété.</li>
    <li>Je récupère les URI des lots de semence insérés et je les utilise pour créer mes objets scientifiques. </li>
    </ol>
  </div>
  <div v-else>
    <h1 id="how-to-add-germplasm-">How to add germplasm ?</h1>
      <h2 id="introduction">Introduction</h2>
      <p>Here you can insert every germplasm you use in your experimentations.
      You have to insert them before inserting your scientific objects.</p>
      <p>Here are presented all the germplasm types you can add, from the more general to the most precised:</p>
      <ul>
      <li>Species</li>
      <li>Variety</li>
      <li>Accession (or clone)</li>
      <li>PlantMaterialLot (The physical part of the accession which has been used. This can be a seed lot or a vitroplants lot for example.</li>
      </ul>
      <p>Depending on the selected germplasm type, the insertion table will change.</p>
      <h2 id="rules-">Rules :</h2>
      <p><strong>General</strong> :</p>
      <ul>
      <li>The name is mandatory for every germplasm type</li>
      <li>If the URI is not given, then the system will automatically generate a URI. This URI is the unique identifier in the database.</li>
      <li>All other columns are optional excepted Species URI,variety URI and accession URI.</li>
      <li>To add several synonyms, use | as seperator</li>
      <li>You can add additional information by clicking on the button <em>Add Column</em>. (this new attributes are used for information but won&#39;t be used as search filter for example.)</li>
      </ul>
      <p><strong>Species insertion</strong><br>The species defined by Agrovoc ontology have been automatically inserted inside the system database. If a species is missing, you can add it by using this functionnality but it won&#39;t be translated.</p>
      <p><strong>Variety insertion</strong> </p>
      <ul>
      <li>The species URI is mandatory and the species must exist in the database. If it doesn&#39;t exist, you have to insert it.</li>
      </ul>
      <p><strong>Accession insertion</strong> </p>
      <ul>
      <li>It is mandatory to fill the variety URI or Species URI.</li>
      <li>If the variety is given but not the species, then the system will automatically look for the species linked to this variety and fill it for the accession.</li>
      <li>If you fill the species URI and the variety URI, then the system will check that the variety belongs to the species.</li>
      </ul>
      <p><strong>Lot insertion (Seedlot or other)</strong> </p>
      <ul>
      <li>It is mandatory to fill the accession URI, variety URI or Species URI.</li>
      <li>If the accession URI is given but not the variety, neither the species, then the system will automatically look for the species and the variety linked to this accession and fill them for the lot.</li>
      <li>If you fill species URI, variety URI and accession URI, then the system will check that the accession belongs to the variety and the species.</li>
      </ul>
      <h2 id="scenario-example-">Scenario example :</h2>
      <p>I want to insert plants of maize on my experiment. I know the variety and the seedlot of each plant.</p>
      <ol>
      <li>I check that the species maize exists and I retrieve its URI.</li>
      <li>I insert all the varieties by giving the maize species URI.</li>
      <li>I retrieve the varieties URI and I insert all the seedlots with the right variety URIs.</li>
      <li>I retrieve the seedlots URIs and use them to create my scientific objects.</li>
      </ol>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class FactorsHelp extends Vue {
  $opensilex: any;
  $store: any;
  $t: any;
  $i18n: any;
  $router: any;

  get lang() {
    console.log(this.$store.getters.language);
    return this.$store.getters.language;
  }

  selectedFactor: any = {
    uri: null,
    name: null,
    category: null,
    comment: null,
    exactMatch: [],
    closeMatch: [],
    broader: [],
    narrower: [],
  };

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
}
</script>

<style scoped lang="scss">
</style>

