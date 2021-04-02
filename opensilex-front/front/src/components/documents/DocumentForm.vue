<template>
  <b-form>
    <!-- URI or URL-->
    <opensilex-UriForm
      :uri.sync="form.description.uri" 
      label="DocumentForm.uri"
      :editMode="editMode"
      :generated.sync="uriGenerated"
      helpMessage="DocumentForm.uri-help"
    ></opensilex-UriForm>

    <!-- identifier -->
    <opensilex-InputForm
      :value.sync="form.description.identifier"
      label="DocumentForm.identifier"
      type="text"
      helpMessage="DocumentForm.identifier-help"
      placeholder= "DocumentForm.placeholder-identifier"
    ></opensilex-InputForm>

    <!-- type -->
    <opensilex-TypeForm
      :type.sync="form.description.rdf_type"
      :baseType="$opensilex.Oeso.DOCUMENT_TYPE_URI"
      helpMessage="DocumentForm.type-help"    
      :required="true"
    ></opensilex-TypeForm>

    <!-- title -->
    <opensilex-InputForm
      :value.sync="form.description.title"
      label="DocumentForm.title"
      type="text"
      helpMessage="DocumentForm.title-help"
      :required="true"
    ></opensilex-InputForm>

    <!-- date -->
    <opensilex-InputForm
      :value.sync="form.description.date"
      label="DocumentForm.date"
      type="date"
      helpMessage="DocumentForm.date-help"
    ></opensilex-InputForm>

    <!-- description -->
    <opensilex-TextAreaForm
      :value.sync="form.description.description"
      label="DocumentForm.description"
      type="text"
      helpMessage="DocumentForm.description-help"
    ></opensilex-TextAreaForm>

    <!-- targets -->
    <!-- {regex: /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$|(.)+:(.)+/} -->
    <ValidationProvider
      name="Target"     
      rules="url"         
      :skipIfEmpty="true"
      v-slot="{ errors }"
    >
      <div class="error-message alert alert-danger">{{ errors[0] }}</div>
      <opensilex-TagInputForm
        class="overflow-auto"
        style="height: 90px"
        :value.sync="form.description.targets"
        :baseType="$opensilex.Oeso.targets"
        label="DocumentForm.targets"
        helpMessage="DocumentForm.targets-help"
        type="text"
      ></opensilex-TagInputForm>
    </ValidationProvider>

    <!--hasAuthors -->
    <opensilex-TagInputForm
      :value.sync="form.description.authors"
      :baseType="$opensilex.Oeso.hasAuthors"
      placeholder= "DocumentForm.placeholder-authors"
      label="DocumentForm.authors"
      helpMessage="DocumentForm.authors-help"
      type="text"
    ></opensilex-TagInputForm>

    <!-- language -->
    <opensilex-InputForm
      :value.sync="form.description.language"
      label="DocumentForm.language"
      type="text"
      helpMessage="DocumentForm.language-help"
      placeholder= "DocumentForm.placeholder-language"
    ></opensilex-InputForm>
<!-- <select class="form-control bfh-languages" data-language="en">
  <option value=""></option><option value="om">Afaan Oromoo</option><option value="aa">Afaraf</option><option value="af">Afrikaans</option><option value="ak">Akan</option><option value="an">Aragonés</option><option value="ig">Asụsụ Igbo</option><option value="gn">Avañe'ẽ</option><option value="ae">Avesta</option><option value="ay">Aymar Aru</option><option value="az">Azərbaycan Dili</option><option value="id">Bahasa Indonesia</option><option value="ms">Bahasa Melayu</option><option value="bm">Bamanankan</option><option value="jv">Basa Jawa</option><option value="su">Basa Sunda</option><option value="bi">Bislama</option><option value="bs">Bosanski Jezik</option><option value="br">Brezhoneg</option><option value="ca">Català</option><option value="ch">Chamoru</option><option value="ny">Chicheŵa</option><option value="sn">Chishona</option><option value="co">Corsu</option><option value="cy">Cymraeg</option><option value="da">Dansk</option><option value="se">Davvisámegiella</option><option value="de">Deutsch</option><option value="nv">Diné Bizaad</option><option value="et">Eesti</option><option value="na">Ekakairũ Naoero</option><option value="es">Español</option><option value="en">English</option><option value="eo">Esperanto</option><option value="eu">Euskara</option><option value="ee">Eʋegbe</option><option value="to">Faka Tonga</option><option value="mg">Fiteny Malagasy</option><option value="fr">Français</option><option value="fy">Frysk</option><option value="ff">Fulfulde</option><option value="fo">Føroyskt</option><option value="ga">Gaeilge</option><option value="gv">Gaelg</option><option value="sm">Gagana Fa'a Samoa</option><option value="gl">Galego</option><option value="sq">Gjuha Shqipe</option><option value="gd">Gàidhlig</option><option value="ki">Gĩkũyũ</option><option value="ha">Hausa</option><option value="ho">Hiri Motu</option><option value="hr">Hrvatski Jezik</option><option value="io">Ido</option><option value="rw">Ikinyarwanda</option><option value="rn">Ikirundi</option><option value="ia">Interlingua</option><option value="nd">Isindebele</option><option value="nr">Isindebele</option><option value="xh">Isixhosa</option><option value="zu">Isizulu</option><option value="it">Italiano</option><option value="ik">Iñupiaq</option><option value="pl">Polski</option><option value="mh">Kajin M̧ajeļ</option><option value="kl">Kalaallisut</option><option value="kr">Kanuri</option><option value="kw">Kernewek</option><option value="kg">Kikongo</option><option value="sw">Kiswahili</option><option value="ht">Kreyòl Ayisyen</option><option value="kj">Kuanyama</option><option value="ku">Kurdî</option><option value="la">Latine</option><option value="lv">Latviešu Valoda</option><option value="lt">Lietuvių Kalba</option><option value="ro">Limba Română</option><option value="li">Limburgs</option><option value="ln">Lingála</option><option value="lg">Luganda</option><option value="lb">Lëtzebuergesch</option><option value="hu">Magyar</option><option value="mt">Malti</option><option value="nl">Nederlands</option><option value="no">Norsk</option><option value="nb">Norsk Bokmål</option><option value="nn">Norsk Nynorsk</option><option value="uz">O'zbek</option><option value="oc">Occitan</option><option value="ie">Interlingue</option><option value="hz">Otjiherero</option><option value="ng">Owambo</option><option value="pt">Português</option><option value="ty">Reo Tahiti</option><option value="rm">Rumantsch Grischun</option><option value="qu">Runa Simi</option><option value="sc">Sardu</option><option value="za">Saɯ Cueŋƅ</option><option value="st">Sesotho</option><option value="tn">Setswana</option><option value="ss">Siswati</option><option value="sl">Slovenski Jezik</option><option value="sk">Slovenčina</option><option value="so">Soomaaliga</option><option value="fi">Suomi</option><option value="sv">Svenska</option><option value="mi">Te Reo Māori</option><option value="vi">Tiếng Việt</option><option value="lu">Tshiluba</option><option value="ve">Tshivenḓa</option><option value="tw">Twi</option><option value="tk">Türkmen</option><option value="tr">Türkçe</option><option value="ug">Uyƣurqə</option><option value="vo">Volapük</option><option value="fj">Vosa Vakaviti</option><option value="wa">Walon</option><option value="tl">Wikang Tagalog</option><option value="wo">Wollof</option><option value="ts">Xitsonga</option><option value="yo">Yorùbá</option><option value="sg">Yângâ Tî Sängö</option><option value="is">ÍSlenska</option><option value="cs">čEština</option><option value="el">ελληνικά</option><option value="av">авар мацӀ</option><option value="ab">аҧсуа бызшәа</option><option value="ba">башҡорт теле</option><option value="be">беларуская мова</option><option value="bg">български език</option><option value="os">ирон æвзаг</option><option value="kv">коми кыв</option><option value="ky">Кыргызча</option><option value="mk">македонски јазик</option><option value="mn">монгол</option><option value="ce">нохчийн мотт</option><option value="ru">Русский язык</option><option value="sr">српски језик</option><option value="tt">татар теле</option><option value="tg">тоҷикӣ</option><option value="uk">українська мова</option><option value="cv">чӑваш чӗлхи</option><option value="cu">ѩзыкъ словѣньскъ</option><option value="kk">қазақ тілі</option><option value="hy">Հայերեն</option><option value="yi">ייִדיש</option><option value="he">עברית</option><option value="ur">اردو</option><option value="ar">العربية</option><option value="fa">فارسی</option><option value="ps">پښتو</option><option value="ks">कश्मीरी</option><option value="ne">नेपाली</option><option value="pi">पाऴि</option><option value="bh">भोजपुरी</option><option value="mr">मराठी</option><option value="sa">संस्कृतम्</option><option value="sd">सिन्धी</option><option value="hi">हिन्दी</option><option value="as">অসমীয়া</option><option value="bn">বাংলা</option><option value="pa">ਪੰਜਾਬੀ</option><option value="gu">ગુજરાતી</option><option value="or">ଓଡ଼ିଆ</option><option value="ta">தமிழ்</option><option value="te">తెలుగు</option><option value="kn">ಕನ್ನಡ</option><option value="ml">മലയാളം</option><option value="si">සිංහල</option><option value="th">ไทย</option><option value="lo">ພາສາລາວ</option><option value="bo">བོད་ཡིག</option><option value="dz">རྫོང་ཁ</option><option value="my">ဗမာစာ</option><option value="ka">ქართული</option><option value="ti">ትግርኛ</option><option value="am">አማርኛ</option><option value="iu">ᐃᓄᒃᑎᑐᑦ</option><option value="oj">ᐊᓂᔑᓈᐯᒧᐎᓐ</option><option value="cr">ᓀᐦᐃᔭᐍᐏᐣ</option><option value="km">ខ្មែរ</option><option value="zh">中文&nbsp;(Zhōngwén)</option><option value="ja">日本語&nbsp;(にほんご)</option><option value="ii">ꆈꌠ꒿ Nuosuhxop</option><option value="ko">한국어&nbsp;(韓國語)</option>
</select> -->

    <!-- keywords -->
    <opensilex-TagInputForm
      :value.sync="form.description.keywords"
      label="DocumentForm.keywords"
      type="text"
      helpMessage="DocumentForm.keywords-help"
    ></opensilex-TagInputForm>

    <!-- deprecated -->
    <opensilex-CheckboxForm
      v-if="editMode"
      :value.sync="form.description.deprecated"
      label="DocumentForm.deprecated"
      title="DocumentForm.deprecated-title"
      helpMessage="DocumentForm.deprecated-help"
    ></opensilex-CheckboxForm>

    <!-- File -->
    <opensilex-FileInputForm
      v-if="!editMode"
      :file.sync="form.file"
      label="DocumentForm.file"
      type="file"
      helpMessage="DocumentForm.file-help"
      browse-text="DocumentForm.browse"
      rules="size:100000"
      required=true
    ></opensilex-FileInputForm>

  </b-form>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { DocumentsService,DocumentGetDTO } from "opensilex-core/index"; 
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class DocumentForm extends Vue {
  $opensilex: any;
  service: DocumentsService;
  $store: any;
  $t: any;
  file;
  uriGenerated = true;

  get user() {
    return this.$store.state.user;
  } 

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        description: {
          uri: undefined,
          identifier: undefined,
          rdf_type: undefined,
          title: undefined,
          date: undefined,
          description: undefined,
          targets: undefined,
          authors: undefined,
          language: undefined,
          deprecated: undefined,
          keywords: undefined
        },
        file: undefined
      };
    }
  })
  form: any;

  reset() {
    this.uriGenerated = true;
  }

  getEmptyForm() {
    return {
      description: {
          uri: undefined,
          identifier: undefined,
          rdf_type: undefined,
          title: undefined,
          date: undefined,
          description: undefined,
          targets: undefined,
          authors: undefined,
          language: undefined,
          deprecated: undefined,
          keywords: undefined
        },
        file: undefined
      };
  }

  create(form) {
    return this.$opensilex
     .uploadFileToService("/core/documents", this.form, null, false)
     .then((http: OpenSilexResponse<any>) => {
        let uri = http.result;
        console.debug("document created", uri);
        this.$emit("onCreate", form);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Document already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$i18n.t("DoucmentForm.document-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    return this.$opensilex
     .uploadFileToService("/core/documents", this.form, null, true)
     .then((http: OpenSilexResponse<any>) => {
        let uri = http.result;
        console.debug("Document updated", uri);
        this.$emit("onUpdate", form);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  DocumentForm:
    uri: URI or URL
    uri-help: Unique document identifier autogenerated OR uncheck to insert a URL to an external file
    type: Type
    type-help: Document Type
    title: Title
    title-help: A title given to the resource
    authors: Authors
    placeholder-authors : Last name, First name
    authors-help: An entity primarily responsible for making the resource. Recommended practice is to identify the creator with a URI. If this is not possible or feasible, a literal value that identifies the creator may be provided.
    language : Language 
    language-help: A language of the resource
    placeholder-language: en
    date: Date
    date-help: Creation Date
    format: Format 
    format-help: The file format, physical medium, or dimensions of the resource.
    description: Description
    description-help: Description associated to the document metadata
    keywords: Keywords
    keywords-help: A topic of the resource. Typically, the subject will be represented using keywords, key phrases, or classification codes. Recommended best practice is to use a controlled vocabulary.
    targets: Target
    targets-help: List of resources's URI concerned by the document
    targets-error: Concerned item's URI expected
    deprecated: Deprecated
    deprecated-help: Deprecated File
    deprecated-title: Select this option to make deprecated document
    file: Document
    file-help: Document to upload limit to 100MB
    identifier: Identifier
    identifier-help: Recommended practice is to identify the resource by means of a string conforming to an identification system. Examples include International Standard Book Number (ISBN), Digital Object Identifier (DOI), and Uniform Resource title (URN). Persistent identifiers should be provided as HTTP URIs.
    placeholder-identifier: doi:10.1340/309registries
    browse: Browse

fr:
  DocumentForm:
    uri: URI ou URL
    uri-help: Identifiant unique du document généré automatiquement OU décochez la case pour lier une URL de fichier externe
    type: Type
    type-help: Type de Document 
    title: Titre
    title-help: Titre de la ressource
    authors: Auteurs 
    placeholder-authors : Nom, Prénom
    authors-help: Une entité à l'origine de la creation la ressource. La pratique recommandée consiste à identifier le créateur avec un URI. Si cela n'est pas possible ou faisable, une valeur littérale identifiant le créateur peut être fournie.
    language : Langue 
    language-help: Langue de la ressource
    placeholder-language: fr
    date: Date
    date-help: Date de création
    format: Format 
    format-help: Le format de fichier, le support physique ou les dimensions de la ressource
    description: Description
    description-help: Description associée aux métadonnées du document
    keywords: Mots-clés
    keywords-help: Le(s) sujet(s) de la ressource. En règle générale, le sujet sera représenté à l'aide de mots-clés, d'expressions clés ou de codes de classification. La meilleure pratique recommandée consiste à utiliser un vocabulaire contrôlé.
    targets: Cible
    targets-help: Liste d'URI des ressources concernées par le document
    targets-error: URI de l'élément concerné attendu
    deprecated: Obsolète
    deprecated-help: Fichier obsolète
    deprecated-title: Sélectionnez cette option pour rendre le document obsolète
    file: Document
    file-help: Document à insérer limité à 100MB
    identifier: Identifiant
    identifier-help: La pratique recommandée est d'identifier la ressource au moyen d'une chaîne conforme à un système d'identification. Les exemples incluent le numéro international normalisé du livre (ISBN), l'identificateur d'objet numérique (DOI) et le nom uniforme de ressource (URN). Les identificateurs persistants doivent être fournis sous forme d'URI HTTP.
    placeholder-identifier: doi:10.1340/309registries
    browse: Parcourir

</i18n>
