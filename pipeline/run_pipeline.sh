#!/bin/bash

# ============================================================
# Input: Name URL Dir Release CommitID
# ============================================================

# work fine
./pipeline.sh "tika" "https://github.com/apache/tika.git" "tika-core" "2.6.0" "41319f3c294b13de5342a80570b4540f7dd04a3e";
./pipeline.sh "undertow" "https://github.com/undertow-io/undertow.git" "core" "2.2.21.Final" "56c91f129b1c2a55cf3287836cc68c80acce54c6";
./pipeline.sh "mybatis-3" "https://github.com/mybatis/mybatis-3.git" "" "mybatis-3.5.11" "c195f12808a88a1ee245dc86d9c1621042655970";
./pipeline.sh "httpcomponents-client" "https://github.com/apache/httpcomponents-client.git" "httpclient5" "5.2.1-RC1" "d8f702fb4d44c746bb0edf00643aa7139cb8bdf7";
./pipeline.sh "tablesaw" "https://github.com/jtablesaw/tablesaw.git" "json" "v0.43.1" "05823f66246ea191e62ad0658d2fed0b080d5334";
./pipeline.sh "pdfbox" "https://github.com/apache/pdfbox.git" "pdfbox" "2.0.27" "e72963ca5b283a87828ee731cd85c0b6baf1ff57";
./pipeline.sh "error-prone" "https://github.com/google/error-prone.git" "core" "2.16.0" "b1dcc4c9fef5c4e601b0a8124432a9504930686f";
./pipeline.sh "jooby" "https://github.com/jooby-project/jooby.git" "jooby" "2.16.1" "4d7be54dad429b5aeb5266387df14b0781c78357";
./pipeline.sh "jacop" "https://github.com/radsz/jacop.git" "" "4.9.0" "1a395e6add22caf79590fe9d1b2223bfb6ed0cd0";
./pipeline.sh "auto" "https://github.com/google/auto.git" "common" "1.10.1" "c698816ebd45185e0265ed566b267bc786a306c1";
./pipeline.sh "para" "https://github.com/Erudika/para.git" "para-core" "1.47.2" "41d900574e2e159b05fbd23aaab1f6e554ab8fc3";
./pipeline.sh "javaparser" "https://github.com/javaparser/javaparser.git" "javaparser-symbol-solver-core" "3.24.8" "3926ccabdac3341f365bf867ea2c2a11d1ab224b";
./pipeline.sh "spoon" "https://github.com/INRIA/spoon.git" "" "10.2.0" "ee73f4376aa929d8dce950202fabb8992a77c9fb";
./pipeline.sh "java-faker" "https://github.com/DiUS/java-faker.git" "" "1.0.2" "e23d6067c8f83b335a037d24e6107a37eb0b9e6e";
./pipeline.sh "spark" "https://github.com/perwendel/spark.git" "" "2.9.3" "48a94f1b9e0b0353642415fbfd97d5e1669c0f1b";
./pipeline.sh "vertx-unit" "https://github.com/vert-x3/vertx-unit.git" "" "4.3.6" "0af930f47f71b229de48f5d4308a841f599dcc7e";
./pipeline.sh "commons-pool" "https://github.com/apache/commons-pool.git" "" "2.11.1" "abb1a0797b406566f0214c688871ab7e8fdc2601";
./pipeline.sh "Chronicle-Map" "https://github.com/OpenHFT/Chronicle-Map.git" "" "3.23.5" "26e26c132290ad8049c97e0c44eb7f33b63c1c60";
./pipeline.sh "gson" "https://github.com/google/gson.git" "metrics" "2.10" "dd92e49b279f335006433148e673fdfb2c387074";
./pipeline.sh "checkstyle" "https://github.com/checkstyle/checkstyle.git" "" "10.5.0" "dbeb9024c861ad11b194e40d8c6e08d7e6ec5122";
./pipeline.sh "dubbo" "https://github.com/apache/dubbo.git" "dubbo-common" "3.1.4" "941f1b4530606b8d210a1e37fcb4d5869b3faca2";
./pipeline.sh "zxing" "https://github.com/zxing/zxing.git" "core" "3.5.1" "bb75858c9b391d37e8d78b5a5b640ff758df42fb";
./pipeline.sh "deeplearning4j" "https://github.com/deeplearning4j/deeplearning4j.git" "omnihub" "1.0.0" "f775f84e8ff25e96116af2b24ac7b1052d59f4b5";
./pipeline.sh "vert.x" "https://github.com/eclipse-vertx/vert.x.git" "" "4.3.7" "2095bf92d0109c95ab65b9ac84d80872d6b27cc8";
./pipeline.sh "spark" "https://github.com/perwendel/spark.git" "" "2.9.3" "48a94f1b9e0b0353642415fbfd97d5e1669c0f1b";
./pipeline.sh "jedis" "https://github.com/redis/jedis.git" "" "4.4.0" "0cf061ca34a39283f0a64269724a0f4ad708a6ff";
./pipeline.sh "webmagic" "https://github.com/code4craft/webmagic.git" "webmagic-core" "0.8.0" "43ce1a0db94f22e69f71de4fedc9df203890c397";
./pipeline.sh "redisson" "https://github.com/redisson/redisson.git" "redisson" "3.19.0" "cb305d35bb5fc3678e825cc3d7ed6009b721c552";
./pipeline.sh "spring-boot-admin" "https://github.com/codecentric/spring-boot-admin.git" "spring-boot-admin-server" "2.7.9" "0ac7a6be5f849b6260e38e68e6f0610f39a1a729";
./pipeline.sh "swagger-api" "https://github.com/swagger-api/swagger-core.git" "modules/swagger-core" "2.2.7" "603a037514efe20960d08a481552e0c0dd0e3869";

# not work
#./pipeline.sh "jenkins" "https://github.com/jenkinsci/jenkins.git" "core" "2.381" "467d2fd1d8444ce1542c0f3a35353970f5a01a66";
#./pipeline.sh "jHiccup" "https://github.com/giltene/jHiccup.git" "" "2.0.10" "a440bdaed143e1445cbeab7c5bffd30989a435d0"; # not dependencies specialized
#./pipeline.sh "subzero" "https://github.com/square/subzero.git" "java/shared" "1.0.0" "d051983f5d9f400771f175b0db1fc6a362992d75"; # doesn't build
#./pipeline.sh "teavm" "https://github.com/konsoletyper/teavm.git" "core" "0.6.0" "379fae1c3b9aa9193753bebdd171ed383e2cff39"; # no dependency information available
#./pipeline.sh "commons-configuration" "https://github.com/apache/commons-configuration" "" "2.8.0" "59e5152722198526c6ffe5361de7d1a6a87275c7"; # Unknown error
#./pipeline.sh "github-api" "https://github.com/hub4j/github-api" "" "1.313" "9f1636f77a5c83eb6f4ec7e307c39a91f04a3d5c"; # Tests Errors
#./pipeline.sh "woodstox" "https://github.com/FasterXML/woodstox.git" "" "6.4.0" "e8f00401bebd103f62d51383ef53da2cd58bd89e"; # requires old java version
#./pipeline.sh "lanterna" "https://github.com/mabe02/lanterna.git" "native-integration" "3.1.1" "08d04a2a1d7b8a788d0d18f2d8d999b8fedaa1c4";
#./pipeline.sh "RxReplayingShare" "https://github.com/JakeWharton/RxReplayingShare.git" "replaying-share" "3.0.0" "ee5b7f45fb306cba5f2654b0d770c8bb52b28902";
#./pipeline.sh "Mybatis-PageHelper" "https://github.com/pagehelper/Mybatis-PageHelper.git" "" "5.3.2" "cfc4e0adbf1557f50c2d3647b14ac6b72a20d5e3";
#./pipeline.sh "qart4j" "https://github.com/dieforfree/qart4j.git" "" "1.0.0" "e82c7ba34ef648883b933817a395821d6cf8b2ff"; # requires old java version
#./pipeline.sh "RxRelay" "https://github.com/JakeWharton/RxRelay.git" "" "3.0.1" "f31451511dfd535ec0e7e302d19b90341f45d3a2"; # requires old java version
# ./pipeline.sh "java-apns" "https://github.com/notnoop/java-apns.git" "" "0.2.3" "094fc2e819a1b134e07ff89aa6fed215a3d08e8d";
#./pipeline.sh "AutoLoadCache" "https://github.com/qiujiayu/AutoLoadCache.git" "autoload-cache-core" "7.0.4" "c99cfb98b94a2fd2dd1862c0018c1a461d1fe1cd";

# ============================================================
# Generate dependency graph image
# mvn com.github.ferstl:depgraph-maven-plugin:4.0.2:aggregate -DcreateImage=true -DreduceEdges=false -DshowDuplicates=true -DshowConflicts=true -Dscopes=compile,provided
# dot -Tsvg dependency-graph.dot > dependency-graph.svg