void call() {
    // Define variables
    String JENKINS_ADMIN_USERNAME = sh(script: "oc get secret jenkins-admin-token -o jsonpath={.data.username} " +
            "-n ${NAMESPACE} | base64 --decode", returnStdout: true)
    String JENKINS_ADMIN_PASSWORD = sh(script: "oc get secret jenkins-admin-token -o jsonpath={.data.password} " +
            "-n ${NAMESPACE} | base64 --decode", returnStdout: true)
    String JENKINS_URL_WITH_CREDS = "http://$JENKINS_ADMIN_USERNAME:$JENKINS_ADMIN_PASSWORD@jenkins.${NAMESPACE}.svc:8080"
    String gerritSecretName = "gerrit-ciuser-password"
    String registryRepoName = "registry-regulations"
    String gerritUser = sh(script: "oc get secret $gerritSecretName -o jsonpath={.data.user} " +
            "-n $NAMESPACE | base64 --decode", returnStdout: true)
    String gerritPass = sh(script: "oc get secret $gerritSecretName -o jsonpath={.data.password} " +
            "-n $NAMESPACE | base64 --decode", returnStdout: true)
    String gerritHost = "gerrit.${NAMESPACE}.svc.cluster.local:8080"
    String repoUrl = "http://$gerritUser:$gerritPass@$gerritHost/$registryRepoName"
    String isRepoExists = sh(script: "set +x; git ls-remote $repoUrl | grep master > /dev/null", returnStatus: true)

    // if not first deploy
    if (isRepoExists == '0') {
        sh(script: "set +x; git clone $repoUrl")
        // bump version in settings.yaml
        def settingsYaml = readYaml file: "$registryRepoName/settings.yaml"
        def arrayVersion = settingsYaml.settings.general.version.tokenize('.')
        int regulationsVersion = arrayVersion[2].toInteger().next()
        settingsYaml.settings.general.version = "${arrayVersion[0]}.${arrayVersion[1]}.${regulationsVersion}"
        writeYaml file: "$registryRepoName/settings.yaml", data: settingsYaml, overwrite: true

        // add \n to the main-liquibase.xml
        sh(script: "echo -ne '\n' >> $registryRepoName/data-model/main-liquibase.xml")

        // commit changes to the registry-regulations
        sh(script: "set +x; cd $registryRepoName " +
                "&& git config user.name \"$gerritUser\" " +
                "&& git config user.email \"jenkins@example.com\" " +
                "&& git config user.password \"$gerritPass\" " +
                "&& git add . && git commit -m 'Redeploy DataModel' " +
                "&& git push origin master")

        // trigger jenkins registry-regulations build job
        sh(script: "curl -XPOST \"${JENKINS_URL_WITH_CREDS}/job/${registryRepoName}/job/MASTER-Build-${registryRepoName}/buildWithParameters\"")
    }
}

return this;
