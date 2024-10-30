// ./jenkins-config/init.groovy.d/0200-create-job.groovy
import jenkins.model.*
import hudson.model.*
import hudson.tasks.Shell
import java.util.logging.Logger

def logger = Logger.getLogger("0200-create-job.groovy")
def jenkins = Jenkins.getInstance()

def jobName = "cloudsmith-usage"

if (jenkins.getItem(jobName) == null) {
    try {
        def job = jenkins.createProject(FreeStyleProject.class, jobName)
        
        job.setDescription("Print OpenID Connect Token")
        
        def shell = new Shell('''
response=$(curl -X POST -H "Content-Type: application/json" -d "{\\"oidc_token\\": \\"$OIDC_TOKEN\\", \\"service_slug\\": \\"$CLOUDSMITH_SERVICE_ACCOUNT_SLUG\\"}" https://api.cloudsmith.io/openid/$CLOUDSMITH_ORG/)
echo $response
token=$(echo "$response" | jq -r ".token")
python3 -m venv jenkins
PIP_INDEX_URL="https://token:$token@dl.cloudsmith.io/basic/${CLOUDSMITH_ORG}/${CLOUDSMITH_REPO}/python/simple/"
./jenkins/bin/pip install flake8 pytest --index-url $PIP_INDEX_URL
''')
        
        job.getBuildersList().add(shell)
        
        def bindingWrapper = new org.jenkinsci.plugins.credentialsbinding.impl.SecretBuildWrapper([
            new org.jenkinsci.plugins.credentialsbinding.impl.StringBinding(
                "OIDC_TOKEN",
                "oidc-token-cred"
            )
        ])
        
        job.getBuildWrappersList().add(bindingWrapper)
        
        job.save()        
    } catch (Exception e) {
        logger.severe("Failed to create job: " + e.getMessage())
        e.printStackTrace()
    }
}