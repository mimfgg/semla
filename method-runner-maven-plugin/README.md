method-runner-maven-plugin
==========

A utility plugin to invoke arbitrary methods during a given maven phase

Usage:
```xml
<plugin>
    <groupId>io.semla</groupId>
    <artifactId>method-runner-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <classname>com.project.YourTypeGenerator</classname>
        <methodname>generateSomething</methodname>
        <parameters>
            <arg0>some parameter needed for your code to generate something</arg0>
        </parameters>
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

