#!/bin/bash

# ============================================================
# Input: Name URL Dir Release CommitID
# ============================================================

# work fine
./pipeline.sh "tika" "https://github.com/apache/tika.git" "tika-core" "2.6.0" "41319f3c294b13de5342a80570b4540f7dd04a3e"
./pipeline.sh "undertow" "https://github.com/undertow-io/undertow.git" "core" "2.2.21.Final" "56c91f129b1c2a55cf3287836cc68c80acce54c6"
./pipeline.sh "mybatis-3" "https://github.com/mybatis/mybatis-3.git" "" "mybatis-3.5.11" "c195f12808a88a1ee245dc86d9c1621042655970"
./pipeline.sh "httpcomponents-client" "https://github.com/apache/httpcomponents-client.git" "httpclient5" "5.2.1-RC1" "d8f702fb4d44c746bb0edf00643aa7139cb8bdf7"
./pipeline.sh "tablesaw" "https://github.com/jtablesaw/tablesaw.git" "json" "v0.43.1" "05823f66246ea191e62ad0658d2fed0b080d5334"
./pipeline.sh "pdfbox" "https://github.com/apache/pdfbox.git" "pdfbox" "2.0.27" "e72963ca5b283a87828ee731cd85c0b6baf1ff57"
./pipeline.sh "error-prone" "https://github.com/google/error-prone.git" "core" "2.16.0" "b1dcc4c9fef5c4e601b0a8124432a9504930686f"
./pipeline.sh "jooby" "https://github.com/jooby-project/jooby.git" "jooby" "2.16.1" "4d7be54dad429b5aeb5266387df14b0781c78357"
./pipeline.sh "jacop" "https://github.com/radsz/jacop.git" "" "4.9.0" "1a395e6add22caf79590fe9d1b2223bfb6ed0cd0"
./pipeline.sh "auto" "https://github.com/google/auto.git" "common" "1.10.1" "c698816ebd45185e0265ed566b267bc786a306c1"
./pipeline.sh "para" "https://github.com/Erudika/para.git" "para-core" "1.47.2" "41d900574e2e159b05fbd23aaab1f6e554ab8fc3"
./pipeline.sh "javaparser" "https://github.com/javaparser/javaparser.git" "javaparser-symbol-solver-core" "3.24.8" "3926ccabdac3341f365bf867ea2c2a11d1ab224b"
./pipeline.sh "spoon" "https://github.com/INRIA/spoon.git" "" "10.2.0" "ee73f4376aa929d8dce950202fabb8992a77c9fb"
./pipeline.sh "java-faker" "https://github.com/DiUS/java-faker.git" "" "1.0.2" "e23d6067c8f83b335a037d24e6107a37eb0b9e6e"
./pipeline.sh "spark" "https://github.com/perwendel/spark.git" "" "2.9.3" "48a94f1b9e0b0353642415fbfd97d5e1669c0f1b"
./pipeline.sh "vertx-unit" "https://github.com/vert-x3/vertx-unit.git" "" "4.3.6" "0af930f47f71b229de48f5d4308a841f599dcc7e"
./pipeline.sh "commons-pool" "https://github.com/apache/commons-pool.git" "" "2.11.1" "abb1a0797b406566f0214c688871ab7e8fdc2601"

# may work
#./pipeline.sh "checkstyle" "https://github.com/checkstyle/checkstyle.git" "" "10.5.0" "dbeb9024c861ad11b194e40d8c6e08d7e6ec5122"

# not work
#./pipeline.sh "jenkins" "https://github.com/jenkinsci/jenkins.git" "core" "2.381" "467d2fd1d8444ce1542c0f3a35353970f5a01a66"
#./pipeline.sh "jHiccup" "https://github.com/giltene/jHiccup.git" "" "2.0.10" "a440bdaed143e1445cbeab7c5bffd30989a435d0" # not dependencies specialized
#./pipeline.sh "subzero" "https://github.com/square/subzero.git" "java/shared" "1.0.0" "d051983f5d9f400771f175b0db1fc6a362992d75" # doesn't build
#./pipeline.sh "teavm" "https://github.com/konsoletyper/teavm.git" "core" "0.6.0" "379fae1c3b9aa9193753bebdd171ed383e2cff39" # no dependency information available
#./pipeline.sh "commons-configuration" "https://github.com/apache/commons-configuration" "" "2.8.0" "59e5152722198526c6ffe5361de7d1a6a87275c7" # Unknown error
#./pipeline.sh "github-api" "https://github.com/hub4j/github-api" "" "1.313" "9f1636f77a5c83eb6f4ec7e307c39a91f04a3d5c" # Tests Errors
#./pipeline.sh "woodstox" "https://github.com/FasterXML/woodstox.git" "" "6.4.0" "e8f00401bebd103f62d51383ef53da2cd58bd89e" # requires old java version

# ============================================================
# Generate image
# mvn com.github.ferstl:depgraph-maven-plugin:4.0.2:aggregate -DcreateImage=true -DreduceEdges=false -DshowDuplicates=true -DshowConflicts=true -Dscopes=compile,provided
# dot -Tsvg dependency-graph.dot > dependency-graph.svg