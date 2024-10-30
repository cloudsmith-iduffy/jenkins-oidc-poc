import jenkins.model.*
import java.util.logging.Logger

def logger = Logger.getLogger("0001-install-plugins.groovy")
def instance = Jenkins.getInstance()
def pluginManager = instance.getPluginManager()
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

// Update the Update Center
logger.info("Updating the update center")
updateCenter.updateAllSites()

// List of required plugins
def requiredPlugins = [
    'oidc-provider',
    'credentials-binding'
]

def installedPlugins = false

requiredPlugins.each { pluginName ->
    def plugin = updateCenter.getById('default').getPlugin(pluginName)
    
    if (!pluginManager.getPlugin(pluginName)) {
        if (plugin) {
            logger.info("Installing ${pluginName}")
            def installFuture = plugin.deploy()
            installedPlugins = true
            
            // Wait for installation to complete
            while(!installFuture.isDone()) {
                logger.info("Waiting for plugin ${pluginName} installation to complete...")
                Thread.sleep(2000)
            }
        } else {
            logger.severe("Could not find plugin ${pluginName} in update center!")
        }
    } else {
        logger.info("Plugin ${pluginName} is already installed")
    }
}

if (installedPlugins) {
    logger.info("Plugins installation completed, waiting for 30 seconds before restart")
    Thread.sleep(30000)
    instance.save()
    instance.restart()
    return
}

logger.info("All required plugins are installed")