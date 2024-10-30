import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import io.jenkins.plugins.oidc_provider.*
import java.util.logging.Logger
import groovy.json.JsonOutput
import net.sf.json.JSONObject
import net.sf.json.JSONArray

def logger = Logger.getLogger("0100-create-oidc-credential.groovy")
def instance = Jenkins.getInstance()
def issuerUrl = System.getenv('NGROK_URL')

def credentialId = "oidc-token-cred"

def credentialsStore = SystemCredentialsProvider.getInstance().getStore()
def domain = Domain.global()
def existingCredentials = credentialsStore.getCredentials(domain).find { it.id == credentialId }
def oidcCredential

if (existingCredentials == null) {
    try {
        oidcCredential = new IdTokenStringCredentials(
            CredentialsScope.GLOBAL,
            credentialId,
            "OIDC Token for Jenkins"
        )
        oidcCredential.setAudience("jenkins")
        
        // Set issuer to the ngrok URL
        oidcCredential.setIssuer(issuerUrl)
        
        credentialsStore.addCredentials(domain, oidcCredential)
        logger.info("Created OIDC token credential")
    } catch (Exception e) {
        logger.severe("Failed to create OIDC credential: " + e.getMessage())
        e.printStackTrace()
    }
} else {
    oidcCredential = existingCredentials
}

instance.save()

// Create directory if it doesn't exist
def publicHttpServerDir = new File("/public-http-server")
if (!publicHttpServerDir.exists()) {
    publicHttpServerDir.mkdirs()
}

// Create .well-known directory
def wellKnownDir = new File(publicHttpServerDir, ".well-known")
if (!wellKnownDir.exists()) {
    wellKnownDir.mkdirs()
}

try {
    // Get OpenID Configuration
    def configJson = Keys.openidConfiguration(oidcCredential.getIssuer())
    new File(wellKnownDir, "openid-configuration").setText(JsonOutput.prettyPrint(configJson.toString()))
    logger.info("Successfully wrote OpenID Configuration content to disk")
} catch (Exception e) {
    logger.severe("Failed to write OpenID Configuration content: " + e.getMessage())
    e.printStackTrace()
}

try {
    // Get JWKS
    def jwksJson = new JSONObject().accumulate("keys", new JSONArray().element(Keys.key(oidcCredential)))
    new File(publicHttpServerDir, "jwks").setText(JsonOutput.prettyPrint(jwksJson.toString()))
    logger.info("Successfully wrote JWKS content to disk")
} catch (Exception e) {
    logger.severe("Failed to write JWKS content: " + e.getMessage())
    e.printStackTrace()
}

println("####### OIDC Provider has been configured #######")
println("Please configure cloudsmith with provider url " + oidcCredential.getIssuer())
