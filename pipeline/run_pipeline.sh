#!/bin/bash

# ============================================================
# Input: Name URL Dir Release CommitID
# ============================================================

# To run in background in aquiles server:
# nohup /home/ubuntu/cesarsv/pipeline/run_pipeline.sh > run_pipeline.log &
# scp -i ~/.ssh/aquiles.pem -r ubuntu@129.192.81.91:/home/ubuntu/cesarsv/pipeline/results.zip ~/IdeaProjects/deptrim-experiments/pipeline/results/results.zip

# WORK WELL (Build is successful and Depclean is successful)
# ./pipeline.sh "jimfs" "https://github.com/google/jimfs.git" "jimfs" "1.2" "3bc54fae2feb218dcf5427d2626720fc09ef38d1";
# ./pipeline.sh "modelmapper" "https://github.com/modelmapper/modelmapper.git" "core" "3.1.1" "894c658609f47f817ce20e41f6e87e1f803663ee";
# ./pipeline.sh "OpenPDF" "git@github.com:LibrePDF/OpenPDF.git" "openpdf" "1.3.30" "0c9c4ca393b01444b7cb13bb1d12da202bd0d458"; # to be added to the paper
# ./pipeline.sh "scribejava" "https://github.com/scribejava/scribejava.git" "scribejava-core" "8.3.2" "466f37c6faf5a9a2de9c87ae1bce71b617a6185b";
# ./pipeline.sh "lettuce-core" "https://github.com/lettuce-io/lettuce-core.git" "" "6.2.2" "77c4ea587fdba73f688c95a481d2743b6fc94fcb"; # to be added to the paper, has 37 specialized DTs and 2600 tests
# ./pipeline.sh "classgraph" "https://github.com/classgraph/classgraph.git" "" "4.8.154" "92b644677964496fb841ba41bed52247f8a24786";
# ./pipeline.sh "helidon" "https://github.com/helidon-io/helidon.git" "openapi" "2.5.6" "99cf5add9b4049581f08aae9eddaf0280070f2bb";
./pipeline.sh "poi-tl" "https://github.com/Sayi/poi-tl.git" "poi-tl" "1.12.2" "5d5004311a406b7d5843be76322bf208071b5969"; # to be added to the paper
# ./pipeline.sh "immutables" "https://github.com/immutables/immutables.git" "gson" "2.9.3" "6e192030320eaf7a8b5f146c39ae5a17b413aa37"; # to be added to the paper
# ./pipeline.sh "guice" "https://github.com/google/guice.git" "core" "5.1.0" "b0ff10c8ec8911137451623a333d6daa65f73d8a";
# ./pipeline.sh "Chronicle-Map" "https://github.com/OpenHFT/Chronicle-Map.git" "" "3.23.5" "26e26c132290ad8049c97e0c44eb7f33b63c1c60";
# ./pipeline.sh "graphhopper" "git@github.com:graphhopper/graphhopper.git" "core" "6.2" "6d3da37960f56aa6b9c4b1ffd77f70ebebff8747";
# ./pipeline.sh "Recaf" "git@github.com:Col-E/Recaf.git" "" "2.21.13" "c66f23801493bd866db757b0594c1fceaa30dce0";
# ./pipeline.sh "RxRelay" "https://github.com/cesarsotovalero/RxRelay.git" "" "3.0.1" "e9fc1586192ca1ecdbc41ae39036cbf0d09428b5";
# ./pipeline.sh "checkstyle" "https://github.com/checkstyle/checkstyle.git" "" "10.5.0" "dbeb9024c861ad11b194e40d8c6e08d7e6ec5122";
# ./pipeline.sh "commons-validator" "https://github.com/apache/commons-validator.git" "" "1.7" "f9bb21748a9f9c50fbc31862de25ed49433ecc88";
# ./pipeline.sh "httpcomponents-client" "https://github.com/apache/httpcomponents-client.git" "httpclient5" "5.2.1-RC1" "d8f702fb4d44c746bb0edf00643aa7139cb8bdf7"
# ./pipeline.sh "jacop" "https://github.com/cesarsotovalero/jacop.git" "" "4.9.0" "4fa3b8f2ea74df17b40ebe564552c28735a82885";
# ./pipeline.sh "java-faker" "https://github.com/cesarsotovalero/java-faker.git" "" "1.0.2" "0bed1d8381cd0b3319a6f3bb03efc74728334f6a";
# ./pipeline.sh "jcabi-github" "https://github.com/jcabi/jcabi-github.git" "" "1.27.0" "02f3ab93156349c2f66989ac675bd6292462d724";
# ./pipeline.sh "jooby" "https://github.com/jooby-project/jooby.git" "jooby" "2.16.1" "4d7be54dad429b5aeb5266387df14b0781c78357";
# ./pipeline.sh "mybatis-3" "https://github.com/mybatis/mybatis-3.git" "" "mybatis-3.5.11" "c195f12808a88a1ee245dc86d9c1621042655970";
# ./pipeline.sh "pdfbox" "https://github.com/cesarsotovalero/pdfbox.git" "pdfbox" "2.0.27" "07ddd189b4f76c9e265d7c80d95979642567af6b";
# ./pipeline.sh "qart4j" "https://github.com/cesarsotovalero/qart4j.git" "" "1.0.0" "04d17792030e02792a198c92c3dfee53a81bb051";
# ./pipeline.sh "tablesaw" "https://github.com/jtablesaw/tablesaw.git" "json" "v0.43.1" "05823f66246ea191e62ad0658d2fed0b080d5334";
# ./pipeline.sh "tika" "https://github.com/apache/tika.git" "tika-core" "2.6.0" "41319f3c294b13de5342a80570b4540f7dd04a3e";
# ./pipeline.sh "undertow" "https://github.com/undertow-io/undertow.git" "core" "2.2.21.Final" "56c91f129b1c2a55cf3287836cc68c80acce54c6";
# ./pipeline.sh "woodstox" "https://github.com/cesarsotovalero/woodstox.git" "" "6.4.0" "6de8bf7bfed4baa1c05d5a916fd5f96335708a84";
# ./pipeline.sh "flink" "https://github.com/apache/flink.git" "flink-java" "1.15.3" "c41c8e5cfab683da8135d6c822693ef851d6e2b7";
# ./pipeline.sh "pf4j" "git@github.com:pf4j/pf4j.git" "pf4j" "release-3.8.0" "efaed93c10dd9d114335e2a344e8bca04fd00c63";
# ./pipeline.sh "CoreNLP" "https://github.com/stanfordnlp/CoreNLP" "" "v4.5.1" "f7782ff5f235584b0fc559f266961b5ab013556a";

# not work
#./pipeline.sh "sofa-bolt" "https://github.com/sofastack/sofa-bolt.git" "" "1.6.6" "25157f74611d25df821a866d6e47207810c6d30a";
#./pipeline.sh "seckill" "https://github.com/codingXiaxw/seckill.git" "" "1.0" "fd24d99d232424cd141e64600b277fb8cf3950e2";
#./pipeline.sh "jeromq" "https://github.com/zeromq/jeromq.git" "" "0.5.4" "c678eaba2c26d3daf9cad372b0183afbe288a7c4";
#./pipeline.sh "C-OCR" "https://github.com/ctripcorp/C-OCR.git" "Detection" "1.0.0" "345ec57c9e0afaf2fab903f90cadbc5376a8ad74";
#./pipeline.sh "Bukkit" "https://github.com/Bukkit/Bukkit.git" "" "1.7.9" "9b2d3d1a5a303c393d89cd27ff12fadd589b37f1";
#./pipeline.sh "soot" "https://github.com/soot-oss/soot.git" "" "4.4.0" "3ef70c0f310db256a20b722517bd4753d603573b";
#./pipeline.sh "Mapper" "https://github.com/abel533/Mapper.git" "core" "4.2.2" "a29b37dc9b9b76290329603aa467d92e16c53978";
#./pipeline.sh "hertzbeat" "https://github.com/dromara/hertzbeat.git" "collector" "1.2.4" "900d3716d14418507b2a456d4e6f9dc7f24565dc";
#./pipeline.sh "gecco" "https://github.com/xtuhcy/gecco.git" "" "1.2.38" "aa35e24aa83e3f7ae1be5fb9c2a6505f227bceb7";
#./pipeline.sh "flume" "https://github.com/apache/flume.git" "flume-ng-core" "1.11" "1a15927e594fd0d05a59d804b90a9c31ec93f5e1";
#./pipeline.sh "iotdb" "https://github.com/apache/iotdb.git" "cli" "1.0.0" "fbbca3ffa3d54b2757c22502f7211b5a2be72c96";
#./pipeline.sh "light-task-scheduler" "https://github.com/ltsopensource/light-task-scheduler.git" "lts-core" "1.7.0" "124fc63c5292191ac72486cf4cf9505334689764";
#./pipeline.sh "WebCollector" "https://github.com/CrawlScript/WebCollector.git" "" "2.73" "a130996dac3219049befec2cb5d5cc8f7f2e0f2f";
#./pipeline.sh "shopizer" "https://github.com/shopizer-ecommerce/shopizer.git" "sm-core" "3.2.5" "052d2ed3c026525329405cef433c0aca7dd2cee3";
#./pipeline.sh "Bastillion" "https://github.com/bastillion-io/Bastillion.git" "" "3.14.0" "62bc1d2af69b83f42b42f16aa98b319d5df0bd44";
#./pipeline.sh "curator" "https://github.com/apache/curator.git" "curator-client" "5.4.0" "526e38c77fdd3d24f155ab49886fe7a9d8aab2d5";
#./pipeline.sh "jersey" "https://github.com/javaee/jersey.git" "core-server" "2.27" "8e2466c2d3826b31cd28593cb515b26be8cd1065";
#./pipeline.sh "thymeleaf" "https://github.com/thymeleaf/thymeleaf.git" "" "3.1.1" "1995e915be287a534068b4ec0ecfc086389887c8";
#./pipeline.sh "weixin-popular" "https://github.com/liyiorg/weixin-popular.git" "" "2.8.30" "499b2483bf2949e68c27476e4e6a4c3e82e392e1";
#./pipeline.sh "heritrix3" "https://github.com/internetarchive/heritrix3.git" "engine" "3.4.0" "292d2998cb94963a12944ccf4775a006d59c40dc";
#./pipeline.sh "Luyten" "https://github.com/deathmarine/Luyten.git" "" "0.5.35" "5ab63d4050bdb24d1662b1eaf63b6ded8af263c9";
#./pipeline.sh "cglib" "https://github.com/cglib/cglib.git" "cglib" "3.3.0" "9a14a6cc0b2ef50e69839b754d32ba4d8675d3ab";
#./pipeline.sh "jstorm" "https://github.com/alibaba/jstorm.git" "jstorm-core" "2.4.0" "c90575474ca9d40f6ec45a1818b5d42fd95edac3";
#./pipeline.sh "liquibase" "https://github.com/liquibase/liquibase.git" "liquibase-coreite" "4.19.0" "705547f489b09cbd722b838d153d6741484e5aad";
#./pipeline.sh "parceler" "https://github.com/johncarl81/parceler.git" "parceler" "1.1.13" "0a282021fd2e4a19f6ea4cd7e072ce57e0ff6498";
#./pipeline.sh "light-4j" "https://github.com/networknt/light-4j.git" "audit" "2.1.5" "9da5f16b59d92078c2c28b6739ca036f503da723";
#./pipeline.sh "kylin" "https://github.com/apache/kylin.git" "core-job" "4.0.3" "322ab6e5ee9738c5a07165af398c1faeeeacb079";
#./pipeline.sh "Alink" "https://github.com/alibaba/Alink.git" "core" "1.6.0" "5bf7ba1cd08283519ed06d499d38e2de1bfd8ab9";
#./pipeline.sh "cobar" "https://github.com/alibaba/cobar.git" "manager" "1.2.7" "58099876f2710c05af418d9542e166a85887e082";
#./pipeline.sh "sofa-jraft" "https://github.com/sofastack/sofa-jraft.git" "jraft-core" "1.3.12" "2bc70de21d0237b495e732b714c6a069e9be94f6";
#./pipeline.sh "ksql" "https://github.com/confluentinc/ksql.git" "ksqldb-engine" "5.0.0" "47f29cc991d3d06778702df9038698c8fbcb3ef5";
#./pipeline.sh "quartz" "https://github.com/quartz-scheduler/quartz.git" "quartz-core" "2.3.4" "3533e4063644c0436ac5e873a75b647703aea6dd";
#./pipeline.sh "hazelcast" "https://github.com/hazelcast/hazelcast.git" "hazelcast" "5.2.1" "ebe76d349499e1ba29aeebef35a1d8c81edfcfab";
#./pipeline.sh "gephi" "https://github.com/gephi/gephi.git" "" "0.10.1" "13c875d5e6c08828468a85a3f99f86c3b8d36b40";
#./pipeline.sh "uid-generator" "https://github.com/baidu/uid-generator.git" "" "1.0.0" "2fcbc13d2016fcfb7648a18296951f6942215255";
#./pipeline.sh "google-java-format" "https://github.com/google/google-java-format.git" "core" "1.15.0" "edf036c7fd07a719c5197e04f8da181e7cdb81ee";
#./pipeline.sh "kafka-ui" "https://github.com/provectus/kafka-ui.git" "kafka-ui-api" "0.5.0" "027d9b4653e2f3ea13d4de6a0b2bd568106ffb40";
#./pipeline.sh "hbase" "https://github.com/apache/hbase.git" "hbase-client" "2.5.2" "3e28acf0b819f4b4a1ada2b98d59e05b0ef94f96";
#./pipeline.sh "PowerJob" "https://github.com/PowerJob/PowerJob.git" "powerjob-client" "4.2.1" "ccbe11ed0e44b3c92c14d04e3746363b1d5ea084";
#./pipeline.sh "alluxio" "https://github.com/Alluxio/alluxio.git" "shell" "2.9.0" "d5919d8d80ae7bfdd914ade30620d5ca14f3b67e";
#./pipeline.sh "jsonschema2pojo-core" "https://github.com/joelittlejohn/jsonschema2pojo.git" "jsonschema2pojo-core" "1.1.2" "97ab6af78deb4ba42d55a5177a4d9a6cf2db89ef";
#./pipeline.sh "javacv" "https://github.com/bytedeco/javacv.git" "platform" "1.5.8" "022992443611cecec1c23d3f8d3d1588936d13af";
#./pipeline.sh "elasticsearch-sql" "https://github.com/NLPchina/elasticsearch-sql.git" "" "8.5.3" "426ab94077368dab7bbe383a952d90d7f2dd9dde";
#./pipeline.sh "vjtools" "https://github.com/vipshop/vjtools.git" "" "1.0.8" "5c27d752d6ccfee6841d80383b729b2007f15a3a";
#./pipeline.sh "Mycat-Server" "https://github.com/MyCATApache/Mycat-Server.git" "" "1.6.7.6" "f3ea7bde8ab7d6e33d2df166cec0f34d8e5b11ff";
#./pipeline.sh "OpenRefine" "https://github.com/OpenRefine/OpenRefine.git" "main" "3.6.2" "579a6f718c83d9b5d4c88ca0cf3d44b1b74e6d60";
#./pipeline.sh "cryptomator" "https://github.com/cryptomator/cryptomator.git" "" "1.6.17" "c0f73a2802a095e9f282fecc316b8db145969630";
#./pipeline.sh "cim" "https://github.com/crossoverJie/cim.git" "vjtop" "1.0.5" "19540d84797ad377941b571804ffdf610376e83f";
#./pipeline.sh "zeppelin" "git@github.com:apache/zeppelin.git" "zeppelin-common" "v0.10.1" "fd74504199247f8a9f9c3950abf193c97cf1d771";
#./pipeline.sh "redisson" "https://github.com/redisson/redisson.git" "redisson" "3.19.0" "cb305d35bb5fc3678e825cc3d7ed6009b721c552";  # tests are skipped in the config file
#./pipeline.sh "javaparser" "https://github.com/javaparser/javaparser.git" "javaparser-symbol-solver-core" "3.24.8" "3926ccabdac3341f365bf867ea2c2a11d1ab224b"; # has no tests
#./pipeline.sh "gson" "https://github.com/google/gson.git" "metrics" "2.10" "dd92e49b279f335006433148e673fdfb2c387074"; # has no tests
#./pipeline.sh "para" "https://github.com/Erudika/para.git" "para-core" "1.47.2" "41d900574e2e159b05fbd23aaab1f6e554ab8fc3"; # has no tests
#./pipeline.sh "lanterna" "https://github.com/cesarsotovalero/lanterna.git" "native-integration" "3.1.1" "1b74f1c07a769f83b5beb5cb7d62bd51d53de449"; # has no tests
#./pipeline.sh "onedev" "https://github.com/theonedev/onedev.git" "server-core" "7.9.0" "e8724f4d8b6fefa377645810400b9a8d723efa7c";
#./pipeline.sh "webmagic" "https://github.com/code4craft/webmagic.git" "webmagic-core" "0.8.0" "43ce1a0db94f22e69f71de4fedc9df203890c397";
#./pipeline.sh "neo4j" "https://github.com/neo4j/neo4j.git" "annotations" "4.4.16" "9e386f7db1834e2a74f1040899db6f18a0fbe4b3";
#./pipeline.sh "jedis" "https://github.com/redis/jedis.git" "" "4.3.1" "448207be431d6832ede5ddd28f40508e033905c8";
#./pipeline.sh "zookeeper" "https://github.com/apache/zookeeper.git" "zookeeper-server" "3.6.4" "d65253dcf68e9097c6e95a126463fd5fdeb4521c";
#./pipeline.sh "commons-pool" "https://github.com/apache/commons-pool.git" "" "2.11.1" "abb1a0797b406566f0214c688871ab7e8fdc2601";
#./pipeline.sh "spring-boot-admin" "https://github.com/codecentric/spring-boot-admin.git" "spring-boot-admin-server" "2.7.9" "0ac7a6be5f849b6260e38e68e6f0610f39a1a729";
#./pipeline.sh "nifi" "https://github.com/apache/nifi.git" "nifi-bootstrap" "1.19.1" "a7236ecc9123113ba5b9aaa3baab06354778116f";
#./pipeline.sh "rultor" "https://github.com/yegor256/rultor.git" "" "1.71.2" "2d36b41262b6285d4c1d41b9b55b6d5c20cfa3fb";
#./pipeline.sh "error-prone" "https://github.com/google/error-prone.git" "core" "2.16.0" "b1dcc4c9fef5c4e601b0a8124432a9504930686f";
#./pipeline.sh "vertx-unit" "https://github.com/vert-x3/vertx-unit.git" "" "4.3.6" "0af930f47f71b229de48f5d4308a841f599dcc7e";
#./pipeline.sh "shiro" "https://github.com/apache/shiro.git" "core" "1.10.1" "4a74eed1cdca1fd1ab5cf3cba75a39c7288fb492";
#./pipeline.sh "auto" "https://github.com/google/auto.git" "common" "1.10" "e065d1918f96301b7c2214ace217a69ae6ba8fdd"; # tests errors
#./pipeline.sh "spark" "https://github.com/perwendel/spark.git" "" "2.9.3" "48a94f1b9e0b0353642415fbfd97d5e1669c0f1b"; # tests errors
#./pipeline.sh "spoon" "https://github.com/INRIA/spoon.git" "" "10.2.0" "ee73f4376aa929d8dce950202fabb8992a77c9fb"; # tests errors
#./pipeline.sh "auto" "https://github.com/google/auto.git" "common" "1.10" "e065d1918f96301b7c2214ace217a69ae6ba8fdd"; # tests errors
#./pipeline.sh "jenkins" "https://github.com/jenkinsci/jenkins.git" "core" "2.381" "467d2fd1d8444ce1542c0f3a35353970f5a01a66"; # not dependencies specialized
#./pipeline.sh "jHiccup" "https://github.com/giltene/jHiccup.git" "" "2.0.10" "a440bdaed143e1445cbeab7c5bffd30989a435d0"; # not dependencies specialized
#./pipeline.sh "subzero" "https://github.com/square/subzero.git" "java/shared" "1.0.0" "d051983f5d9f400771f175b0db1fc6a362992d75"; # doesn't build
#./pipeline.sh "teavm" "https://github.com/konsoletyper/teavm.git" "core" "0.6.0" "379fae1c3b9aa9193753bebdd171ed383e2cff39"; # no dependency information available
#./pipeline.sh "commons-configuration" "https://github.com/apache/commons-configuration" "" "2.8.0" "59e5152722198526c6ffe5361de7d1a6a87275c7"; # Unknown error
#./pipeline.sh "github-api" "https://github.com/hub4j/github-api" "" "1.313" "9f1636f77a5c83eb6f4ec7e307c39a91f04a3d5c"; # Tests Errors
#./pipeline.sh "RxReplayingShare" "https://github.com/JakeWharton/RxReplayingShare.git" "replaying-share" "3.0.0" "ee5b7f45fb306cba5f2654b0d770c8bb52b28902";
#./pipeline.sh "Mybatis-PageHelper" "https://github.com/pagehelper/Mybatis-PageHelper.git" "" "5.3.2" "cfc4e0adbf1557f50c2d3647b14ac6b72a20d5e3";
#./pipeline.sh "java-apns" "https://github.com/notnoop/java-apns.git" "" "0.2.3" "094fc2e819a1b134e07ff89aa6fed215a3d08e8d";
#./pipeline.sh "AutoLoadCache" "https://github.com/qiujiayu/AutoLoadCache.git" "autoload-cache-core" "7.0.4" "c99cfb98b94a2fd2dd1862c0018c1a461d1fe1cd";
#./pipeline.sh "zxing" "https://github.com/zxing/zxing.git" "core" "3.5.1" "bb75858c9b391d37e8d78b5a5b640ff758df42fb";
#./pipeline.sh "vert.x" "https://github.com/eclipse-vertx/vert.x.git" "" "4.3.7" "2095bf92d0109c95ab65b9ac84d80872d6b27cc8"; # very long build time
#./pipeline.sh "spark" "https://github.com/perwendel/spark.git" "" "2.9.3" "48a94f1b9e0b0353642415fbfd97d5e1669c0f1b"; # build fails
#./pipeline.sh "jedis" "https://github.com/redis/jedis.git" "" "4.4.0" "0cf061ca34a39283f0a64269724a0f4ad708a6ff"; # build fails due to issue with a plugin fails
#./pipeline.sh "webmagic" "https://github.com/code4craft/webmagic.git" "webmagic-core" "0.8.0" "43ce1a0db94f22e69f71de4fedc9df203890c397"; # build fails
#./pipeline.sh "swagger-core" "https://github.com/swagger-api/swagger-core.git" "modules/swagger-core" "2.2.7" "603a037514efe20960d08a481552e0c0dd0e3869";
#./pipeline.sh "dubbo" "https://github.com/apache/dubbo.git" "dubbo-common" "3.1.4" "941f1b4530606b8d210a1e37fcb4d5869b3faca2";
#./pipeline.sh "deeplearning4j" "https://github.com/deeplearning4j/deeplearning4j.git" "deeplearning4j/deeplearning4j-core" "1.0.0" "f775f84e8ff25e96116af2b24ac7b1052d59f4b5";
#./pipeline.sh "hadoop" "https://github.com/apache/hadoop.git" "hadoop-yarn-project" "3.3.4" "a585a73c3e02ac62350c136643a5e7f6095a3dbb";

# ============================================================
# Generate dependency graph image
# mvn com.github.ferstl:depgraph-maven-plugin:4.0.2:aggregate -DcreateImage=true -DreduceEdges=false -DshowDuplicates=true -DshowConflicts=true -Dscopes=compile,provided;cd target;
# dot -Tsvg dependency-graph.dot > dependency-graph.svg

# ============================================================
# Short commit IDs
# ============================================================
#\href{https://github.com/google/auto/commit/c698816ebd45185e0265ed566b267bc786a306c1}{6a306c1}
#\href{https://github.com/checkstyle/checkstyle/commit/dbeb9024c861ad11b194e40d8c6e08d7e6ec5122}{6ec5122}
#\href{https://github.com/apache/commons-pool/commit/abb1a0797b406566f0214c688871ab7e8fdc2601}{fdc2601}
#\href{https://github.com/apache/commons-validator/commit/f9bb21748a9f9c50fbc31862de25ed49433ecc88}{33ecc88}
#\href{https://github.com/google/error-prone/commit/b1dcc4c9fef5c4e601b0a8124432a9504930686f}{930686f}
#\href{https://github.com/apache/flink/commit/c41c8e5cfab683da8135d6c822693ef851d6e2b7}{1d6e2b7}
#\href{https://https://github.com/google/gson/commit/dd92e49b279f335006433148e673fdfb2c387074}{c387074}
#\href{https://https://github.com/apache/httpcomponents-client/commit/d8f702fb4d44c746bb0edf00643aa7139cb8bdf7}{cb8bdf7}
#\href{https://https://github.com/radsz/jacop/commit/1a395e6add22caf79590fe9d1b2223bfb6ed0cd0}{6ed0cd0}
#\href{https://https://github.com/DiUS/java-faker/commit/e23d6067c8f83b335a037d24e6107a37eb0b9e6e}{b0b9e6e}
#\href{https://https://github.com/javaparser/javaparser/commit/3926ccabdac3341f365bf867ea2c2a11d1ab224b}{1ab224b}
#\href{https://https://github.com/jcabi/jcabi-github/commit/02f3ab93156349c2f66989ac675bd6292462d724}{462d724}
#\href{https://https://github.com/jooby-project/jooby/commit/4d7be54dad429b5aeb5266387df14b0781c78357}{1c78357}
#\href{https://https://github.com/mabe02/lanterna/commit/08d04a2a1d7b8a788d0d18f2d8d999b8fedaa1c4}{edaa1c4}
#\href{https://https://github.com/mybatis/mybatis-3/commit/c195f12808a88a1ee245dc86d9c1621042655970}{2655970}
#\href{https://https://github.com/apache/nifi/commit/a7236ecc9123113ba5b9aaa3baab06354778116f}{778116f}
#\href{https://https://github.com/Erudika/para/commit/41d900574e2e159b05fbd23aaab1f6e554ab8fc3}{4ab8fc3}
#\href{https://https://github.com/apache/pdfbox/commit/e72963ca5b283a87828ee731cd85c0b6baf1ff57}{af1ff57}
#\href{https://https://github.com/dieforfree/qart4j/commit/e82c7ba34ef648883b933817a395821d6cf8b2ff}{cf8b2ff}
#\href{https://https://github.com/redisson/redisson/commit/cb305d35bb5fc3678e825cc3d7ed6009b721c552}{721c552}
#\href{https://https://github.com/yegor256/rultor/commit/2d36b41262b6285d4c1d41b9b55b6d5c20cfa3fb}{0cfa3fb}
#\href{https://https://github.com/apache/shiro/commit/4a74eed1cdca1fd1ab5cf3cba75a39c7288fb492}{28fb492}
#\href{https://https://github.com/perwendel/spark/commit/48a94f1b9e0b0353642415fbfd97d5e1669c0f1b}{69c0f1b}
#\href{https://https://github.com/INRIA/spoon/commit/ee73f4376aa929d8dce950202fabb8992a77c9fb}{a77c9fb}
#\href{https://https://github.com/codecentric/spring-boot-admin/commit/0ac7a6be5f849b6260e38e68e6f0610f39a1a729}{9a1a729}
#\href{https://https://github.com/jtablesaw/tablesaw/commit/05823f66246ea191e62ad0658d2fed0b080d5334}{80d5334}
#\href{https://https://github.com/apache/tika/commit/41319f3c294b13de5342a80570b4540f7dd04a3e}{dd04a3e}
#\href{https://https://github.com/undertow-io/undertow/commit/56c91f129b1c2a55cf3287836cc68c80acce54c6}{cce54c6}
#\href{https://https://github.com/vert-x3/vertx-unit/commit/0af930f47f71b229de48f5d4308a841f599dcc7e}{99dcc7e}
#\href{https://https://github.com/FasterXML/woodstox/commit/e8f00401bebd103f62d51383ef53da2cd58bd89e}{58bd89e}

# PROJECT URL
#\href{https://github.com/checkstyle/checkstyle}{checkstyle}
#\href{https://github.com/apache/commons-validator}{commons-validator}
#\href{https://github.com/apache/httpcomponents-client/tree/master/httpclient5}{httpcomponents-client/httpclient5}
#\href{https://github.com/radsz/jacop}{jacop}
#\href{https://github.com/DiUS/java-faker}{java-faker}
#\href{https://github.com/jcabi/jcabi-github}{jcabi-github}
#\href{https://github.com/jooby-project/jooby/tree/3.x/jooby}{jooby/jooby}
#\href{https://github.com/mybatis/mybatis-3}{mybatis-3}
#\href{https://github.com/apache/pdfbox/tree/trunk/pdfbox}{pdfbox/pdfbox}
#\href{https://github.com/dieforfree/qart4j}{qart4j}
#\href{https://github.com/jtablesaw/tablesaw/tree/master/json}{tablesaw/json}
#\href{https://github.com/apache/tika/tree/main/tika-core}{tika/tika-core}
#\href{https://github.com/undertow-io/undertow/tree/master/core}{undertow/core}
#\href{https://github.com/FasterXML/woodstox}{woodstox}

# ============================================================
# Stars
# ============================================================
#curl https://api.github.com/repos/google/auto | grep 'stargazers_count'
#curl https://api.github.com/repos/checkstyle/checkstyle | grep 'stargazers_count'
#curl https://api.github.com/repos/apache/commons-pool | grep 'stargazers_count'
#curl https://api.github.com/repos/apache/commons-validator | grep 'stargazers_count'
#curl https://api.github.com/repos/google/error-prone | grep 'stargazers_count'
#curl https://api.github.com/repos/apache/flink | grep 'stargazers_count'
#curl https://api.github.com/repos/google/gson | grep 'stargazers_count'
#curl https://api.github.com/repos/apache/httpcomponents-client | grep 'stargazers_count'
#curl https://api.github.com/repos/radsz/jacop | grep 'stargazers_count'
#curl https://api.github.com/repos/DiUS/java-faker | grep 'stargazers_count'
#curl https://api.github.com/repos/javaparser/javaparser | grep 'stargazers_count'
#curl https://api.github.com/repos/jcabi/jcabi-github | grep 'stargazers_count'
#curl https://api.github.com/repos/jooby-project/jooby | grep 'stargazers_count'
#curl https://api.github.com/repos/mabe02/lanterna | grep 'stargazers_count'
#curl https://api.github.com/repos/mybatis/mybatis-3 | grep 'stargazers_count'
#curl https://api.github.com/repos/apache/nifi | grep 'stargazers_count'
#curl https://api.github.com/repos/Erudika/para | grep 'stargazers_count'
#curl https://api.github.com/repos/apache/pdfbox | grep 'stargazers_count'
#curl https://api.github.com/repos/dieforfree/qart4j | grep 'stargazers_count'
#curl https://api.github.com/repos/redisson/redisson | grep 'stargazers_count'
#curl https://api.github.com/repos/yegor256/rultor | grep 'stargazers_count'
#curl https://api.github.com/repos/apache/shiro | grep 'stargazers_count'
#curl https://api.github.com/repos/perwendel/spark | grep 'stargazers_count'
#curl https://api.github.com/repos/INRIA/spoon | grep 'stargazers_count'
#curl https://api.github.com/repos/codecentric/spring-boot-admin | grep 'stargazers_count'
#curl https://api.github.com/repos/jtablesaw/tablesaw | grep 'stargazers_count'
#curl https://api.github.com/repos/apache/tika | grep 'stargazers_count'
#curl https://api.github.com/repos/undertow-io/undertow | grep 'stargazers_count'
#curl https://api.github.com/repos/vert-x3/vertx-unit | grep 'stargazers_count'
#curl https://api.github.com/repos/FasterXML/woodstox | grep 'stargazers_count'





