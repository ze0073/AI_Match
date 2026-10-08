#!/bin/bash
JAR="ai-match-server-1.0.0.jar"
PORT=8080
WAIT=12

echo "========================================="
echo "  AI Match Server Startup"
echo "========================================="
echo ""

# 1. Check Java with clear error
echo "[1/4] Checking Java..."
if ! command -v java &>/dev/null; then
    echo "[ERROR] java command not found. Install JDK 17:"
    echo "  yum install java-17-openjdk -y"
    exit 1
fi
java -version 2>&1 | head -1
echo ""

# 2. Check JAR file
echo "[2/4] Checking JAR file..."
if [ ! -f "$JAR" ]; then
    echo "[ERROR] $JAR not found."
    echo "Files in current directory:"
    ls -lh
    exit 1
fi
JAR_SIZE=$(ls -lh "$JAR" | awk '{print $5}')
echo "  Found: $JAR ($JAR_SIZE)"
echo ""

# 3. Setup firewall (non-fatal)
echo "[3/4] Setting up firewall..."
if command -v firewall-cmd &>/dev/null 2>&1; then
    sudo firewall-cmd --add-port=$PORT/tcp --permanent 2>/dev/null || true
    sudo firewall-cmd --reload 2>/dev/null || true
    echo "  firewalld: OK (port $PORT)"
elif command -v iptables &>/dev/null 2>&1; then
    sudo iptables -I INPUT -p tcp --dport $PORT -j ACCEPT 2>/dev/null || true
    echo "  iptables: OK (port $PORT)"
else
    echo "  Skipped (no firewall tool found)"
fi
echo ""

# 4. Start service
echo "[4/4] Starting application..."

mkdir -p ./data ./logs

OLD_PID=$(ps aux 2>/dev/null | grep "$JAR" | grep -v grep | awk '{print $2}')
if [ -n "$OLD_PID" ]; then
    echo "  Stopping old process PID=$OLD_PID"
    kill $OLD_PID 2>/dev/null
    sleep 2
fi

> logs/app.log
nohup java -jar "$JAR" > logs/app.log 2>&1 &
START_PID=$!

echo -n "  Waiting ${WAIT}s "
for i in $(seq 1 $WAIT); do
    echo -n "."
    sleep 1
done
echo ""

if kill -0 $START_PID 2>/dev/null; then
    IP=$(hostname -I 2>/dev/null | awk '{print $1}')
    echo ""
    echo "========================================="
    echo "  STARTUP SUCCESS"
    echo "  PID:    $START_PID"
    echo "  Port:   $PORT"
    echo "  URL:    http://${IP:-YOUR_IP}:$PORT"
    echo "  Admin:  ADMIN / admin123"
    echo "========================================="
else
    echo ""
    echo "========================================="
    echo "  STARTUP FAILED"
    echo "  Last 40 lines of logs/app.log:"
    echo "========================================="
    tail -40 logs/app.log
    echo "========================================="
fi