pipeline {
 environment {
   jobBaseName = "${env.JOB_NAME}".split('/').first()
 }
 options {
      timeout(time: 10, unit: 'MINUTES') 
  } //option

  agent none
 stages {

   stage("Build and Test") {
   when {
       not {
           branch 'master'
       }
   }
   agent {
                docker {
                    image 'maven:3-alpine'
                    args '-v m2_repos:/root/.m2'
                } //docker
            } // agent

   stages {

   stage('Build') {
     steps {
       echo "Building Chatter"
       sh 'mvn -f Project_src/Chatter/pom.xml install'
       echo "Building Prattle"
       sh 'mvn -f Project_src/Prattle/pom.xml compile'
   }
   } // build
   stage('SonarQube') {
   when {
       not {
           branch 'master'
       }
   }
    steps {
      withSonarQubeEnv('SonarQube') {
        sh 'mvn -f Project_src/Prattle/pom.xml clean install'
        sh 'mvn -f Project_src/Prattle/pom.xml sonar:sonar -Dsonar.projectKey=${jobBaseName} -Dsonar.projectName=${jobBaseName}'
      }

      sh 'sleep 30'
      timeout(time: 10, unit: 'SECONDS') {
       retry(5) {
        script {
          def qg = waitForQualityGate()
          if (qg.status != 'OK') {
            error "Pipeline aborted due to quality gate failure: ${qg.status}"
          }
        }
      }
    }
  } //steps
} //SONAR
}
} //stage 

stage('Master Branch Tasks') {
when {
           branch 'master'
   }
agent any
        steps {
        	echo "Building Prattle"
             sh 'mvn -f Project_src/Prattle/pom.xml package -Dmaven.test.skip=true'          

             script {
              def json = readJSON file:'config.json'
              sh 'cd ${WORKSPACE}'
              sh "chmod 400 ${json.server[0].PEM}"
              sh "scp -oStrictHostKeyChecking=no -i ${json.server[0].PEM} Project_src/Prattle/target/${json.server[0].JARNAME} ${json.server[0].user}@${json.server[0].DNS}:${json.server[0].directory}"
              sh "ssh -oStrictHostKeyChecking=no -i ${json.server[0].PEM} ${json.server[0].user}@${json.server[0].DNS}  pkill java &"
              sh "ssh -oStrictHostKeyChecking=no -i ${json.server[0].PEM} ${json.server[0].user}@${json.server[0].DNS}  nohup java -jar ${json.server[0].directory}/${json.server[0].JARNAME} >nohup.out 2>&1 &"
                 } //script
              }
              }
} // STAGES

 post {       
    success {
           slackSend (baseUrl: "https://cs5500.slack.com/services/hooks/jenkins-ci/", token: "9nDdsokR6x5uqYE1LZuI2Rzh", channel: "#cs5500-team-103-f18", color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME}")
            }
    failure {  
           slackSend (baseUrl: "https://cs5500.slack.com/services/hooks/jenkins-ci/", token: "9nDdsokR6x5uqYE1LZuI2Rzh", channel: "#cs5500-team-103-f18", color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME}")
            }
       }
}
