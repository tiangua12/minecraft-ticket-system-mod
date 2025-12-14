#!/bin/bash
# 找到所有 .java 文件并合并到 output.txt
find . -name "*.java" -print0 | while IFS= read -r -d '' file; do
#    echo "--------------------" >> output.txt
 #   echo "File: $file" >> output.txt
 #   echo "--------------------" >> output.txt
    cat "$file" >> output.txt
   
done
echo "这个是第二个版本" >> output.txt
