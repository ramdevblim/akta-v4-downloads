package com.akta.luraplayersampleapp.modern.utils

import com.akta.luraplayersampleapp.R

object Utils {
    fun map(
        isReverse: Boolean,
        minStart: Float,
        maxStart: Float,
        minResult: Float,
        maxResult: Float,
        desiredValue: Float
    ): Float {
        var mappedValue =
            (desiredValue - minStart) / (maxStart - minStart) * (maxResult - minResult) + minResult

        if (mappedValue > maxResult) mappedValue =
            maxResult else if (mappedValue < minResult) mappedValue = minResult
        if (isReverse) mappedValue = (maxResult - mappedValue)

        return mappedValue
    }

    fun interpolateColor(value: Float, startColor: String, endColor: String): String {
        // Parse the start and end colors into RGB components
        val startR = startColor.substring(1, 3).toInt(16)
        val startG = startColor.substring(3, 5).toInt(16)
        val startB = startColor.substring(5, 7).toInt(16)
        val endR = endColor.substring(1, 3).toInt(16)
        val endG = endColor.substring(3, 5).toInt(16)
        val endB = endColor.substring(5, 7).toInt(16)
        // Interpolate each RGB component
        val newR = (startR + (endR - startR) * value).toInt()
        val newG = (startG + (endG - startG) * value).toInt()
        val newB = (startB + (endB - startB) * value).toInt()
        // Convert the RGB components back to a hexadecimal string
        return "#%02x%02x%02x".format(newR, newG, newB)
    }

    fun getDrawableID(previewName: String?): Int {
        return when (previewName) {
            "charge" -> {
                R.drawable.charge
            }

            "tears_of_steel" -> {
                R.drawable.tears_of_steel
            }

            "big_buck_bunny" -> {
                R.drawable.big_buck_bunny
            }

            "elephants_dream" -> {
                R.drawable.elephants_dream
            }

            "sintel" -> {
                R.drawable.sintel
            }

            "slate" -> {
                R.drawable.slate
            }

            else -> {
                R.drawable.ic_placeholder
            }
        }
    }
}