const utils = require("./utils");
const fs = require('fs');
const shell = require('shelljs')
const path = require("path");

const projectsPath = path.resolve(__dirname, "../data_input");

const deptrimPath = path.resolve(__dirname, ".");
installDeptrim(deptrimPath);

fs.readdirSync(projectsPath).forEach(folder => {
  console.log(folder);
  switch (folder) {
    case "trimmed_pom_should_be_correct": {
      compile("trimmed_pom_should_be_correct");
      runDeptrim("trimmed_pom_should_be_correct");
    }
    case "CoreNLP-4.5.1": {
      compile("CoreNLP-4.5.1");
      runDeptrim("CoreNLP-4.5.1");
    }
    case "flink-release-1.16.0": {
      compile("flink-release-1.16.0/flink-core");
      runDeptrim("flink-release-1.16.0/flink-core");
    }
    case "jenkins-jenkins-2.361.3": {
      compile("jenkins-jenkins-2.361.3/core");
      runDeptrim("jenkins-jenkins-2.361.3/core");
    }
    case "spoon-spoon-core-10.2.0": {
      compile("spoon-spoon-core-10.2.0");
      runDeptrim("spoon-spoon-core-10.2.0");
    }
  }
});


function compile(projectFolder) {
  const path = projectsPath + "/" + projectFolder;
  shell.cd(path);
  shell.exec('export JAVA_HOME=$(/usr/libexec/java_home -v "1.8.333.02")')
  shell.exec('source ~/.zshrc');
  shell.exec('mvn clean compile');
  shell.exec('mvn compiler:testCompile');
}

function installDeptrim(deptrimPath) {
  shell.cd(deptrimPath);
  shell.exec('export JAVA_HOME=$(/usr/libexec/java_home -v "1.8.333.02")')
  shell.exec('source ~/.zshrc');
  shell.exec('git clone https://github.com/castor-software/deptrim.git');
  shell.cd("deptrim");
  shell.exec('mvn clean install -DskipTests');
}

function runDeptrim(projectFolder) {
  // run deptrim on the project folder
  const path = projectsPath + "/" + projectFolder;
  shell.cd(path);
  shell.exec('mvn se.kth.castor:deptrim-maven-plugin:0.0.1:deptrim -DcreatePomTrimmed=true');
}
