#!/bin/bash
cp .env.base .env

SECRETS=("JWT_SECRET_KEY" "VT_API_KEY" "IP_INFO_TOKEN" "EMAIL_PROVIDER_API_KEY")
VAULT_NAME="smartlink-key-vault"
QUERY="value"

for SECRET_NAME in "${SECRETS[@]}"; do
	vaultSecret="${SECRET_NAME//_/-}"
	storedValue=$(az keyvault secret show --name "${vaultSecret}" --vault-name "${VAULT_NAME}" --query "${QUERY}" -o tsv )
	echo "${SECRET_NAME}=${storedValue}" >> .env

done