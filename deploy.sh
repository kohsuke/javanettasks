#!/bin/sh
cd target/docs
cvs $CVSROOT_JAVANET import javanettasks/www/maven site-deployment t`date +%Y%m%d-%H%M%S`
