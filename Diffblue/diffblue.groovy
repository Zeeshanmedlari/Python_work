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
          // if diffblue run is successful, set the description.
          currentBuild.description = "DiffblueRan"
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