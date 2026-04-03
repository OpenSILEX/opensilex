# Contributing to OpenSILEX

The project welcomes contributions, large and small, from anyone.

The mailing list for contributing is [opensilex-help@groupe.renater.fr](mailto:opensilex-help@groupe.renater.fr).

The GitHub repository is a mirror of the official OpenSILEX repository hosted on a private GitLab instance.

The processes described here are guidelines, rather than fixed requirements.


## Contributions

Contributions should include:

* Tests
* Documentation as needed

Java doc is welcomed and other documentation should be located in the `opensilex-doc` module.

## Workflow

### GitHub issues

The project uses GitHub issues to track work.  Please create
a [GitHub issue](https://github.com/OpenSILEX/opensilex/issues) so that we can track a contribution.

As GitHub is not the main repository, please send an email at [opensilex-help@groupe.renater.fr](mailto:opensilex-help@groupe.renater.fr) with a link to the issue you created.

In order to avoid duplicated work or undesired contributions, we highly recommend to discuss your intended contributions
by using the mailing list before starting to work on them. You can still create the issue before to describe your intended contribution.
By talking to developers first, you can make sure that your work aligns with other plans, and that the approach is sound.

See [issue exemple #129](https://github.com/OpenSILEX/opensilex/issues/129)

### Github Pull Requests

Use the issue number (e.g. GH-129) in the pull request title.

To make a contribution:

* On GitHub, fork https://github.com/apache/jena into you GitHub account.
* During forking, deselect "Copy the master branch only".
* Create a branch from **develop** in your fork for the contribution.
* Make your changes. Include the OpenSILEX header at the top of each file.
* Generate a pull request via GitHub. Base branch is **develop**.

See [pull request exemple #130](https://github.com/OpenSILEX/opensilex/pull/130)

### Discussion and Merging

Once the pull request is ready to be reviewed, send again an email to [opensilex-help@groupe.renater.fr](mailto:opensilex-help@groupe.renater.fr)

A project committer will review the contribution and coordinate any project-wide discussion
needed. Review and discussion of the pull request itself takes place on
GitHub. Functional and acceptance tests may be done by the committer.

Please make sur to be up to date with the develop branch before requesting the review.
Also before requesting the review, please make sure that all tests are passing and project is compiling by running `mvn clean install`.

Since GitHub is not the main repository, the final merge will be done by a project committer. In order to do that the committer will
fetch your branch from your fork and merge it into the main repository. Your fix or feature will then be part of the next release.

### Code

Code style is about making the code clear for the next person
who looks at the code.

The project prefers code to be formatted in the common java style.

See, for illustration:
https://google.github.io/styleguide/javaguide.html#s4-formatting

For Vue.js components, follow the style guide at:
[component-guidelines-template.md](opensilex-doc/src/main/resources/vuejs/component-guidelines-template.md)

The codebase has a long history - not all of it follows this style.

Please don't mix reformatting and functional changes in the same file; it makes it harder
to review.

### Resources

Not sure to understand the codebase ? Take a look at the documentation. Good starting points are:
- [OpenSILEX Developer's installation](README.md)
- [Global architecture](opensilex-doc/src/main/resources/architecture/index.md)

Not sur about actual rules of a concept (exemple : rules of accounts, of scientific-objects import, ...) ?
See the [specifications website](https://opensilex.pages-forge.inrae.fr/opensilex-dev/specifications/)