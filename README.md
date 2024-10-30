# Jenkins OIDC Authentication with Cloudsmith

> Note: This is not an official Cloudsmith solution. This is a proof of concept that is not approved or supported by Cloudsmith.

## What This Does

This project shows how to:

1. Set up Jenkins as an OpenID Connect (OIDC) provider
2. Use Jenkins-generated OIDC tokens to access private packages on Cloudsmith
3. Install Python packages from Cloudsmith using these tokens

## Quick Start

1. Set your Ngrok token:
```bash
export NGROK_AUTH=your_ngrok_token
```

2. Adjust docker-compose.yaml with your own Cloudsmith org and service account.

3. Start the services:
```bash
docker-compose up -d
```

4. Find the provider URL in the Jenkins logs:
```
jenkins-1  | ####### OIDC Provider has been configured #######
jenkins-1  | Please configure cloudsmith with provider url https://4c48c7279d89.ngrok.app
```

5. Configure an OIDC provider on cloudsmith.io using the URL from the logs.

6. Run the `cloudsmith-usage` Jenkins job at http://localhost:8080 to see it:
   - Exchange the Jenkins JWT for a Cloudsmith token
   - Successfully fetch pip packages

7. View the OIDC credential configuration at:
   <http://localhost:8080/manage/credentials/store/system/domain/_/credential/oidc-token-cred/update>

## How It Works

1. Jenkins creates OIDC tokens using [OpenID Connect Provider](https://plugins.jenkins.io/oidc-provider/)
2. Jenkins injects the token as an environment variable
3. The token is exchanged for a Cloudsmith temporary token
4. This temporary token is used to download packages

## Required Jenkins Plugins

### [OpenID Connect Provider](https://plugins.jenkins.io/oidc-provider/)
- Creates OIDC tokens for Jenkins jobs
- Generates necessary OIDC configuration files
- Uses RS256 for token signing

### [Credentials Binding Plugin](https://plugins.jenkins.io/credentials-binding/)
- Makes credentials available to Jenkins jobs
- Hides sensitive values in build logs

## Manual Setup

### 1. Install Plugins

1. Go to "Manage Jenkins" → "Plugins" → "Available Plugins"
2. Find and install:
   - "OIDC Connect Provider"
   - "Credentials Binding"
3. Select "Restart Jenkins when installation is complete"

### 2. Create OIDC Credential

![](.images/oidc-provider.png)

1. Go to "Manage Jenkins" → "Credentials" → "System" → "Global credentials"
2. Click "Add Credentials"
3. Select "Kind" → "OpenID Connect ID Token"
4. Fill in:
   - Scope: "Global"
   - ID: "oidc-token-cred"
   - Audience: jenkins
   - Description: "OIDC Token for Jenkins"
   - Issuer URL: Your public URL
5. Click "OK"

### 3. Host OIDC Configuration Files

After creating the credential, Jenkins will show you two URLs where you can get the configuration files:

1. OpenID Configuration URL:

```
http://your-jenkins-instance/manage/descriptorByName/io.jenkins.plugins.oidc_provider.IdTokenStringCredentials/wellKnownOpenidConfiguration?issuer=your-public-url
```

2. JWKS URL:

```
http://your-jenkins-instance/manage/descriptorByName/io.jenkins.plugins.oidc_provider.IdTokenStringCredentials/jwks?id=oidc-token-cred&issuer=your-public-url
```

Requirements for hosting these files:
- Put the OpenID configuration at `/.well-known/openid-configuration`
- Put the JWKS at `/jwks`
- Host domain must match the issuer URL in Jenkins

### 4. Create Jenkins Job

1. Click "New Item"
2. Enter name and select "Freestyle project"
3. In configuration:
   - Under "Build Environment" → "Use secret text(s) or file(s)"
   - Add → "Secret text"
   - Variable: "OIDC_TOKEN"
   - Credentials: Select your OIDC token
4. Add build step "Shell":

```bash
# Get Cloudsmith token
response=$(curl -X POST -H "Content-Type: application/json" \
    -d "{\"oidc_token\":$OIDC_TOKEN, \"service_slug\": $CLOUDSMITH_SERVICE_ACCOUNT_SLUG}" \
    https://api.cloudsmith.io/openid/${CLOUDSMITH_ORG}/)

# Get token from response
token=$(echo "$response" | jq -r ".token")

# Install packages using token
python -m venv jenkins
source ./jenkins/bin/activate
PIP_INDEX_URL="https://token:$token@dl.cloudsmith.io/basic/${CLOUDSMITH_ORG}/${CLOUDSMITH_REPO}/python/simple/"
pip install package-name --index-url $PIP_INDEX_URL
```
