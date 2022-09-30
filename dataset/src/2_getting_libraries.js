const utils = require("./utils");
const fs = require('fs');
const shell = require('shelljs')
const path = require("path");
const librariesPath = path.resolve(__dirname, "/Users/cesarsv/deptrim-experiments-resources/libraries");
const jdblDatasetPath = path.resolve(__dirname, "../data_input/jdbl_dataset.json");

const obj = JSON.parse(fs.readFileSync(jdblDatasetPath, 'utf8'));
describeDataset(obj);

function describeDataset(obj) {
  console.log("Total number of libraries: " + Object.keys(obj).length);
}

for (let lib in obj) {
  console.log(obj[lib]['groupId'] + ":" + obj[lib]['artifactId']);
  let repoName = obj[lib]['repo_name'];
  let projectName = obj[lib]['repo_name'].split("/")[1];
  let releases = obj[lib]['releases'];

  let release = Object.keys(releases)[0];
  let commitId =  Object.values(releases)[0];
  let projectNameAndRelease = projectName + "_" + release;

  console.log("Cloning library " + repoName + " to " + projectNameAndRelease);
  clone(projectNameAndRelease, repoName);

  console.log("Checkout " + projectNameAndRelease + "to " + commitId);
  checkout(projectNameAndRelease, projectName, commitId);
  //
  // console.log("Compiling " + projectNameAndRelease);
  // compile(projectName);
  //
  console.log("Dependency tree " + projectNameAndRelease);
  mavenDependencyTree(projectNameAndRelease, projectName);

  console.log("Dependency list " + projectNameAndRelease);
  mavenDependencyList(projectNameAndRelease, projectName);
}


// for (let release in releases) {
//   let commitId = releases[release];
//   let projectNameAndRelease = projectName + "_" + release;
//
//   console.log("Cloning library " + repoName + " to " + projectNameAndRelease);
//   clone(projectNameAndRelease, repoName);
//
//   console.log("Checkout " + projectNameAndRelease + "to " + commitId);
//   checkout(projectNameAndRelease, projectName, commitId);
//   //
//   // console.log("Compiling " + projectNameAndRelease);
//   // compile(projectName);
//   //
//   console.log("Dependency tree " + projectNameAndRelease);
//   mavenDependencyTree(projectNameAndRelease, projectName);
//
//   console.log("Dependency list " + projectNameAndRelease);
//   mavenDependencyTree(projectNameAndRelease, projectName);
// }


function clone(projectNameAndRelease, repoName) {
  shell.cd(librariesPath);
  shell.mkdir(projectNameAndRelease);
  shell.cd(librariesPath + "/" + projectNameAndRelease);
  shell.exec('git clone https://github.com/' + repoName);
}

function checkout(projectNameAndRelease, projectName, commitId) {
  shell.cd(librariesPath + "/" + projectNameAndRelease + "/" + projectName);
  shell.exec('git checkout ' + commitId);
}

function compile(projectName) {
  const path = librariesPath + "/" + projectName;
  shell.cd(path);
  shell.exec('export JAVA_HOME=$(/usr/libexec/java_home -v "1.8.333.02")')
  shell.exec('source ~/.zshrc');
  shell.exec('mvn compile');
  shell.exec('mvn compiler:testCompile');
}

function mavenDependencyTree(projectNameAndRelease, projectName) {
  shell.cd(librariesPath + "/" + projectNameAndRelease + "/" + projectName);
  shell.exec(
      'mvn dependency:tree -DoutputFile=/Users/cesarsv/deptrim-experiments-resources/libraries/dependency-tree_' +
      projectNameAndRelease + '.txt'
  );
}

function mavenDependencyList(projectNameAndRelease, projectName) {
  shell.cd(librariesPath + "/" + projectNameAndRelease + "/" + projectName);
  shell.exec(
      'mvn dependency:list -DoutputFile=/Users/cesarsv/deptrim-experiments-resources/libraries/dependency-list_' +
      projectNameAndRelease +
      '.txt'
  );
}

function depclean(projectName) {
  const path = librariesPath + "/" + projectName;
  shell.cd(path);
  shell.exec('mvn se.kth.castor:depclean-maven-plugin:2.0.3:depclean -DcreateResultJson=true');
}
