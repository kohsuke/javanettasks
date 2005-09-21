This project uses Maven.

Note that unit tests are rather lengthy because it involves
in accessing java.net. To skip this step, do as follows:

maven -Dmaven.test.skip=true jar

("jar" can be substituted with any other target.)