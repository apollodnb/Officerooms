#!/bin/bash
# Produktionsserver (mit Mods, frische Welt bei $1=fresh) + Dev-Client (Sichtweite 4)
pkill -9 -x java; sleep 3
cd /home/claude/prodtest && rm -f mods/OfficeRooms-*.jar; cp /home/claude/src0/OfficeRooms/build/libs/OfficeRooms-0.39.0.jar mods/
[ "$1" == "fresh" ] && rm -rf world
sed -i 's/view-distance=.*/view-distance=5/; s/level-seed=.*/level-seed=4711/' server.properties
printf -- "-Xmx2000m\n" > user_jvm_args.txt
(PATH=/home/claude/prodtest/temurin/bin:$PATH timeout 3500 ./run.sh nogui > /tmp/devserver.log 2>&1 &)
for i in $(seq 1 60); do sleep 4; if grep -qE "RCON running|Failed to initialize" /tmp/devserver.log; then break; fi; done
cd /home/claude/src0/OfficeRooms && sed -i 's/renderDistance:[0-9]*/renderDistance:4/' run/options.txt && export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
pgrep Xvfb >/dev/null || (Xvfb :99 -screen 0 1280x720x24 > /tmp/xvfb.log 2>&1 &)
(DISPLAY=:99 timeout 3500 ./gradlew runClient --no-daemon -PquickPlay=localhost -PrunXmx=1200m -Dorg.gradle.java.installations.paths=$JAVA_HOME > /tmp/client.log 2>&1 &)
for i in $(seq 1 110); do sleep 5; if grep -q "Dev joined the game" /tmp/devserver.log; then break; fi; done
sleep 5; cd /home/claude/scratch && python3 rcon.py "op Dev" "gamemode spectator Dev" >/dev/null 2>&1
DISPLAY=:99 xdotool key F1
grep "joined" /tmp/devserver.log | tail -1
