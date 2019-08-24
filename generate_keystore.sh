#!/bin/bash

BOUNCYCASTLE_PROVIDER_VERSION=162
BOUNCYCASTLE_PROVIDER_FILE=bcprov-jdk15on-${BOUNCYCASTLE_PROVIDER_VERSION}.jar
BOUNCYCASTLE_PROVIDER_URL=https://downloads.bouncycastle.org/java/${BOUNCYCASTLE_PROVIDER_FILE}

if [[ ! -f ${BOUNCYCASTLE_PROVIDER_FILE} ]]; then
    echo "Bouncycastle provider missing. Downloading from ${BOUNCYCASTLE_PROVIDER_URL}"
    curl ${BOUNCYCASTLE_PROVIDER_URL} -o ${BOUNCYCASTLE_PROVIDER_FILE}
    echo "Download finished."
fi

keytool -genkey -keyalg RSA \
	-alias selfsigned \
	-keystore keystore.jks \
	-storepass password \
	-validity 360 \
	-keysize 2048 \
	-ext SAN=DNS:localhost,IP:127.0.0.1 \
	-validity 9999 \
	-storetype BKS \
	-provider org.bouncycastle.jce.provider.BouncyCastleProvider \
	-providerpath ${BOUNCYCASTLE_PROVIDER_FILE}
	