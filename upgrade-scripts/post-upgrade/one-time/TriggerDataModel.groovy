void call() {
    // Define variables
    String JENKINS_ADMIN_USERNAME = sh(script: "oc get secret jenkins-admin-token -o jsonpath={.data.username} " +
            "-n ${NAMESPACE} | base64 --decode", returnStdout: true)
    String JENKINS_ADMIN_PASSWORD = sh(script: "oc get secret jenkins-admin-token -o jsonpath={.data.password} " +
            "-n ${NAMESPACE} | base64 --decode", returnStdout: true)
    String JENKINS_PATH = sh(script: "oc get route jenkins -o jsonpath={.spec.path} -n $NAMESPACE", returnStdout: true).replaceAll("/\\z", "")
    String JENKINS_URL_WITH_CREDS = "http://$JENKINS_ADMIN_USERNAME:$JENKINS_ADMIN_PASSWORD@jenkins.${NAMESPACE}.svc:8080$JENKINS_PATH"
    String registryRepoName = "registry-regulations"
    // trigger jenkins data-model build job
    sh(script: "set +x; curl -XPOST \"${JENKINS_URL_WITH_CREDS}/job/${registryRepoName}/job/MASTER-Build-${registryRepoName}-data-model/buildWithParameters\"")
}

return this;
