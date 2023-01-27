#!/bin/bash

# Define the directory to search
root_dir=/Users/cesarsv/IdeaProjects/deptrim-experiments/pipeline/results

# Iterate over all subdirectories in the root directory
for dir in "$root_dir"/*/
do
    # Use cloc to count the number of lines of code in Java files
    loc=$(cloc --quiet --csv --include-lang=Java "$dir" | tail -1 | cut -d, -f5)
    # Print the subdirectory name and the number of lines
    echo "$dir : $loc"
done
