package myedu.oshsu.kg

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DictionaryUtils {

    // Default dictionary data embedded in the app
    private val defaultJson = """
    {
      "header_section": "--- HEADERS & TITLES ---",
      "МИНИСТЕРСТВО НАУКИ, ВЫСШЕГО ОБРАЗОВАНИЯ И ИННОВАЦИЙ КЫРГЫЗСКОЙ РЕСПУБЛИКИ": "MINISTRY OF SCIENCE, HIGHER EDUCATION AND INNOVATION OF THE KYRGYZ REPUBLIC",
      "ОШСКИЙ ГОСУДАРСТВЕННЫЙ УНИВЕРСИТЕТ": "OSH STATE UNIVERSITY",
      "Международный медицинский факультет": "International Medical Faculty",
      "СПРАВКА": "REFERENCE",
      "ТРАНСКРИПТ": "TRANSCRIPT",

      "reference_body": "--- REFERENCE (FORM 8) ---",
      "Настоящая справка подтверждает, что": "This reference confirms that",
      "действительно является студентом (-кой)": "is currently a student of the",
      "года обучения": "year of study",
      "специальности/направление": "specialty/course",
      " [профиль: ": " [profile: ",
      "Справка выдана по месту требования.": "Reference is issued for submission to the place of demand.",
      "Достоверность данного документа можно проверить отсканировав QR-код": "The authenticity of this document can be verified by scanning the QR code",
      "Адрес": "Address",
      "Телефон": "Phone",

      "transcript_body": "--- TRANSCRIPT ---",
      "Учебный год": "Academic Year",
      "Зарегистрировано кредитов": "Registered Credits",
      "Дисциплины": "Subjects",
      "Кредит": "Credits",
      "Форма контроля": "Control Form",
      "Баллы": "Score",
      "Цифр. экв.": "GPA",
      "Букв.сист.": "Grade",
      "Трад. сист.": "Perfomance",
      "ФИО:": "Full Name:",
      "ID студента:": "Student ID:",
      "Дата рождения:": "Date of Birth:",
      "Направление:": "Course:",
      "Специальность:": "Specialty:",
      "Форма обучения:": "Form of Study:",
      "Общий GPA:": "Total GPA:",
      "Всего зарегистрированых кредитов:": "Total Registered Credits:",
      "ПРИМЕЧАНИЕ: 1 кредит составляет 30 академических часов.": "NOTE: 1 credit equals 30 academic hours.",
      "Ректор": "Rector",
      "Методист / Офис регистратор": "Registrator's Office",
      "семестр": "Semester",
      "Страница": "Page",
      "из": "of",
      "№": "#",
      "Б.Ч.": "Code",

      "dynamic_data": "--- DYNAMIC DATA & GRADES ---",
      "Лечебное дело": "General Medicine",
      "Очное (специалитет)": "Full-time (Specialist)",
      "Очное": "Full-time",
      "Заочное": "Part-time",
      "Дистантное": "Distance Learning",
      "Вечернее": "Evening",
      "Контракт": "Contract",
      "Бюджет": "Budget",
      "Экзамен": "Exam",
      "Зачет": "Credit",
      "Курсовая работа": "Coursework",
      "Отлично": "Excellent",
      "Очень хорошо": "Very Good",
      "Хорошо": "Good",
      "Удовл.": "Satisfactory",
      "Удовлетворительно": "Satisfactory",
      "Неудовл.": "Unsatisfactory",
      "Зачтено": "Passed",
      "Не зачтено": "Failed",
      "н/у": "F",
      "г. Ош, ул. Ленина 331": "Osh city, Lenin st. 331",
      "Кыргызстан": "Kyrgyzstan",
      "Ошская область": "Osh Region",
      "г. Ош": "Osh City",

      "ordinals": "--- ORDINALS (COURSES) ---",
      "первого": "First",
      "второго": "Second",
      "третьего": "Third",
      "четвертого": "Fourth",
      "пятого": "Fifth",
      "шестого": "Sixth",
      "седьмого": "Seventh",
      "восьмого": "Eighth"
    }
    """.trimIndent()

    fun getDefaultDictionary(): Map<String, String> {
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(defaultJson, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
