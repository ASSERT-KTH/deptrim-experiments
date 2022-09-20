const fs = require('fs');
const shell = require('shelljs')
const path = require("path");
const librariesPath = path.resolve(__dirname, "~/deptrim-experiments-resources/libraries");
const jdblDatasetPath = path.resolve(__dirname, "../data/jdbl_dataset.json");

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
  for (let release in releases){
    let commitId = releases[release];
    let projectNameAndRelease = projectName + "_" + release;

    console.log("Cloning library " + repoName + " to " + projectNameAndRelease + " at " + commitId);
    clone(repoName, release);

    console.log("Checkout " + projectNameAndRelease + "to " + commitId);
    checkout(projectName, commitId);

    console.log("Compiling " + projectNameAndRelease);
    compile(projectName);

    console.log("DepClean " + projectNameAndRelease);
    depclean(projectName);

  }
}

function clone(repoName, release) {
  shell.cd(librariesPath + "/" + release);
  shell.exec('git clone https://github.com/' + repoName);
}

function checkout(projectName, commitId) {
  shell.cd(librariesPath);
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

function depclean(projectName) {
  const path = librariesPath + "/" + projectName;
  shell.cd(path);
  shell.exec(
      'mvn se.kth.castor:depclean-maven-plugin:2.0.3:depclean -DcreateResultJson=true'
  );
}

