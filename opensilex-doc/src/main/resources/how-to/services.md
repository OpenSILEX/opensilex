Services How-to
===============

Un service est une couche d'abstraction technique pour encapsuler une fonctionnalité pouvant avoir de multiples implémentations.

Exemple typique:
- connection à un type de  base de donnée "générique"
- lien avec un logiciel extérieur optionnel
- mise en place d'une fonctionnalité dont l'implémentation peut être changée via la configuration

# Comment créer un nouveau service ?

// TODO

# Comment créer une nouvelle implémentation pour un service ?

Ajouter une implémentation d'un service consiste à créer une nouvelle classe implémentant le service en question où à surcharge une implémentation de ce service existante.

Ci dessous un example de procédure pour créer un service d'authentification LDAP en se basant sur le service d'authentification existant.

- créer une sous-classe de `AuthenticationService` par exemple `LDAPAuthenticationService`
- Implémenter la méthode `setup()̀` -> vérification des paramètre de config pour accèder au LDAP
- Implémenter la méthode `startup()̀` -> initialisation de la connection au server LDAP si besoin
- Surcharger la méthode `public boolean authenticate(UserModel user, String password, List<String> accessList) throws Exception` 
- Implémenter dans cette méthode la requête de vérification vers le serveur LDAP et renvoyer `true` ou `false` (ou lever une exception)
- Implémenter la méthode `shutdown()̀` -> nettoyer ici tout ce qui est initialisé dans `startup()`
- Implémenter la méthode `clean()̀` -> nettoyer ici tout ce qui est initialisé dans `setup()`

Le seul point spécifique ici est la surcharge de la méthode `authenticate` qui correspond à l'interface de `AuthenticationService`.

Les méthodes `setup()̀`, `clean()̀`, `startup()̀` et `shutdown()̀` sont implémentables dans n'importe quel service et sont appelées automatiquement.

Ensuite pour activer ce `LDAPAuthenticationService` via le fichier de configuration YAML de l'application il faut déclarer la partie suivante:

```yml
security:
  authentication:
    implementation: package.to.LDAPAuthenticationService
```

Le seul point spécifique ici est la clé "security.authentication" qui varie selon le service original que l'on souhaite surcharger.


Pour ajouter des paramètres de configuration au nouveau service il faut:

- créer une interface de configuration `LDAPConfig`
- ajouter une méthode annotée pour chaque paramètre nécessaires, par exemple:

```java
public interface LDAPConfig   {

	@ConfigDescription(
	    value = "Connection URI to LDAP server",
	    defaultString = "ldap.server.example.org"
	)
	String serverHost();
}
```

- Ajouter l'annotation suivante dans la classe `LDAPAuthenticationService` et ajouter la configuration comme paramètre du constructeur:

```java
@ServiceDefaultDefinition(
        configClass = LDAPConfig.class,
        configID = "ldap"
)
public class LDAPAuthenticationService extends AuthenticationService {
	public LDAPAuthenticationService(LDAPConfig config) {
		String host = config.serverHost(); // --> "http://ldap.server.example/"
	}
}
```

Ensuite pour surcharger ce paramètre via le fichier de configuration YAML de l'application il faut déclarer la partie suivante:

```yml
security:
  authentication:
    implementation: package.to.LDAPAuthenticationService
    ldap:
      serverHost: mon.ldap.server.org
```

Les points spécifique ici sont:

- la clé "security.authentication" qui varie selon le service original que l'on souhaite surcharger
- la clé "ldap" qui dépend du paramétre ̀`configID` de l'annotation `@ServiceDefaultDefinition`
