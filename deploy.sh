#!/bin/sh -x
#
# generate web site by using Maven and deploy that into
# the java.net CVS repository
# 
maven site:generate
if [ $? != 0 ]; then
  echo maven failed
  exit
fi

cd target/docs
cvs "-d:pserver:kohsuke@cvs.dev.java.net:/cvs" -z3 import -W "*.png -k 'b'" -W "*.gif -k 'b'" -m "deploying the new web contents" javanettasks/www/maven site-deployment t`date +%Y%m%d-%H%M%S`
cd ../..
cd ../www
date >> update.html
cvs commit -m "to work around a bug in java.net web updater" update.html
