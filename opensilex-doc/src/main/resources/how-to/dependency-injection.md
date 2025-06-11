| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2023-10-04 | Valentin Rigolle | 1.1.0             | Document creation |

<!-- TOC -->
* [How to : dependency injection in OpenSILEX](#how-to--dependency-injection-in-opensilex)
  * [Introduction to dependency injection](#introduction-to-dependency-injection)
  * [Basic definitions and workflow](#basic-definitions-and-workflow)
  * [Annotations](#annotations)
    * [`@Contract`](#contract)
    * [`@Service`](#service)
    * [`@Inject`](#inject)
    * [`@SelfBound`](#selfbound)
  * [Binding](#binding)
    * [Other ways of binding contracts](#other-ways-of-binding-contracts)
  * [Usage](#usage)
  * [Example](#example)
  * [Services registered by default](#services-registered-by-default)
  * [Sources](#sources)
<!-- TOC -->

# How to : dependency injection in OpenSILEX

## Introduction to dependency injection

**Dependency injection** is an architectural pattern that is often seen as a good practice in oriented-object
programming. It requires that a class that has _dependencies_ will receive them from external sources, usually in the
form of constructor parameters or using a _dependency injection framework_, instead of instantiating them.

It is usually used in conjunction with other "good practice" principles, such the five 
[SOLID](https://en.wikipedia.org/wiki/SOLID) principles. Dependency injection allows to achieve multiple benefits in
this regard :

- The logic of instantiating the dependencies is moved outside the services, meaning that the class will be able to
  contain only business logic and focus on a single responsibility ("Single responsibility" principle). That also
  means : no need to solve circular dependencies or other weird problems when managing multiple services that depend
  on each other !
- Used along with the "dependency inversion" principle, the dependencies of the class can change without having to
  modify the business logic.
- It allows for the capabilities of a class to be extended by providing more implementations of their dependencies.
  ("Open-closed" principle).

In OpenSILEX, dependency injection is managed by the HK2 framework provided with Jersey. The framework is an
implementation of the JS-330 "dependency injection for Java" specification. It relies on annotations to specify
which classes are contracts, services, and where to inject them.

## Basic definitions and workflow

> The definitions may seem a bit abstract at the first glance. You can look at the [example section](#example) to see
> a basic usage of dependency injection.

- A **service** is a class that provides some useful methods that can be used by other classes. It is composed of 
  a _contract_ and one or more _implementations_.
- A **contract** is a class or interface that specifies a list of methods provided by a _service_.
- A **service implementation** is a class providing all methods specified by a _contract_. In Java, a _service_ is 
  always a class that extends or implements the _contract_. We say that the _implementation_ is **bound** to the
  _contract_.
- A **dependency** is a _service_ that is needed by a class in order to work correctly. The _dependency_ can be declared
  as a _member variable_ or a _constructor parameter_ for example. In the context of _dependency injection_, the type
  of the dependency must be a _contract_.
- A dependency is **injected** into a class when it is provided by an external source, like as a _constructor parameter_
  or by a _dependency injection framework_ instead of being instantiated by the class.
- The **dependency injection framework** manages _services_. In particular, it is able to provide a _service
  implementation_ based on a requested _contract_. If the requested _service_ has _dependencies_ that are also 
  registered in the _framework_, it is able to provide them recursively, making the process completely transparent to
  the user of the framework.

## Annotations

HK2 relies on three main annotations to specify which classes are contracts, services, and where to inject them.

### `@Contract`

The `@Contract` annotation can be placed on interfaces or classes. An interface annotated with `@Contract` is an injectable 
**contract**, meaning that it can be used as an injected method parameter or class member. The dependency injection
framework will look for a service implementation bound to this contract and pass them as the method parameter
or class member.

### `@Service`

The `@Service` annotation can be placed on non-abstract classes to qualify them as an injectable 
**service implementation**. A service implementation must be bound to a contract to inform the dependency injection 
framework that it can be injected as an implementation of this contract.

### `@Inject`

A class member or a method parameter annotated with `@Contract` can be provided by the dependency injection framework
instead of being instantiated by any class that depends on it.

### `@SelfBound`

A class annotated with `@Service` can also be annotated with `@SelfBound` to be automatically registered as both
a **contract** and a **service implementation** of itself.

## Binding

As said above, service implementations must be **bound** to contracts in order to become injectable. In OpenSILEX, 
this logic is defined in the `registerServices` method of `RestApplication`. This method uses an object of type 
`AbstractBinder` to register the bindings.

For modules, the registration of bindings can be done by implementing `APIExtension` and overriding the `bindServices`
method. The parameter `binder` is the same abstract binder as in `registerServices` mentioned above, and can be used
the same way.

The basic syntax for binding a service implementation to a contract is :

```java
binder.bind(MyService.class).to(MyContract.class);
```

If you have only one service implementation for your contract, and you know that you won't need other ones, you can
bind your service to itself as a contract :

```java
binder.bindAsContract(MyService.class)
```

### Other ways of binding contracts

You can also bind to a contract :

- A singleton instance of an object. For example, a single `OpenSilex` instance is created when launching the server,
  and this instance is bound to the `OpenSilex` contract.
- A factory that creates a service implementing the contract. This is the case for some `*Service` classes, like
  `SPARQLService` or `FileStorageService`. That's also how the `@CurrentUser` annotation works by using a factory to
  create an `AccountModel` representing the user making the request.

## Usage

Once your service is bound, you can inject it in any class covered by the dependency injection framework :

- All service implementation classes
- API classes, which are also instantiated by the framework

You can inject your service by adding the `@Inject` annotation before a member variable or the constructor :

```java
import javax.inject.Inject;

class MyClass {
    @Inject
    MyContract memberInjection;
    
    @Inject
    public MyClass(MyContract constructorInjection) {
        //...
    }
}
```

## Example

The `StapleApiUtils` class depends on the `OpenSilex` object to retrieve information from different modules. The class
is defined as a self-bound service with an injected constructor :

```java
@Service
public class StapleApiUtils {
    //...
    
    OpenSilex openSilex;

    @Inject
    public StapleApiUtils(OpenSilex openSilex) {
        this.openSilex = openSilex;
    }

    //...
}
```

The binding logic is registered in `GraphQLModule` :

```java
public class GraphQLModule extends OpenSilexModule implements APIExtension {
    @Override
    public void bindServices(AbstractBinder binder) {
        binder.bindAsContract(StapleApiUtils.class);
    }
}
```

The service can then be used in the `StapleAPI` class by injecting it :

```java
@Api("Staple API")
public class StapleAPI {
    @Inject
    private StapleApiUtils stapleApiUtils;

    @GET
    @Path("ontology_file")
    public Response exportOntologyFile() throws Exception {
        Model stapleModel = stapleApiUtils.getStapleModel();
        
        //...
    }
}
```

In this example, here is the overall workflow of the dependency injection framework when receiving a `GET ontology_file`
request :

1. The framework tries to instantiate a `StapleAPI` object
2. Because the member variable `stapleApiUtils` is annotated with `@Inject`, the framework will look for a service bound
   to the `StapleApiUtils` contract
3. The framework finds that the contract is bound to the `StapleApiUtils` class, so it tries to instantiate it
4. Because the constructor is annotated with `@Inject`, the framework will look for a service bound to the `OpenSilex`
   contract
5. The framework finds that the `OpenSilex` contract is bound to a singleton instance of the `OpenSilex` class
6. The framework can now call the `StapleApiUtils` constructor using the instance of `OpenSilex` that it found
7. The framework can now instantiate `StapleAPI` using the `StapleApiUtils` that was just instantiated

## Services registered by default

Some services are registered at the startup by the `RestApplication` class. These are :

- The unique instance of the `OpenSilex` class
- For each module :
  - The instance of the module class (subclass of `OpenSilexModule`)
  - The instance of the configuration object, if defined (such as `CoreConfig` for the core module)
- Classes that implements `Service` and their factory (such as `SPARQLServiceFactory` which provides `SPARQLService` 
  objects)
- Classes that are annotated with both `@Service` and `@SelfBound` annotations

## Sources

- https://en.wikipedia.org/wiki/Dependency_injection
- https://javaee.github.io/hk2/introduction.html
- https://mkyong.com/webservices/jax-rs/jersey-and-hk2-dependency-injection-auto-scanning/
