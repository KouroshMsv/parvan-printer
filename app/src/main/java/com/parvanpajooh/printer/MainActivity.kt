package com.parvanpajooh.printer

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.parvanpajooh.sunmi.SunmiPrintHelper
import java.math.BigDecimal

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        SunmiPrintHelper.initSunmiPrinterService(this)
        Handler().postDelayed({
            SunmiPrintHelper.apply {

                printPickup(
                    SunmiPrintHelper.Pickup(
                        "۱۳۹۹/۰۴/۰۴-۰۳:۲۷",
                        "112313424",
                        "علی احمدی",
                        mutableListOf(
                            SunmiPrintHelper.Parcel("تهران", BigDecimal(24), 4),
                            SunmiPrintHelper.Parcel("شیراز", BigDecimal(12), 5),
                            SunmiPrintHelper.Parcel("اصفهان", BigDecimal(2), 16)
                        )
                    ), false
                )
                feedPaper()
            }

        }, 2000)
    }
}