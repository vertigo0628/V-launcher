package com.example.launcher.logic

import java.util.Calendar

object HolidayManager {
    fun getHoliday(calendar: Calendar): String? {
        val month = calendar.get(Calendar.MONTH) // 0-11
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val weekOfMonth = calendar.get(Calendar.DAY_OF_WEEK_IN_MONTH)

        return when {
            // Fixed Date Holidays
            month == Calendar.JANUARY && day == 1 -> "NEW YEAR'S DAY"
            month == Calendar.FEBRUARY && day == 14 -> "VALENTINE'S DAY"
            month == Calendar.MARCH && day == 17 -> "ST. PATRICK'S DAY"
            month == Calendar.MAY && day == 1 -> "LABOUR DAY"
            month == Calendar.JUNE && day == 19 -> "JUNETEENTH"
            month == Calendar.JULY && day == 4 -> "INDEPENDENCE DAY"
            month == Calendar.OCTOBER && day == 31 -> "HALLOWEEN"
            month == Calendar.NOVEMBER && day == 1 -> "ALL SAINTS' DAY"
            month == Calendar.NOVEMBER && day == 11 -> "VETERANS DAY"
            month == Calendar.DECEMBER && day == 25 -> "CHRISTMAS DAY"
            month == Calendar.DECEMBER && day == 31 -> "NEW YEAR'S EVE"

            // Floating Holidays (US-centric examples, can be expanded)
            month == Calendar.JANUARY && dayOfWeek == Calendar.MONDAY && weekOfMonth == 3 -> "MLK JR. DAY"
            month == Calendar.FEBRUARY && dayOfWeek == Calendar.MONDAY && weekOfMonth == 3 -> "PRESIDENTS' DAY"
            month == Calendar.MAY && dayOfWeek == Calendar.SUNDAY && weekOfMonth == 2 -> "MOTHER'S DAY"
            month == Calendar.MAY && dayOfWeek == Calendar.MONDAY && isLastWeek(calendar) -> "MEMORIAL DAY"
            month == Calendar.JUNE && dayOfWeek == Calendar.SUNDAY && weekOfMonth == 3 -> "FATHER'S DAY"
            month == Calendar.SEPTEMBER && dayOfWeek == Calendar.MONDAY && weekOfMonth == 1 -> "LABOR DAY"
            month == Calendar.OCTOBER && dayOfWeek == Calendar.MONDAY && weekOfMonth == 2 -> "COLUMBUS DAY"
            month == Calendar.NOVEMBER && dayOfWeek == Calendar.THURSDAY && weekOfMonth == 4 -> "THANKSGIVING"

            else -> null
        }
    }

    private fun isLastWeek(calendar: Calendar): Boolean {
        val temp = calendar.clone() as Calendar
        temp.add(Calendar.DAY_OF_MONTH, 7)
        return temp.get(Calendar.MONTH) != calendar.get(Calendar.MONTH)
    }
}
