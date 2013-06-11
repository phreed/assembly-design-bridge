assembly-design-bridge
======================

Used by meta to perform interactive component assembly.

To build...
pushd cdb-protobuf
mvn clean install
popd

push cdb-java
mvn clean install
java -jar ./target/meta-cdb-java-1.0.0.jar
popd


The subprojects are:


cdb-clojure :
Primarily used for testing.
Depends on cdb-java for framing and payload.

cdb-cpp :
The files for c++ framing, used by Creo.
Depends on cdb-protobuf.

cdb-csharp :
The files for c# framing.
Depends on cdb-protobuf.

cdb-java :
The files for java framing.
Includes the CDB message server.
Depends on cdb-protobuf.

cdb-protobuf :
The protobuf files for component assembly design.
Contains files generated (in part) from cdb-schema.

cdb-python :
The files for python framing.
Primarily used for testing.
Depends on cdb-protobuf.

cdb-schema :
The files from the GME CyPhy UML model.
The protobuf files are generated from xsd files
using protomak.
Protomak has some issues but appears to be 
adequate for our purposes.

README.md
