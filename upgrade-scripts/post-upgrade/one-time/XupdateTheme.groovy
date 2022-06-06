void call() {
    String JENKINS_ADMIN_USERNAME = sh(script: "oc get secret jenkins-admin-token -o jsonpath={.data.username} " +
            "-n ${NAMESPACE} | base64 --decode", returnStdout: true)
    String JENKINS_ADMIN_PASSWORD = sh(script: "oc get secret jenkins-admin-token -o jsonpath={.data.password} " +
            "-n ${NAMESPACE} | base64 --decode", returnStdout: true)
    String JENKINS_PATH = sh(script: "oc get route jenkins -o jsonpath={.spec.path} -n $NAMESPACE", returnStdout: true).replaceAll("/\\z", "")
    String JENKINS_URL_WITH_CREDS = "http://$JENKINS_ADMIN_USERNAME:$JENKINS_ADMIN_PASSWORD@jenkins.${NAMESPACE}.svc:8080$JENKINS_PATH"
    String gerritSecretName = "gerrit-ciuser-password"
    String registryRepoName = "registry-regulations"
    String gerritUser = sh(script: "oc get secret $gerritSecretName -o jsonpath={.data.user} " +
            "-n $NAMESPACE | base64 --decode", returnStdout: true)
    String gerritPass = sh(script: "oc get secret $gerritSecretName -o jsonpath={.data.password} " +
            "-n $NAMESPACE | base64 --decode", returnStdout: true)
    String gerritPath = sh(script: "oc get route gerrit -o jsonpath={.spec.path} -n $NAMESPACE", returnStdout: true).replaceAll("/\\z", "")
    String gerritHost = "gerrit.${NAMESPACE}.svc.cluster.local:8080$gerritPath"
    String repoUrl = "http://$gerritUser:$gerritPass@$gerritHost/$registryRepoName"
    String isRepoExists = sh(script: "set +x; git ls-remote $repoUrl | grep master > /dev/null", returnStatus: true)
    String filePath = "$registryRepoName/global-vars/camunda-global-system-vars.yml"
    if (isRepoExists == '0') {
        sh(script: "set +x; rm -rf $registryRepoName; git clone $repoUrl")
        try {
            LinkedHashMap camundaGlobalSystemVars = readYaml file: filePath
            camundaGlobalSystemVars["themeFile"] = "DiiaLight.js"
            writeYaml file: filePath, data: camundaGlobalSystemVars, overwrite: true
            sh(script: "set +x; cd $registryRepoName " +
                    "&& git config user.name \"$gerritUser\" " +
                    "&& git config user.email \"jenkins@example.com\" " +
                    "&& git config user.password \"$gerritPass\" " +
                    "&& git add . && git commit -m 'Update themeFile in global-vars/camunda-global-system-vars.yml' " +
                    "&& git push origin master" +
                    "&& cd .. " +
                    "&& rm -rf $registryRepoName")
        } catch (Exception e) {
            println(e)
        }
        sh(script: "curl -XPOST \"${JENKINS_URL_WITH_CREDS}/job/${registryRepoName}/job/MASTER-Build-${registryRepoName}/buildWithParameters\"")
    }
}

return this;
