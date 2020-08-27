#!/usr/bin/env bash

mkdir json-ld-11.org
mkdir json-ld-11.org/compact
mkdir json-ld-11.org/expand
mkdir json-ld-11.org/flatten
mkdir json-ld-11.org/frame
mkdir json-ld-11.org/toRdf
mkdir json-ld-11.org/fromRdf

SUITE_FRAMING_URL=https://w3c.github.io/json-ld-framing/tests/
SUITE_URL=https://w3c.github.io/json-ld-api/tests/

echo "Downloading compact test suite"
curl -s "$SUITE_URL/compact-manifest.jsonld" -o json-ld-11.org/compact-manifest.jsonld > /dev/null
for entry in $(cat json-ld-11.org/compact-manifest.jsonld | jq '[.sequence[] | {input: .input, expect: .expect, context: .context}]' | grep 'compact/' | cut -d '"' -f4)
do 
	curl -s "${SUITE_URL}${entry}" -o json-ld-11.org/$entry > /dev/null
done

echo "Downloading expand test suite"
curl -s "$SUITE_URL/expand-manifest.jsonld" -o json-ld-11.org/expand-manifest.jsonld > /dev/null
for entry in $(cat json-ld-11.org/expand-manifest.jsonld | jq '[.sequence[] | {input: .input, expect: .expect}]' | grep 'expand/' | cut -d '"' -f4)
do 
	curl -s "${SUITE_URL}${entry}" -o json-ld-11.org/$entry > /dev/null
done

echo "Downloading flatten test suite"
curl -s "$SUITE_URL/flatten-manifest.jsonld" -o json-ld-11.org/flatten-manifest.jsonld > /dev/null
for entry in $(cat json-ld-11.org/flatten-manifest.jsonld | jq '[.sequence[] | {input: .input, expect: .expect, context: .context}]' | grep 'flatten/' | cut -d '"' -f4)
do 
	curl -s "${SUITE_URL}${entry}" -o json-ld-11.org/$entry > /dev/null
done


echo "Downloading framing test suite"
curl -s "$SUITE_FRAMING_URL/frame-manifest.jsonld" -o json-ld-11.org/frame-manifest.jsonld > /dev/null
for entry in $(cat json-ld-11.org/frame-manifest.jsonld | jq '[.sequence[] | {input: .input, expect: .expect, frame: .frame}]' | grep 'frame/' | cut -d '"' -f4)
do 
	curl -s "${SUITE_FRAMING_URL}${entry}" -o json-ld-11.org/$entry > /dev/null
done

echo "Downloading fromRdf test suite"
curl -s "$SUITE_URL/fromRdf-manifest.jsonld" -o json-ld-11.org/fromRdf-manifest.jsonld > /dev/null
for entry in $(cat json-ld-11.org/fromRdf-manifest.jsonld | jq '[.sequence[] | {input: .input, expect: .expect}]' | grep 'fromRdf/' | cut -d '"' -f4)
do 
	curl -s "${SUITE_URL}${entry}" -o json-ld-11.org/$entry > /dev/null
done

echo "Downloading toRdf test suite"
curl -s "$SUITE_URL/toRdf-manifest.jsonld" -o json-ld-11.org/toRdf-manifest.jsonld > /dev/null
for entry in $(cat json-ld-11.org/toRdf-manifest.jsonld | jq '[.sequence[] | {input: .input, expect: .expect}]' | grep 'toRdf/' | cut -d '"' -f4)
do 
	curl -s "${SUITE_URL}${entry}" -o json-ld-11.org/$entry > /dev/null
done

tar -czvf json-ld-11.org.tgz json-ld-11.org
mv json-ld-11.org.tgz src/test/resources/
rm -rf json-ld-11.org



