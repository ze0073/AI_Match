@echo off
cd /d E:\codex\AI_Match\ai-match-server
mvn spring-boot:run -o -s settings.xml -DskipTests >> server.log 2>&1