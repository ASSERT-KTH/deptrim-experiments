# Parses all dependency trees in a directory and output a list of all the dependencies in a TXT file.

END_FILE="_dependencies_all.txt"

for filename in /Users/cesarsv/deptrim-experiments-resources/libraries/*.txt; do
  echo "Processing $filename";
  grep "\---" $filename | grep -v "project " | cut -d "-" -f 4-7 | grep -v "(*)" | sed 's/->.*$/ /p' | sed -E 's/:[0-9]+\.[0-9]+.*$/ /g' | sed 's/[[:blank:]]*$//' | grep -v "\---" | sort -u > $filename$END_FILE
  echo "Created file in $filename$END_FILE";
done


#FILES='/Users/cesarsv/deptrim-experiments-resources/libraries/'
#
## shellcheck disable=SC2041
#for f in $FILES;
#do
#
#done


#grep "\---" besu-21.10.6_dependencies_tree.txt |
##grep -v "project " |
#cut -d "-" -f 4-7 |
#grep -v "(*)" |
#sed 's/->.*$/ /p' |
#sed -E 's/:[0-9]+\.[0-9]+.*$/ /g' |
#sed 's/[[:blank:]]*$//' |
#grep -v "\---" |
#sort -u > besu-21.10.6_dependencies_all.txt