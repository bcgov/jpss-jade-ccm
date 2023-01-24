# How-to manage keystores using keytool

This document summarizes how to use keytool to manage keystores (and trust stores).

## Import a certificate into an existing (or new) keystore

keytool -importcert -file ca.cer -storetype PKCS12 -keystore store.p12

## List certificates in a keystore

keytool -list -v -keystore store.p12

## Print details for a given certificate

keytool -printcert -v -file server-ca.cer