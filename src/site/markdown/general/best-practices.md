OpenSILEX - Best pratices
================================================================================


# Follow Unix Philosophy
--------------------------------------------------------------------------------

[Do one thing and do it well !](https://en.wikipedia.org/wiki/Unix_philosophy#Do_One_Thing_and_Do_It_Well)

This helps to keep application architecture clean and independent.


# Make It (Work|Right|Fast)
--------------------------------------------------------------------------------

- Start coding with minimal required features
- Add extension features one by one
- Optimize only when you observe performance issues

This helps to avoid [premature optimization traps](https://medium.com/@thiagoricieri/anti-patterns-by-example-premature-optimization-f46056dd1e39).


# Organize code by feature
--------------------------------------------------------------------------------

Each module can contains one or many features.

Each feature must have it's own package.

A feature can have sub-packages.

If a feature start to be too big, it should be moved in a separated module.

This helps to keep smaller packages, modules independence and improve testability.


# Use semantic versionning
--------------------------------------------------------------------------------

We use ["SemVer 2.0"](https://semver.org/) semantic versionning system.

Version X.Y.Z numbers must change according to the following rules.

- X: Major change in the API which change existing behavior and breaks compatibility.
- Y: Minor change in the API add new optional feature or behavior with no breaking changes since the last Major.
- Z: Patch changes for bug fixes or estetic changes.


# Comment wisely
--------------------------------------------------------------------------------

We use [Javadoc](https://www.tutorialspoint.com/java/java_documentation.htm) to comment Java code.

Whe use it with the following rules:

- Add header to all classes:
```
// TODO INSERER HEADER TEMPLATE
```

- Comment with javadoc all classes.
When updating any class, add your name as `@author`.
```
// TODO INSERER EXAMPLE
```

- Comment with javadoc all methods with there parameters and return types.
```
// TODO INSERER EXAMPLE
```

- Comment with javadoc all members.
```
// TODO INSERER EXAMPLE
```

- Comment with javadoc all constants.
```
// TODO INSERER EXAMPLE
```

- Comment complex SPARQL requests in javadoc.
```
// TODO INSERER EXAMPLE
```

- Inside methods prefer usage of `LOGGER.debug` instead of inline comments.
```
// TODO INSERER EXAMPLE
```

- When adding new feature or option use Javadoc tag `@since X.Y.Z` to describe the change.
```
// TODO INSERER EXAMPLE
```

- When adding new feature or option also add entry into the `CHANGELOG` file of corresponding module(s)
```
// TODO INSERER EXAMPLE
```

This helps generating a good code documentation and debugging.


# Choose libraries carefully
--------------------------------------------------------------------------------

Before adding a library to the application, especially if this library is shared,
take time to think about:

- Is it well documented ?
- Is it in active development ?
- Is there a big community around this library ?
- Is it easy to integrate ?
- Does it make the technological stack more complex ?
- Is it optional ?
- Do I really need it ?

Library should be integrated in OpenSilex application as their own module,
unless it brings usefulls feature for every modules without major change.

This helps to add feature to the application that are fully optionnal and independent.


# Test your code
--------------------------------------------------------------------------------

Create Unit Test to check classes behaviors.

Create Integration Test to check API behaviors

This helps to reduce maintenance effort and regressions during software life.


# Avoid String manipulation
--------------------------------------------------------------------------------

Convert as soon as possible String into real Java typed object models.

Create serialization and deserialization methods to add new models.

This helps to keep application logic clean.


# Avoid null value
--------------------------------------------------------------------------------

Avoid to return null values from methods.

If you have to, at least create a "withNullCheck" version of this method which 
throw an Exception in case of null result.

This helps to avoid hard to debug NullPointerException.

