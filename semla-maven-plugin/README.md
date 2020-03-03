semla-maven-plugin
==========

A maven plugin to generate typed EntityManager classes

Usage:
```xml
<plugin>
    <groupId>io.semla</groupId>
    <artifactId>semla-maven-plugin</artifactId>
    <version>0.0.1-alpha</version>
    <configuration>
        <sources>
            <source>/src/main/java/model/**</source>
        </sources>
    </configuration>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```
