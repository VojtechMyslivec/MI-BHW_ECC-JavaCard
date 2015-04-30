#!/bin/bash

retez="final byte[] expanze = {"

for nibble in {0,1}{0,1}{0,1}{0,1}; do 
   echo "$retez"

   binarne=`echo "$nibble" |sed 's/\(.\)/0\1/g'`
   dekadicky=`echo "obase=10;ibase=2; $binarne" | bc`

   retez=`printf "   (byte) 0x%02x, // '%s' -> '%s'" "$dekadicky" "$nibble" "$binarne"`
done

echo "${retez/,/ }"
echo "};"

