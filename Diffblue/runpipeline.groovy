def call(Map config = [:]) {
  def triggerCommand = 'start test'
  def clusterBranch = config.get('clusterBranch', 'sanity-test')
  def commit = ''
  def status = 0
  def diffblueTimeLimit = config.get('diffblueTimeLimit', 360).toInteger()
  boolean runDiffblue = config.get('runDiffblue', false)
  boolean forceRunDiffblue = config.get('forceRunDiffblue', false)
  

  properties ([
    disableConcurrentBuilds(),
    pipelineTriggers([
        issueCommentTriggers(triggerCommand)
    ])
  ])

  try {
    if (!env.CHANGE_BRANCH) {
        println "Skipping pipeline run for branch"
        return
    }
    createWorker(customImage: workerImage) {
        stage('configure') {
            timeout(30) {
                withCredentials ([ gitUsernamePassword(credentialsID: 'GEN_GITHUB_PAT') ]) {
                    sh "git config --global --add safe.directory ${WORKSPACE}"
                }
                commit = sh (script: "git log -n 1 --pretty=format: '%H'", returnStdout: true)
                checkout([

                ])
            }
        }
        stage('Diffblue') {
            timeout(diffblueTimeLimit) {
                // check the last build result
                def lastBuild = currentBuild.previousBuild
                def skipDiffblue = false
                if (lastBuild && lastBuild.description == "DiffblueRan") {
                  skipDiffblue = true
                }
                if (runDiffblue !skipDiffblue) {
                    def statusCode = diffblue(testFramework: testFramework)
                    status = statusCode
                    if (statusCode == 1) {
                        error 'failing in Diffblue stage'
                    }
                } else if (skipDiffblue) {
                  println "Diffblue stage is passed in previous run skipping..."
                } else {
                  println "skipping diffblue run as it was not enabled in jenkinsfile"
                }
            }
        }

        stage('Clean-up') {
            timeout(10) {
                if (status < 100) {
                    withCredentials ([
                        string(credentialsID: 'GITHUB_KEY', variable: 'GITHUB_TOKEN'),
                    ]) {
                        withEnv([
                            "GIT_COMMIT=${commit}"
                        ]) {
                            sh "python3 pulsar-test-automation/cicd/wrapper.py --run revert_pipeline_changes"
                        }
                    }
                } else {
                    echo "skipping..."
                }
            }
        }
    }
  }
  catch (exception ex) {
    echo "Error during pipeline run"
        throw ex
  }
}