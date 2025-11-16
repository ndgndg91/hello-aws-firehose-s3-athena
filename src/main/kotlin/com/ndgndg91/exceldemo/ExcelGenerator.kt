package com.ndgndg91.exceldemo

import org.apache.poi.poifs.crypt.EncryptionInfo
import org.apache.poi.poifs.crypt.EncryptionMode
import org.apache.poi.poifs.crypt.Encryptor
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class ExcelGenerator {

    fun generateExcel(name: String, startDate: LocalDate, endDate: LocalDate, password: String? = null): String {
        val workbook = XSSFWorkbook()

        // Sheet 1: 이용 내역
        workbook.createSheet("이용 내역")

        // Sheet 2: 제공 내역
        workbook.createSheet("제공 내역")

        // Title format: {name}님의 개인(신용)정보 이용_제공_내역_{YYYY.MM.DD ~ YYYY.MM.DD}
        val dateFormatter = DateTimeFormatter.ofPattern("YYYY.MM.DD")
        val formattedStartDate = startDate.format(dateFormatter)
        val formattedEndDate = endDate.format(dateFormatter)
        val title = "${name}님의 개인(신용)정보 이용_제공_내역_${formattedStartDate} ~ ${formattedEndDate}"

        val tempFileName = "${title}.xlsx"

        if (password.isNullOrBlank()) {
            FileOutputStream(tempFileName).use { outputStream ->
                workbook.write(outputStream)
            }
        } else {
            POIFSFileSystem().use { fs ->
                val info = EncryptionInfo(EncryptionMode.agile)
                val encryptor = info.encryptor
                encryptor.confirmPassword(password)

                encryptor.getDataStream(fs).use { os ->
                    workbook.write(os)
                }

                FileOutputStream(tempFileName).use { fos ->
                    fs.writeFilesystem(fos)
                }
            }
        }

        workbook.close()
        return tempFileName
    }
}
