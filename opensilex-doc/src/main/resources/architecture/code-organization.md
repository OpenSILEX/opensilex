Code organization
=================

# Typical module structure for implementing a "Concept" API

This stucture can apply to a new module or by only adding concept's specific folders to an existing module.

Try to keep modules coherent and minimize dependencies between modules if possible.

```
opensilex --> Root of OpenSilex sources
|
+-- my-module --> Root module folder
|   |
|   +-- src/main/java --> Java sources
|   |   |
|   |   +-- org/example/myModule --> Package name
|   |       |
|   |       +-- MyConfig.java --> MyConfig interface defined for MyModule
|   |       |
|   |       +-- MyModule.java --> MyModule implementation extending class OpenSilexModule and eventually implementing extensions
|   |       |
|   |       +-- cli --> Package for OpenSilexCommand extensions (optional)
|   |       |   |
|   |       |   +-- ConceptCommands.java --> Command class containing all CLI operations about the concept (if required)
|   |       |
|   |       +-- concept/api --> API package for a concept, a module can have many concept implemented but should stay coherent
|   |       |   |
|   |       |   +-- ConceptAPI.java --> API class to access concept where Services are injected, DAO created and used together
|   |       |   |
|   |       |   +-- ConceptGetDTO.java, ConceptCreationDTO.java   --> DTOs used by ConceptAPI contains transformations from/to DTO <-> Model 
|   |       |
|   |       +-- concept/dal --> Data access layer for concept /!\ No reference to API or DTO in this package /!\
|   |           |
|   |           +-- ConceptModel.java --> API class to access concept (Should be annotated for usage with ORM-like tools)
|   |           |
|   |           +-- ConceptDAO.java --> DAO used to manipulate ConceptModel using Services generic functions (set as constructor parameters)
|   |
|   +-- src/main/resources --> Other resources
|   |   |
|   |   +-- config/(dev|prod|test)/opensilex.yml --> Configuration override for the different profiles (optional)
|   |   |
|   |   +-- ontologies/ --> Folder containing ontology files where Concept is defined
|   |
|   +-- src/test/java --> Java test sources
|   |   |
|   |   +-- org/example/myModule/concept/api --> Test Package for ConceptAPI
|   |   |   |
|   |   |   +-- ConceptAPITest.java --> Integration test class for concept API extends AbstractIntegrationTest
|   |   |
|   |   +-- org/example/myModule/concept/dal --> Test Package for ConceptDAO
|   |       |
|   |       +-- ConceptDAOTest.java --> Unit test class for concept DAO extends AbstractUnitTest
|   |
|   +-- front/src/ --> Vue.js sources
|   |   |
|   |   +-- lib/ --> Automatically generated TypeScript library for OpenSilex API
|   |   |
|   |   +-- components/concept/ --> Folder with all Vue.js components for concept, same remark as for "concept/api" package
|   |   |
|   |   +-- lang/ --> Folder containing translation files for concept components
|   |   |
|   |   +-- types/ --> Automatically generated folder containing TypeScript typings files for generated API
|   |   |
|   |   +-- index.ts --> Vue.js entry point where all components must be declared
|   |
|   +-- pom.xml --> Maven my-module configuration (with properties artifactId=my-module and groupId=org.example)
|
+-- pom.xml --> Existing Maven module configuration to update with `my-module` dependency
```

# Service

A service should be at least in it's own package and probably in it's own module.

You should not have more than one service definition by module.

A service should be able to to work with a every-time working configuration if possible.

If it's not possible please be sure to check and report error to user in case of bad service configuration (at startup if possible).

# Theme definition

```
opensilex --> Root of OpenSilex sources
|
+-- my-module --> Root module folder
    |
    +-- front/theme/my-theme --> Vue.js theme folder
        |
        +-- my-theme.yml --> Theme configuration file (TODO: add link to vuejs doc)
        |
        +-- fonts/ --> Folder containing fonts to load for current theme, one by sub-folder
        |
        +-- images/ --> Folder containing static images for the theme
        |
        +-- xxx.css --> CSS file(s) for the theme (order and files configured in my-theme.yml)
        |
        +-- yyy.scss --> SCSS file(s) for the theme (order and files configured in my-theme.yml)
```

To enable your theme, you must add `my-module#my-theme` as parameter value for configuration `front.theme`:

```yaml
front:
    theme: my-module#my-theme
```

# Where to add new libraries

## Java 

Add common new libraries as dependencies into `pom.xml` of `opensilex-parent` module for built-in usage.

Otherwise for specific or optional features then dependencies should be added into `pom.xml` of `your-module`

## Vue.js

Add common new librairies to `package.json` file located in `opensilex-front/front/` folder for built-in usage.

Then you can initialize them if needed in `opensilex-front/front/src/main.ts` file

Otherwise libraries should be added in `package.json` of `your-module/front/` folder.

In that case you must initialize them if needed in `your-module/front/src/index.ts` in the `install` method as shown below:

```ts
// Import generated TypeScript library
import { ApiServiceBinder } from './lib';

// Import components
import ConceptView from './components/concept/ConceptView.vue';

// Import libraries
// import * from 'A LIB';
export default {
    install(Vue, options) {
        // LOAD TYPESCRIPT API
        ApiServiceBinder.with(Vue.$opensilex.getServiceContainer());

        // TODO: INIT LIB HERE...
    },
    components: {
         "my-module-ConceptView": ConceptView,
    },
    lang: {
        "fr": require("./lang/message-fr.json"),
        "en": require("./lang/message-en.json"),
    }

}
```