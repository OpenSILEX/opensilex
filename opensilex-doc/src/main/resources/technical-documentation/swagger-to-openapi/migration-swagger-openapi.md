# Migration Swagger → OpenAPI 3.1 — Documentation Complète

**Document de référence** pour la migration de Swagger 2.0 (OpenAPI 2.0) vers OpenAPI 3.1 dans le projet OpenSILEX.

---

## Table des Matières

1. [Résumé Exécutif](#1-résumé-exécutif)
2. [Schéma Global de l'Architecture](#2-schéma-global-de-larchitecture)
3. [Schéma de Flux de Données](#3-schéma-de-flux-de-données)
4. [Comparatif Avant / Après](#4-comparatif-avant--après)
5. [Schéma de Génération du Spec OpenAPI](#5-schéma-de-génération-du-spec-openapi)
6. [Schéma des Annotations Java](#6-schéma-des-annotations-java)
7. [Schéma des Dépendances Maven](#7-schéma-des-dépendances-maven)
8. [Schéma de Génération TypeScript](#8-schéma-de-génération-typescript)
9. [Schéma de l'Interface OpenApiExtension](#9-schéma-de-linterface-openapiextension)
10. [Schéma du Pipeline Complet](#10-schéma-du-pipeline-complet)
11. [Schéma des Interceptors Frontend](#11-schéma-des-interceptors-frontend)
12. [Schéma de l'API RestApplication](#12-schéma-de-lapi-restapplication)
13. [Schéma des Tests](#13-schéma-des-tests)
14. [Checklist de Migration](#14-checklist-de-migration)
15. [Résumé des Points Clés](#15-résumé-des-points-clés)

---

## 1. Résumé Exécutif

### 1.1. Qu'est-ce que cette migration ?

Cette migration consiste à remplacer l'ancienne technologie **Swagger 2.0** (aussi appelée OpenAPI 2.0) par la nouvelle norme **OpenAPI 3.1**. Swagger 2.0 était une spécification plus ancienne et simplifiée pour décrire les APIs REST. OpenAPI 3.1 est sa version moderne, plus puissante et plus standardisée, qui suit les dernières recommandations du W3C pour les schémas JSON.

En pratique, cela signifie :
- **Côté Java** : Remplacement de toutes les annotations `@Api*` (Swagger 2) par des annotations `@Tag`, `@Operation`, `@Schema` (OpenAPI 3)
- **Côté Build** : Remplacement du plugin de génération TypeScript maison par le plugin officiel `openapi-generator-maven-plugin`
- **Côté Frontend** : Migration du client HTTP basé sur InversifyJS vers `@hey-api/client-fetch` avec génération automatique de code TypeScript

### 1.2. Pourquoi cette migration ?

| Raison | Détail |
|---|---|
| **Standardisation** | OpenAPI 3.1 est la norme actuelle de l'industrie pour décrire les APIs REST |
| **Meilleur support** | Les outils de génération de clients sont plus nombreux et mieux maintenus |
| **Typage fort** | Le nouveau pipeline génère des types TypeScript plus précis |
| **Maintenance** | Le plugin maison `opensilex-swagger-codegen-maven-plugin` n'est plus maintenu |
| **Compatibilité** | OpenAPI 3.1 est compatible avec JSON Schema 2020-12 |

### 1.3. Impact de la migration

- **213 fichiers** modifiés
- **+3685 lignes** ajoutées
- **-3072 lignes** supprimées
- **Tous les DTOs** (Data Transfer Objects) ont été mis à jour
- **Toutes les APIs REST** ont été migrées
- **Le pipeline de build** a été complètement refondu

---

## 2. Schéma Global de l'Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              ARCHITECTURE OPENSILEX OPENAPI                            │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌──────────────────────┐    ┌──────────────────────┐    ┌──────────────────────┐      │
│  │   MODULES JAVA       │    │   MODULES JAVA       │    │   MODULES JAVA       │      │
│  │                      │    │                      │    │                      │      │
│  │  opensilex-core      │    │  opensilex-security  │    │  opensilex-front     │      │
│  │                      │    │                      │    │                      │      │
│  │  - @Tag              │    │  - @Tag              │    │  - Vue.js            │      │
│  │  - @Operation        │    │  - @Operation        │    │  - TypeScript        │      │
│  │  - @Schema           │    │  - @Schema           │    │  - @hey-api/client   │      │
│  │                      │    │                      │    │                      │      │
│  └──────────┬───────────┘    └──────────┬───────────┘    └──────────┬───────────┘      │
│             │                           │                           │                   │
│             │                           │                           │                   │
│             ▼                           ▼                           ▼                   │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                    PHASE DE BUILD (Maven)                                        │    │
│  │                                                                                 │    │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │    │
│  │  │  1. Compilation Java                                                    │   │    │
│  │  │     - AnnotationProcessor (Hibernate Validator)                         │   │    │
│  │  │     - Génération des classes .class                                     │   │    │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │    │
│  │                                                                                 │    │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │    │
│  │  │  2. SwaggerAPIGenerator (exec-maven-plugin)                             │   │    │
│  │  │     - Scan Reflections des classes @Tag                                 │   │    │
│  │  │     - Lecture des annotations @Operation, @Schema                       │   │    │
│  │  │     - Appel OpenApiExtension.getAdditionalOpenApiDefinitions()           │   │    │
│  │  │     - Sérialisation JSON → front/src/lib/openapi.json                   │   │    │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │    │
│  │                                                                                 │    │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │    │
│  │  │  3. openapi-ts CLI (exec-maven-plugin)                                 │   │    │
│  │  │     - openapi-ts -i openapi.json -o front/src/lib/generated             │   │    │
│  │  │     - Plugins: @hey-api/client-fetch, @hey-api/typescript, @hey-api/sdk │   │    │
│  │  │     - Génère: types.gen.ts, sdk.gen.ts, client.gen.ts                   │   │    │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │    │
│  │                                                                                 │    │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │    │
│  │  │  4. index.ts boilerplate (exec-maven-plugin)                           │   │    │
│  │  │     - Génère: HttpResponse.ts, IHttpClient.ts, Headers.ts               │   │    │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │    │
│  │                                                                                 │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                    PHASE D'EXÉCUTION (Tomcat)                                    │    │
│  │                                                                                 │    │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐    │    │
│  │  │  RestApplication (démarrage Tomcat)                                     │    │    │
│  │  │     - Enregistrement de OpenApiResource                                 │    │    │
│  │  │     - GET /rest/openapi.json (spec dynamique)                          │    │    │
│  │  │     - GET /api-docs (Swagger UI interactive)                           │    │    │
│  │  │     - SecurityScheme Bearer JWT                                          │    │    │
│  │  └─────────────────────────────────────────────────────────────────────────┘    │    │
│  │                                                                                 │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐    │
│  │                    PHASE FRONTEND (Vue.js + Vite)                                │    │
│  │                                                                                 │    │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐    │    │
│  │  │  client.ts                                                              │    │    │
│  │  │     - registerOpenSilexClient() → config baseUrl, interceptors          │    │    │
│  │  │     - Import SDK: import { authenticate } from 'opensilex-security'     │    │    │
│  │  │     - async/await avec { data, error }                                  │    │    │
│  │  └─────────────────────────────────────────────────────────────────────────┘    │    │
│  │                                                                                 │    │
│  └─────────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Schéma de Flux de Données

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              FLUX DE DONNÉES OPENAPI                                   │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐         │
│  │   Java Code  │    │  Annotations │    │  Reflections │    │  OpenAPI     │         │
│  │              │    │   @Tag       │    │  Scanner     │    │  JSON Spec   │         │
│  │  class API   │───▶│   @Operation │───▶│  Scan        │───▶│  openapi.json│         │
│  │  @Path       │    │   @Schema    │    │  Classes     │    │              │         │
│  │              │    │              │    │              │    │              │         │
│  └──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘         │
│                                                                                         │
│                              ▼                                                         │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐         │
│  │  openapi-ts  │    │  @hey-api    │    │  TypeScript  │    │  Vue.js      │         │
│  │  CLI         │    │  Plugins     │    │  SDK         │    │  Frontend    │         │
│  │              │    │  client-fetch│    │  types.gen   │    │  client.ts   │         │
│  │  openapi.json│───▶│  typescript  │───▶│  sdk.gen     │───▶│  async/await │         │
│  │              │    │  sdk         │    │  client.gen  │    │  { data, err}│         │
│  └──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘         │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Comparatif Avant / Après

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              COMPARATIF AVANT / APRÈS                                  │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                              AVANT (Swagger 2.0)                                │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  Java Annotations                                                       │   │   │
│  │  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │  │  @Api("Annotations")                                            │   │   │
│  │  │  │  @Path("/core/annotations")                                     │   │   │
│  │  │  │  public class AnnotationAPI {                                   │   │   │
│  │  │  │      @ApiOperation("Créer une annotation")                      │   │   │
│  │  │  │      @ApiResponses(value = {                                    │   │   │
│  │  │  │          @ApiResponse(code = 201, message = "Créée"),           │   │   │
│  │  │  │          @ApiResponse(code = 409, message = "Existe déjà")      │   │   │
│  │  │  │      })                                                         │   │   │
│  │  │  │      @POST                                                      │   │   │
│  │  │  │      public Response create(...) { ... }                        │   │   │
│  │  │  │  }                                                              │   │   │
│  │  │  └─────────────────────────────────────────────────────────────────┘   │   │
│  │  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │  │  DTO Annotations                                                │   │   │
│  │  │  │  @ApiModel                                                      │   │   │
│  │  │  │  public class AnnotationCreationDTO {                           │   │   │
│  │  │  │      @ApiModelProperty(required = true, example = "...")        │   │   │
│  │  │  │      public String getDescription() { return description; }      │   │   │
│  │  │  │  }                                                              │   │   │
│  │  │  └─────────────────────────────────────────────────────────────────┘   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  Build & Generation                                                     │   │   │
│  │  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │  │  exec-maven-plugin → SwaggerAPIGenerator                        │   │   │
│  │  │  │     → front/src/lib/swagger.json                                │   │   │
│  │  │  │                                                                 │   │   │
│  │  │  │  opensilex-swagger-codegen-maven-plugin (maison)                │   │   │
│  │  │  │     → templates/typescript-inversify                            │   │   │
│  │  │  │     → front/src/lib/generated/                                  │   │   │
│  │  │  └─────────────────────────────────────────────────────────────────┘   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  Frontend Client                                                        │   │   │
│  │  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │  │  Inversify Service Locator                                      │   │   │
│  │  │  │  const service = $opensilex.getService("opensilex.UriSearch")   │   │   │
│  │  │  │  service.getUriTypes().then(response => { ... })                │   │   │
│  │  │  └─────────────────────────────────────────────────────────────────┘   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                              APRÈS (OpenAPI 3.1)                                │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  Java Annotations                                                       │   │   │
│  │  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │  │  @Tag(name = "Annotations")                                     │   │   │
│  │  │  │  @Path("/core/annotations")                                     │   │   │
│  │  │  │  public class AnnotationAPI {                                   │   │   │
│  │  │  │      @Operation(summary = "Créer une annotation")               │   │   │
│  │  │  │      @ApiResponses(value = {                                    │   │   │
│  │  │  │          @ApiResponse(responseCode = "201", description = ...), │   │   │
│  │  │  │          @ApiResponse(responseCode = "409", description = ...)  │   │   │
│  │  │  │      })                                                         │   │   │
│  │  │  │      @POST                                                      │   │   │
│  │  │  │      public Response create(...) { ... }                        │   │   │
│  │  │  │  }                                                              │   │   │
│  │  │  └─────────────────────────────────────────────────────────────────┘   │   │
│  │  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │  │  DTO Annotations                                                │   │   │
│  │  │  │  @Schema                                                        │   │   │
│  │  │  │  public class AnnotationCreationDTO {                           │   │   │
│  │  │  │      @Schema(requiredMode = Schema.RequiredMode.REQUIRED)       │   │   │
│  │  │  │      public String getDescription() { return description; }      │   │   │
│  │  │  │  }                                                              │   │   │
│  │  │  └─────────────────────────────────────────────────────────────────┘   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  Build & Generation                                                     │   │   │
│  │  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │  │  exec-maven-plugin → SwaggerAPIGenerator                        │   │   │
│  │  │  │     → front/src/lib/openapi.json                                │   │   │
│  │  │  │                                                                 │   │   │
│  │  │  │  openapi-generator-maven-plugin (officiel)                      │   │   │
│  │  │  │     → openapi-ts CLI avec @hey-api/plugins                      │   │   │
│  │  │  │     → front/src/lib/generated/                                  │   │   │
│  │  │  └─────────────────────────────────────────────────────────────────┘   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  Frontend Client                                                        │   │   │
│  │  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │  │  @hey-api/client-fetch                                          │   │   │
│  │  │  │  import { getUriTypes } from "opensilex-core"                   │   │   │
│  │  │  │  const { data, error } = await getUriTypes()                    │   │   │
│  │  │  └─────────────────────────────────────────────────────────────────┘   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Schéma de Génération du Spec OpenAPI

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        GÉNÉRATION DU SPECS OPENAPI                                     │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    SwaggerAPIGenerator.main()                                    │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  1. Initialisation de l'instance OpenSilex                             │   │   │
│  │  │     - Chargement des modules                                           │   │   │
│  │  │     - Configuration des classes de base                                │   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  2. Scan des modules implémentant OpenApiExtension                     │   │   │
│  │  │     - opensilex-core → getAdditionalOpenApiDefinitions()               │   │   │
│  │  │     - opensilex-security → getAdditionalOpenApiDefinitions()           │   │   │
│  │  │     - opensilex-brapi → getAdditionalOpenApiDefinitions()              │   │   │
│  │  │     - ...                                                              │   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  3. Scan Reflections des classes @Tag                                  │   │   │
│  │  │     - Lecture de toutes les classes annotées avec @Tag                 │   │   │
│  │  │     - Extraction des annotations @Operation, @Schema, @Parameter       │   │   │
│  │  │     - Construction de l'objet OpenAPI                                │   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  4. Injection des schémas GeoJson                                      │   │   │
│  │  │     - GeoJsonConverter.injectGeoJsonSchema(openAPI)                    │   │   │
│  │  │     - Ajout des types GeoJSON dans components/schemas                  │   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  5. Sérialisation JSON                                                 │   │   │
│  │  │     - Json.pretty().writeValue(openApiFile, openAPI)                  │   │   │
│  │  │     - → front/src/lib/openapi.json                                     │   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Schéma des Annotations Java

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        HIÉRARCHIE DES ANNOTATIONS JAVA                                   │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    AVANT (Swagger 2.0)                                          │   │
│  │                                                                                 │   │
│  │  io.swagger.annotations                                                         │   │
│  │  ├── @Api                    → Description de l'API                            │   │
│  │  ├── @ApiOperation           → Description d'une opération                     │   │
│  │  ├── @ApiParam               → Paramètre d'une requête                         │   │
│  │  ├── @ApiResponses           → Réponses possibles                              │   │
│  │  ├── @ApiResponse            → Une réponse possible                            │   │
│  │  ├── @ApiModel               → Description d'un modèle                         │   │
│  │  ├── @ApiModelProperty       → Propriété d'un modèle                           │   │
│  │  ├── @ApiIgnore              → Ignorer un élément                              │   │
│  │  └── @Contact                → Informations de contact                         │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    APRÈS (OpenAPI 3.1)                                          │   │
│  │                                                                                 │   │
│  │  io.swagger.v3.oas.annotations                                                  │   │
│  │  ├── tags                                                                      │   │
│  │  │   └── @Tag                → Description de l'API                            │   │
│  │  ├── info                                                                        │   │
│  │  │   ├── @Info               → Informations générales                          │   │
│  │  │   ├── @Contact            → Informations de contact                         │   │
│  │  │   └── @License            → Licence                                         │   │
│  │  ├── media                                                                       │   │
│  │  │   ├── @Schema             → Description d'un modèle                         │   │
│  │  │   └── @Content            → Format de contenu                               │   │
│  │  ├── operations                                                                    │   │
│  │  │   ├── @Operation          → Description d'une opération                     │   │
│  │  │   ├── @Parameter          → Paramètre d'une requête                         │   │
│  │  │   ├── @Parameters         → Paramètres multiples                            │   │
│  │  │   ├── @ApiResponse        → Une réponse possible                            │   │
│  │  │   └── @ApiResponses       → Réponses possibles                              │   │
│  │  ├── security                                                                    │   │
│  │  │   └── @SecurityRequirement → Exigence de sécurité                           │   │
│  │  └── extensions                                                                    │   │
│  │      └── @OpenAPIDefinition → Définition OpenAPI                              │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Schéma des Dépendances Maven

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        DÉPENDANCES MAVEN                                               │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    AVANT (Swagger 2.0)                                          │   │
│  │                                                                                 │   │
│  │  io.swagger:swagger-jersey2-jaxrs:1.6.16                                       │   │
│  │  ├── io.swagger:swagger-core:1.6.16                                            │   │
│  │  │   ├── io.swagger:swagger-annotations:1.6.16                                 │   │
│  │  │   ├── io.swagger:swagger-models:1.6.16                                      │   │
│  │  │   └── io.swagger:swagger-parser:1.0.66                                      │   │
│  │   └── io.swagger:swagger-jaxrs:1.6.16                                         │   │
│  │       └── io.swagger:swagger-jaxrs-reader:1.6.16                              │   │
│  │                                                                                 │   │
│  │  opensilex:opensilex-swagger-codegen-maven-plugin:${revision}                   │   │
│  │  └── Template maison pour génération TypeScript                                │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    APRÈS (OpenAPI 3.1)                                          │   │
│  │                                                                                 │   │
│  │  io.swagger.core.v3:swagger-jaxrs2:2.2.53                                      │   │
│  │  ├── io.swagger.core.v3:swagger-core:2.2.53                                    │   │
│  │  │   ├── io.swagger.core.v3:swagger-annotations:2.2.53                         │   │
│  │  │   ├── io.swagger.core.v3:swagger-models:2.2.53                              │   │
│  │  │   └── io.swagger.core.v3:swagger-integration:2.2.53                         │   │
│  │  └── io.swagger.core.v3:swagger-jaxrs2:2.2.53                                  │   │
│  │                                                                                 │   │
│  │  org.openapitools:openapi-generator-maven-plugin:7.11.0                        │   │
│  │  └── openapi-generator-core:7.11.0                                             │   │
│  │                                                                                 │   │
│  │  @hey-api/openapi-ts (npm)                                                      │   │
│  │  ├── @hey-api/client-fetch                                                     │   │
│  │  ├── @hey-api/typescript                                                       │   │
│  │  └── @hey-api/sdk                                                              │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Schéma de Génération TypeScript

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        GÉNÉRATION TYPESCRIPT                                           │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    openapi-ts CLI                                                │   │
│  │                                                                                 │   │
│  │  openapi-ts                                                                     │   │
│  │  ├── --plugins @hey-api/client-fetch                                           │   │
│  │  │   └── Génère: client.gen.ts                                                 │   │
│  │  │       └── Client HTTP de base avec interceptors                             │   │
│  │  ├── --plugins @hey-api/typescript                                             │   │
│  │  │   └── Génère: types.gen.ts                                                  │   │
│  │  │       └── Interfaces TypeScript strictes                                    │   │
│  │  └── --plugins @hey-api/sdk                                                    │   │
│  │      └── Génère: sdk.gen.ts                                                    │   │
│  │          └── Fonctions SDK typées                                               │   │
│  │                                                                                 │   │
│  │  Entrée: openapi.json                                                           │   │
│  │  Sortie: front/src/lib/generated/                                               │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    Fichiers Générés                                              │   │
│  │                                                                                 │   │
│  │  front/src/lib/generated/                                                       │   │
│  │  ├── types.gen.ts                                                              │   │
│  │  │   └── Interfaces TypeScript pour DTOs                                       │   │
│  │  │       ├── DataCreationDTO                                                   │   │
│  │  │       ├── DataUpdateDTO                                                     │   │
│  │  │       ├── DataGetDTO                                                        │   │
│  │  │       └── ...                                                               │   │
│  │  │                                                                             │   │
│  │  ├── sdk.gen.ts                                                                │   │
│  │  │   └── Fonctions SDK typées                                                  │   │
│  │  │       ├── authenticate()                                                    │   │
│  │  │       ├── renewToken()                                                      │   │
│  │  │       ├── getConfig()                                                       │   │
│  │  │       └── ...                                                               │   │
│  │  │                                                                             │   │
│  │  ├── client.gen.ts                                                             │   │
│  │  │   └── Client HTTP de base                                                   │   │
│  │  │       └── @hey-api/client-fetch                                             │   │
│  │  │                                                                             │   │
│  │  └── index.ts                                                                  │   │
│  │      └── Barrel file ré-exportant tout                                          │   │
│  │          ├── export * from './types.gen'                                       │   │
│  │          ├── export * from './sdk.gen'                                         │   │
│  │          └── export * from './client.gen'                                      │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Schéma de l'Interface OpenApiExtension

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        INTERFACE OpenApiExtension                                        │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    AVANT (SwaggerExtension)                                      │   │
│  │                                                                                 │   │
│  │  public interface SwaggerExtension {                                            │   │
│  │      List<Class<?>> getAdditionalSwaggerDefinitions();                           │   │
│  │  }                                                                              │   │
│  │                                                                                 │   │
│  │  Implémentations:                                                               │   │
│  │  ├── opensilex-core → DataCreationDTO, DataUpdateDTO, ...                       │   │
│  │  ├── opensilex-security → AccountCreationDTO, AccountUpdateDTO, ...             │   │
│  │  └── opensilex-brapi → BrAPIv1StudyDTO, BrAPIv1ObservationDTO, ...             │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    APRÈS (OpenApiExtension)                                      │   │
│  │                                                                                 │   │
│  │  public interface OpenApiExtension {                                            │   │
│  │      List<Class<?>> getAdditionalOpenApiDefinitions();                          │   │
│  │  }                                                                              │   │
│  │                                                                                 │   │
│  │  public interface SwaggerExtension extends OpenApiExtension {                   │   │
│  │      List<Class<?>> getAdditionalSwaggerDefinitions();                          │   │
│  │                                                                                 │   │
│  │      @Override                                                                 │   │
│  │      default List<Class<?>> getAdditionalOpenApiDefinitions() {                 │   │
│  │          return getAdditionalSwaggerDefinitions();                              │   │
│  │      }                                                                         │   │
│  │  }                                                                              │   │
│  │                                                                                 │   │
│  │  Implémentations:                                                               │   │
│  │  ├── opensilex-core → DataCreationDTO, DataUpdateDTO, ...                       │   │
│  │  ├── opensilex-security → AccountCreationDTO, AccountUpdateDTO, ...             │   │
│  │  └── opensilex-brapi → BrAPIv1StudyDTO, BrAPIv1ObservationDTO, ...             │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 10. Schéma du Pipeline Complet

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        PIPELINE COMPLET DE BUILD                                        │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    Phase 1: Compilation Java                                     │   │
│  │                                                                                 │   │
│  │  mvn compile                                                                    │   │
│  │  ├── maven-compiler-plugin                                                       │   │
│  │  │   ├── Compilation des classes .java → .class                                │   │
│  │  │   └── AnnotationProcessor (Hibernate Validator)                             │   │
│  │  └── serviceloader-maven-plugin                                                 │   │
│  │      └── Génération des META-INF/services                                       │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    Phase 2: Génération du Spec OpenAPI                           │   │
│  │                                                                                 │   │
│  │  exec-maven-plugin → SwaggerAPIGenerator.main()                                 │   │
│  │  ├── Scan Reflections des classes @Tag                                           │   │
│  │  ├── Lecture des annotations @Operation, @Schema, @Parameter                     │   │
│  │  ├── Appel OpenApiExtension.getAdditionalOpenApiDefinitions()                   │   │
│  │  ├── Injection des schémas GeoJson                                              │   │
│  │  └── Sérialisation JSON → front/src/lib/openapi.json                           │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    Phase 3: Génération TypeScript                                │   │
│  │                                                                                 │   │
│  │  exec-maven-plugin → openapi-ts CLI                                             │   │
│  │  ├── openapi-ts -i openapi.json -o front/src/lib/generated                     │   │
│  │  │   ├── @hey-api/client-fetch → client.gen.ts                                 │   │
│  │  │   ├── @hey-api/typescript → types.gen.ts                                    │   │
│  │  │   └── @hey-api/sdk → sdk.gen.ts                                             │   │
│  │  └── exec-maven-plugin → index.ts boilerplate                                  │   │
│  │      ├── HttpResponse.ts                                                       │   │
│  │      ├── IHttpClient.ts                                                        │   │
│  │      └── Headers.ts                                                            │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    Phase 4: Build Vue.js                                         │   │
│  │                                                                                 │   │
│  │  frontend-maven-plugin → npm run build                                          │   │
│  │  ├── Installation des dépendances npm                                          │   │
│  │  ├── Compilation TypeScript                                                     │   │
│  │  └── Build Vite → front/dist/                                                  │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    Phase 5: Packaging                                            │   │
│  │                                                                                 │   │
│  │  maven-jar-plugin                                                               │   │
│  │  ├── Compilation des classes .class → .jar                                     │   │
│  │  ├── Copie des fichiers front/dist/ dans le JAR                                │   │
│  │  └── Génération du JAR final                                                   │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 11. Schéma des Interceptors Frontend

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        INTERCEPTORS FRONTEND                                           │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    registerOpenSilexClient()                                     │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  Request Interceptor                                                     │   │   │
│  │  │  ┌─────────────────────────────────────────────────────────────────┐   │   │   │
│  │  │  │  1. Ajout du baseUrl                                             │   │   │   │
│  │  │  │     - getBaseApi() → "http://localhost:8666/rest"               │   │   │   │
│  │  │  │  2. Ajout du token Authorization                                 │   │   │   │
│  │  │  │     - getAuthToken() → "Bearer <token>"                         │   │   │   │
│  │  │  │  3. Ajout du header Accept-Language                              │   │   │   │
│  │  │  │     - "fr" par défaut                                            │   │   │   │
│  │  │  │  4. Log debug si activé                                          │   │   │   │
│  │  │  │     - console.groupCollapsed()                                   │   │   │   │
│  │  │  │     - Affiche: URL, Method, Headers, Body, Query                │   │   │   │
│  │  │  └─────────────────────────────────────────────────────────────────┘   │   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  Response Interceptor                                                    │   │   │
│  │  │  ┌─────────────────────────────────────────────────────────────────┐   │   │   │
│  │  │  │  1. Log debug si activé                                          │   │   │   │
│  │  │  │     - Affiche: Status, Duration, Response Payload               │   │   │   │
│  │  │  │  2. Gestion du 401 Unauthorized                                  │   │   │   │
│  │  │  │     - localStorage.removeItem("opensilex_token")                │   │   │   │
│  │  │  │     - window.dispatchEvent(new CustomEvent("opensilex:unauthorized"))  │   │   │   │
│  │  │  │  3. Gestion des erreurs HTTP                                     │   │   │   │
│  │  │  │     - OpenSilexResponseError avec status et details             │   │   │   │
│  │  │  └─────────────────────────────────────────────────────────────────┘   │   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 12. Schéma de l'API RestApplication

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        RESTAPPLICATION (Tomcat)                                         │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    RestApplication.java                                          │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  1. Configuration Jersey                                                 │   │   │
│  │  │     - register(MultiPartFeature.class)                                   │   │   │
│  │  │     - register(ObjectMapperContextResolver.class)                        │   │   │
│  │  │     - register(GZipEncoder.class)                                        │   │   │
│  │  │     - register(EncodingFilter.class)                                     │   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  2. Enregistrement des packages API                                      │   │   │
│  │  │     - getAPIExtensionModules() → packageList                            │   │   │
│  │  │     - packages(String.join(";", packageList))                           │   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  3. Initialisation OpenAPI                                               │   │   │
│  │  │     - SwaggerAPIGenerator.configureModelConverters()                    │   │   │
│  │  │     - OpenAPI openAPI = new OpenAPI()                                   │   │   │
│  │  │     │   ├── info(new Info().title("OpenSilex API").version(version))    │   │   │
│  │  │     │   ├── components(new Components().addSecuritySchemes("Bearer", ...))  │   │   │
│  │  │     │   └── addServersItem(new Server().url("/rest"))                  │   │   │
│  │  │     - SwaggerConfiguration oasConfig = new SwaggerConfiguration()       │   │   │
│  │  │     │   ├── openAPI(openAPI)                                            │   │   │
│  │  │     │   ├── prettyPrint(true)                                           │   │   │
│  │  │     │   └── resourcePackages(new HashSet<>(packageList))               │   │   │
│  │  │     - OpenApiResource openApiResource = new OpenApiResource()           │   │   │
│  │  │     │   └── openApiResource.openApiConfiguration(oasConfig)             │   │   │
│  │  │     - register(openApiResource)                                          │   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  4. Enregistrement des Services                                         │   │   │
│  │  │     - bind(opensilex).to(OpenSilex.class)                               │   │   │
│  │  │     - bind(module).to(module.getClass())                                │   │   │
│  │  │     - bind(module.getConfig()).to(moduleConfigClass)                    │   │   │
│  │  │     - bind(implementation).named(name).to(serviceClass)                 │   │   │
│  │  │     - bindAsContract(serviceClass).in(RequestScoped.class)              │   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  │  ┌─────────────────────────────────────────────────────────────────────────┐   │   │
│  │  │  5. Initialisation des Modules                                          │   │   │
│  │  │     - getAPIExtensionModules() → initRestApplication(this)              │   │   │
│  │  └─────────────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 13. Schéma des Tests

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        STRATÉGIE DE TESTS                                              │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    Tests Java (Backend)                                          │   │
│  │                                                                                 │   │
│  │  OpenApiValidationTest.java                                                     │   │
│  │  ├── testOpenApiSpecGeneration()                                                 │   │
│  │  │   ├── SwaggerAPIGenerator.getFullApi() ≠ null                                │   │
│  │  │   ├── openAPI.getPaths() ≠ null                                              │   │
│  │  │   └── openAPI.getComponents().getSchemas() ≠ null                            │   │
│  │  │                                                                              │   │
│  │  ├── testAllRestEndpointsHaveOpenApiAnnotations()                                │   │
│  │  │   ├── Tous les @Path ont un @Tag                                            │   │
│  │  │   └── Vérification via Reflections                                           │   │
│  │  │                                                                              │   │
│  │  └── testRestApplicationInitOpenApi()                                            │   │
│  │      ├── RestApplication instance ≠ null                                        │   │
│  │      └── OpenApiResource correctement enregistré                                │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    Tests TypeScript (Frontend)                                   │   │
│  │                                                                                 │   │
│  │  client.spec.ts                                                                 │   │
│  │  ├── test getBaseApi()                                                          │   │
│  │  │   ├── URL contient "/rest"                                                   │   │
│  │  │   └── URL correcte en dev et production                                      │   │
│  │  │                                                                              │   │
│  │  ├── test OpenSilexResponseError                                                │   │
│  │  │   ├── name = "OpenSilexResponseError"                                        │   │
│  │  │   ├── status = 401                                                           │   │
│  │  │   ├── message = "Non autorisé"                                               │   │
│  │  │   └── details = { title: "Erreur" }                                          │   │
│  │  │                                                                              │   │
│  │  ├── test mode debug                                                            │   │
│  │  │   ├── isDebugApiEnabled() = true/false                                       │   │
│  │  │   └── setApiDebug() modifie l'état                                           │   │
│  │  │                                                                              │   │
│  │  ├── test logs console                                                          │   │
│  │  │   ├── console.groupCollapsed() appelé                                        │   │
│  │  │   └── console.log() appelé avec détails                                      │   │
│  │  │                                                                              │   │
│  │  ├── test Security.authenticate()                                                │   │
│  │  │   ├── mockPost appelé avec URL "/security/authenticate"                      │   │
│  │  │   └── response instanceof OpenSilexResponse                                  │   │
│  │  │                                                                              │   │
│  │  └── test méthodes backward-compatible                                          │   │
│  │      ├── api.GET() appelé                                                       │   │
│  │      ├── api.POST() appelé                                                      │   │
│  │      ├── api.PUT() appelé                                                       │   │
│  │      └── api.DELETE() appelé                                                    │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 14. Checklist de Migration

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        CHECKLIST DE MIGRATION                                          │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    Backend Java                                                  │   │
│  │                                                                                 │   │
│  │  [ ] Remplacer tous les imports Swagger 2 par OpenAPI 3                         │   │
│  │      ├── io.swagger.annotations → io.swagger.v3.oas.annotations                │   │
│  │      ├── @Api → @Tag                                                           │   │
│  │      ├── @ApiOperation → @Operation                                            │   │
│  │      ├── @ApiModel → @Schema                                                   │   │
│  │      └── @ApiModelProperty → @Schema                                           │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour les annotations @ApiResponses                                │   │
│  │      ├── code → responseCode                                                   │   │
│  │      └── message → description                                                 │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour les annotations @Parameter                                  │   │
│  │      └── Ajout de @SecurityRequirement(name = "Bearer") via @ApiProtected       │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour les DTOs avec @Schema                                       │   │
│  │      ├── requiredMode = Schema.RequiredMode.REQUIRED                           │   │
│  │      └── description au lieu de value                                          │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour SwaggerAPIGenerator.java                                    │   │
│  │      ├── JenaModelConverter pour Jena/RDF4J                                    │   │
│  │      ├── configureModelConverters() avec ModelResolver                         │   │
│  │      └── GeoJsonConverter.injectGeoJsonSchema()                                │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour RestApplication.java                                        │   │
│  │      ├── OpenApiResource au lieu de BeanConfig                                 │   │
│  │      ├── SecurityScheme Bearer JWT                                             │   │
│  │      └── SwaggerConfiguration oasConfig                                        │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour GeoJsonConverter.java                                       │   │
│  │      ├── @OpenAPIDefinition au lieu de @SwaggerDefinition                      │   │
│  │      └── injectGeoJsonSchema(OpenAPI openAPI) au lieu de afterScan()           │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour les interfaces d'extension                                  │   │
│  │      ├── OpenApiExtension.java (nouvelle)                                      │   │
│  │      └── SwaggerExtension extends OpenApiExtension (rétrocompatibilité)         │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    Build Maven                                                   │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour opensilex-parent/pom.xml                                    │   │
│  │      ├── swagger.v3.version = 2.2.53                                           │   │
│  │      ├── openapi.generator.version = 7.11.0                                    │   │
│  │      ├── swagger-jaxrs2 au lieu de swagger-jersey2-jaxrs                      │   │
│  │      ├── openapi.json au lieu de swagger.json                                  │   │
│  │      ├── openapi-generator-maven-plugin au lieu de opensilex-swagger-codegen  │   │
│  │      └── skipFrontTypesGeneration = ${skipFrontBuild}                          │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour les POMs des modules                                        │   │
│  │      ├── opensilex-core/pom.xml                                                │   │
│  │      ├── opensilex-security/pom.xml                                            │   │
│  │      └── opensilex-front/pom.xml                                               │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour openapi-ts.config.ts                                        │   │
│  │      ├── input: opensilex-front/front/src/lib/openapi.json                    │   │
│  │      ├── output: opensilex-front/front/src/lib/generated                      │   │
│  │      └── plugins: @hey-api/client-fetch, @hey-api/typescript, @hey-api/sdk    │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    Frontend TypeScript                                           │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour opensilex-front/front/src/api/client.ts                     │   │
│  │      ├── registerOpenSilexClient()                                             │   │
│  │      ├── interceptors.request.use()                                            │   │
│  │      ├── interceptors.response.use()                                           │   │
│  │      └── api.Security.authenticate()                                           │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour les imports dans les composants                             │   │
│  │      ├── import { getUriTypes } from "opensilex-core"                         │   │
│  │      ├── import { authenticate } from "opensilex-security"                    │   │
│  │      └── async/await au lieu de .then()/.catch()                              │   │
│  │                                                                                 │   │
│  │  [ ] Mettre à jour les tests frontend                                          │   │
│  │      ├── client.spec.ts                                                        │   │
│  │      └── npm test / vitest run                                                │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                    Tests                                                         │   │
│  │                                                                                 │   │
│  │  [ ] Exécuter les tests Java                                                    │   │
│  │      ├── mvn test                                                              │   │
│  │      └── OpenApiValidationTest.java                                            │   │
│  │                                                                                 │   │
│  │  [ ] Exécuter les tests TypeScript                                              │   │
│  │      ├── cd opensilex-front/front                                              │   │
│  │      └── npm test                                                             │   │
│  │                                                                                 │   │
│  │  [ ] Vérifier la génération du spec                                             │   │
│  │      ├── ls -la opensilex-core/front/src/lib/openapi.json                      │   │
│  │      ├── ls -la opensilex-security/front/src/lib/openapi.json                  │   │
│  │      └── ls -la opensilex-front/front/src/lib/openapi.json                     │   │
│  │                                                                                 │   │
│  │  [ ] Vérifier la génération TypeScript                                          │   │
│  │      ├── ls opensilex-core/front/src/lib/generated/types.gen.ts                │   │
│  │      ├── ls opensilex-core/front/src/lib/generated/sdk.gen.ts                  │   │
│  │      └── ls opensilex-core/front/src/lib/generated/client.gen.ts               │   │
│  │                                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 15. Résumé des Points Clés

1. **Migration des annotations Java** : Toutes les annotations `@Api*` (Swagger 2) ont été remplacées par des annotations `@Tag`, `@Operation`, `@Schema` (OpenAPI 3).

2. **Migration du build** : Le plugin maison `opensilex-swagger-codegen-maven-plugin` a été remplacé par le plugin officiel `openapi-generator-maven-plugin` avec `@hey-api/openapi-ts`.

3. **Migration du client frontend** : Le client HTTP basé sur InversifyJS a été remplacé par `@hey-api/client-fetch` avec génération automatique de code TypeScript.

4. **Rétrocompatibilité** : L'interface `SwaggerExtension` a été conservée et étend `OpenApiExtension` pour assurer la rétrocompatibilité.

5. **Tests** : De nouveaux tests ont été ajoutés pour valider la génération du spec OpenAPI et le fonctionnement du client TypeScript.

6. **Pipeline de build** : Le pipeline a été complètement refondu pour utiliser `@hey-api/openapi-ts` avec les plugins `@hey-api/client-fetch`, `@hey-api/typescript` et `@hey-api/sdk`.

7. **Sécurité** : Le mécanisme d'authentification Bearer JWT a été conservé et amélioré avec `@ApiProtected` et `@ApiTranslatable`.

8. **GeoJSON** : Le support GeoJSON a été migré vers OpenAPI 3.1 avec `GeoJsonConverter.injectGeoJsonSchema()`.

---

## 16. Annexes

### 16.1. Exemple Complet : DTO `DataCreationDTO`

**Avant (Swagger 2) :**
```java
@ValidURI
@ApiModelProperty(example = DataAPI.DATA_EXAMPLE_URI)
protected URI uri;

@Required
@ApiModelProperty(value = "date ou datetime", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE, required = true)
private String date;

@ApiModelProperty(value = "URI cible sur laquelle les données ont été collectées", example = "http://plot01")
@JsonDeserialize(using = UriJsonDeserializer.class)
private URI target;

@NotNull
@ApiModelProperty(value = "URI de la variable", example = DataAPI.DATA_EXAMPLE_VARIABLEURI, required = true)
@JsonDeserialize(using = UriJsonDeserializer.class)
private URI variable;

@NotNull
@ApiModelProperty(value = "peut être décimal, entier, booléen, chaîne ou date", example = DataAPI.DATA_EXAMPLE_VALUE)
private Object value;
```

**Après (OpenAPI 3) :**
```java
@ValidURI
@Schema(example = DataAPI.DATA_EXAMPLE_URI)
protected URI uri;

@Required
@Schema(description = "date ou datetime", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE, requiredMode = Schema.RequiredMode.REQUIRED)
private String date;

@Schema(description = "URI cible sur laquelle les données ont été collectées", example = "http://plot01")
@JsonDeserialize(using = UriJsonDeserializer.class)
private URI target;

@NotNull
@Schema(description = "URI de la variable", example = DataAPI.DATA_EXAMPLE_VARIABLEURI, requiredMode = Schema.RequiredMode.REQUIRED)
@JsonDeserialize(using = UriJsonDeserializer.class)
private URI variable;

@NotNull
@Schema(description = "peut être décimal, entier, booléen, chaîne ou date", example = DataAPI.DATA_EXAMPLE_VALUE)
private Object value;
```

### 16.2. Exemple Complet : API Resource

**Avant (Swagger 2) :**
```java
@Api("Annotations")
@Path("/core/annotations")
public class AnnotationAPI {

    @ApiOperation("Créer une annotation")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Une annotation est créée", response = URI.class),
        @ApiResponse(code = 409, message = "Déjà existante", response = ErrorResponse.class)
    })
    @POST
    public Response create(...) { ... }
}
```

**Après (OpenAPI 3) :**
```java
@Tag(name = "Annotations", description = "API de gestion des annotations")
@Path("/core/annotations")
public class AnnotationAPI {

    @Operation(summary = "Créer une annotation", description = "Crée une nouvelle annotation avec l'URI fournie")
    @ApiProtected
    @ApiCredential(credentialId = "annotation-modification", credentialLabelKey = "credential.default.modification")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Une annotation est créée",
                content = @Content(schema = @Schema(implementation = ObjectUriResponse.class))),
        @ApiResponse(responseCode = "409", description = "Une annotation avec la même URI existe déjà",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Valid AnnotationCreationDTO dto) { ... }
}
```

### 16.3. Exemple Complet : Utilisation Frontend

**Avant (Inversify Service Locator) :**
```typescript
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const uriSearchService = ref<UriSearchService>();
uriSearchService.value = $opensilex.getService("opensilex.UriSearchService");
uriSearchService.value.getUriTypes()
  .then((response: any) => {
    const result = response.result;
    console.log("Types URI:", result);
  });
```

**Après (OpenAPI SDK généré) :**
```typescript
import { getUriTypes } from "opensilex-core";

try {
  const { data, error } = await getUriTypes();
  if (error || !data) throw error;
  const result = (data as any)?.result ?? data;
  console.log("Types URI:", result);
} catch (error: any) {
  $opensilex.errorHandler(error);
}
```

---

**Fin du document**