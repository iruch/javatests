#!/usr/bin/env bash
# Полная пересборка приложения «Java-тесты для Study Hub» в Docker (macOS и Linux).
# Запуск: ./scripts/rebuild.sh из папки проекта; на macOS также двойной щелчок по scripts/rebuild.command.
#   --yes             не спрашивать подтверждение;
#   --dry-run         только показать, что будет сделано, ничего не удаляя;
#   --close-terminal  (macOS) закрыть окно Терминала после успешной пересборки.
set -u

cd "$(dirname "$0")/.." || exit 1

APP_URL="http://localhost:8097"
TITLE="Java-тесты для Study Hub"
QUESTION="Приложение будет полностью удалено и заново пересобрано, все данные прошлых тестов будут удалены. Продолжить?"

ASSUME_YES=false
DRY_RUN=false
CLOSE_TERMINAL=false
for arg in "$@"; do
    case "$arg" in
        --yes) ASSUME_YES=true ;;
        --dry-run) DRY_RUN=true ;;
        --close-terminal) CLOSE_TERMINAL=true ;;
    esac
done

fail() {
    echo
    echo "Ошибка: $1"
    read -r -p "Нажмите Enter, чтобы закрыть окно..." _ || true
    exit 1
}

# Отказ по умолчанию: удалить данные можно только явным «Да».
confirm() {
    if [ "$(uname)" = "Darwin" ] && command -v osascript >/dev/null 2>&1; then
        answer=$(osascript -e "button returned of (display dialog \"$QUESTION\" with title \"$TITLE\" buttons {\"Нет\", \"Да\"} default button \"Нет\" cancel button \"Нет\" with icon caution)" 2>/dev/null) || answer="Нет"
        [ "$answer" = "Да" ]
    elif command -v zenity >/dev/null 2>&1 && [ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]; then
        zenity --question --title="$TITLE" --text="$QUESTION" --ok-label="Да" --cancel-label="Нет" --default-cancel 2>/dev/null
    else
        read -r -p "$QUESTION [Да/Нет]: " answer || answer=""
        case "$answer" in
            Да|да|ДА|Д|д|yes|Yes|YES|y|Y) return 0 ;;
            *) return 1 ;;
        esac
    fi
}

step() {
    echo "$1"
    shift
    if [ "$DRY_RUN" = true ]; then
        echo "  [dry-run] docker $*"
        return 0
    fi
    docker "$@"
}

open_url() {
    if [ "$DRY_RUN" = true ]; then
        echo "  [dry-run] open $APP_URL"
    elif [ "$(uname)" = "Darwin" ]; then
        open "$APP_URL"
    elif command -v xdg-open >/dev/null 2>&1; then
        xdg-open "$APP_URL" >/dev/null 2>&1 &
    else
        echo "Откройте в браузере: $APP_URL"
    fi
}

if [ "$ASSUME_YES" != true ] && ! confirm; then
    exit 0
fi

command -v docker >/dev/null 2>&1 || fail "Docker не найден. Установите Docker и повторите."
docker info >/dev/null 2>&1 || fail "Docker не запущен. Запустите Docker, дождитесь готовности и повторите."

if ! step "1/4 Удаляю контейнеры, образы и базу данных приложения..." compose down --volumes --rmi all --remove-orphans; then
    echo "Часть ресурсов удалить не удалось (например, образ используется другим контейнером). Продолжаю."
fi

step "2/4 Собираю и запускаю приложение заново..." compose up -d --build \
    || fail "docker compose up завершился с ошибкой. Подробности выше."

echo "3/4 Жду, пока приложение запустится..."
ready=false
for _ in $(seq 1 90); do
    if curl -fs -o /dev/null --max-time 3 "$APP_URL"; then
        ready=true
        break
    fi
    sleep 2
done
[ "$ready" = true ] || fail "Приложение не ответило за 3 минуты. Логи: docker compose logs app"

echo "4/4 Открываю $APP_URL"
open_url

if [ "$CLOSE_TERMINAL" = true ] && [ "$DRY_RUN" != true ] && [ "$(uname)" = "Darwin" ]; then
    (sleep 1; osascript -e 'tell application "Terminal" to close (every window whose name contains "rebuild.command")') >/dev/null 2>&1 &
fi
exit 0
