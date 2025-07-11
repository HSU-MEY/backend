#!/bin/bash

# 로그 디렉토리 및 파일 생성
mkdir -p /home/ubuntu/mey/logs
touch /home/ubuntu/mey/logs/deploy.log

# 배포된 JAR 파일 찾기 (CodeDeploy에 의해 복사된 파일)
BUILD_JAR=$(ls /home/ubuntu/mey/*.jar | head -1)
JAR_NAME=$(basename $BUILD_JAR)
echo "> build 파일명: $JAR_NAME" >> /home/ubuntu/mey/logs/deploy.log

echo "> 현재 실행중인 애플리케이션 pid 확인" >> /home/ubuntu/mey/logs/deploy.log
CURRENT_PID=$(pgrep -f $JAR_NAME)

if [ -z $CURRENT_PID ]
then
  echo "> 현재 구동중인 애플리케이션이 없으므로 종료하지 않습니다." >> /home/ubuntu/mey/logs/deploy.log
else
  echo "> kill -15 $CURRENT_PID" >> /home/ubuntu/mey/logs/deploy.log
  kill -15 $CURRENT_PID
  sleep 5
fi

echo "> JAR 배포 시작" >> /home/ubuntu/mey/logs/deploy.log
nohup java -jar $BUILD_JAR >> /home/ubuntu/mey/logs/deploy.log 2>/home/ubuntu/mey/logs/deploy_err.log &
echo "JAR 배포 완료" >> /home/ubuntu/mey/logs/deploy.log
