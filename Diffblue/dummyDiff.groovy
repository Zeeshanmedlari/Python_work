def call(Map config = [:]) {
    def gradleBuild() {
        sh '''#!/bin/bash -ex
            echo 'starting gradle build'
            items= (`ls -d images/*`)
            for i in "${items[@]}"; do
                cd ${i}
                if [ -f "build.gradle" ]; then
                    ./gradlew clean build jacocoTestReport --stacktrace -x generateGitProperties
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
            echo ${DCOVLICENSE} > ~/.diffblue/offline/ls_activation.lic
            dcover activate --offline ${DCOVKEY}
            dcover license
        '''
    }

    def diffblueRun() {
        sh '''#!/bin/bash -ex
            echo "starting diffblue Run"
            items=(`ls -d images/*`)
            # checking if patch mode can be run
            if [ -f "$CHECKING_FILE" ]; then
                export DIFFBLUE_MODE=PATCH
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

    // New code to control Diffblue execution
    def diffblueRunRequested = false // Initialize to false
    if (env.FORCE_DIFFBLUE == "true") {
        diffblueRunRequested = true
    } else if (env.DIFFBLUE_RUN == "true") {
        echo "Diffblue stage already executed during this PR run. Skipping."
    } else {
        // Set to true if conditions for running Diffblue are met
        // Add any additional conditions as needed
        if (/* Add conditions here */) {
            diffblueRunRequested = true
        }
    }

    if (diffblueRunRequested) {
        gradleBuild()
        withCredentials([
            string(credentialsID: 'dcover.lic', variable: 'DCOVLICENSE'),
            string(credentialsID: 'dcover_key', variable: 'DCOVKEY'),
        ]) {
            activateDiffblue()
        }
        diffblueRun()
    } else {
        echo "Diffblue stage skipped."
    }
}