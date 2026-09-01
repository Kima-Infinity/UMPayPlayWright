// UMPayPlayWright on Jenkins: on demand, or on a schedule.
//
// Kept in the repository rather than configured in the Jenkins UI so that a change to how
// the suite runs is reviewed the same way a change to the suite is, and so a lost Jenkins
// can be rebuilt from the repository alone.
//
// WHAT THIS SUITE DOES WHEN IT RUNS, AND WHY THE SCHEDULE IS NOT THE WHOLE SUITE
//
// Some of these scenarios spend real things on the test environment:
//
//   Deposit, Withdraw, Convert, GlobalTransfer   move real money between real wallets
//   Register                                     creates a real account, every run
//   the reset scenarios                          are rate limited, and the limit escalates
//                                                from a minute to an hour if pushed
//
// So the nightly schedule runs SAFE_TAGS - the areas that only read, open forms and assert,
// and send nothing - and the money is spent on purpose, by a person, from Build with
// Parameters. Change TAGS there to "" for everything the runner would normally run.

pipeline {

    agent any

    parameters {
        string(
            name: 'TAGS',
            defaultValue: '@login or @transfer',
            description: '''Cucumber tag expression. Leave empty to run everything the runner
                            allows (which moves real money and registers an account).
                            "not @manual" is always added.'''
        )
        booleanParam(
            name: 'HEADLESS',
            defaultValue: true,
            description: 'Run without a visible browser window. Agents normally have no display.'
        )
    }

    triggers {
        // 2am daily, with H so Jenkins spreads the load rather than every job firing at once.
        // Only the safe areas: see the note at the top before widening this.
        cron('H 2 * * *')
    }

    options {
        timeout(time: 90, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        // The suite reads the registration code out of a mailbox and emails its report.
        // MailCredentials looks for this variable; the password is never in the repository.
        // Add it in Jenkins as a Secret text credential with this id.
        UMPAY_MAIL_PASSWORD = credentials('umpay-mail-password')

        // Where Playwright keeps its browsers. Pinned to the workspace-adjacent cache so a
        // fresh agent downloads them once rather than once per build.
        PLAYWRIGHT_BROWSERS_PATH = "${env.JENKINS_HOME}/.cache/ms-playwright"
    }

    stages {

        stage('Checkout') {
            steps {
                // checkout scm rather than naming the repository again: it takes the branch,
                // the URL and the credentials from the job's own SCM configuration, and
                // checks out the very revision this Jenkinsfile was read from.
                //
                // Spelling it out a second time here meant the branch was configured in two
                // places, and the job's copy still said master while this one said main -
                // which is a build failure that says "couldn't find remote ref
                // refs/heads/master" and tells you nothing about there being two settings.
                checkout scm
            }
        }

        stage('Tools') {
            steps {
                // Fails early and legibly if the agent is missing something, rather than
                // halfway through a suite that has already registered an account.
                script {
                    if (isUnix()) {
                        sh 'java -version && mvn -v && python3 --version'
                    } else {
                        bat 'java -version && mvn -v && python --version'
                    }
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    // The runner already excludes @manual and @e2e. A -D tag expression
                    // replaces that entirely, so @manual has to be put back or the
                    // scenarios that lock the account would run unattended.
                    def tagArg = params.TAGS?.trim()
                        ? "-Dcucumber.filter.tags=\"(${params.TAGS}) and not @manual\""
                        : ''

                    // config.properties names the Python that runs the captcha OCR by
                    // absolute path, which is right for a developer machine and wrong here.
                    def python = isUnix() ? 'python3' : 'python'

                    def goals = "clean test -Dheadless=${params.HEADLESS} " +
                                "-Dcaptcha.ocr.python=${python} ${tagArg}"

                    if (isUnix()) {
                        sh "mvn ${goals}"
                    } else {
                        bat "mvn ${goals}"
                    }
                }
            }
        }
    }

    post {

        always {
            // Cucumber writes JUnit XML; this is what gives Jenkins its test trend and lets
            // a build be unstable rather than simply failed.
            junit allowEmptyResults: true, testResults: 'target/cucumber.xml'

            archiveArtifacts artifacts: 'Reports/*.html, target/cucumber-reports.html, target/cucumber.json',
                             allowEmptyArchive: true,
                             fingerprint: false

            // Screenshots are taken on both pass and fail and get large, so only the
            // failures are worth keeping.
            archiveArtifacts artifacts: 'Screenshots/*.png',
                             allowEmptyArchive: true,
                             onlyIfSuccessful: false
        }

        unstable {
            echo 'Some scenarios failed. Known failures are listed in README.md - check ' +
                 'them off before treating this as a regression.'
        }

        failure {
            echo 'The build failed rather than the tests. Usually the agent: no Maven, no ' +
                 'Python for the captcha OCR, or the umpay-mail-password credential missing.'
        }
    }
}
