# Guide de migration d'un module OpenSILEX : Swagger 2.0 → OpenAPI 3.1

## 1. Vue d'ensemble

La migration concerne 3 couches : **Backend Java** (annotations), **Build Maven** (génération de spécification et clients TypeScript), et **Frontend** (si le module expose une bibliothèque TypeScript).

---

## 2. Backend Java

### 2.1 Dépendances Maven

**Dans `pom.xml` du module**, supprimer le plugin maison :

```xml
<!-- À SUPPRIMER -->
<plugin>
    <groupId>org.opensilex</groupId>
    <artifactId>opensilex-swagger-codegen-maven-plugin</artifactId>
    <version>${revision}</version>
</plugin>
```

**Dans `opensilex-parent/pom.xml`**, les dépendances sont déjà gérées au niveau parent :

| Avant | Après |
|---|---|
| `io.swagger:swagger-jersey2-jaxrs:1.6.5` | `io.swagger.core.v3:swagger-jaxrs2:2.2.53` |
| — | `io.swagger.core.v3:swagger-annotations:2.2.53` |

### 2.2 Migration des annotations API (Controllers)

**Imports :**

```java
// AVANT
import io.swagger.annotations.*;

// APRÈS
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ArraySchema;
```

**Classe API (Controller) :**

```java
// AVANT
@Api(DataAPI.CREDENTIAL_DATA_GROUP_ID)

// APRÈS
@Tag(name = DataAPI.CREDENTIAL_DATA_GROUP_ID)
```

**Méthode :**

```java
// AVANT
@ApiOperation("Add data")
@ApiResponses(value = {
    @ApiResponse(code = 201, message = "Add data", response = URI.class),
    @ApiResponse(code = 400, message = "Bad user request", response = ErrorResponse.class)
})
public Response addListData(
    @ApiParam("Data description") @Valid @NotNull @NotEmpty List<DataCreationDTO> dtoList
)

// APRÈS
@Operation(summary = "Add data")
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Add data",
        content = @Content(schema = @Schema(implementation = URI.class))),
    @ApiResponse(responseCode = "400", description = "Bad user request",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public Response addListData(
    @Parameter(description = "Data description") @Valid @NotNull @NotEmpty List<DataCreationDTO> dtoList
)
```

**Tableau de correspondance complet :**

| Swagger 2.0 | OpenAPI 3.1 |
|---|---|
| `@Api(value = "...")` | `@Tag(name = "...")` |
| `@ApiOperation(value = "...")` | `@Operation(summary = "...")` |
| `@ApiParam(value = "...")` | `@Parameter(description = "...")` |
| `@ApiResponses(value = {...})` | `@ApiResponses(value = {...})` (inchangé) |
| `@ApiResponse(code = 201, message = "...", response = X.class)` | `@ApiResponse(responseCode = "201", description = "...", content = @Content(schema = @Schema(implementation = X.class)))` |
| `@ApiImplicitParam(...)` | `@Parameter(name = "...", schema = @Schema(type = "..."), in = ParameterIn.HEADER, description = "...")` |
| `@ApiImplicitParams({...})` | `@Parameters({...})` |

### 2.3 Migration des annotations DTO

```java
// AVANT
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class MyDTO {
    @ApiModelProperty(required = true, example = "http://...")
    public URI getUri() { ... }
}

// APRÈS
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class MyDTO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "http://...")
    public URI getUri() { ... }
}
```

**Tableau de correspondance DTO :**

| Swagger 2.0 | OpenAPI 3.1 |
|---|---|
| `@ApiModel` | `@Schema` |
| `@ApiModel(value = "...")` | `@Schema(description = "...")` |
| `@ApiModelProperty(value = "...")` | `@Schema(description = "...")` |
| `@ApiModelProperty(required = true)` | `@Schema(requiredMode = Schema.RequiredMode.REQUIRED)` |
| `@ApiModelProperty(example = "...")` | `@Schema(example = "...")` |

### 2.4 Migration des méta-annotations de sécurité

**ApiProtected.java :**

```java
// AVANT
@ApiImplicitParams({
    @ApiImplicitParam(name = "Authorization", required = true, dataType = "string", paramType = "header", value = "Authentication token"),
    @ApiImplicitParam(name = "Accept-Language", dataType = "string", paramType = "header", value = "Request accepted language", example = "fr")
})

// APRÈS
@SecurityRequirement(name = "Bearer")
@Parameter(name = "Accept-Language",
    schema = @Schema(type = "string"),
    in = ParameterIn.HEADER,
    description = "Request accepted language",
    example = "fr")
```

**ApiTranslatable.java :**

```java
// AVANT
@ApiImplicitParams({
    @ApiImplicitParam(name = "Accept-Language", dataType = "string", paramType = "header", value = "Request accepted language", example = "fr")
})

// APRÈS
@Parameters({
    @Parameter(name = "Accept-Language",
        schema = @Schema(type = "string"),
        in = ParameterIn.HEADER,
        description = "Request accepted language",
        example = "fr")
})
```

### 2.5 Migration de SwaggerExtension (si le module implémente cette interface)

Le module doit implémenter `SwaggerExtension` (qui extends `OpenApiExtension`) :

```java
public class MyModule extends OpenSilexModule implements SwaggerExtension {
    @Override
    public List<Class<?>> getAdditionalSwaggerDefinitions() {
        return List.of(MyCustomDTO.class);
    }
}
```

L'interface `SwaggerExtension` assure la rétrocompatibilité via :
```java
public interface SwaggerExtension extends OpenApiExtension {
    List<Class<?>> getAdditionalSwaggerDefinitions();

    @Override
    default List<Class<?>> getAdditionalOpenApiDefinitions() {
        return getAdditionalSwaggerDefinitions();
    }
}
```

---

## 3. Build Maven

### 3.1 Configuration exec-maven-plugin (opensilex-parent/pom.xml)

Le plugin `exec-maven-plugin` est mis à jour pour générer `openapi.json` au lieu de `swagger.json` :

```xml
<!-- AVANT -->
<execution>
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

<!-- APRÈS -->
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

### 3.2 Génération TypeScript (remplacement du plugin maison)

**Supprimer :**
```xml
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

**Remplacer par :**
```xml
<execution>
    <id>generate-typescript-schema</id>
    <phase>compile</phase>
    <goals><goal>exec</goal></goals>
    <configuration>
        <skip>${skipTypeScriptLibBuild}</skip>
        <executable>${maven.multiModuleProjectDirectory}/.node/node/node</executable>
        <arguments>
            <argument>${maven.multiModuleProjectDirectory}/node_modules/.bin/openapi-ts</argument>
            <argument>--plugins</argument><argument>@hey-api/client-fetch</argument>
            <argument>--plugins</argument><argument>@hey-api/typescript</argument>
            <argument>--plugins</argument><argument>@hey-api/sdk</argument>
            <argument>-i</argument><argument>${project.basedir}/front/src/lib/openapi.json</argument>
            <argument>-o</argument><argument>${project.basedir}/front/src/lib/generated</argument>
        </arguments>
    </configuration>
</execution>
```

### 3.3 Fichier openapi-ts.config.ts (racine du projet)

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

---

## 4. Frontend (si le module expose une bibliothèque TypeScript)

### 4.1 Fichier `front/src/index.ts`

```typescript
// AVANT
export default {
    install(Vue, options) {
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());
    }
};

// APRÈS
import { client } from './lib/generated/client.gen';

export default {
    install(app, options) {
        if (app?.config?.globalProperties?.$opensilex?.registerClient) {
            app.config.globalProperties.$opensilex.registerClient(client);
        }
        ApiServiceBinder.with(app.$opensilex.getServiceContainer());
    }
};
export * from './lib';
```

### 4.2 Fichier `front/package.json`

```json
{
    "scripts": {
        "serve": "mkdir -p src/lib/model && echo export interface ModelObject {} > src/lib/model/modelObject.ts && vite build --config vite.config.ts --watch",
        "build": "mkdir -p src/lib/model && echo export interface ModelObject {} > src/lib/model/modelObject.ts && vite build --config vite.config.ts"
    }
}
```

### 4.3 Migration du code client

**Avant (service locator InversifyJS) :**
```typescript
const authService = $opensilex.getService<AuthenticationService>(
    "opensilex-security.AuthenticationService"
);
const response = await authService.authenticate({...});
```

**Après (SDK typé) :**
```typescript
import { authenticate } from 'opensilex-security';
import { OpenSilexResponse } from "@/models/HttpResponse";

const response: OpenSilexResponse<TokenDTO> = await authenticate({
    identifier: "admin@opensilex.org",
    password: "password"
});
const userToken: TokenDTO = response.result;
```

---

## 5. Checklist de migration par module

### Fichiers à modifier :

| Fichier | Action |
|---|---|
| `src/main/java/.../api/*API.java` | Migrer annotations `@Api*` → `@Tag`, `@Operation`, `@Parameter`, `@ApiResponse` |
| `src/main/java/.../api/*DTO.java` | Migrer `@ApiModel` → `@Schema`, `@ApiModelProperty` → `@Schema` |
| `src/main/java/.../api/*CreationDTO.java` | Migrer `@ApiModel` → `@Schema`, `@ApiModelProperty` → `@Schema` |
| `src/main/java/.../api/*UpdateDTO.java` | Migrer `@ApiModel` → `@Schema`, `@ApiModelProperty` → `@Schema` |
| `src/main/java/.../api/*GetDTO.java` | Migrer `@ApiModel` → `@Schema`, `@ApiModelProperty` → `@Schema` |
| `pom.xml` | Supprimer `opensilex-swagger-codegen-maven-plugin` |
| `front/src/index.ts` (si applicable) | Ajouter enregistrement client SDK |
| `front/package.json` (si applicable) | Mettre à jour scripts `build`/`serve` |

### Étapes recommandées :

1. **Exécuter le script de migration** : `python3 migrate_annotations.py /chemin/vers/le/projet`
2. **Vérifier les fichiers modifiés** : `git diff --name-only | grep '\.java$'`
3. **Revoir manuellement** chaque fichier pour les cas particuliers (liste, imports manquants, etc.)
4. **Compiler** : `mvn clean compile`
5. **Vérifier** : `openapi.json` est généré correctement dans `front/src/lib/`

### Vérifications :

- [ ] Script `migrate_annotations.py` exécuté sur le codebase
- [ ] Tous les imports `io.swagger.annotations.*` remplacés par `io.swagger.v3.oas.annotations.*`
- [ ] `@Api` → `@Tag(name = "...")`
- [ ] `@ApiOperation` → `@Operation(summary = "...")`
- [ ] `@ApiParam` → `@Parameter(description = "...")`
- [ ] `@ApiResponses` avec `code` → `responseCode` et ajout de `content = @Content(...)`
- [ ] `@ApiModel` → `@Schema`
- [ ] `@ApiModelProperty(required = true)` → `@Schema(requiredMode = Schema.RequiredMode.REQUIRED)`
- [ ] `@ApiImplicitParam` → `@Parameter(...)` avec `in = ParameterIn.HEADER`
- [ ] Plugin `opensilex-swagger-codegen-maven-plugin` supprimé du POM
- [ ] Module implémente `SwaggerExtension` si besoin de DTOs supplémentaires
- [ ] Build Maven passe sans erreur (`mvn clean compile`)
- [ ] `openapi.json` est généré correctement dans `front/src/lib/`
- [ ] Tests unitaires passent

---

## 6. Exemple complet : migration d'une classe API

```java
// ============================================================
// AVANT (Swagger 2.0)
// ============================================================
package org.opensilex.myapp.api;

import io.swagger.annotations.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/myapp/myentity")
@Api(MyEntityAPI.CREDENTIAL_GROUP_ID)
@ApiCredentialGroup(
    groupId = MyEntityAPI.CREDENTIAL_GROUP_ID,
    credentialId = {CREDENTIAL_CREATE_ID, CREDENTIAL_UPDATE_ID, CREDENTIAL_DELETE_ID}
)
public class MyEntityAPI {
    public static final String CREDENTIAL_GROUP_ID = "MyEntity";
    public static final String CREDENTIAL_CREATE_ID = "myentity-create";

    @Inject
    private MyEntityLogic myEntityLogic;

    @POST
    @ApiProtected
    @ApiOperation("Create a new MyEntity")
    @ApiCredential(credentialId = CREDENTIAL_CREATE_ID)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Entity created", response = URI.class),
        @ApiResponse(code = 400, message = "Bad request", response = ErrorResponse.class),
        @ApiResponse(code = 401, message = "Unauthorized", response = ErrorResponse.class)
    })
    public Response create(
        @ApiParam(value = "Entity data", required = true) @Valid @NotNull MyEntityCreationDTO dto
    ) throws Exception {
        URI uri = myEntityLogic.create(dto);
        return Response.created(uri).build();
    }

    @GET
    @Path("{uri}")
    @ApiOperation("Get MyEntity by URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Entity retrieved", response = MyEntityGetDTO.class),
        @ApiResponse(code = 404, message = "Entity not found", response = ErrorResponse.class)
    })
    public Response get(
        @ApiParam(value = "Entity URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        MyEntityGetDTO dto = myEntityLogic.get(uri);
        return Response.ok(dto).build();
    }
}

// ============================================================
// APRÈS (OpenAPI 3.1)
// ============================================================
package org.opensilex.myapp.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/myapp/myentity")
@Tag(name = MyEntityAPI.CREDENTIAL_GROUP_ID)
@ApiCredentialGroup(
    groupId = MyEntityAPI.CREDENTIAL_GROUP_ID,
    credentialId = {CREDENTIAL_CREATE_ID, CREDENTIAL_UPDATE_ID, CREDENTIAL_DELETE_ID}
)
public class MyEntityAPI {
    public static final String CREDENTIAL_GROUP_ID = "MyEntity";
    public static final String CREDENTIAL_CREATE_ID = "myentity-create";

    @Inject
    private MyEntityLogic myEntityLogic;

    @POST
    @ApiProtected
    @Operation(summary = "Create a new MyEntity")
    @ApiCredential(credentialId = CREDENTIAL_CREATE_ID)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Entity created",
            content = @Content(schema = @Schema(implementation = URI.class))),
        @ApiResponse(responseCode = "400", description = "Bad request",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response create(
        @Parameter(description = "Entity data", required = true) @Valid @NotNull MyEntityCreationDTO dto
    ) throws Exception {
        URI uri = myEntityLogic.create(dto);
        return Response.created(uri).build();
    }

    @GET
    @Path("{uri}")
    @Operation(summary = "Get MyEntity by URI")
    @ApiProtected
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Entity retrieved",
            content = @Content(schema = @Schema(implementation = MyEntityGetDTO.class))),
        @ApiResponse(responseCode = "404", description = "Entity not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response get(
        @Parameter(description = "Entity URI", required = true) @PathParam("uri") @NotNull URI uri
    ) throws Exception {
        MyEntityGetDTO dto = myEntityLogic.get(uri);
        return Response.ok(dto).build();
    }
}
```

---

## 7. Script de migration automatisé

### 7.1 Le script `migrate_annotations.py`

Un script Python est fourni pour automatiser la migration des annotations Swagger 2.0 → OpenAPI 3.1 sur l'ensemble du codebase Java.

**Emplacement du script :** `opensilex-doc/src/main/resources/technical-documentation/swagger-to-openapi/migrate_annotations.py`

**Utilisation :**
```bash
# Depuis la racine du projet
python3 opensilex-doc/src/main/resources/technical-documentation/swagger-to-openapi/migrate_annotations.py /chemin/vers/le/projet
```

**Ce que le script fait automatiquement :**

| Transformation | Exemple |
|---|---|
| Imports | `io.swagger.annotations.*` → `io.swagger.v3.oas.annotations.*` |
| `@Api(...)` | → `@Tag(name = ...)` |
| `@ApiOperation(...)` | → `@Operation(summary = ...)` |
| `@ApiParam(...)` | → `@Parameter(description = ...)` |
| `@ApiModel` | → `@Schema` |
| `@ApiModelProperty(...)` | → `@Schema(description = ..., requiredMode = ...)` |
| `@ApiImplicitParam(...)` | → `@Parameter(in = ParameterIn.HEADER, ...)` |
| `@ApiResponse(code = 201, message = "...", response = X.class)` | → `@ApiResponse(responseCode = "201", description = "...", content = @Content(schema = @Schema(implementation = X.class)))` |

**Ce que le script NE fait PAS (nécessite une revue manuelle) :**

- Migration des imports `ArraySchema` pour les listes (`responseContainer = "List"`)
- Ajout des imports manquants spécifiques à chaque fichier
- Migration des annotations `@SecurityRequirement` sur `@ApiProtected`
- Migration des fichiers de test
- Migration du code frontend TypeScript

**Recommandation :** Utiliser le script comme **première étape** d'automatisation, puis effectuer une **revue manuelle** de chaque fichier modifié pour corriger les cas particuliers.

### 7.2 Vérification post-migration

Après exécution du script :

```bash
# Compter les fichiers modifiés
git diff --name-only | grep '\.java$'

# Vérifier les imports restants de Swagger 2
grep -r "io\.swagger\.annotations\." --include="*.java" | grep -v "v3\.oas\.annotations"

# Vérifier les annotations non migrées
grep -r "@Api(" --include="*.java" | grep -v "@Tag"
grep -r "@ApiOperation(" --include="*.java"
grep -r "@ApiParam(" --include="*.java"
grep -r "@ApiModel(" --include="*.java"
grep -r "@ApiModelProperty(" --include="*.java"
```

### 7.3 Exemple d'exécution

```bash
# Migration complète du projet
python3 migrate_annotations.py .

# Résultat attendu :
# Migrated: opensilex-core/src/main/java/org/opensilex/core/data/api/DataAPI.java
# Migrated: opensilex-core/src/main/java/org/opensilex/core/annotation/api/AnnotationCreationDTO.java
# ...
# Total files updated: 175
```

---

## 8. Points d'attention

1. **Rétrocompatibilité** : L'interface `SwaggerExtension` extends `OpenApiExtension`, donc les modules existants continuent de fonctionner sans modification.
2. **GeoJsonConverter** : Le convertisseur GeoJSON est migré vers OpenAPI 3.1 avec injection manuelle des schémas via `injectGeoJsonSchema(OpenAPI openAPI)`.
3. **RemoveRestPrefixFilter** : Un nouveau filtre supprime le préfixe `/rest` des chemins dans la spécification OpenAPI générée.
4. **JenaAnnotationIntrospector** : Des mixins Jackson sont ajoutés pour gérer les classes Apache Jena RDF qui posent des problèmes de sérialisation.
5. **SecurityScheme** : Le schéma d'authentification JWT Bearer est configuré dans `RestApplication.initOpenApi()`.