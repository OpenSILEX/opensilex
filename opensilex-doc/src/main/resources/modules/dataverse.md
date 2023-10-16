# opensilex-dataverse module activation

To include this module you need to :
* remove or comment out this line in the opensilex.yml config file if it is present :
```yaml
        org.opensilex.opensilex-dataverse.DataverseModule: opensilex-dataverse.jar
```
* complete the config for your dataverse. ex :
__NOTE__ : the `externalAPIKey` and `dataverseAlias` are only used by the services when no key or alias is given for tests purposes.
  (More detail in [the config par of the docs](../configutation/dataverse.md))
```yaml
dataverse:
  externalAPIKey: "****-***-***-****"
  rechercheDataGouvBasePath: "https://data-preproduction.inrae.fr"
  dataverseAlias: "opensilex-tests"
  dataverseLanguages:
    - en
    - fr
  datasetMetadataLanguages:
    - en
    - fr
```
* uncomment these dependencies in the root pom.xml :
```xml
        <!-- Dataverse module -->
        <!--<module>opensilex-dataverse</module>-->
```
```xml
        <!-- Dataverse dependency -->
        <!--<dependency>
            <groupId>org.opensilex</groupId>
            <artifactId>opensilex-dataverse</artifactId>
            <version>${revision}</version>
        </dependency>-->
```
* Change this in main.ts at row 483 :
```ts
$opensilex.loadModules([
  "opensilex-security",
  "opensilex-core"
])
```
to :
```ts
$opensilex.loadModules([
  "opensilex-security", 
  "opensilex-core",
  "opensilex-dataverse"
])
```
This last point is suboptimal, but it is the only quick fix I found for the translation files not being loaded when the
application starts but only once you click on the respective menu.
* Compile the project to obtain an archive with the opensilex-dataverse module activated
