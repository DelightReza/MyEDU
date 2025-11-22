package com.example.myedu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {

    private val PAGE_WIDTH = 595
    private val PAGE_HEIGHT = 842
    private val MARGIN_LEFT = 40f
    private val MARGIN_RIGHT = 25f
    private val MARGIN_TOP = 25f
    private val MARGIN_BOTTOM = 40f
    private val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 9f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        isAntiAlias = true
    }
    private val boldPaint = Paint().apply {
        color = Color.BLACK
        textSize = 9f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }
    private val headerPaint = Paint().apply {
        color = Color.BLACK
        textSize = 11f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val linePaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }

    private val colWeights = floatArrayOf(10f, 40f, 180f, 30f, 45f, 35f, 35f, 30f, 45f)
    private val totalWeight = colWeights.sum()
    private val colWidths = colWeights.map { (it / totalWeight) * CONTENT_WIDTH }.toFloatArray()
    private val headers = listOf("№", "Б.Ч.", "Дисциплины", "Кредит", "Форма\nконтроля", "Баллы", "Цифр.\nэкв.", "Букв.\nсист.", "Трад.\nсист.")

    // --- TRANSCRIPT (FORM 13) ---
    fun generateTranscriptPdf(transcriptJson: String, studentName: String, studentId: String, infoJson: String): File? {
        val pdfDocument = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN_TOP

        fun startNewPage() {
            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            y = MARGIN_TOP + 20 
        }

        try {
            drawCommonHeader(canvas, "ТРАНСКРИПТ")
            y += 90
            drawStudentInfoBlock(canvas, y, studentName, studentId, infoJson)
            y += 120

            val data = JSONArray(transcriptJson)
            for (i in 0 until data.length()) {
                val eduYearObj = data.getJSONObject(i)
                val year = eduYearObj.optString("edu_year", "")
                
                if (y + 20 > PAGE_HEIGHT - MARGIN_BOTTOM) startNewPage()
                canvas.drawText("Учебный год $year", PAGE_WIDTH / 2f, y, boldPaint.apply { textAlign = Paint.Align.CENTER })
                y += 15

                val semesters = eduYearObj.optJSONArray("semesters") ?: continue
                for (j in 0 until semesters.length()) {
                    val semesterObj = semesters.getJSONObject(j)
                    val semesterName = semesterObj.optString("semester", "")
                    
                    if (y + 20 > PAGE_HEIGHT - MARGIN_BOTTOM) startNewPage()
                    val semPaint = Paint().apply { color = Color.LTGRAY }
                    canvas.drawRect(MARGIN_LEFT, y - 10, PAGE_WIDTH - MARGIN_RIGHT, y + 5, semPaint)
                    canvas.drawText(semesterName, PAGE_WIDTH / 2f, y, boldPaint)
                    y += 15

                    if (y + 30 > PAGE_HEIGHT - MARGIN_BOTTOM) startNewPage()
                    drawTableRow(canvas, y, headers, true)
                    y += 25

                    val subjects = semesterObj.optJSONArray("subjects") ?: continue
                    for (k in 0 until subjects.length()) {
                        val subj = subjects.getJSONObject(k)
                        val rowData = listOf(
                            (k + 1).toString(),
                            subj.optString("code", ""),
                            subj.optString("subject", ""),
                            subj.optString("credit", ""),
                            subj.optJSONObject("exam_rule")?.optString("control_form", "Exam") ?: "Exam",
                            subj.optJSONObject("mark_list")?.optString("total", "") ?: "",
                            subj.optJSONObject("exam_rule")?.optString("digital", "") ?: "",
                            subj.optJSONObject("exam_rule")?.optString("alphabetic", "") ?: "",
                            subj.optJSONObject("exam_rule")?.optString("word_ru", "") ?: ""
                        )
                        val rowHeight = calculateRowHeight(rowData[2])
                        if (y + rowHeight > PAGE_HEIGHT - MARGIN_BOTTOM) startNewPage()
                        drawTableRow(canvas, y, rowData, false, rowHeight)
                        y += rowHeight
                    }
                    if (y + 20 > PAGE_HEIGHT - MARGIN_BOTTOM) startNewPage()
                    val gpa = semesterObj.optString("gpa", "0")
                    canvas.drawText("GPA: $gpa", PAGE_WIDTH - MARGIN_RIGHT - 50, y + 10, boldPaint)
                    y += 20
                }
            }
            y += 30
            if (y + 100 > PAGE_HEIGHT - MARGIN_BOTTOM) startNewPage()
            canvas.drawText("ПРИМЕЧАНИЕ: 1 кредит составляет 30 академических часов.", MARGIN_LEFT, y, textPaint.apply { textAlign = Paint.Align.LEFT })
            y += 40
            canvas.drawText("Ректор _________________", PAGE_WIDTH - MARGIN_RIGHT, y, boldPaint.apply { textAlign = Paint.Align.RIGHT })
            y += 20
            canvas.drawText("Методист / Офис регистратор _________________", PAGE_WIDTH - MARGIN_RIGHT, y, boldPaint)

        } catch (e: Exception) { e.printStackTrace() }

        drawFooter(canvas, pageNumber)
        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MyEDU_Transcript.pdf")
        try { pdfDocument.writeTo(FileOutputStream(file)) } catch (e: IOException) { return null }
        pdfDocument.close()
        return file
    }

    // --- REFERENCE (FORM 8) ---
    fun generateReferencePdf(studentName: String, studentId: String, infoJson: String): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        var y = MARGIN_TOP + 20

        try {
            drawCommonHeader(canvas, null)
            y += 40

            var speciality = ""; var specCode = ""; var eduForm = ""; var paymentForm = ""; var activeSemester = 1; var orderSecond = ""
            try {
                val obj = JSONObject(infoJson)
                speciality = obj.optString("speciality_ru", "")
                specCode = obj.optString("spec_code", "")
                eduForm = obj.optString("edu_form_ru", "")
                paymentForm = obj.optString("payment_form_ru", "")
                activeSemester = obj.optInt("active_semester", 1)
                orderSecond = obj.optString("second_id", System.currentTimeMillis().toString().takeLast(6))
            } catch (e: Exception) { }

            val course = (activeSemester + 1) / 2
            val courseText = when(course) { 
                1 -> "первого"
                2 -> "второго"
                3 -> "третьего"
                4 -> "четвертого"
                5 -> "пятого"
                6 -> "шестого"
                else -> "$course"
            }
            val refId = "$activeSemester-$orderSecond-$studentId-2"

            canvas.drawText("СПРАВКА № $refId", PAGE_WIDTH / 2f, y, boldPaint.apply { textSize = 14f; textAlign = Paint.Align.CENTER })
            y += 40

            val bodyPaint = Paint().apply { color = Color.BLACK; textSize = 11f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL); isAntiAlias = true; textAlign = Paint.Align.CENTER }

            canvas.drawText("Настоящая справка подтверждает, что", PAGE_WIDTH / 2f, y, bodyPaint)
            y += 25
            canvas.drawText(studentName, PAGE_WIDTH / 2f, y, boldPaint.apply { textSize = 16f })
            y += 25
            canvas.drawText("действительно является студентом (-кой) $courseText года обучения", PAGE_WIDTH / 2f, y, bodyPaint)
            y += 20
            canvas.drawText("специальности/направление ($specCode) $speciality", PAGE_WIDTH / 2f, y, bodyPaint)
            y += 20
            canvas.drawText("($eduForm, $paymentForm)", PAGE_WIDTH / 2f, y, bodyPaint)
            y += 60

            val footerPaint = Paint().apply { color = Color.BLACK; textSize = 10f; textAlign = Paint.Align.LEFT }
            canvas.drawText("Справка выдана по месту требования.", MARGIN_LEFT, y, footerPaint)
            y += 40
            
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.US)
            val addressLine = "Кыргызстан, 723500, г. Ош, ул. Алымбека датки, 331, ${sdf.format(Date())}"
            canvas.drawText(addressLine, MARGIN_LEFT, y, footerPaint)

        } catch (e: Exception) { e.printStackTrace() }

        pdfDocument.finishPage(page)
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MyEDU_Reference.pdf")
        try { pdfDocument.writeTo(FileOutputStream(file)) } catch (e: IOException) { return null }
        pdfDocument.close()
        return file
    }

    private fun drawCommonHeader(canvas: Canvas, subTitle: String?) {
        var hy = MARGIN_TOP
        val headerLines = listOf("МИНИСТЕРСТВО НАУКИ, ВЫСШЕГО ОБРАЗОВАНИЯ И ИННОВАЦИЙ КЫРГЫЗСКОЙ РЕСПУБЛИКИ", "ОШСКИЙ ГОСУДАРСТВЕННЫЙ УНИВЕРСИТЕТ", "Международный медицинский факультет")
        headerLines.forEach { line -> canvas.drawText(line, PAGE_WIDTH / 2f, hy, headerPaint); hy += 15 }
        if (subTitle != null) { hy += 5; canvas.drawText(subTitle, PAGE_WIDTH / 2f, hy, headerPaint) }
    }

    private fun drawStudentInfoBlock(canvas: Canvas, startY: Float, name: String, id: String, json: String) {
        var dob = ""; var direction = ""; var speciality = ""; var eduForm = ""
        try { val obj = JSONObject(json); dob = obj.optString("birthday",""); speciality = obj.optString("speciality",""); direction = obj.optString("direction",""); eduForm = obj.optString("edu_form","") } catch(e: Exception) {}
        var y = startY; val labelX = MARGIN_LEFT; val valueX = MARGIN_LEFT + 100f
        val info = listOf("ФИО:" to name, "ID студента:" to id, "Дата рождения:" to dob, "Направление:" to direction, "Специальность:" to speciality, "Форма обучения:" to eduForm)
        info.forEach { (label, value) -> canvas.drawText(label, labelX, y, boldPaint.apply { textAlign = Paint.Align.LEFT }); canvas.drawText(value, valueX, y, textPaint); y += 12 }
    }

    private fun drawTableRow(canvas: Canvas, y: Float, row: List<String>, isHeader: Boolean, height: Float = 20f) {
        var currentX = MARGIN_LEFT; val paint = if (isHeader) boldPaint else textPaint
        canvas.drawLine(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y, linePaint)
        row.forEachIndexed { index, text ->
            val width = colWidths[index]
            canvas.drawLine(currentX, y, currentX, y + height, linePaint)
            val textX = if (index == 2) currentX + 2 else currentX + width / 2
            val align = if (index == 2) Paint.Align.LEFT else Paint.Align.CENTER
            paint.textAlign = align
            if (index == 2 && text.length > 30) {
                val split = text.chunked(30); var textY = y + 10
                split.forEach { line -> canvas.drawText(line, textX, textY, paint); textY += 10 }
            } else canvas.drawText(text, textX, y + 12, paint)
            currentX += width
        }
        canvas.drawLine(currentX, y, currentX, y + height, linePaint); canvas.drawLine(MARGIN_LEFT, y + height, PAGE_WIDTH - MARGIN_RIGHT, y + height, linePaint)
    }

    private fun calculateRowHeight(subject: String): Float { val lines = (subject.length / 30) + 1; return if (lines > 1) (lines * 10f) + 10f else 20f }
    private fun drawFooter(canvas: Canvas, pageNum: Int) { val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US); val paint = Paint().apply { color = Color.BLACK; textSize = 8f }; val y = PAGE_HEIGHT - 20f; canvas.drawText("MYEDU ${sdf.format(Date())}", MARGIN_LEFT, y, paint); canvas.drawText("Страница $pageNum", PAGE_WIDTH - MARGIN_RIGHT - 50, y, paint) }
}
