const utils = require("./utils");
const fs = require('fs');
const depcleanResults = 'depclean-results.json';

ls("~/deptrim-experiments-resources/clients")

async function ls(path) {
  const clientsDirectory = await fs.promises.opendir(path)
  for await (const projectName of clientsDirectory) {
    if (isFileInDirectory(depcleanResults, path + "/" + projectName.name)) {
      // console.log("depclean-results.json exists");
      const obj = JSON.parse(
          fs.readFileSync(
              path + "/" + projectName.name + "/" + depcleanResults,
              'utf8'
          )
      );
      getCommonsCollectionsUsage(obj);
    }
  }
}

function getCommonsCollectionsUsage(obj) {
  const clientName = obj["artifactId"];
  const dependencies = obj["children"];
  dependencies.forEach((element, index, array) => {
    let artifactId = element["artifactId"];
    if (artifactId === "commons-collections4") {
      let usedTypes = element["usedTypes"];
      let allTypes = element["allTypes"];
      if(usedTypes.length > 0) {
        allTypes.forEach((element, index, array) => {
          if (usedTypes.includes(element)) {
            console.log(clientName + "," + element + "," + "used");
          } else {
            console.log(clientName + "," + element + "," + "unused");
          }
        });
      }
    }
  });
}

function isFileInDirectory(filename, dir) {
  return fs.readdirSync(dir).includes(filename);
}