import jenkins.model.*
import java.util.logging.Logger

def logger = Logger.getLogger("0000-init-update-center.groovy")
def instance = Jenkins.getInstance()
def updateCenter = instance.getUpdateCenter()

// Wait for update center to initialize
int maxRetries = 5
int retryCount = 0
while (updateCenter.getSites().isEmpty() && retryCount < maxRetries) {
    logger.info("Update center not initialized yet. Waiting...")
    Thread.sleep(5000)
    retryCount++
}

if (updateCenter.getSites().isEmpty()) {
    logger.severe("Update center failed to initialize!")
    return
}

logger.info("Update center initialized")
updateCenter.updateAllSites()