package com.powertracker.platform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toKotlinLocalDate

/** Today on this phone's clock, in its time zone. The coach never reads the clock; screens pass this in. */
fun systemToday(): LocalDate = java.time.LocalDate.now().toKotlinLocalDate()
