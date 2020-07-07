package com.parvanpajooh.sunmi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import com.parvanpajooh.sunmi.utils.Alignment
import com.parvanpajooh.sunmi.utils.ESCUtil
import com.parvanpajooh.sunmi.utils.PrinterConnectionStatus
import com.sunmi.peripheral.printer.*
import java.math.BigDecimal

object SunmiPrintHelper {
    private lateinit var mContext: Context
    private var sunmiPrinterService: SunmiPrinterService? = null
    private val innerPrinterCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService) {
            sunmiPrinterService = service
            checkSunmiPrinterService(service)
        }

        override fun onDisconnected() {
            sunmiPrinterService = null
            printerConnectionStatus = PrinterConnectionStatus.LOST_PRINTER
        }
    }

    var printerConnectionStatus = PrinterConnectionStatus.CHECK_PRINTER
    fun getPrinterPaperSize() = getService {
        if (it.printerPaper == 1) "58mm" else "80mm"
    }

    val hasPrinterService = sunmiPrinterService != null

    fun initSunmiPrinterService(context: Context) {
        mContext = context
        try {
            val ret = InnerPrinterManager.getInstance().bindService(mContext, innerPrinterCallback)
            if (!ret) {
                printerConnectionStatus = PrinterConnectionStatus.NO_PRINTER
            }
        } catch (e: InnerPrinterException) {
            printerConnectionStatus = PrinterConnectionStatus.NO_PRINTER
            showError(e, "init error")
            e.printStackTrace()
        }
    }

    fun deInitSunmiPrinterService(context: Context) {
        try {
            if (sunmiPrinterService != null) {
                InnerPrinterManager.getInstance().unBindService(context, innerPrinterCallback)
                sunmiPrinterService = null
                printerConnectionStatus = PrinterConnectionStatus.LOST_PRINTER
            }
        } catch (e: InnerPrinterException) {
            showError(e, "deInit error")
            e.printStackTrace()
        }
    }


    /**
     * send esc cmd
     */
    fun sendRawData(data: ByteArray) {
        getService { it.sendRAWData(data, null) }
    }

    data class Pickup(
        val pickupDateTime: String,
        val pickupId: String,
        val courierName: String,
        val parcels: MutableList<Parcel>
    )

    data class Parcel(val city: String, val totalWeight: BigDecimal, val count: Int)

    fun printPickup(pickup: Pickup, withSign: Boolean) {
        setAlign(Alignment.CENTER)
        printBitmap(BitmapFactory.decodeResource(mContext.resources, R.drawable.mahex))
        enter()
        print("mahex.com         ۰۲۱-۹۶۹۶")
        enter()
        val totalCount = pickup.parcels.sumBy { it.count }
        var totalWeight = BigDecimal(0.0)
        pickup.parcels.forEach { totalWeight = totalWeight.add(it.totalWeight) }
        printMap(
            mapOf(
                "جمع\u200Cآوری" to pickup.pickupDateTime.enToFa(),
                "کدپیگیری" to pickup.pickupId.enToFa(),
                "کاربر" to pickup.courierName.enToFa(),
                "تعداد/وزن\u200Cکل" to "${totalCount.toString().enToFa()} / ${totalWeight.toString()
                    .enToFa()} کیلو"
            )
        )
        print("..............................")
        printMap(mapOf("شهر" to "تعداد / وزن کیلوگرم"))
        printMap(pickup.parcels.associate {
            it.city to "${it.totalWeight.toString().enToFa()} /${it.count.toString().enToFa()
                .withPad()}"
        })
        if (withSign)
            print(
                """ ..........................
.                          .
.                          .
.                          .
.           امضا            .
.                          .
.                          .
.                          .
 ..........................

                """.trimIndent()
            )
        feedPaper()
    }

    fun enter() {
        getService {
            it.lineWrap(1, null)
        }
    }

    /**
     * Get paper specifications
     */
    fun getPrinterHead(callbcak: InnerResultCallbcak) {
        getService {
            it.getPrinterFactory(callbcak)
        }
    }


    /**
     * Get printing distance since boot
     * Get printing distance through interface callback since 1.0.8(printerlibrary)
     */
    fun getPrinterDistance(callback: InnerResultCallbcak) {
        getService {
            it.getPrintedLength(callback)
        }
    }

    /**
     * Set printer alignment
     */
    fun setAlign(align: Alignment) {
        getService {
            it.setAlignment(align.code, null)
        }
    }

    /**
     * Due to the distance between the paper hatch and the print head,
     * the paper needs to be fed out automatically
     * But if the Api does not support it, it will be replaced by printing three lines
     */
    fun feedPaper() {
        getService {
            it.autoOutPaper(null)
        }
    }

    /**
     * print text
     * setPrinterStyle Api require V4.2.22 or later, So use esc cmd instead when not supported
     * More settings reference documentation [WoyouConsts]
     */
    fun printText(content: String, size: Float, isBold: Boolean, isUnderLine: Boolean) {
        getService {
            try {
                it.setPrinterStyle(
                    WoyouConsts.ENABLE_BOLD,
                    if (isBold) WoyouConsts.ENABLE else WoyouConsts.DISABLE
                )
            } catch (e: RemoteException) {
                if (isBold) {
                    it.sendRAWData(ESCUtil.boldOn(), null)
                } else {
                    it.sendRAWData(ESCUtil.boldOff(), null)
                }
            }
            try {
                it.setPrinterStyle(
                    WoyouConsts.ENABLE_UNDERLINE,
                    if (isUnderLine) WoyouConsts.ENABLE else WoyouConsts.DISABLE
                )
            } catch (e: RemoteException) {
                if (isUnderLine) {
                    it.sendRAWData(ESCUtil.underlineWithOneDotWidthOn(), null)
                } else {
                    it.sendRAWData(ESCUtil.underlineOff(), null)
                }
            }
            it.printTextWithFont(content, null, size, null)
        }
    }

    /**
     * print Bar Code
     */
    fun printBarCode(
        data: String,
        symbology: Int,
        height: Int,
        width: Int,
        textposition: Int
    ) {
        getService {
            it.printBarCode(data, symbology, height, width, textposition, null)
        }
    }

    /**
     * print Qr Code
     */
    fun printQr(data: String, modulesize: Int, errorlevel: Int) {
        getService {
            it.printQRCode(data, modulesize, errorlevel, null)
        }
    }

    /**
     * Print a row of a table
     */
    fun printTable(
        txts: Array<String>,
        width: IntArray,
        align: IntArray
    ) {
        getService {
            it.printColumnsString(txts, width, align, null)
        }
    }

    /**
     * Print pictures and text in the specified orde
     * After the picture is printed,
     * the line feed output needs to be called,
     * otherwise it will be saved in the cache
     * In this example, the image will be printed because the print text content is added
     */
    fun printBitmap(bitmap: Bitmap) {
        getService {
            setAlign(Alignment.CENTER)
            it.printBitmap(bitmap, null)
        }
    }

    /**
     * Gets whether the current printer is in black mark mode
     */
    fun isBlackLabelMode() = getService { it.printerMode == 1 } ?: false


    /**
     * Gets whether the current printer is in label-printing mode
     */
    fun isLabelMode(): Boolean {
        return getService {
            it.printerMode == 2
        } ?: false
    }


    /* */
    /**
     * Sample print receipt
     *//*
    fun printExample(context: Context) {
        getService {
            val paper = it.printerPaper
            it.printerInit(null)
            it.setAlignment(1, null)
            it.printText("测试样张\n", null)
            val bitmap =
                BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
            it.printBitmap(bitmap, null)
            it.lineWrap(1, null)
            it.setAlignment(0, null)
            try {
                it.setPrinterStyle(WoyouConsts.SET_LINE_SPACING, 0)
            } catch (e: RemoteException) {
                it.sendRAWData(byteArrayOf(0x1B, 0x33, 0x00), null)
            }
            it.printTextWithFont(
                "说明：这是一个自定义的小票样式例子,开发者可以仿照此进行自己的构建\n",
                null, 12f, null
            )
            if (paper == 1) {
                it.printText("--------------------------------\n", null)
            } else {
                it.printText(
                    "------------------------------------------------\n",
                    null
                )
            }
            try {
                it.setPrinterStyle(WoyouConsts.ENABLE_BOLD, WoyouConsts.ENABLE)
            } catch (e: RemoteException) {
                it.sendRAWData(ESCUtil.boldOn(), null)
            }
            val txts = arrayOf("商品", "价格")
            val width = intArrayOf(1, 1)
            val align = intArrayOf(0, 2)
            it.printColumnsString(txts, width, align, null)
            try {
                it.setPrinterStyle(WoyouConsts.ENABLE_BOLD, WoyouConsts.DISABLE)
            } catch (e: RemoteException) {
                it.sendRAWData(ESCUtil.boldOff(), null)
            }
            if (paper == 1) {
                it.printText("--------------------------------\n", null)
            } else {
                it.printText(
                    "------------------------------------------------\n",
                    null
                )
            }
            txts[0] = "汉堡"
            txts[1] = "17¥"
            it.printColumnsString(txts, width, align, null)
            txts[0] = "可乐"
            txts[1] = "10¥"
            it.printColumnsString(txts, width, align, null)
            txts[0] = "薯条"
            txts[1] = "11¥"
            it.printColumnsString(txts, width, align, null)
            txts[0] = "炸鸡"
            txts[1] = "11¥"
            it.printColumnsString(txts, width, align, null)
            txts[0] = "圣代"
            txts[1] = "10¥"
            it.printColumnsString(txts, width, align, null)
            if (paper == 1) {
                it.printText("--------------------------------\n", null)
            } else {
                it.printText(
                    "------------------------------------------------\n",
                    null
                )
            }
            it.printTextWithFont("总计:          59¥\b", null, 40f, null)
            it.setAlignment(1, null)
            it.printQRCode("谢谢惠顾", 10, 0, null)
            it.setFontSize(36f, null)
            it.printText("谢谢惠顾", null)
            it.autoOutPaper(null)
        }
    }
*/
    /**
     * Used to report the real-time query status of the printer, which can be used before each
     * printing
     */
    fun showPrinterStatus(context: Context) {

        var result = "Interface is too low to implement interface"
        getService {
            val res = it.updatePrinterState()
            when (res) {
                1 -> result = "printer is running"
                2 -> result = "printer found but still initializing"
                3 -> result = "printer hardware interface is abnormal and needs to be reprinted"
                4 -> result = "printer is out of paper"
                5 -> result = "printer is overheating"
                6 -> result = "printer's cover is not closed"
                7 -> result = "printer's cutter is abnormal"
                8 -> result = "printer's cutter is normal"
                9 -> result = "not found black mark paper"
                505 -> result = "printer does not exist"
                else -> {
                }
            }
        }
        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
    }
    /**
     * Demo printing a label
     * After printing one label, in order to facilitate the user to tear the paper, call
     * labelOutput to push the label paper out of the paper hatch
     * 演示打印一张标签
     * 打印单张标签后为了方便用户撕纸可调用labelOutput,将标签纸推出纸舱口
     *//*
    fun printOneLabel() {
        getService {
            it.labelLocate()
            printLabelContent()
            it.labelOutput()
        }
    }*/

    /**
     *
     * Custom label ticket content
     * In the example, not all labels can be applied. In actual use, please pay attention to adapting the size of the label. You can adjust the font size and content position.
     * 自定义的标签小票内容
     * 例子中并不能适用所有标签纸，实际使用时注意要自适配标签纸大小，可通过调节字体大小，内容位置等方式
     */
    /*@Throws(RemoteException::class)
    fun printLabelContent() {
        getService {
            it.setPrinterStyle(WoyouConsts.ENABLE_BOLD, WoyouConsts.ENABLE)
            it.lineWrap(1, null)
            it.setAlignment(0, null)
            it.printText("商品         豆浆\n", null)
            it.printText("到期时间         12-13  14时\n", null)
            it.printBarCode("{C1234567890123456", 8, 90, 2, 2, null)
            it.lineWrap(1, null)
        }
    }*/

    fun print(
        text: String,
        fontSize: Float = 24f,
        alignment: Alignment = Alignment.CENTER,
        isBold: Boolean = false,
        isUnderLine: Boolean = false
    ) {
        setAlign(alignment)
        printText(text, fontSize, isBold, isUnderLine)
        enter()
    }

    fun printMap(map: Map<String, String>) {
        getService { sunmiPrinterService ->
            sunmiPrinterService.setFontSize(22f, null)
            map.forEach {
                sunmiPrinterService.sendRAWData(ESCUtil.boldOn(), null)
                printTable(
                    arrayOf(it.value, it.key),
                    intArrayOf(8, 5),
                    intArrayOf(Alignment.RIGHT.code, Alignment.RIGHT.code)
                )

            }
            sunmiPrinterService.sendRAWData(ESCUtil.boldOff(), null)
        }
    }

    private fun checkSunmiPrinterService(service: SunmiPrinterService) {
        var ret = false
        try {
            ret = InnerPrinterManager.getInstance().hasPrinter(service)
        } catch (e: InnerPrinterException) {
            e.printStackTrace()
        }
        printerConnectionStatus =
            if (ret) PrinterConnectionStatus.FOUND_PRINTER else PrinterConnectionStatus.NO_PRINTER
    }

    private fun handleRemoteException(e: RemoteException) {
        showError(e, "RemoteException")
        e.printStackTrace()
    }

    private fun showError(e: Exception, defaultMessage: String) =
        showError(e.message ?: e.localizedMessage ?: defaultMessage)

    private fun showError(text: String) =
        Log.w("Printer", text)//Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show()

    private fun <T> getService(program: (service: SunmiPrinterService) -> T?): T? {
        return if (sunmiPrinterService == null) {
            showError("Service disconnection processing")
            null
        } else try {
            program(sunmiPrinterService!!)
        } catch (e: RemoteException) {
            handleRemoteException(e)
            null
        }
    }

    private fun String.withPad() = padStart(("تعداد".length))

    private fun String.enToFa(): String {
        var content = this
        for ((en, fa) in numberMaps) {
            content = content.replace(en, fa)
        }
        return content
    }

    private val numberMaps = mapOf(
        "0" to "۰",
        "1" to "۱",
        "2" to "۲",
        "3" to "۳",
        "4" to "۴",
        "5" to "۵",
        "6" to "۶",
        "7" to "۷",
        "8" to "۸",
        "9" to "۹"

    )
}