package com.ndgndg91.exceldemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class ExcelDemoApplication

//    @Bean
//    fun run(excelGenerator: ExcelGenerator) = CommandLineRunner {
//        val name = "홍길동"
//        val startDate = LocalDate.of(2023, 1, 1)
//        val endDate = LocalDate.of(2023, 12, 31)
//        val filePath = excelGenerator.generateExcel(name, startDate, endDate, "911107")
//        println("Excel file generated at: $filePath")
//    }
//}

fun main(args: Array<String>) {
    runApplication<ExcelDemoApplication>(*args)
}
