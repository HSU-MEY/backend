#!/bin/bash
# 디렉토리 생성 및 권한 설정
mkdir -p /home/ubuntu/mey
mkdir -p /home/ubuntu/mey/logs
chown -R ubuntu:ubuntu /home/ubuntu/mey
chmod -R 755 /home/ubuntu/mey

# 기존 애플리케이션 종료
CURRENT_PID=$(pgrep -f "\.jar")
if [ ! -z $CURRENT_PID ]; then
  echo "기존 애플리케이션 종료: $CURRENT_PID"
  kill -15 $CURRENT_PID
  sleep 5
fi
