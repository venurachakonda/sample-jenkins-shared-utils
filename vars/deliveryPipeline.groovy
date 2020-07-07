def call(Map pipelineParams) {

   pipeline {
       agent {
           docker {
               image 'maven:3-alpine'
               args '-v /root/.m2:/root/.m2'
           }
       }
       stages {
           stage('Build') {

               steps {
                   sh 'mvn -B -DskipTests clean package'
               }
           }
           stage('Test') {
               when {
                  expression { pipelineParams.unitTests == true }
               }            
               steps {
                   sh 'mvn test'
               }
               post {
                   always {
                       junit 'target/surefire-reports/*.xml'
                   }
               }
           }
           stage('Deliver') {
               steps {
                   sh './jenkins/scripts/deliver.sh'
               }
           }
       }
   }
}
