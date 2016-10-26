#!/bin/bash
ps -ef | grep floodlight | awk '{ print $2 }' | xargs -I {} kill -9 {}
