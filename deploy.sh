#!/bin/sh

cd out

# delete existing ones
rm `find . -not -type d | grep -v CVS`

# copy new contents from the targets folder
cd ../target/docs
cp -r --parents . ../../out/
cd ../..

# add new directories
cd out
for f in `find . -type d | grep -v CVS`
do
  if [ -d ${f}/CVS ]; then
  else
    echo adding new directory $f
    cvs add $f
  fi
done



# obtain the diff
cvs -nq upd | tr ' ' '\t' > ../list

# add new files and remove removed ones.
if [ `cat ../list | grep "^\?" | wc -l` -ne 0 ]; then
  cvs add    `cat ../list | grep "^\?" | cut -f2`
fi
if [ `cat ../list | grep "^\U" | wc -l` -ne 0 ]; then
  cvs remove `cat ../list | grep "^U"  | cut -f2`
fi

cvs -q commit -m "deployed new contents"
cvs -q upd -Pd

rm ../list
