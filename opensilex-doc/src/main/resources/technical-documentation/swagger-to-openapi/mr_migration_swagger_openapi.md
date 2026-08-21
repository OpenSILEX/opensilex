---
changelog: # Migration Swagger 2.0 → OpenAPI 3.1 avec génération de clients TypeScript via @hey-api
ignore-changelog: false
---

- [ ] Relecture MR
- [ ] Tests écrits et OK
- [ ] Documentation technique
- [ ] Specifications fonctionnelles validées par l'exploitation
- [ ] Testé
- [ ] Remplir l'entrée changelog ou la marquer comme ignorée
- [ ] Les composants Vue suivent les bonnes pratiques

# Contexte

Cette Merge Request réalise la migration complète de **Swagger 2.0** (OpenAPI 2.0) vers **OpenAPI 3.1** dans le projet OpenSILEX. La migration concerne l'ensemble du backend Java, le pipeline de build Maven, et le frontend TypeScript/Vue.js.

**Objectifs de la migration :**
- Passer de la spécification Swagger 2.0 à OpenAPI 3.1 (norme W3C actuelle)
- Remplacer le plugin de génération TypeScript maison par le plugin officiel `openapi-generator-maven-plugin`
- Migrer le client HTTP frontend de `openapi-fetch` vers `@hey-api/client-fetch` avec génération automatique de code SDK typé
- Améliorer le typage fort des réponses API via `OpenSilexResponse<T>`
- Centraliser la gestion des intercepteurs (authentification JWT, langue, gestion des erreurs)

**Impact :**
- **213 fichiers** modifiés
- **+3685 lignes** ajoutées
- **-3072 lignes** supprimées
- **Tous les modules** OpenSILEX concernés

[Carte trello](https://trello.com/)


## Contexte technique détaillé

La migration s'articule autour de 3 axes principaux :

1. **Backend Java** : Remplacement des annotations Swagger 2 (`@Api`, `@ApiOperation`, `@ApiModel`, `@ApiModelProperty`) par les annotations OpenAPI 3 (`@Tag`, `@Operation`, `@Schema`)
2. **Build Maven** : Remplacement du plugin `opensilex-swagger-codegen-maven-plugin` (maison) par `openapi-generator-maven-plugin` (officiel) avec génération de clients TypeScript via `@hey-api/openapi-ts`
3. **Frontend Vue.js** : Migration du client HTTP vers `@hey-api/client-fetch` avec interceptors centralisés et typage fort des réponses


# Changements

## Backend Java

### `opensilex-parent/pom.xml`
Migration des dépendances et plugins Maven :
- **Dépendances** : Remplacement de `io.swagger:swagger-jersey2-jaxrs:1.6.16` par `io.swagger.core.v3:swagger-jaxrs2:2.2.53` et `io.swagger.core.v3:swagger-annotations:2.2.53`
- **Plugin de génération** : Remplacement de `opensilex-swagger-codegen-maven-plugin` (maison) par `org.openapitools:openapi-generator-maven-plugin:7.11.0`
- **Configuration** : Passage de `swagger.json` à `openapi.json`, ajout des plugins `@hey-api/client-fetch`, `@hey-api/typescript`, `@hey-api/sdk`

### `opensilex-main/src/main/java/org/opensilex/utils/SwaggerAPIGenerator.java`
Réécriture complète du générateur de spécification OpenAPI :
- Remplacement de `io.swagger.models.Swagger` par `io.swagger.v3.oas.models.OpenAPI`
- Ajout de `JenaAnnotationIntrospector` et `JenaModelConverter` pour gérer les conflits de réflexion Jackson sur les classes Apache Jena RDF
- Implémentation du fallback `Reflections` pour les builds Maven headless
- Sérialisation JSON formatée avec `Json.pretty()` et découverte automatique des modules Jackson

### `opensilex-main/src/main/java/org/opensilex/server/rest/RestApplication.java`
Mise à jour de l'initialisation OpenAPI dans l'application Tomcat :
- Remplacement de `BeanConfig` par `OpenApiResource` et `SwaggerConfiguration`
- Configuration du `SecurityScheme` Bearer JWT pour l'authentification
- Enregistrement du filtre `RemoveRestPrefixFilter`
- Passage de `initSwagger()` à `initOpenApi()`

### `opensilex-main/src/main/java/org/opensilex/server/rest/serialization/GeoJsonConverter.java`
Migration du convertisseur GeoJSON vers OpenAPI 3.1 :
- Remplacement de `@SwaggerDefinition` par `@OpenAPIDefinition`
- Suppression de l'interface `ReaderListener` (avantScan/afterScan)
- Nouvelle méthode statique `injectGeoJsonSchema(OpenAPI openAPI)` pour injecter les schémas GeoJSON dans `components/schemas`
- Utilisation de `ObjectSchema`, `StringSchema`, `ArraySchema` au lieu de `ModelImpl`, `PropertyBuilder`

### `opensilex-main/src/main/java/org/opensilex/OpenApiExtension.java` (NOUVEAU)
Nouvelle interface pour l'enregistrement de DTOs supplémentaires :
```java
public interface OpenApiExtension {
    List<Class<?>> getAdditionalOpenApiDefinitions();
}
```

### `opensilex-main/src/main/java/org/opensilex/SwaggerExtension.java`
Rétrocompatibilité avec l'ancienne interface :
```java
public interface SwaggerExtension extends OpenApiExtension {
    List<Class<?>> getAdditionalSwaggerDefinitions();

    @Override
    default List<Class<?>> getAdditionalOpenApiDefinitions() {
        return getAdditionalSwaggerDefinitions();
    }
}
```

### `opensilex-security/src/main/java/org/opensilex/security/authentication/ApiProtected.java`
Mise à jour des méta-annotations de sécurité :
- Ajout de `@SecurityRequirement(name = "Bearer")` pour l'authentification JWT
- Remplacement de `@ApiImplicitParam` par `@Parameter` avec `Accept-Language`
- Migration des imports vers `io.swagger.v3.oas.annotations`

### `opensilex-security/src/main/java/org/opensilex/security/authentication/ApiTranslatable.java`
Migration vers les annotations OpenAPI 3 pour les réponses traductibles :
- Utilisation de `@Parameters` avec `@Parameter` pour `Accept-Language`

### Migration des annotations dans tous les modules

**Fichiers API (Controllers) :**
- `opensilex-core/src/main/java/org/opensilex/core/*/api/*API.java` (141 fichiers)
- `opensilex-security/src/main/java/org/opensilex/security/*/api/*API.java` (34 fichiers)
- `opensilex-brapi/src/main/java/org/opensilex/brapi/api/*API.java` (4 fichiers)
- `opensilex-faidare/src/main/java/org/opensilex/faidare/api/*API.java` (6 fichiers)

**Migration des annotations :**
| Swagger 2.0 | OpenAPI 3.1 |
|---|---|
| `@Api(value = "...")` | `@Tag(name = "...")` |
| `@ApiOperation(value = "...")` | `@Operation(summary = "...")` |
| `@ApiParam(value = "...")` | `@Parameter(description = "...")` |
| `@ApiResponses(value = {...})` | `@ApiResponses(value = {...})` |
| `@ApiResponse(code = 201, message = "...")` | `@ApiResponse(responseCode = "201", description = "...")` |

**Fichiers DTO (Data Transfer Objects) :**
- `opensilex-core/src/main/java/org/opensilex/core/*/api/*DTO.java` (141 fichiers)
- `opensilex-security/src/main/java/org/opensilex/security/*/api/*DTO.java` (34 fichiers)
- `opensilex-brapi/src/main/java/org/opensilex/brapi/model/*DTO.java` (4 fichiers)

**Migration des annotations DTO :**
| Swagger 2.0 | OpenAPI 3.1 |
|---|---|
| `@ApiModel` | `@Schema` |
| `@ApiModelProperty(value = "...", required = true)` | `@Schema(description = "...", requiredMode = Schema.RequiredMode.REQUIRED)` |

### `opensilex-main/src/test/java/org/opensilex/unit/rest/OpenApiValidationTest.java` (NOUVEAU)
Nouveau test unitaire pour valider la migration :
```java
@Test
public void testOpenApiSpecGeneration() throws Exception {
    OpenAPI openAPI = SwaggerAPIGenerator.getFullApi(opensilex.getReflections());
    assertNotNull("Generated OpenAPI object should not be null", openAPI);
    assertNotNull("OpenAPI paths should not be null", openAPI.getPaths());
    assertNotNull("OpenAPI components schemas should not be null", openAPI.getComponents().getSchemas());
}

@Test
public void testAllRestEndpointsHaveOpenApiAnnotations() {
    var pathClasses = opensilex.getReflections().getTypesAnnotatedWith(Path.class);
    for (Class<?> clazz : pathClasses) {
        assertTrue("Class " + clazz.getName() + " should be annotated with @Tag",
                   clazz.isAnnotationPresent(Tag.class));
    }
}
```


## Frontend TypeScript/Vue.js

### `openapi-ts.config.ts` (NOUVEAU)
Configuration de génération des clients TypeScript via `@hey-api/openapi-ts` :
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

### `opensilex-front/front/src/api/client.ts` (NOUVEAU)
Nouveau client HTTP centralisé avec interceptors :
- **Gestion de l'URL de base** : `getBaseApi()` pour la résolution dynamique (dev/production)
- **Gestion du token** : `getAuthToken()` depuis localStorage ou cookies
- **Intercepteur de requête** :
  - Ajout du `baseUrl`
  - Ajout du header `Authorization: Bearer <token>`
  - Ajout du header `Accept-Language: fr`
  - Logging debug conditionnel
- **Intercepteur de réponse** :
  - Logging debug conditionnel
  - Gestion du 401 Unauthorized (clear storage + event)
  - Transformation des erreurs en `OpenSilexResponseError`
- **Enregistrement des clients** : `registerOpenSilexClient()` pour les clients générés par module
- **API backward-compatible** : `api.GET()`, `api.POST()`, `api.PUT()`, `api.DELETE()`
- **Namespace typed** : `api.Security.authenticate()`

### `opensilex-front/front/src/api/client.spec.ts` (NOUVEAU)
Tests unitaires pour le client HTTP :
```typescript
describe("Client API OpenSILEX (@hey-api/client-fetch)", () => {
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
});
```

### `opensilex-front/front/src/components/layout/DefaultLoginComponent.vue`
Migration de l'authentification vers le nouveau pattern SDK :

**Avant (vue3/main) - Service Locator InversifyJS :**
```typescript
const authService = $opensilex.getService<AuthenticationService>(
  "opensilex-security.AuthenticationService"
);
const response: HttpResponse<OpenSilexResponse<TokenGetDTO>> =
  await authService.authenticate({
    identifier: form.value.email,
    password: form.value.password
  });
const user = User.fromToken(response.response.result.token);
```

**Après (test/arnaud) - SDK Typé :**
```typescript
import { authenticate } from 'opensilex-security';
import { OpenSilexResponse } from "@/models/HttpResponse";

const response: OpenSilexResponse<TokenDTO> = await authenticate({
  identifier: form.value.email,
  password: form.value.password
});
const userToken: TokenDTO = response.result;
```

### `opensilex-front/front/src/main.ts`
Configuration de l'application Vue.js avec enregistrement des clients :
```typescript
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

### `opensilex-front/front/vite.config.ts`
Configuration Vite avec plugin Hey API :
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

### `opensilex-front/front/src/models/OpenSilexVuePlugin.ts`
Mise à jour du plugin Vue.js pour supporter le nouveau client :
- Ajout de `registerClient()` pour l'enregistrement dynamique des clients
- Mise à jour de `loadModules()` pour charger les clients générés
- Conservation de la rétrocompatibilité avec le service locator

### `opensilex-front/front/src/models/HttpResponse.ts` (NOUVEAU)
Définition des types de réponse :
```typescript
export class OpenSilexResponse<T = any> {
  constructor(public result: T, public metadata: MetadataDTO) {}
}

export class MetadataDTO {
  constructor(
    public pagination: PaginationDTO,
    public status: Array<StatusDTO>,
    public datafiles: Array<string>
  ) {}
}

export class OpenSilexResponseError extends Error {
  public status: number;
  public details: any;
  constructor(message: string, status: number, details?: any) {
    super(message);
    this.name = "OpenSilexResponseError";
    this.status = status;
    this.details = details;
  }
}
```

### `opensilex-security/front/src/index.ts`
Barrel file pour le module security :
```typescript
export * from './lib/generated';
```

### `opensilex-core/front/src/index.ts`
Barrel file pour le module core :
```typescript
export * from './lib/generated';
```

### `opensilex-front/front/src/lib/generated/` (Généré)
Fichiers générés automatiquement par `@hey-api/openapi-ts` :
- `types.gen.ts` : Interfaces TypeScript strictes pour DTOs
- `sdk.gen.ts` : Fonctions SDK typées
- `client.gen.ts` : Client HTTP de base
- `index.ts` : Barrel file

### `opensilex-front/front/package.json`
Mise à jour des dépendances :
- Ajout de `@hey-api/client-fetch`
- Ajout de `@hey-api/typescript`
- Ajout de `@hey-api/sdk`
- Ajout de `@hey-api/vite-plugin` (devDependencies)


## Fichiers de configuration

### `package.json` (racine)
Ajout des dépendances et scripts pour la génération TypeScript :
```json
{
  "dependencies": {
    "@hey-api/client-fetch": "^0.x.x",
    "@hey-api/typescript": "^0.x.x",
    "@hey-api/sdk": "^0.x.x"
  },
  "devDependencies": {
    "@hey-api/vite-plugin": "^0.x.x"
  },
  "scripts": {
    "gen:types": "openapi-ts --config openapi-ts.config.ts"
  }
}
```

### `opensilex-core/pom.xml`
Configuration du plugin openapi-generator pour le module core :
```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <configuration>
        <inputSpec>${project.basedir}/front/src/lib/openapi.json</inputSpec>
        <generatorName>typescript-inversify</generatorName>
        <output>${project.basedir}/front/src/lib/</output>
        <skipIfSpecIsUnchanged>true</skipIfSpecIsUnchanged>
    </configuration>
</plugin>
```

### `opensilex-security/pom.xml`
Configuration du plugin openapi-generator pour le module security :
```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <configuration>
        <inputSpec>${project.basedir}/front/src/lib/openapi.json</inputSpec>
        <generatorName>typescript-inversify</generatorName>
        <output>${project.basedir}/front/src/lib/</output>
        <skipIfSpecIsUnchanged>true</skipIfSpecIsUnchanged>
    </configuration>
</plugin>
```

### `opensilex-front/pom.xml`
Configuration du plugin openapi-generator pour le frontend :
```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <configuration>
        <inputSpec>${project.basedir}/front/src/lib/openapi.json</inputSpec>
        <generatorName>typescript-inversify</generatorName>
        <output>${project.basedir}/front/src/lib/</output>
        <skipIfSpecIsUnchanged>true</skipIfSpecIsUnchanged>
    </configuration>
</plugin>
```

### `opensilex-dev-tools/pom.xml`
Ajout de la dépendance `org.openapitools:openapi-generator` pour les outils de développement :
```xml
<dependency>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator</artifactId>
    <version>${openapi.generator.version}</version>
</dependency>
```

### `opensilex-dev-tools/src/main/java/org/opensilex/dev/ResetTypeScriptLib.java`
Mise à jour de l'outil de développement pour utiliser le nouveau plugin :
```java
// Appel de openapi-generator API
OpenApiGenerator.generate(
    inputSpec,
    generatorName,
    outputDir,
    configOptions
);
```

# Autres

## Liens

- [OpenAPI Specification 3.1.0](https://spec.openapis.org/oas/v3.1.0)
- [Hey API Documentation](https://heyapi.com/)
- [OpenAPI Generator Documentation](https://openapi-generator.tech/)
- [Vue.js Composition API](https://vuejs.org/guide/extras/composition-api-faq.html)
- [Vite Plugin Documentation](https://vitejs.dev/guide/api-plugin.html)

## Glossaire

| Terme | Définition |
|---|---|
| **Swagger 2.0** | Ancienne spécification pour décrire les APIs REST (maintenant appelée OpenAPI 2.0) |
| **OpenAPI 3.1** | Dernière version de la spécification OpenAPI, norme W3C pour les APIs REST |
| **DTO** | Data Transfer Object - Objet de transfert de données |
| **SDK** : Software Development Kit - Kit de développement logiciel |
| **InversifyJS** | Framework de dependency injection pour TypeScript/JavaScript |
| **Hey API** | Outil de génération de clients HTTP typés à partir de spécifications OpenAPI |
| **Interceptor** | Fonction intermédiaire qui intercepte les requêtes/réponses HTTP |
| **Barrel file** | Fichier TypeScript qui ré-exporte les modules d'un dossier |

## Cartes trello liées

- [Carte trello](https://trello.com/)