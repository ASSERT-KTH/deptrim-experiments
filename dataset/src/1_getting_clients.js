const fs = require('fs');
const shell = require('shelljs')
const path = require("path");
const clientsPath = path.resolve(__dirname, "~/deptrim-experiments-resources/clients");
const jdblDatasetPath = path.resolve(__dirname, "../data/jdbl_dataset.json");

const obj = JSON.parse(fs.readFileSync(jdblDatasetPath, 'utf8'));
describeDataset(obj);

function describeDataset(obj) {
  console.log("Total number of libraries: " + Object.keys(obj).length);
}

// Choose commons-collections for the initial data collection
const commonsCollections = obj["org.apache.commons:commons-collections"];
const commonsCollectionsRepoName = obj["org.apache.commons:commons-collections"]["repo_name"];
const clients = commonsCollections["clients"];

for (let client in clients) {
  console.log(client)
  clients[client].forEach((element, index, array) => {
    let repoName = element["repo_name"];
    let projectName = element["repo_name"].split("/")[1];
    let commitId = element["commit"];
    console.log("Cloning client " + repoName + "to " + projectName);
    clone(repoName, projectName, commitId);
    console.log("Checkout " + projectName + "to " + commitId);
    checkout(projectName, commitId);
    console.log("Compiling " + projectName);
    compile(projectName);
    console.log("DepClean " + projectName);
    depclean(projectName);
  });
}

function clone(repoName) {
  shell.cd(clientsPath);
  shell.exec('git clone https://github.com/' + repoName);
}

function checkout(projectName, commitId) {
  shell.cd(clientsPath);
  shell.exec('git checkout ' + commitId);
}

function compile(projectName) {
  const path = clientsPath + "/" + projectName;
  shell.cd(path);
  shell.exec('export JAVA_HOME=$(/usr/libexec/java_home -v "1.8.333.02")')
  shell.exec('source ~/.zshrc');
  shell.exec('mvn compile');
  shell.exec('mvn compiler:testCompile');
}

function depclean(projectName) {
  const path = clientsPath + "/" + projectName;
  shell.cd(path);
  shell.exec(
      'mvn se.kth.castor:depclean-maven-plugin:2.0.3-SNAPSHOT:depclean -DcreateResultJson=true'
  );
}

