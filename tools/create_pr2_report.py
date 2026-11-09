"""Generates the Practical Work 2 report PDF (output/pdf/WellnessTracker_Practical_Work_2.pdf)."""
from pathlib import Path
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import cm
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Preformatted

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output" / "pdf" / "WellnessTracker_Practical_Work_2.pdf"
OUT.parent.mkdir(parents=True, exist_ok=True)

FD = "/usr/share/fonts/truetype/liberation/"
pdfmetrics.registerFont(TTFont("TNR", FD + "LiberationSerif-Regular.ttf"))
pdfmetrics.registerFont(TTFont("TNR-B", FD + "LiberationSerif-Bold.ttf"))
pdfmetrics.registerFont(TTFont("TNR-I", FD + "LiberationSerif-Italic.ttf"))
pdfmetrics.registerFont(TTFont("TNR-BI", FD + "LiberationSerif-BoldItalic.ttf"))
pdfmetrics.registerFontFamily("TNR", normal="TNR", bold="TNR-B", italic="TNR-I", boldItalic="TNR-BI")
pdfmetrics.registerFont(TTFont("MONO", "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"))

REPO = "https://github.com/Hamed745/WellnessTracker"
body = ParagraphStyle("body", fontName="TNR", fontSize=14, leading=21, alignment=TA_JUSTIFY, firstLineIndent=1.25 * cm)
plainL = ParagraphStyle("plainL", parent=body, firstLineIndent=0, alignment=TA_LEFT)
h1 = ParagraphStyle("h1", fontName="TNR-B", fontSize=14, leading=21, leftIndent=1.25 * cm, spaceBefore=12, spaceAfter=6, keepWithNext=1)
center = ParagraphStyle("center", fontName="TNR", fontSize=14, leading=18, alignment=TA_CENTER)
centerb = ParagraphStyle("centerb", parent=center, fontName="TNR-B")
cell = ParagraphStyle("cell", fontName="TNR", fontSize=12, leading=14, alignment=TA_LEFT)
cellb = ParagraphStyle("cellb", parent=cell, fontName="TNR-B")
code = ParagraphStyle("code", fontName="MONO", fontSize=9, leading=11.5, leftIndent=1.25 * cm)


def P(t, s=body):
    return Paragraph(t, s)


def table(rows, widths):
    data = [[Paragraph(c, cellb if i == 0 else cell) for c in r] for i, r in enumerate(rows)]
    t = Table(data, colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([("GRID", (0, 0), (-1, -1), 0.6, colors.black), ("VALIGN", (0, 0), (-1, -1), "TOP"),
                           ("BACKGROUND", (0, 0), (-1, 0), colors.Color(0.92, 0.92, 0.92)),
                           ("TOPPADDING", (0, 0), (-1, -1), 4), ("BOTTOMPADDING", (0, 0), (-1, -1), 4)]))
    return t


def footer(c, doc):
    c.setFont("TNR", 12)
    c.drawCentredString(A4[0] / 2, 1.0 * cm, str(doc.page))


doc = SimpleDocTemplate(str(OUT), pagesize=A4, leftMargin=3 * cm, rightMargin=1 * cm, topMargin=2 * cm,
                        bottomMargin=2 * cm, title="Практическая работа №2 — Реализация пользовательского интерфейса",
                        author="Хегази Х.А")
s = [P("МИНИСТЕРСТВО НАУКИ И ВЫСШЕГО ОБРАЗОВАНИЯ РОССИЙСКОЙ ФЕДЕРАЦИИ", center),
     P("«СИБИРСКИЙ ФЕДЕРАЛЬНЫЙ УНИВЕРСИТЕТ»", centerb),
     P("Институт космических и информационных технологий", center),
     P("Кафедра программной инженерии", center), Spacer(1, 16),
     P("ОТЧЁТ", centerb), P("по практической работе №2", centerb),
     P("Реализация пользовательского интерфейса", centerb),
     P("по дисциплине «Разработка мобильных приложений»", center), Spacer(1, 10),
     P("Выполнил: студент группы КИ23-16/1б Хегази Х.А.", plainL),
     P("Проверил: Мымликов В.Н.", plainL)]

s += [P("1 ЦЕЛЬ РАБОТЫ", h1),
      P("Реализовать в Android Studio пользовательский интерфейс, который был спроектирован в практической работе №1: "
        "разместить элементы интерфейса с помощью XML-разметки так, чтобы приложение выглядело как прототип. "
        "Составить инструкцию по сборке apk-файла через командную строку.")]

s += [P("2 ЧТО РЕАЛИЗОВАНО", h1),
      P("Приложение Wellness Tracker написано на Kotlin. Интерфейс сделан в XML (Material Design 3), "
        "экраны — это фрагменты, переключение — через нижнее меню. Все экраны из прототипа есть в приложении:"),
      table([["Экран прототипа", "Что есть в приложении", "Файлы"],
             ["Сегодня", "Шаги, вода, сон, калории, балл здоровья, карточка погоды", "DashboardFragment, fragment_dashboard.xml"],
             ["Активность", "График шагов за 7 дней, сводка за неделю, список дней", "ActivityFragment, BarChartView, fragment_activity.xml"],
             ["Дневник", "Список заметок, экран «Запись» (создать, изменить, удалить), база SQLite", "DiaryFragment, NoteEditorFragment, NotesDb"],
             ["Карта", "Макет карты, метки мест, карточка места, переключатели категорий", "MapFragment, MapMockView, fragment_map.xml"],
             ["Погода (API)", "Температура и состояние неба из Open-Meteo, пример запроса", "WeatherFragment, WeatherService"],
             ["Профиль", "Ввод данных; ИМТ и норма воды считаются сразу", "ProfileFragment, HealthCalculator"]],
            [3.2 * cm, 7.0 * cm, 5.4 * cm]), Spacer(1, 6),
      P("Дополнительно: в верхней панели есть переключатель EN | RU — язык интерфейса меняется внутри приложения.")]

s += [P("3 ДАННЫЕ В ИНТЕРФЕЙСЕ", h1),
      P("Места на карте (фитнес-клубы, парки, беговые дорожки) — статические примеры, как в задании. Остальные данные "
        "пользователь вводит сам, и они хранятся на телефоне: заметки — в базе SQLite, данные по дням и профиль — "
        "в локальном хранилище приложения. Для проверки можно ввести тестовый профиль (имя Алекс, 22 года, "
        "175 см, 70 кг, город Красноярск), нажать «+500 шагов», «+250 мл» и записать сон.")]

s += [P("4 СБОРКА APK ЧЕРЕЗ КОМАНДНУЮ СТРОКУ", h1),
      P("Нужны JDK 17–21 и Android SDK (платформа 36). Команды для Windows PowerShell:", plainL),
      Preformatted('git clone https://github.com/Hamed745/WellnessTracker.git\ncd WellnessTracker\n'
                   '$env:JAVA_HOME = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.101-hotspot"\n'
                   '$sdk = ("$env:LOCALAPPDATA\\Android\\Sdk") -replace \'\\\\\',\'/\'\n'
                   'Set-Content -Path local.properties -Value "sdk.dir=$sdk" -Encoding ASCII\n'
                   '.\\gradlew.bat --no-daemon --console=plain assembleDebug', code),
      Spacer(1, 6),
      P("Готовый файл: app/build/outputs/apk/debug/app-debug.apk. Установка на телефон: "
        "adb install -r app\\build\\outputs\\apk\\debug\\app-debug.apk. Полная инструкция (проверка тестов, "
        "release-сборка, частые ошибки) находится в файле BUILD_RU.md в репозитории.", plainL)]

s += [P("5 РЕЗУЛЬТАТ", h1),
      P("Исходный код и инструкция опубликованы в открытом репозитории:", plainL),
      P("<link href='%s' color='blue'>%s</link> (инструкция: BUILD_RU.md и BUILD_INSTRUCTIONS.md)" % (REPO, REPO), plainL)]

doc.build(s, onFirstPage=lambda c, d: None, onLaterPages=footer)
print("built", OUT)
