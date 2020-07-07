def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
       agent {
           docker {
               image 'maven:3-alpine'
               args '-v /root/.m2:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock'
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
                when {
                    expression { pipelineParams.branch == 'master'}
                } 
               steps {
                   sh './jenkins/scripts/deliver.sh'
               }
           }
       }
    }
}