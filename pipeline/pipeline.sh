#!/bin/bash
# ============================================================
# This script is used to run the pipeline on a single project.
# ============================================================

java -version

# Get the start time
start=$(date +%s)

# Input arguments
REPO_NAME=$1
GITHUB_URL=$2
MODULE_DIR=$3
RELEASE=$4
COMMIT=$5

# Variables
PROJECTS_DIR="projects"
RESULTS_DIR="results"
logger_deptrim="[DEPTRIM]"
logger_maven="[MAVEN]"
logger_pipeline="[PIPELINE]"
CURRENT_DIR=$(pwd)

# Create data storage directories
mkdir -p "$CURRENT_DIR"/"$PROJECTS_DIR"
mkdir -p "$CURRENT_DIR"/"$RESULTS_DIR"
mkdir -p "$CURRENT_DIR"/"$RESULTS_DIR"/"$REPO_NAME"

# Clone the repo from URL
echo "====================================================="
echo "${logger_pipeline} Cloning $REPO_NAME"
cd "$CURRENT_DIR"/$PROJECTS_DIR
git clone --quiet "$GITHUB_URL"

# CD into the project directory
echo "${logger_pipeline} CDing into $REPO_NAME"
cd "$CURRENT_DIR"/$PROJECTS_DIR/"$REPO_NAME"

# Checkout the release
echo "${logger_pipeline} Checking out version $RELEASE at commit $COMMIT"
git checkout --quiet "$COMMIT"

# CD into the module if any
if [ -z "$MODULE_DIR" ]; then
  echo "${logger_pipeline} Project is single-module, will build $REPO_NAME"
  cd "$CURRENT_DIR"/$PROJECTS_DIR/"$REPO_NAME"
else
  echo "${logger_pipeline} Project is multi-module, CDing into $MODULE_DIR"
  cd "$CURRENT_DIR"/$PROJECTS_DIR/"$REPO_NAME"/"$MODULE_DIR"
fi

# Run the pipeline from module directory if any
echo "=========================================================================================="
echo "${logger_pipeline} Building original project and storing results in $REPO_NAME/$MODULE_DIR/original"
mkdir original
cp pom.xml pom-original.xml
cp pom.xml original/pom-original.xml
mvn -B clean package -Dcheckstyle.skip -DskipITs -Drat.skip=true -Dtidy.skip=true -Denforcer.skip=true -Dmaven.javadoc.skip=true -DskipBundle=true -Dlicense.skip=true -Dmaven.clean.skip=true >>original/maven.log
cp target/*.jar original/
mvn -B dependency:copy-dependencies -DincludeScope=runtime >>original/compile-scope-dependencies.log
mkdir original/compile-scope-dependencies/
cp -r target/dependency original/compile-scope-dependencies/
mvn -B dependency:copy-dependencies >>original/all-dependencies.log
mkdir original/all-dependencies/
cp -r target/dependency original/all-dependencies/
mvn -B dependency:tree >>original/dependency-tree.log
mvn -B dependency:list >>original/dependency-list.log

# RUN DEPTRIM WITH ALL DEPENDENCIES SPECIALIZED
echo "====================================================="
echo "${logger_deptrim} Running DepTrim with all dependencies specialized"
mkdir deptrim
mvn -B se.kth.castor:deptrim-maven-plugin:0.1.2:deptrim -DcreateSinglePomSpecialized=true -DverboseMode=true -DignoreScopes=test,provided,system,import,runtime >>deptrim/deptrim.log
mv libs-specialized deptrim
mvn -B clean package -Dcheckstyle.skip -DskipITs -Drat.skip=true -Dtidy.skip=true -Denforcer.skip=true -Dmaven.javadoc.skip=true -DskipBundle=true -Dmaven.clean.skip=true >>deptrim/maven.log

# CLEANUP
mvn -B clean package -q -Dcheckstyle.skip -DskipTests -Drat.skip=true -Dtidy.skip=true -Dtidy.skip=true -Denforcer.skip=true -Dmaven.javadoc.skip=true -DskipBundle=true

# RUN DEPTRIM WITH ONE DEPENDENCY SPECIALIZED PER POM
echo "====================================================="
echo "${logger_deptrim} Running DepTrim with one dependency specialized per pom"
mvn -B se.kth.castor:deptrim-maven-plugin:0.1.2:deptrim -DcreateDependencySpecializedPerPom=true -DverboseMode=true -DignoreScopes=test,provided,system,import,runtime >>deptrim.log

# EXECUTING POMS
echo "====================================================="
echo "${logger_deptrim} Number of poms generated by DepTrim:"
poms=$(find . -name "pom-specialized*.xml")
echo "${poms}"
successful_ssts="successful-ssts.log"
touch ${successful_ssts}
TST=0

for i in ${poms}; do
  length=${#i}
  cut_length=$((length - 4))
  specialized_pom=$(echo ${i} | cut -c 3-)
  # create a folder for the specialized pom
  output=$(echo ${i} | cut -c 3-${cut_length})
  mkdir "${output}"
  cp "${specialized_pom}" "${output}"/
  # Setting as main pom
  mv "${specialized_pom}" pom.xml
  # Running mvn clean package
  echo "====================================================="
  echo "${logger_pipeline} Building ${REPO_NAME}/${MODULE_DIR} with ${specialized_pom}"
  mvn -B clean package -Dcheckstyle.skip -DskipITs -Drat.skip=true -Dtidy.skip=true -Denforcer.skip=true -Dmaven.javadoc.skip=true -DskipBundle=true -Dlicense.skip=true -Dmaven.clean.skip=true >>${output}/maven.log
  mvn -B dependency:tree >>${output}/dependency-tree.log
  cp target/*.jar "${output}"/
  # Getting build status from maven log
  build_status=$(grep --text "BUILD SUCCESS" ${output}/maven.log | wc -l)
  test_status=$(grep --text "There are test failures" ${output}/maven.log | wc -l)
  # If BUILD SUCCESS, getting artifactId of specialized dependency
  if [ $build_status = 1 ] && [ $test_status = 0 ] && [[ $specialized_pom == *"_"* ]]; then
    line_number=$(grep -n "<groupId>se.kth.castor.deptrim.spl</groupId>" pom.xml | cut -d ":" -f 1)
    specialized_dependency=$(head -n $(($line_number+1)) pom.xml | tail -1)
    echo "${logger_pipeline} BUILD SUCCESS, will include ${specialized_dependency} in PST"
    echo $specialized_dependency >> ${successful_ssts}
  else
    echo "${logger_pipeline} will not include in PST"
  fi
  # Setting TST to 1 if fully specialized pom builds
  if [[ $specialized_pom != *"_"* ]]; then
    build_status=$(grep --text "BUILD SUCCESS" ${output}/maven.log | wc -l)
    test_status=$(grep --text "There are test failures" ${output}/maven.log | wc -l)
    if [ $build_status = 1 ] && [ $test_status = 0 ]; then
      TST=1
    fi
  fi
  # Restoring pom number
  mv pom.xml "${specialized_pom}"
  # Copying the results to deptrim directory
  mv "${output}" deptrim
done

rm -r libs-specialized

# RUN DEPCLEAN
echo "====================================================="
echo "${logger_deptrim} Running DepClean"
if [ -z "$MODULE_DIR" ]; then
  # shellcheck disable=SC2164
  cd "$CURRENT_DIR"/$PROJECTS_DIR/"$REPO_NAME"
else
  # shellcheck disable=SC2164
  cd "$CURRENT_DIR"/$PROJECTS_DIR/"$REPO_NAME"/"$MODULE_DIR"
fi
mv pom-original.xml pom.xml
mkdir depclean
mvn -B clean compile -q -Drat.skip=true -Dtidy.skip=true -Denforcer.skip=true -Dmaven.javadoc.skip=true -DskipBundle=true
mvn -B compiler:testCompile -q -Drat.skip=true -Dtidy.skip=true -Denforcer.skip=true -Dmaven.javadoc.skip=true -DskipBundle=true
mvn -B se.kth.castor:depclean-maven-plugin:2.0.6:depclean -DcreatePomDebloated=true -DignoreScopes=test,provided,system,import,runtime >>depclean/depclean.log

# BUILD WITH pom-debloated.xml
echo "====================================================="
echo "${logger_pipeline}  Building with pom-debloated.xml"
if [ -z "$MODULE_DIR" ]; then
  # shellcheck disable=SC2164
  cd "$CURRENT_DIR"/$PROJECTS_DIR/"$REPO_NAME"
else
  # shellcheck disable=SC2164
  cd "$CURRENT_DIR"/$PROJECTS_DIR/"$REPO_NAME"/"$MODULE_DIR"
fi
mv pom.xml pom-original.xml
mv pom-debloated.xml pom.xml
mkdir depclean/pom-debloated
mvn -B clean package -Dcheckstyle.skip -DskipITs -Drat.skip=true -Dtidy.skip=true -Denforcer.skip=true -Dmaven.javadoc.skip=true -DskipBundle=true -Dlicense.skip=true -Dmaven.clean.skip=true >>depclean/pom-debloated/maven.log
cp target/*.jar depclean/pom-debloated/
mvn -B dependency:copy-dependencies >>depclean/pom-debloated/all-dependencies.log
mv target/dependency depclean/pom-debloated/all-dependencies/
mv pom.xml depclean/pom-debloated/pom-debloated.xml

# BUILD pom-pst.xml WITH SUCCESSFUL SSTs
if [ $TST = 0 ]; then
  echo "====================================================="
  echo "${logger_pipeline}  Preparing pom-pst.xml and building it"
  pom_pst="pom-pst.xml"
  # Using pom-debloated.xml as baseline
  cp depclean/pom-debloated/pom-debloated.xml ${pom_pst}
  current_line=0
  while read l; do
    ((current_line+=1))
    # updating groupId if a successful build with an sst was logged previously
    was_successful=$(grep "$l" successful-ssts.log | wc -l)
    if [ $was_successful = 1 ]; then
      sed -i "$(($current_line-1))s/.*/<groupId>se.kth.castor.deptrim.spl<\/groupId>/" ${pom_pst}
    fi
  done < "$pom_pst"
  # Building and moving results to deptrim/pst/
  mv ${pom_pst} pom.xml
  mkdir deptrim/pst
  mvn clean package -Dcheckstyle.skip -DskipITs -Drat.skip=true -Dtidy.skip=true -Denforcer.skip=true -Dmaven.javadoc.skip=true -DskipBundle=true -Dlicense.skip=true -Dmaven.clean.skip=true >>deptrim/pst/maven.log
  mvn dependency:tree >>deptrim/pst/dependency-tree.log
  cp target/*.jar deptrim/pst/
  mv pom.xml deptrim/pst/${pom_pst}
  mv ${successful_ssts} deptrim/pst/
fi

# Restore original pom
echo "====================================================="
echo "${logger_pipeline}  Restoring original pom and exiting"
mv pom-original.xml pom.xml

echo "====================================================="
echo "${logger_pipeline}  Writing experiment execution time"
if [ -z "$MODULE_DIR" ]; then
  # shellcheck disable=SC2164
  cd "$CURRENT_DIR"/$PROJECTS_DIR/"$REPO_NAME"
else
  # shellcheck disable=SC2164
  cd "$CURRENT_DIR"/$PROJECTS_DIR/"$REPO_NAME"/"$MODULE_DIR"
fi
end=$(date +%s)
total_time_seconds=$((end - start))
total_time_minutes=$((total_time_seconds / 60))
total_time_hours=$((total_time_minutes / 60))
echo "$REPO_NAME,$total_time_seconds seconds,$total_time_minutes minutes,$total_time_hours hours" >>experiment_execution_time.txt

# Copy the results to the results directory
echo "=========================================================================================="
if [ -z "$MODULE_DIR" ]; then
  echo "${logger_pipeline}  Copying the results to ${CURRENT_DIR}/${RESULTS_DIR}/${REPO_NAME}"
  cp -r . "$CURRENT_DIR"/"$RESULTS_DIR"/"$REPO_NAME"
else
  echo "${logger_pipeline}  Copying the results to ${CURRENT_DIR}/${RESULTS_DIR}/${REPO_NAME}/${MODULE_DIR}"
  cp -r . "$CURRENT_DIR"/"$RESULTS_DIR"/"$REPO_NAME"/"$MODULE_DIR"
fi

exit 0
