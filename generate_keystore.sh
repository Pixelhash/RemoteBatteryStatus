#!/bin/bash

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
	-providerpath bcprov-jdk15on-160.jar
	