import com.onresolve.osgi.AllBundlesApplicationContext
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl

def allBundlesApplicationContext = ScriptRunnerImpl.scriptRunner.getBean(AllBundlesApplicationContext)
def policyEnforcer = allBundlesApplicationContext.getBean(
    'com.atlassian.upm.atlassian-universal-plugin-manager-plugin', 'policyEnforcer'
)

policyEnforcer.getClass().declaredFields.find { 
    it.name == 'pluginUploadEnabled' 
}.with {
    setAccessible(true)
    setBoolean(policyEnforcer, true)
}