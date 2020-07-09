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
               environment {
                ENV = "dev"
               }
               steps {
                   script {
                    loadEnvironmentVariables('dev')
                   }
                   sh 'printenv'
                   sh '''
                   echo "kafka host in $ENV: ${KAFKA_HOST_IP}"
                   '''                   
                   sh 'mvn -B -DskipTests clean package'
               }
           }
           stage('Test') {
               when {
                  expression { pipelineParams.unitTests == true }
               }
               environment {
                ENV = "prod"
               }
               steps {
                   script {
                    loadEnvironmentVariables("${ENV}")
                   }                
                   sh '''
                   printenv
                   echo "kafka host in $ENV: ${KAFKA_HOST_IP}"
                   echo "$DB_HOST"
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

/*
def loadEnvironmentVariables(){
    def props = readYaml (file: '/var/jenkins_home/devops/env_properties.yaml')
    keys= props.keySet()
    for(key in keys) {
        value = props["${key}"]
        env."${key}" = "${value}"
    }
}

*/

def loadEnvironmentVariables(env){
    def props = readProperties (file: "/var/jenkins_home/devops/${env}.properties")
    keys= props.keySet()
    for(key in keys) {
        value = props["${key}"]
        env."${key}" = "${value}"
    }
}