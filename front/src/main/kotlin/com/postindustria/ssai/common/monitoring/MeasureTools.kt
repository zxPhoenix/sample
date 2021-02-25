package com.postindustria.ssai.common.monitoring

class MeasureTools {
    companion object {
        fun measureTimeInMillis(func: ()->Unit): Long {
            val time = System.currentTimeMillis();

            func.invoke()

            return System.currentTimeMillis() - time;
        }
    }
}