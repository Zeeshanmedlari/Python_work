As a devops engineer, i have a requirement to ensure the diffblue stage should be executed only once during the sanity pipeline run, if the last or any previous stage passed the diffblue stage then for subsequent run diffblue stage can be skipped.
Acceptence criteria:
1. No impact in current sanity pipeline
2. diffblue stage to be skipped if it passed once during a PR run.
3. give a option to force run diffblue stage if required.

I have 2 files ie ie diffblue.groovy and runpipeline.groovy, i'll provide these 2 files here, please make changes to those files so it can relate to the above requirement

Diffblue.groovy 

def gradleBuild() {
  sh '''#!/bin/bash -ex
    echo 'starting gradle build'
    items= (`ls -d images/*`)
    for i in "${items[@]}"; do
      cd ${i}
      if [ -f "build.gradle" ]; then
        ./gradlew clean build jacocoTestReport --stacktrace -x generateGitPropperties
        ls -lrt
      else
        echo "src folder not found...  skipping"
      fi
      cd $WORKSPACE
    done
  '''
}

def activateDiffblue() {
    sh ''' #!/bin/bash
      echo "activating diffblue.."
      mkdir -p ~/.diffblue/offline
      touch ~/.diffblue/offline/ls_activation.lic
      echo ${DCOVLICENSE} > ~./diffblue/offline/ls_activation.lic
      dcover activate --offline ${DCOVKEY}
      dcover license
      '''
}

def diffblueRun() {
  sh '''#!/bin/bash -ex
    echo "starting diffblue Run"
    items=(`ls -d images/*`))
    # checking if patch mode can be run
    if [ -f "$CHECKING_FILE "]; then
      export DIFFBLUE MODE=PATCH
      printenv | sort
      for i in "${items[@]}"; do
        cd ${i}
        if [ -f "build.gradle" ]; then
          git diff ${CHANGE_TARGET} ${CHANGE_BRANCH} >> diffblue.patch
          echo "PR change set (diffs)"
          pwd
          cat diffblue.patch
          dcover create --coverage-reports --report ${DB_REPORTING_SERVICE_URL} --batch --gradle --project=${REPO} --name=${JOB_NAME}_${BUILD_NUMBER} --testing-framework=${TEST_FRAMEWORK} --patch-only diffblue.patch --verbose
        cd $WORKSPACE
        fi
      done
    else
    # full mode 
      export DIFFBLUE_MODE=FULL
      printenv | sort
      touch $CHECK_FILE
      git add $CHECK_FILE
      for i in "${items[@]}"; do 
        cd ${i}
        if [ -f "build.gradle" ]; then
          dcover create --coverage-reports --report ${DB_REPORTING_SERVICE_URL} --batch --gradle --project=${REPO} --name=${JOB_NAME}_${BUILD_NUMBER} --testing-framework=${TEST_FRAMEWORK} --verbose
        fi
        cd $WORKSPACE
        done
    fi
  '''
}

def call(Map config = [:]) {
  def diffBlueCommit = "Pipeline AutoCommit: Added Diffblue Tests"
  def repo = env.JOB_NAME.split('/')[1]
  def testFramework = config.get('testFramework')
  withCredentials ([
    string(credentialsID: 'GEN_USER', variable: 'ARTIFACTORY_READONLY_USER'),
    string(credentialsID: 'ARTIFACTORY_KEY', variable: 'ARTIFACTORY_READONLY_TOKEN'),
    gitUsernamePassword(credentialsID: 'GEN_GITHUB_PAT')
  ]) {
    withEnv([
        "DB_REPORTING_SERVICE_URL=http://cc-diffblue-lnx:8080",
        "REPO=${repo}",
        "TEST_FRAMEWORK=${testFramework}",
        "DIFFBLUE_MODE=",
        "CHECK_FILE=.diffblueInit"
      ]) {
          def lasCommitUser = sh (script:"git show -s --pretty=%an", returnStdout: true).trim()
          def lasCommitMessage = sh (script:"git show -s --pretty=%B", returnStdout true).trim()
          if (lasCommitUser == env.GIT_USERNAME && lasCommitMessage == diffBlueCommit) {
            echo "skipping run as last commit was for diffBlue"
            return 0
          }
          gradleBuild()
          withCredentials ([
            string(credentialsID:'dcover.lic', variable: 'DCOVLICENSE'),
            string(credentialsID:'dcover_key', variable: 'DCOVKEY'),
          ]) {
            activateDiffblue()
          }
          diffblueRun()
          // commiting..
          sh '''
            echo "trying commit..."
            git add iamges/**/src/test/*
            git status
            git config user.mail ${GIT_USERNAME}@cisco.com
            git config user.name ${GIT_USERNAME}
            git config --global push.default matching
          '''
          def isFileStaged = sh (script:"git diff --cached --exit-code --quiet", returnstatus: true)
          if ( isFileStaged == 0) {
            echo "No diffblue changes to commit"
            return 0
          } else {
            sh "git commit -m '${diffBlueCommit}'"
            sh "git push"
            currentBuild.result = 'NOT_BUILT'
            return 103
          }
      }
  }
}

And this is my another file
runpipeline.groovy

def call(Map config = [:]) {
  def triggerCommand = 'start test'
  def clusterBranch = config.get('clusterBranch', 'sanity-test')
  def commit = ''
  def status = 0
  def diffblueTimeLimit = config.get('diffblueTimeLimit', 360).toInteger()
  boolean runDiffblue = config.get('runDiffblue', false)
  

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
                if (runDiffblue) {
                    def statusCode = diffblue(testFramework: testFramework)
                    status = statusCode
                    if (statusCode == 1) {
                        error 'failing in Diffblue stage'
                    }
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

In Jenkins shared library We have conditions here 
1. if we pass @Library('iota_lib@DEX-662') _
            runPipeline(runDiffblue: true,
            testFramework: 'junit-4'
)

This should check for the status for previous diffblue Run in the PR, if it was successful then it should skip and print as "Skipping diffblue as a previous run was successful ", if it was abort or failed then it should run and print as " as a previous run was not success, we'll run it again"  
Please provide the logic for same in the above code
This should run the Diffblue without skipping it.

2. if we pass @Library('iota_lib@DEX-662') _
               runPipeline(runDiffblue: true,
               testFramework: 'junit-4',
               forceRunDiffblue: true
)
This should run the Diffblue without skipping it.Since we are explicitly passing it 

3. if we pass @Library('iota_lib@DEX-662') _
               runPipeline()

This should not execute diffblue stage and skip printing "skipping diffblue run as it was not enabled in jenkinsfile"               

Please make the changes in both the files accordingly and please provide the logic for above requirement.