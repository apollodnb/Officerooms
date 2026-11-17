cd /home/claude/scratch
R(){ python3 rcon.py "$@" 2>/dev/null | grep -v "^>\|^auth\|^$"; }; shot(){ DISPLAY=:99 import -window root -crop 854x480+213+120 /tmp/ft_$1.png; }
MOBS="gaunt wolfspider smiler colony deathmoth hound partygoer headless_warden flayed aranea_membri hunter observer"
R "gamerule sendCommandFeedback false" "difficulty normal" "effect give Dev night_vision 99999 0 true" "effect give Dev resistance 99999 4 true" "effect give Dev saturation 99999 0 true" >/dev/null
stage(){ # name, tp-command
  w0=$(grep -c "Can't keep up" /tmp/devserver.log); e0=$(grep -ciE "ERROR|Exception" /tmp/devserver.log)
  R "gamemode creative Dev" >/dev/null; R "$2" >/dev/null; sleep 4; R "gamemode survival Dev" >/dev/null; sleep 45
  p=$(python3 rcon.py "data get entity Dev Pos" 2>/dev/null | grep -o "\[.*\]" | sed 's/d//g')
  m=""; for t in $MOBS; do c=$(R "execute as Dev at Dev if entity @e[type=office_rooms:$t,distance=..80]" | grep -o "count: [0-9]*" | grep -o "[0-9]*"); [ -n "$c" ] && m="$m $t=$c"; done
  tps=$(R "forge tps office_rooms:office_rooms" | grep -o "Mean tick time: [0-9.]* ms" | head -1)
  w1=$(grep -c "Can't keep up" /tmp/devserver.log); e1=$(grep -ciE "ERROR|Exception" /tmp/devserver.log)
  R "gamemode creative Dev" >/dev/null; shot $1
  echo "$1 | pos $p | mobs:${m:- none} | $tps | lag warnings +$((w1-w0)) | errors +$((e1-e0))"
}
OF="execute in office_rooms:office_rooms run"
R "$OF tp Dev 0 101 0" >/dev/null; sleep 15
stage office_mid "$OF tp Dev 0 101 0"
stage office_high "$OF tp Dev 0 449 0"
for t in poolrooms neighbourhood maze hotel industrial level_fun cinema; do stage $t "execute as Dev at Dev run officerooms goto $t"; done
echo DONE
