const REQUIRED_JAVA_MAJOR = 25;
const REQUIRED_NODE_MAJOR = 24;

module.exports = {
  apps: [
    {
      name: 'jingxuan-back',
      cwd: '/opt/jingxuan',
      script: 'java',
      args: '-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxMetaspaceSize=256m -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom -jar backend/jingxuan-backend-1.0.0.jar --spring.profiles.active=prod --server.tomcat.threads.max=400 --server.tomcat.accept-count=100',
    }
  ]
};
