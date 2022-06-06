void call() {
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
    String configMapName = "registry-logos"
    String logoHeader = sh(script: "oc get configmap ${configMapName}  -o jsonpath='{.binaryData.header-logo\\.svg}' " +
            "-n ${NAMESPACE}", returnStdout: true)
    String logoLoader = sh(script: "oc get configmap ${configMapName}  -o jsonpath='{.binaryData.loader-logo\\.svg}' " +
            "-n ${NAMESPACE}", returnStdout: true)
    String logoFavicon = sh(script: "oc get configmap ${configMapName}  -o jsonpath='{.binaryData.favicon\\.png}' " +
            "-n ${NAMESPACE}", returnStdout: true)
    String assetsDirectoryPath = "$registryRepoName/assets"
    if (isRepoExists == '0') {
        sh(script: "set +x; git clone $repoUrl")
        try {
            sh "mkdir $assetsDirectoryPath"
            sh(script: "echo $logoHeader | base64 -d > $assetsDirectoryPath/header-logo.svg")
            sh(script: "echo $logoLoader | base64 -d > $assetsDirectoryPath/loader-logo.svg")
            sh(script: "echo $logoFavicon | base64 -d > $assetsDirectoryPath/favicon.png")
            sh(script: "set +x; cd $registryRepoName " +
                    "&& git config user.name \"$gerritUser\" " +
                    "&& git config user.email \"jenkins@example.com\" " +
                    "&& git config user.password \"$gerritPass\" " +
                    "&& git add . && git commit -m 'Add logos' " +
                    "&& git push origin master" +
                    "&& cd .. " +
                    "&& rm -rf $registryRepoName")
        } catch (Exception e) {
            println(e)
        }
    }
}

return this;
