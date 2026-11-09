"""Generate the short Russian Practical Work 2 submission report."""
from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf" / "WellnessTracker_Practical_Work_2.pdf"
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
pdfmetrics.registerFont(TTFont("DejaVu", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"))
pdfmetrics.registerFont(TTFont("DejaVuBold", "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"))

c = canvas.Canvas(str(OUTPUT), pagesize=(595, 842))
c.setTitle("Практическая работа №2 — Wellness Tracker")

def line(txt, y, size=11, bold=False):
    c.setFont("DejaVuBold" if bold else "DejaVu", size)
    c.drawString(54, y, txt)

line("Практическая работа № 2", 775, 17, True)
line("Реализация пользовательского интерфейса", 746, 14, True)
line("Дисциплина: Разработка мобильных приложений", 718)
line("Студент: Хегази Х. А.    Группа: КИ23-16/1б", 695)
line("Приложение: Wellness Tracker", 672)

line("1. Цель работы", 635, 12, True)
line("Реализовать интерфейс из практической работы № 1 в Android Studio", 611)
line("с помощью XML-разметки и отобразить демонстрационные данные.", 592)

line("2. Реализованные экраны", 556, 12, True)
for y, txt in [
    (531, "Сегодня — шаги, вода, сон, калории и показатель здоровья."),
    (510, "Активность — данные по дням и переход к недельной статистике."),
    (489, "Дневник — создание, изменение и удаление заметок о самочувствии."),
    (468, "Карта — поиск фитнес-клубов, парков и беговых дорожек."),
    (447, "Профиль — имя, возраст, рост, вес, активность и город."),
]:
    line(txt, y)

line("3. Технологии", 411, 12, True)
line("Kotlin, Android XML Layouts, Material Design, ViewBinding, MVVM,", 387)
line("SharedPreferences/JSON, Android Sensors, Open-Meteo API и Gradle.", 368)
line("Для проверки интерфейса можно ввести тестовый профиль и вручную", 348)
line("добавить шаги, воду, сон и запись в дневник.", 329)

line("4. Сборка APK через командную строку", 293, 12, True)
line("1) Установить Android Studio, SDK Platform 36 и JDK 17+.", 269)
line("2) Открыть терминал в каталоге проекта.", 248)
line("3) Выполнить:  .\\gradlew.bat assembleDebug", 227)
line("4) APK: app/build/outputs/apk/debug/app-debug.apk", 206)
line("Подробная инструкция находится в BUILD_INSTRUCTIONS.md.", 185)

line("5. Результат", 149, 12, True)
line("Исходный код опубликован в открытом репозитории GitHub:", 125)
line("https://github.com/Hamed745/WellnessTracker", 104)
line("Примечание: сборка APK в текущей среде не проверена из-за", 78, 9)
line("недоступности загрузки дистрибутива Gradle.", 63, 9)
c.save()
print(OUTPUT)
