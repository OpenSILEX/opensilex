# Additional instructions for installing OpenSILEX on versions prior to 1.5.2

Due to an unpinned version of the Typescript dependency in the build process, running a build (with `mvn install`) will
fail for the source code of versions older than 1.5.2.

To fix this issue, we recommend upgrading your OpenSILEX installation to a later version (at least 1.5.2). If you cannot
do that, you can still make the build work on earlier versions by changing a line in the file
[opensilex-main/pom.xml](../../../../../opensilex-main/pom.xml) :

1. Find the plugin declaration for `com.github.eirslett:frontend-maven-plugin`
2. Find the execution section titled `install typescript and dts-generator globally`
3. Change the configuration arguments from `<arguments>global add typescript dts-generator</arguments>` 
   to `<arguments>global add typescript@6.0.3 dts-generator@3.0.0</arguments>`

The execution section should look like the following code example.

```xml
<!-- install typescript and dts-generator types definition generator globally -->
<execution>
    <id>install typescript and dts-generator globally</id>
    <goals>
        <goal>yarn</goal>
    </goals>
    <configuration>
        <arguments>global add typescript@6.0.3 dts-generator@3.0.0</arguments>
    </configuration>
    <phase>initialize</phase>
</execution>
```
