# Configuration for opensilex-dataverse module

## Example of an opensilex-dataverse config :
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

## Detail of the parameters

* `externalAPIKey` is the API token of a testing account created on the corresponding dataverse (Your account in the top right corner > API Token)
It is used when no API token is given to the webservices and for test purposes.
* `rechercheDataGouvBasePath` is the URL of your instance of RechercheDataGouv
* `dataverseAlias` is the alias of a test collection on the dataverse (can be found in the url of your collection : <rechercheDataGouvBasePath>/dataverse/<dataverseAlias>)
* `dataverseLanguages` are the languages (in ISO 2 format) supported for the files added to the dataverse.
If your dataverse has more you can add them here.
If you don't know ask your dataverse administrators or leave only "en".
* `datasetMetadataLanguages` are the languages (in ISO 2 format) supported for the metadata of the files added to the dataverse.
If your dataverse has more you can add them here.
If you don't know ask your dataverse administrators or leave only "en".
