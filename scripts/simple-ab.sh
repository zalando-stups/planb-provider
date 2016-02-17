#!/bin/bash
# simple Apache Bench
echo -n 'username=test1&scope=uid&password=test1&grant_type=password&realm=%2Fservices' > temp-post.data
ab -n 100 -c 10 -T 'application/x-www-form-urlencoded' -A test1:test1 -p temp-post.data $1
