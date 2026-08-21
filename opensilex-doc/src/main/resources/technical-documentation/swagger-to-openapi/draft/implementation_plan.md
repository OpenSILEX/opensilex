# Plan de Migration Swagger → OpenAPI 3.1 — Document Complet

**Référence** : Migration entre les branches `vue3/main` et `test/arnaud`

---

## Table des Matières

1. [Vue d'Ensemble](#1-vue-densemble)
2. [Partie Java](#2-partie-java)
   - [2.1. Annotations Java](#21-annotations-java)
   - [2.2. Infrastructure Core](#22-infrastructure-core)
   - [2.3. Configuration Maven](#23-configuration-maven)
   - [2.4. Exemples de Code](#24-exemples-de-code)
3. [Partie Frontend](#3-partie-frontend)
   - [3.1. Client HTTP et Interceptors](#31-client-http-et-interceptors)
   - [3.2. Génération TypeScript](#32-génération-typescript)
   - [3.3. Intégration Vue.js](#33-intégration-vuejs)
   - [3.4. Exemples de Code](#34-exemples-de-code)
4. [Comparaison Branches vue3/main vs test/arnaud](#4-comparaison-branches-vue3main-vs-testarnaud)
5. [Plan de Test et Vérification](#5-plan-de-test-et-vérification)

---

## 1. Vue d'Ensemble

### 1.1. Objectif de la Migration

Cette migration consiste à remplacer **Swagger 2.0** (OpenAPI 2.0) par **OpenAPI 3.1** dans le projet OpenSILEX. La branche `test/arnaud` contient l'ensemble des modifications par rapport à `vue3/main`.

**Impact global :**
- **213 fichiers** modifiés
- **+3685 lignes** ajoutées
- **-3072 lignes** supprimées
- **Tous les modules** OpenSILEX concernés

### 1.2. Architecture Cible

```
┌─────────────────────────────────────────────────────────────────┐
│                    ARCHITECTURE OPENAPI 3.1                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐     │
│  │  Modules     │    │  Annotations │    │  OpenAPI     │     │
│  │  Java        │───▶│  @Tag        │───▶│  JSON Spec   │     │
│  │  @Path       │    │  @Operation  │    │  openapi.json│     │
│  │  @Schema     │    │  @Parameter  │    │              │     │
│  └──────────────┘    └──────────────┘    └──────────────┘     │
│                                   │                            │
│                                   ▼                            │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐     │
│  │  Maven       │    │  openapi-ts  │    │  TypeScript  │     │
│  │  Build       │    │  CLI         │    │  SDK         │     │
│  │  exec-maven  │───▶│  @hey-api    │───▶│  types.gen   │     │
│  │  plugin      │    │  plugins     │    │  sdk.gen     │     │
│  └──────────────┘    └──────────────┘    └──────────────┘     │
│                                   │                            │
│                                   ▼                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Frontend Vue.js                       │   │
│  │  client.ts → interceptors → api.Security → async/await  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Partie Java

### 2.1. Annotations Java

#### 2.1.1. Mapping des Annotations

| Swagger 2.0 (vue3/main) | OpenAPI 3.1 (test/arnaud) | Usage |
|---|---|---|
| `io.swagger.annotations.Api` | `io.swagger.v3.oas.annotations.tags.Tag` | Classe controller |
| `io.swagger.annotations.ApiOperation` | `io.swagger.v3.oas.annotations.Operation` | Méthode endpoint |
| `io.swagger.annotations.ApiParam` | `io.swagger.v3.oas.annotations.Parameter` | Paramètre requête |
| `io.swagger.annotations.ApiModel` | `io.swagger.v3.oas.annotations.media.Schema` | Classe DTO |
| `io.swagger.annotations.ApiModelProperty` | `io.swagger.v3.oas.annotations.media.Schema` | Champ DTO |
| `io.swagger.annotations.ApiResponse` | `io.swagger.v3.oas.annotations.responses.ApiResponse` | Réponse HTTP |
| `io.swagger.annotations.ApiResponses` | `io.swagger.v3.oas.annotations.responses.ApiResponses` | Multiples réponses |

#### 2.1.2. Exemple : Annotation Controller

**Avant (vue3/main) - `AnnotationAPI.java` :**
```java
@Api("Annotations")
@Path("/core/annotations")
public class AnnotationAPI {

    @ApiOperation("Create an annotation")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "An annotation is created", response = URI.class),
        @ApiResponse(code = 409, message = "Already exists", response = ErrorResponse.class)
    })
    @POST
    public Response create(...) { ... }
}
```

**Après (test/arnaud) - `AnnotationAPI.java` :**
```java
@Tag(name = AnnotationAPI.CREDENTIAL_ANNOTATION_GROUP_ID)
@Path("/core/annotations")
public class AnnotationAPI {

    @Operation(summary = "Create an annotation")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "An annotation is created"),
        @ApiResponse(responseCode = "409", description = "Already exists")
    })
    @POST
    public Response create(...) { ... }
}
```

#### 2.1.3. Exemple : Annotation DTO

**Avant (vue3/main) - `DataCreationDTO.java` :**
```java
@ValidURI
@ApiModelProperty(example = DataAPI.DATA_EXAMPLE_URI)
protected URI uri;

@Required
@ApiModelProperty(value = "date or datetime", example = "2024-01-01", required = true)
private String date;
```

**Après (test/arnaud) - `DataCreationDTO.java` :**
```java
@ValidURI
@Schema(example = DataAPI.DATA_EXAMPLE_URI)
protected URI uri;

@Required
@Schema(description = "date or datetime", example = "2024-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
private String date;
```

### 2.2. Infrastructure Core

#### 2.2.1. SwaggerAPIGenerator.java

**Avant (vue3/main) :**
```java
// Swagger 2.0 - Utilisation de io.swagger.models.Swagger
public static synchronized Swagger getFullApi(Reflections reflection) {
    Swagger swagger = null;
    SwaggerContextService ctx = new SwaggerContextService();
    swagger = ctx.getSwagger();
    Map<String, Class<?>> availableAPI = OpenSilex.getAnnotatedClassesMap(Api.class, reflection);
    Set<Class<?>> classes = new HashSet<>(availableAPI.values());
    if (classes.size() > 0) {
        Reader reader = new Reader(swagger);
        swagger = reader.read(classes);
    }
    return swagger;
}
```

**Après (test/arnaud) :**
```java
// OpenAPI 3.1 - Utilisation de io.swagger.v3.oas.models.OpenAPI
public static synchronized OpenAPI getFullApi(Reflections reflection) {
    configureModelConverters();
    OpenAPI openAPI = new OpenAPI();
    openAPI.setInfo(new Info().title("OpenSilex API").version("1.0.0"));
    Map<String, Class<?>> availableAPI = OpenSilex.getAnnotatedClassesMap(Tag.class, reflection);
    Set<Class<?>> classes = new HashSet<>(availableAPI.values());
    Reader reader = new Reader(openAPI);
    if (!classes.isEmpty()) {
        openAPI = reader.read(classes);
    }
    if (openAPI.getComponents() == null) {
        openAPI.setComponents(new Components());
    }
    if (openAPI.getPaths() == null) {
        openAPI.setPaths(new io.swagger.v3.oas.models.Paths());
    }
    GeoJsonConverter.injectGeoJsonSchema(openAPI);
    return openAPI;
}
```

#### 2.2.2. RestApplication.java

**Avant (vue3/main) :**
```java
private void initSwagger() {
    BeanConfig beanConfig = new BeanConfig();
    beanConfig.setVersion(opensilex.getModuleByClass(ServerModule.class).getOpenSilexVersion());
    beanConfig.setResourcePackage(String.join(",", packageList));
    beanConfig.setTitle("OpenSilex API");
    beanConfig.setExpandSuperTypes(false);
    beanConfig.setScan(true);
}
```

**Après (test/arnaud) :**
```java
private void initOpenApi() {
    SwaggerAPIGenerator.configureModelConverters();
    List<String> packageList = new ArrayList<>();
    getAPIExtensionModules().forEach((APIExtension api) -> {
        packageList.addAll(api.apiPackages());
    });
    packageList.add("org.opensilex.server.rest.serialization");

    String version = "1.0.0";
    try {
        version = opensilex.getModuleByClass(ServerModule.class).getOpenSilexVersion();
    } catch (OpenSilexModuleNotFoundException ex) {
        LOGGER.warn("Error while getting API version", ex);
    }

    OpenAPI openAPI = new OpenAPI()
            .info(new Info().title("OpenSilex API").version(version))
            .components(new Components().addSecuritySchemes("Bearer",
                    new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")))
            .addServersItem(new io.swagger.v3.oas.models.servers.Server().url("/rest"));
    SwaggerConfiguration oasConfig = new SwaggerConfiguration()
            .openAPI(openAPI)
            .prettyPrint(true)
            .resourcePackages(new HashSet<>(packageList))
            .filterClass(RemoveRestPrefixFilter.class.getName());

    OpenApiResource openApiResource = new OpenApiResource();
    openApiResource.openApiConfiguration(oasConfig);
    register(openApiResource);
}
```

#### 2.2.3. GeoJsonConverter.java

**Avant (vue3/main) :**
```java
@SwaggerDefinition
public class GeoJsonConverter implements MessageBodyReader<GeoJsonObject>,
        MessageBodyWriter<GeoJsonObject>, ReaderListener {

    @Override
    public void afterScan(Reader reader, Swagger swagger) {
        ModelImpl geoJsonModel = new ModelImpl();
        Map<PropertyBuilder.PropertyId, Object> typeProperties = new HashMap<>();
        typeProperties.put(PropertyBuilder.PropertyId.ENUM, Arrays.asList(
                "Feature", "Polygon", "MultiPolygon", ...));
        typeProperties.put(PropertyBuilder.PropertyId.REQUIRED, true);
        Property typeProperty = PropertyBuilder.build("string", null, typeProperties);
        // ... construction du modèle
        swagger.getDefinitions().put("GeoJsonObject", geoJsonModel);
    }
}
```

**Après (test/arnaud) :**
```java
@OpenAPIDefinition
public class GeoJsonConverter implements MessageBodyReader<GeoJsonObject>,
        MessageBodyWriter<GeoJsonObject> {

    public static void injectGeoJsonSchema(OpenAPI openAPI) {
        if (openAPI == null) { return; }
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
        }
        ObjectSchema geoJsonSchema = new ObjectSchema();
        geoJsonSchema.setName("GeoJsonObject");
        StringSchema typeSchema = new StringSchema();
        typeSchema.setEnum(Arrays.asList(
                "Feature", "Polygon", "MultiPolygon", ...));
        geoJsonSchema.addProperty("type", typeSchema);
        // ... construction du schéma
        openAPI.getComponents().addSchemas("GeoJsonObject", geoJsonSchema);
    }
}
```

#### 2.2.4. Interface OpenApiExtension

**Nouvelle interface (test/arnaud) :**
```java
public interface OpenApiExtension {
    List<Class<?>> getAdditionalOpenApiDefinitions();
}
```

**Interface SwaggerExtension mise à jour (test/arnaud) :**
```java
public interface SwaggerExtension extends OpenApiExtension {
    List<Class<?>> getAdditionalSwaggerDefinitions();

    @Override
    default List<Class<?>> getAdditionalOpenApiDefinitions() {
        return getAdditionalSwaggerDefinitions();
    }
}
```

### 2.3. Configuration Maven

#### 2.3.1. opensilex-parent/pom.xml

**Avant (vue3/main) :**
```xml
<swagger.jersey2.jaxrs.version>1.6.16</swagger.jersey2.jaxrs.version>

<dependency>
    <groupId>io.swagger</groupId>
    <artifactId>swagger-jersey2-jaxrs</artifactId>
    <version>${swagger.jersey2.jaxrs.version}</version>
    <exclusions>
        <exclusion>
            <groupId>javax.validation</groupId>
            <artifactId>validation-api</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- Plugin swagger-codegen maison -->
<plugin>
    <groupId>org.opensilex</groupId>
    <artifactId>opensilex-swagger-codegen-maven-plugin</artifactId>
    <version>${revision}</version>
    <configuration>
        <inputSpec>${project.basedir}/front/src/lib/swagger.json</inputSpec>
        <templateDirectory>${project.basedir}/../opensilex-main/src/main/resources/swagger/templates/typescript-inversify</templateDirectory>
        <language>typescript-inversify</language>
        <output>${project.basedir}/front/src/lib/</output>
    </configuration>
</plugin>
```

**Après (test/arnaud) :**
```xml
<swagger.v3.version>2.2.53</swagger.v3.version>
<openapi.generator.version>7.11.0</openapi.generator.version>

<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-jaxrs2</artifactId>
    <version>${swagger.v3.version}</version>
</dependency>
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-annotations</artifactId>
    <version>${swagger.v3.version}</version>
</dependency>

<!-- Plugin openapi-generator officiel -->
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version>${openapi.generator.version}</version>
    <configuration>
        <skip>${skipTypeScriptLibBuild}</skip>
    </configuration>
    <executions>
        <execution>
            <phase>compile</phase>
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/front/src/lib/openapi.json</inputSpec>
                <generatorName>typescript-inversify</generatorName>
                <output>${project.basedir}/front/src/lib/</output>
                <skipIfSpecIsUnchanged>true</skipIfSpecIsUnchanged>
                <skipValidateSpec>true</skipValidateSpec>
                <configOptions>
                    <packageName>${project.name}</packageName>
                    <packageVersion>${revision}</packageVersion>
                    <npmName>${project.name}</npmName>
                    <usePromise>true</usePromise>
                    <supportsES6>true</supportsES6>
                    <modelPropertyNaming>original</modelPropertyNaming>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

#### 2.3.2. exec-maven-plugin

**Avant (vue3/main) :**
```xml
<execution>
    <id>generate-swagger-spec</id>
    <phase>compile</phase>
    <goals><goal>java</goal></goals>
    <configuration>
        <mainClass>org.opensilex.utils.SwaggerAPIGenerator</mainClass>
        <arguments>
            <argument>${project.basedir}/src/main/java/</argument>
            <argument>${project.basedir}/front/src/lib/swagger.json</argument>
        </arguments>
    </configuration>
</execution>
```

**Après (test/arnaud) :**
```xml
<execution>
    <id>generate-openapi-spec</id>
    <phase>compile</phase>
    <goals><goal>java</goal></goals>
    <configuration>
        <mainClass>org.opensilex.utils.SwaggerAPIGenerator</mainClass>
        <arguments>
            <argument>${project.basedir}/src/main/java/</argument>
            <argument>${project.basedir}/front/src/lib/openapi.json</argument>
        </arguments>
    </configuration>
</execution>
```

### 2.4. Exemples de Code

#### 2.4.1. Annotation ApiProtected

**Avant (vue3/main) - `ApiProtected.java` :**
```java
@io.swagger.annotations.ApiImplicitParam(
    name = "Authorization",
    value = "Bearer token",
    required = true,
    paramType = "header"
)
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiProtected { ... }
```

**Après (test/arnaud) - `ApiProtected.java` :**
```java
@SecurityRequirement(name = "Bearer")
@Parameter(name = HttpHeaders.ACCEPT_LANGUAGE,
        schema = @Schema(type = "string"),
        in = ParameterIn.HEADER,
        description = "Request accepted language",
        example = OpenSilex.DEFAULT_LANGUAGE)
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiProtected { ... }
```

#### 2.4.2. Exemple Complet DTO

**Avant (vue3/main) - `DataCreationDTO.java` :**
```java
@ValidURI
@ApiModelProperty(example = DataAPI.DATA_EXAMPLE_URI)
protected URI uri;

@Required
@ApiModelProperty(value = "date or datetime", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE, required = true)
private String date;

@ApiModelProperty(value = "target URI on which the data have been collected (e.g. a scientific object)", example = "http://plot01")
@JsonDeserialize(using = UriJsonDeserializer.class)
private URI target;

@NotNull
@ApiModelProperty(value = "variable URI", example = DataAPI.DATA_EXAMPLE_VARIABLEURI, required = true)
@JsonDeserialize(using = UriJsonDeserializer.class)
private URI variable;
```

**Après (test/arnaud) - `DataCreationDTO.java` :**
```java
@ValidURI
@Schema(example = DataAPI.DATA_EXAMPLE_URI)
protected URI uri;

@Required
@Schema(description = "date or datetime", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE, requiredMode = Schema.RequiredMode.REQUIRED)
private String date;

@Schema(description = "target URI on which the data have been collected (e.g. a scientific object)", example = "http://plot01")
@JsonDeserialize(using = UriJsonDeserializer.class)
private URI target;

@NotNull
@Schema(description = "variable URI", example = DataAPI.DATA_EXAMPLE_VARIABLEURI, requiredMode = Schema.RequiredMode.REQUIRED)
@JsonDeserialize(using = UriJsonDeserializer.class)
private URI variable;
```

---

## 3. Partie Frontend

### 3.1. Client HTTP et Interceptors

#### 3.1.1. Nouvelle Architecture Client

**Avant (vue3/main) :**
- Utilisation d'`openapi-fetch` pour les appels HTTP
- Pas d'interceptors centralisés
- Pas de typage fort des réponses

**Après (test/arnaud) :**
- Utilisation de `@hey-api/client-fetch`
- Interceptors centralisés dans `client.ts`
- Typage fort via `OpenSilexResponse<T>`

#### 3.1.2. Configuration openapi-ts.config.ts

**Nouveau fichier (test/arnaud) :**
```typescript
import { defineConfig } from '@hey-api/openapi-ts';
import { resolve } from 'path';

export default defineConfig({
  input: resolve(__dirname, 'opensilex-front/front/src/lib/openapi.json'),
  output: resolve(__dirname, 'opensilex-front/front/src/lib/generated'),
  plugins: [
    '@hey-api/client-fetch',
    '@hey-api/typescript',
    '@hey-api/sdk'
  ]
});
```

#### 3.1.3. Client Interceptors

**Nouveau fichier (test/arnaud) - `client.ts` :**
```typescript
import { createClient } from "@hey-api/client-fetch";
import { OpenSilexResponse, MetadataDTO } from "../models/HttpResponse";

/**
 * Résolution de l'URL de base API selon l'environnement.
 */
export function getBaseApi(): string {
  if (typeof window !== "undefined" && window.location) {
    if (import.meta.env.DEV) {
      return "http://localhost:8666/rest";
    }
    const splitURI = window.location.href.split("/app");
    return splitURI[0] + "/rest";
  }
  return "http://localhost:8666/rest";
}

/**
 * Résolution du token d'authentification depuis localStorage ou cookies.
 */
export function getAuthToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  const localToken = localStorage.getItem("opensilex_token");
  if (localToken) {
    return localToken;
  }
  if (document.cookie) {
    const cookies = document.cookie.split(";");
    for (const c of cookies) {
      const [key, val] = c.trim().split("=");
      if (key && key.startsWith("opensilex-token") && val) {
        return decodeURIComponent(val);
      }
    }
  }
  return null;
}

/**
 * Enregistrement dynamique du client avec interceptors.
 */
export function registerOpenSilexClient<T extends Client = Client>(clientToRegister: T): T {
  if (!clientToRegister || registeredClients.has(clientToRegister)) {
    return clientToRegister;
  }

  clientToRegister.setConfig({
    baseUrl: getBaseApi()
  });

  // Interceptor de requête : ajout du token et de la langue
  clientToRegister.interceptors.request.use(async (request, options) => {
    const baseApi = getBaseApi();
    if (request.url && request.url.startsWith("/")) {
      const base = baseApi.endsWith("/") ? baseApi.slice(0, -1) : baseApi;
      request.url = `${base}${request.url}`;
    }

    const token = getAuthToken();
    if (token) {
      request.headers.set("Authorization", `Bearer ${token}`);
    }
    if (!request.headers.has("Accept-Language")) {
      request.headers.set("Accept-Language", "fr");
    }

    // Log debug si activé
    if (isDebugApiEnabled()) {
      // ... logging détaillé
    }

    return request;
  });

  // Interceptor de réponse : gestion des erreurs et 401
  clientToRegister.interceptors.response.use(async (response, request) => {
    // Log debug si activé
    if (isDebugApiEnabled()) {
      // ... logging détaillé
    }

    if (response.status === 401) {
      localStorage.removeItem("opensilex_token");
      window.dispatchEvent(new CustomEvent("opensilex:unauthorized"));
    }

    if (!response.ok) {
      let errorPayload: any = {};
      try {
        errorPayload = await response.clone().json();
      } catch {
        errorPayload = { title: response.statusText };
      }

      const errorMessage =
        errorPayload?.message ||
        errorPayload?.result?.message ||
        errorPayload?.title ||
        `HTTP Error ${response.status}: ${response.statusText}`;

      throw new OpenSilexResponseError(errorMessage, response.status, errorPayload);
    }

    return response;
  });

  registeredClients.add(clientToRegister);
  return clientToRegister;
}
```

### 3.2. Génération TypeScript

#### 3.2.1. Fichiers Générés

**Après (test/arnaud) - Structure des fichiers générés :**

```
front/src/lib/generated/
├── types.gen.ts      # Interfaces TypeScript strictes pour DTOs
├── sdk.gen.ts        # Fonctions SDK typées (authenticate, renewToken, etc.)
├── client.gen.ts     # Client HTTP de base @hey-api/client-fetch
└── index.ts          # Barrel file ré-exportant tout
```

#### 3.2.2. Configuration Vite

**Après (test/arnaud) - `vite.config.ts` :**
```typescript
import { heyApiPlugin } from '@hey-api/vite-plugin';

export default defineConfig({
  plugins: [
    vue(),
    heyApiPlugin({
      input: resolve(__dirname, 'front/src/lib/openapi.json'),
      output: resolve(__dirname, 'front/src/lib/generated'),
      plugins: [
        '@hey-api/client-fetch',
        '@hey-api/typescript',
        '@hey-api/sdk'
      ]
    })
  ],
  resolve: {
    alias: {
      'opensilex-security': resolve(__dirname, '../../opensilex-security/front/src/index.ts'),
      'opensilex-core': resolve(__dirname, '../../opensilex-core/front/src/index.ts')
    }
  }
});
```

### 3.3. Intégration Vue.js

#### 3.3.1. Utilisation dans les Composants

**Avant (vue3/main) - Pattern Service Locator InversifyJS :**
```typescript
// DefaultLoginComponent.vue
import type {AuthenticationService, TokenGetDTO} from "opensilex-security/index";
import type {OpenSilexResponse} from "opensilex-security/HttpResponse";

const onLogin = async () => {
  $opensilex.showLoader();
  try {
    // Récupération du service via le service locator InversifyJS
    const authService = $opensilex.getService<AuthenticationService>(
      "opensilex-security.AuthenticationService"
    );

    // Appel de la méthode du service
    const response: HttpResponse<OpenSilexResponse<TokenGetDTO>> =
      await authService.authenticate({
        identifier: form.value.email,
        password: form.value.password
      });

    const user = User.fromToken(response.response.result.token);
    $opensilex.setCookieValue(user);
    store.commit("login", user);
    store.commit("refresh");
  } catch (error: any) {
    if (error.status === 403) {
      $opensilex.errorHandler(error, t("LoginComponent.invalidCredentials"));
    } else {
      $opensilex.errorHandler(error);
    }
  } finally {
    $opensilex.hideLoader();
  }
};
```

**Après (test/arnaud) - Pattern SDK Typé avec async/await :**
```typescript
// Utilisation directe du SDK généré
import { authenticate } from 'opensilex-security';
import { OpenSilexResponse } from "@/models/HttpResponse";

try {
  const response: OpenSilexResponse<TokenDTO> = await authenticate({
    identifier: form.value.email,
    password: form.value.password
  });

  const userToken: TokenDTO = response.result;
  const pagination = response.metadata.pagination;
} catch (error: any) {
  $opensilex.errorHandler(error);
}

// Ou via le namespace typed api
const { data, error } = await api.GET("/vuejs/config");
```

#### 3.3.2. Configuration main.ts

**Après (test/arnaud) - `main.ts` :**
```typescript
import { createApp } from 'vue';
import { registerOpenSilexClient } from './api/client';
import { client as coreClient } from '../../opensilex-core/front/src/lib/generated/client.gen';
import { client as securityClient } from '../../opensilex-security/front/src/lib/generated/client.gen';

// Enregistrement des clients
registerOpenSilexClient(coreClient);
registerOpenSilexClient(securityClient);

const app = createApp(App);
app.use(store);
app.mount('#app');
```

### 3.4. Exemples de Code

#### 3.4.1. Exemple Complet : Authentification (DefaultLoginComponent.vue)

**Avant (vue3/main) - Utilisation du Service Locator InversifyJS :**
```typescript
// DefaultLoginComponent.vue
import type {AuthenticationService, TokenGetDTO} from "opensilex-security/index";
import type {OpenSilexResponse} from "opensilex-security/HttpResponse";
import HttpResponse from "opensilex-security/HttpResponse";

const login = async () => {
  $opensilex.showLoader();
  try {
    // Récupération du service via le service locator InversifyJS
    const authService = $opensilex.getService<AuthenticationService>(
      "opensilex-security.AuthenticationService"
    );

    // Appel de la méthode du service
    const response: HttpResponse<OpenSilexResponse<TokenGetDTO>> =
      await authService.authenticate({
        identifier: form.value.email,
        password: form.value.password,
      });

    const user = User.fromToken(response.response.result.token);
    $opensilex.setCookieValue(user);
    store.commit("login", user);
    store.commit("refresh");
  } catch (error: any) {
    if (error.status === 403) {
      $opensilex.errorHandler(error, t("LoginComponent.invalidCredentials"));
    } else {
      $opensilex.errorHandler(error);
    }
  } finally {
    $opensilex.hideLoader();
  }
};
```

**Après (test/arnaud) - Utilisation du SDK Généré @hey-api/client-fetch :**
```typescript
// Utilisation directe du SDK généré
import { authenticate } from 'opensilex-security';
import { OpenSilexResponse } from "@/models/HttpResponse";

try {
  const response: OpenSilexResponse<TokenDTO> = await authenticate({
    identifier: form.value.email,
    password: form.value.password
  });

  const userToken: TokenDTO = response.result;
  const pagination = response.metadata.pagination;
} catch (error: any) {
  $opensilex.errorHandler(error);
}
```

#### 3.4.2. Exemple Complet : Migration vers le Pattern SDK Typé

**Avant (vue3/main) - Pattern Service Locator InversifyJS :**
```typescript
// Récupération du service via le service locator
const uriSearchService = $opensilex.getService<UriSearchService>("opensilex.UriSearchService");

// Appel asynchrone avec Promise
uriSearchService.value.getUriTypes()
  .then((response: any) => {
    const result = response.result;
    console.log("URI Types:", result);
  })
  .catch((error: any) => {
    $opensilex.errorHandler(error);
  });
```

**Après (test/arnaud) - Pattern SDK Typé avec async/await :**
```typescript
// Import direct des fonctions SDK typées
import { getUriTypes, searchCategories } from "opensilex-core";

try {
  const { data, error } = await getUriTypes();
  if (error || !data) {
    throw error;
  }
  const result = (data as any)?.result ?? data;
  console.log("URI Types:", result);
} catch (error: any) {
  $opensilex.errorHandler(error);
}

// Ou via le namespace typed api
const { data, error } = await api.GET("/vuejs/config");
```

#### 3.4.3. Méthodes Helper Backward-Compatible

**Après (test/arnaud) - `client.ts` :**
```typescript
export const api = {
  // Instance @hey-api/client-fetch sous-jacente
  fetchClient,

  // Méthodes HTTP Helper pour la rétrocompatibilité
  async GET<T = any>(url: string, options?: any): Promise<{ data?: T; error?: any }> {
    try {
      const response = await fetchClient.get({ url, ...options });
      return { data: response.data as T };
    } catch (error) {
      return { error };
    }
  },

  async POST<T = any>(url: string, options?: any): Promise<{ data?: T; error?: any }> {
    try {
      const response = await fetchClient.post({ url, ...options });
      return { data: response.data as T };
    } catch (error) {
      return { error };
    }
  },

  async PUT<T = any>(url: string, options?: any): Promise<{ data?: T; error?: any }> {
    try {
      const response = await fetchClient.put({ url, ...options });
      return { data: response.data as T };
    } catch (error) {
      return { error };
    }
  },

  async DELETE<T = any>(url: string, options?: any): Promise<{ data?: T; error?: any }> {
    try {
      const response = await fetchClient.delete({ url, ...options });
      return { data: response.data as T };
    } catch (error) {
      return { error };
    }
  },

  // Noms de services SDK typés
  Security: {
    async authenticate(auth: { identifier?: string; password?: string; [key: string]: any }): Promise<OpenSilexResponse<any>> {
      const response = await fetchClient.post({
        url: "/security/authenticate",
        body: auth
      });
      const data: any = response.data;
      const result = data?.result ?? data;
      const metadata = data?.metadata ?? new MetadataDTO(null as any, [], []);
      return new OpenSilexResponse(result, metadata);
    }
  }
};
```

---

## 4. Comparaison Branches vue3/main vs test/arnaud

### 4.1. Fichiers Modifiés

| Catégorie | vue3/main | test/arnaud | Changement |
|---|---|---|---|
| **Annotations Java** | `@Api`, `@ApiOperation`, `@ApiModel` | `@Tag`, `@Operation`, `@Schema` | Migration complète |
| **Dépendances Maven** | `io.swagger:swagger-jersey2-jaxrs:1.6.16` | `io.swagger.core.v3:swagger-jaxrs2:2.2.53` | Upgrade majeur |
| **Plugin TypeScript** | `opensilex-swagger-codegen-maven-plugin` | `openapi-generator-maven-plugin:7.11.0` | Remplacement |
| **Client HTTP** | `openapi-fetch` | `@hey-api/client-fetch` | Remplacement |
| **Fichier Spec** | `swagger.json` | `openapi.json` | Renommage |
| **Interface Extension** | `SwaggerExtension` | `OpenApiExtension` + `SwaggerExtension extends OpenApiExtension` | Extension |

### 4.2. Modules Concernés

**vue3/main → test/arnaud :**

| Module | Fichiers Modifiés | Type de Changement |
|---|---|---|
| `opensilex-core` | 141+ fichiers | Annotations + DTOs |
| `opensilex-security` | 34 fichiers | Annotations + DTOs |
| `opensilex-brapi` | 4 fichiers | Annotations |
| `opensilex-faidare` | 6 fichiers | Annotations |
| `opensilex-front` | 100+ fichiers | Client HTTP + Intégration |
| `opensilex-main` | 5 fichiers | Infrastructure Core |
| `opensilex-parent` | 1 fichier | Dépendances Maven |

### 4.3. Impact sur les Tests

**vue3/main :**
- Tests unitaires Java existants
- Tests frontend basés sur `openapi-fetch`

**test/arnaud :**
- Nouveau test `OpenApiValidationTest.java`
- Nouveau test `client.spec.ts` pour les interceptors
- Migration des tests existants vers le nouveau pattern

---

## 5. Plan de Test et Vérification

### 5.1. Tests Backend Java

#### 5.1.1. OpenApiValidationTest.java

**Nouveau fichier (test/arnaud) :**
```java
package org.opensilex.unit.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.Test;
import org.opensilex.unit.test.AbstractUnitTest;
import org.opensilex.utils.SwaggerAPIGenerator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static org.junit.Assert.*;

public class OpenApiValidationTest extends AbstractUnitTest {

    @Tag(name = "TestAPI", description = "Test API for OpenAPI validation")
    @Path("/test")
    public static class TestEndpoint {
        @GET
        @Operation(summary = "Test method")
        @Produces(MediaType.APPLICATION_JSON)
        public String getTest() { return "ok"; }
    }

    @Test
    public void testOpenApiSpecGeneration() throws Exception {
        OpenAPI openAPI = SwaggerAPIGenerator.getFullApi(opensilex.getReflections());
        assertNotNull("Generated OpenAPI object should not be null", openAPI);
        assertNotNull("OpenAPI paths should not be null", openAPI.getPaths());
        assertNotNull("OpenAPI components schemas should not be null",
                      openAPI.getComponents().getSchemas());
    }

    @Test
    public void testAllRestEndpointsHaveOpenApiAnnotations() {
        var pathClasses = opensilex.getReflections().getTypesAnnotatedWith(Path.class);
        for (Class<?> clazz : pathClasses) {
            assertTrue("Class " + clazz.getName() + " should be annotated with @Tag",
                       clazz.isAnnotationPresent(Tag.class));
        }
    }

    @Test
    public void testRestApplicationInitOpenApi() throws Exception {
        org.opensilex.server.rest.RestApplication app = new org.opensilex.server.rest.RestApplication(opensilex);
        assertNotNull("RestApplication instance should not be null", app);
    }
}
```

### 5.2. Tests Frontend TypeScript

#### 5.2.1. client.spec.ts

**Nouveau fichier (test/arnaud) :**
```typescript
// @vitest-environment happy-dom
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { api, fetchClient, getBaseApi, OpenSilexResponseError, isDebugApiEnabled, setApiDebug } from "./client";
import { OpenSilexResponse, MetadataDTO } from "../models/HttpResponse";

describe("Client API OpenSILEX (@hey-api/client-fetch)", () => {
  beforeEach(() => {
    localStorage.clear();
    setApiDebug(null);
    vi.clearAllMocks();
  });

  afterEach(() => { setApiDebug(null); });

  it("doit retourner une URL de base API valide", () => {
    const url = getBaseApi();
    expect(url).toContain("/rest");
  });

  it("doit instancier OpenSilexResponseError correctement", () => {
    const err = new OpenSilexResponseError("Non autorisé", 401, { title: "Erreur" });
    expect(err.name).toBe("OpenSilexResponseError");
    expect(err.status).toBe(401);
    expect(err.message).toBe("Non autorisé");
    expect(err.details).toEqual({ title: "Erreur" });
  });

  it("doit gérer correctement l'état du mode debug", () => {
    setApiDebug(true);
    expect(isDebugApiEnabled()).toBe(true);
    setApiDebug(false);
    expect(isDebugApiEnabled()).toBe(false);
    setApiDebug(null);
  });

  it("doit afficher des logs console groupés en mode debug", async () => {
    setApiDebug(true);
    const groupCollapsedSpy = vi.spyOn(console, "groupCollapsed").mockImplementation(() => {});
    const groupEndSpy = vi.spyOn(console, "groupEnd").mockImplementation(() => {});
    const logSpy = vi.spyOn(console, "log").mockImplementation(() => {});

    const mockGet = vi.spyOn(fetchClient, "get").mockResolvedValue({ data: { result: "ok" } } as any);
    await api.GET("/vuejs/config");
    expect(mockGet).toHaveBeenCalledWith({ url: "/vuejs/config" });
    setApiDebug(false);
  });

  it("doit exposer la méthode typée Security.authenticate", async () => {
    const mockPost = vi.spyOn(fetchClient, "post").mockResolvedValue({
      data: { result: { token: "jwt_token_12345" }, metadata: { status: [], pagination: null } }
    } as any);

    const response: OpenSilexResponse<any> = await api.Security.authenticate({
      identifier: "admin@opensilex.org",
      password: "password"
    });

    expect(mockPost).toHaveBeenCalledWith({
      url: "/security/authenticate",
      body: { identifier: "admin@opensilex.org", password: "password" }
    });
    expect(response).toBeInstanceOf(OpenSilexResponse);
    expect(response.result).toEqual({ token: "jwt_token_12345" });
  });

  it("doit fournir les méthodes backward-compatible GET, POST, PUT, DELETE", async () => {
    const mockGet = vi.spyOn(fetchClient, "get").mockResolvedValue({ data: { result: "ok" } } as any);
    const { data } = await api.GET("/vuejs/config");
    expect(mockGet).toHaveBeenCalledWith({ url: "/vuejs/config" });
    expect(data).toEqual({ result: "ok" });
  });
});
```

### 5.3. Commandes de Vérification

```bash
# 1. Exécuter les tests Java
mvn test -pl opensilex-main -Dtest=OpenApiValidationTest

# 2. Build complet sans tests
mvn clean install -DskipTests -DskipFrontBuild=true

# 3. Vérifier la génération du spec OpenAPI
ls -la opensilex-core/front/src/lib/openapi.json
ls -la opensilex-security/front/src/lib/openapi.json
ls -la opensilex-front/front/src/lib/openapi.json

# 4. Vérifier la génération TypeScript
ls opensilex-core/front/src/lib/generated/types.gen.ts
ls opensilex-core/front/src/lib/generated/sdk.gen.ts
ls opensilex-core/front/src/lib/generated/client.gen.ts

# 5. Exécuter les tests frontend
cd opensilex-front/front && npm test
```

### 5.4. Vérification Manuelle

1. **Accéder à `/rest/openapi.json`** sur un serveur en cours d'exécution
2. **Accéder à `/api-docs`** pour vérifier Swagger UI interactive
3. **Tester le flux de connexion** dans `DefaultLoginComponent.vue` en mode dev
4. **Vérifier la génération TypeScript** dans le frontend Vue.js

---

## 6. Checklist de Migration

### 6.1. Backend Java

- [ ] Remplacer tous les imports Swagger 2 par OpenAPI 3
- [ ] Mettre à jour les annotations `@ApiResponses`
- [ ] Mettre à jour les annotations `@Parameter`
- [ ] Mettre à jour les DTOs avec `@Schema`
- [ ] Mettre à jour `SwaggerAPIGenerator.java`
- [ ] Mettre à jour `RestApplication.java`
- [ ] Mettre à jour `GeoJsonConverter.java`
- [ ] Mettre à jour les interfaces d'extension

### 6.2. Build Maven

- [ ] Mettre à jour `opensilex-parent/pom.xml`
- [ ] Mettre à jour les POMs des modules
- [ ] Mettre à jour `openapi-ts.config.ts`

### 6.3. Frontend TypeScript

- [ ] Mettre à jour `client.ts`
- [ ] Mettre à jour les imports dans les composants
- [ ] Mettre à jour les tests frontend

### 6.4. Tests

- [ ] Exécuter les tests Java
- [ ] Exécuter les tests TypeScript
- [ ] Vérifier la génération du spec
- [ ] Vérifier la génération TypeScript

---

**Fin du document**