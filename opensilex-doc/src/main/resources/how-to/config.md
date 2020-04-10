Configuration How-to
====================

Ce document à pour but de lister des examples pratiques pour ajouter, modifier ou supprimer des paramètres de configuration dans OpenSilex.

Le fichier de configuration d'OpenSilex est un fichier [YAML](https://yaml.org/) où chaque section correspond à un module.

Ce fichier est ensuite automatiquement déserialisé vers un objet Java repésentant une interface.

Pour plus d'information sur les détails sur la configuration et les détails d'implémentation voir [ici](../architecture/main.md#Configuration)

# Comment connaitre les paramètres de configuration existants ?

Dans un terminal taper la commande suivante:

`opensilex system full-config`

# Comment ajouter une section de configuration pour un module ?

Dans la classe du module, implémenter les méthodes `getConfigId` et `getConfigClass` qui représentent respectivement l'identifiant de la section de configuration et l'interface de configuration associée.

```java
public class MyModule extends OpenSilexModule {

    @Override
    public Class<?> getConfigClass() {
        return MyModuleConfig.class;
    }

    @Override
    public String getConfigId() {
        return "my-module";
    }
}
```

Ensuite créer l'interface de configuration `MyModuleConfig`.

Les méthodes de l'interface DOIVENT être annotées avec `@ConfigDescription` pour être prises en compte.

Cette annotation fourni un description et éventuellement une valeur par défaut correspondant au type de retour de la méthode.

Bonne pratique: placer cette interface dans le même package que le module `MyModule`

Ci-dessous l'exemple de la configuration du `ServerModule`.

```java
public interface ServerConfig {

    @ConfigDescription(
            value = "Server public URI",
            defaultString = "http://localhost:8666/"
    )
    public String publicURI();

    @ConfigDescription(
            value = "Available application language list",
            defaultList = {OpenSilex.DEFAULT_LANGUAGE, "fr"}
    )
    public List<String> availableLanguages();

    @ConfigDescription(
            value = "Tomcat system properties"
    )
    public Map<String, String> tomcatSystemProperties();

    @ConfigDescription(
            value = "Enable Tomcat anti-thread lock mechanism with StuckThreadDetectionValve",
            defaultBoolean = true
    )
    public boolean enableAntiThreadLock();
}
```

Qui correspondrait au YAML suivant dans la configuration globale:

```java
my-module:
  publicURI: http://example.com
  availableLanguages: 
    - en
    -fr
  tomcatSystemProperties:
    p1: v1
    p2: v2
  enableAntiThreadLock: true
```

# Comment accéder à un paramètre de configuration ?

Si le code où vous souhaitez utiliser ce paramètres est:
- Un module
- Un service
- Une commande

Ou que vous avez accès à des instances de ces objets, vous pouvez alors accèder à l'instance d'OpenSilex à l'aide de la méthode `getOpenSilex`.

De là vous pouvez charger directement l'instance de configuration en connaissant sa classe et celle de son module:

```java
OpenSilex opensilex = obj.getOpenSilex();
ServerConfig config = opensilex.getModuleConfig(MyModule.class, MyConfig.class);
config.myParam(); // --> return parameter myParam value
```

# Quels sont les types de données possibles comme paramètres

## Valeurs primitives

Les primitives correspondent au "feuilles" de l'arbre YAML.

Les types  acceptés comme primitives sont:
- boolean
- int
- long
- float
- double
- char
- short
- byte
- String

## Type complexes

On peut définir comme type une interface dont les méthodes sont également annotées avec `@ConfigDescription`.

Le nom de la méthode correspondra alors à la clé dans la configuration YAML et les sous-clé les valeurs de l'interface.

Par exemple la configuration YAML suivante avec les classes d'interface Java correspondantes pour un module ayant pour identifiant de configuration "my-module":

```java
public interface MyModuleConfig {

    @ConfigDescription(
        value = "A complexe sub-category"
    )
    ChildConfig child1();

    @ConfigDescription(
        value = "Another complexe sub-category"
    )
    ChildConfig child2();
}

public interface ChildConfig {

    @ConfigDescription(
        value = "A string value"
    )
    String stringValue();

    @ConfigDescription(
        value = "An integer value"
    )
    int intValue();
}
```

```yml
my-module:
  child1:
    stringValue: "1234"
  intValue: 4
  child2:
    stringValue: "azerty"
    intValue: 0
```

## Listes générique

On peux définir comme type une liste générique en paramêtre de configuration.

Le type générique de cette liste DOIT être n'importe quel autre type, simple ou complexe, accépté comme type de donnée de configuration.

Le type générique NE DOIT PAS être du type `List<? extends ...>`, il doit directement correspondre à une classes ou une interface gérée.

En revanche le type générique PEUX être une liste de liste ou de map de manière récursive.

```java
public interface MyModuleConfig {

    @ConfigDescription(
        value = "A list of primitive"
    )
    List<Integer> simpleList();

    @ConfigDescription(
        value = "A list of complexe sub type"
    )
    List<ComplexSubTypeConfig> complexList();
}

public interface ComplexSubTypeConfig {

    @ConfigDescription(
        value = "A string value"
    )
    String stringValue();

    @ConfigDescription(
        value = "An integer value"
    )
    int intValue();
}
```

```yml
my-module:
  simpleList:
    - 1
    - 2
    - 3
  complexList:
    - stringValue: "azerty"
      intValue: 0
    - stringValue: "abcde"
      intValue: -25    
```

## Map générique

On peux définir comme type une map de String et d'un paramètre générique en paramêtre de configuration.

Le type générique des map à les même contrainte que celui des listes.

En revanche le type générique PEUX être une map de liste ou de map de manière récursive.


```java
public interface MyModuleConfig {

    @ConfigDescription(
        value = "A map of primitive"
    )
    Map<String, Boolean> simpleMap();

    @ConfigDescription(
        value = "A map of complexe sub type"
    )
    Map<String, ComplexSubTypeConfig> complexMap();
}

public interface ComplexSubTypeConfig {

    @ConfigDescription(
        value = "A string value"
    )
    String stringValue();

    @ConfigDescription(
        value = "An integer value"
    )
    int intValue();
}
```

```yml
my-module:
  simpleMap:
    s1: true
    s2: false
    s3: true
  complexMap:
    c1: 
      stringValue: "azerty"
      intValue: 0
    c2: false
      stringValue: "azerty"
      intValue: 0
```

## Services

Il est possible de définir des services automatiquement chargés grâce à la configuration.

Cela permet de pouvoir facilement interchanger les implémentations de ces services.

Ce cas intervient lorque la classe du type de retour de la méthode de la classe de configuration implémente la classe `org.opensilex.service.Service`.

Pour plus d'information sur les services et leur configuration voir [ici](./services.md)