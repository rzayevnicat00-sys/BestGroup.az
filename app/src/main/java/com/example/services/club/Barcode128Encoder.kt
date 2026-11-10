package com.example.services.club

/**
 * Standard ISO/IEC 15417 Code 128 barcode encoder.
 * Produces real, scannable barcode module patterns (bars and spaces)
 * that can be read by any optical handheld scanner, Google Lens, or mobile camera scanner.
 */
object Barcode128Encoder {

    // Widths of 3 bars and 3 spaces (sum = 11) for symbols 0..105,
    // and 4 bars and 3 spaces (sum = 13) for stop symbol 106.
    private val PATTERNS = arrayOf(
        "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
        "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
        "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
        "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
        "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
        "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
        "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
        "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
        "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
        "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
        "114131", "311141", "411131", "211412", "211214", "211232", "2331112"
    )

    private const val START_C = 105
    private const val STOP = 106

    /**
     * Encodes a 16-digit (or any even-length numeric string) into a standard Code 128 Subset C barcode.
     * Subset C encodes 2 digits per symbol, yielding the highest density and smallest barcode width.
     *
     * @param numericCode 16-digit card number (e.g. "8620491036287154")
     * @param quietZoneModules Number of white margin modules on each side (default: 10)
     * @return BooleanArray where true = black bar module, false = white space module
     */
    fun encodeCode128C(numericCode: String, quietZoneModules: Int = 10): BooleanArray {
        val cleanDigits = numericCode.filter { it.isDigit() }.let {
            if (it.length % 2 != 0) "0$it" else it
        }.ifEmpty { "8620202611100000" }

        // Break into pairs of 2 digits (each pair 00..99 corresponds directly to symbol 0..99)
        val pairs = mutableListOf<Int>()
        for (i in 0 until cleanDigits.length step 2) {
            pairs.add(cleanDigits.substring(i, i + 2).toInt())
        }

        // Calculate modulo 103 checksum: Start_C (105) + sum((index + 1) * pairValue)
        var checksumSum = START_C
        for (i in pairs.indices) {
            checksumSum += (i + 1) * pairs[i]
        }
        val checksumSymbol = checksumSum % 103

        // Sequence of symbol indices: [Start_C, ...pairs, checksum, Stop]
        val symbols = ArrayList<Int>(pairs.size + 3)
        symbols.add(START_C)
        symbols.addAll(pairs)
        symbols.add(checksumSymbol)
        symbols.add(STOP)

        // Convert symbols to binary modules (true = black bar, false = white space)
        val moduleList = ArrayList<Boolean>()

        // Left Quiet Zone
        for (q in 0 until quietZoneModules) {
            moduleList.add(false)
        }

        for (sym in symbols) {
            val pattern = PATTERNS[sym]
            var isBar = true
            for (c in pattern) {
                val width = c.digitToInt()
                for (w in 0 until width) {
                    moduleList.add(isBar)
                }
                isBar = !isBar
            }
        }

        // Right Quiet Zone
        for (q in 0 until quietZoneModules) {
            moduleList.add(false)
        }

        val result = BooleanArray(moduleList.size)
        for (i in moduleList.indices) {
            result[i] = moduleList[i]
        }
        return result
    }

    /**
     * Formats a 16-digit card number with spaces every 4 digits:
     * e.g. "8620491036287154" -> "8620  4910  3628  7154"
     */
    fun format16DigitCardNumber(raw: String): String {
        val digits = raw.filter { it.isDigit() }.padEnd(16, '0').take(16)
        return digits.chunked(4).joinToString("  ")
    }
}
