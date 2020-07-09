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
           stage('Initialize') {
              steps {
                script {
                  loadEnvironmentVariables()
                }
              }
           }
           stage('Build') {
               environment {
                ENV = "DEV"
               }
               steps {
                   sh 'printenv'
                   sh '''
                   echo DEV KAFKA: ${ENV.KAFKA_HOST_IP}
                   '''                   
                   sh 'mvn -B -DskipTests clean package'
               }
           }
           stage('Test') {
               when {
                  expression { pipelineParams.unitTests == true }
               }
               environment {
                ENV = "PROD"
               }
               steps {
                   sh '''
                   echo PROD DB_HOST: ${ENV.DB_HOST}
                   '''                
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

def loadEnvironmentVariables(){
    def props = readYaml (file: '/var/jenkins_home/devops/env_properties.yaml')
    keys= props.keySet()
    for(key in keys) {
        value = props["${key}"]
        env."${key}" = "${value}"
    }
}

