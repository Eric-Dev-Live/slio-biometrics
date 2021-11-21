package com.sliosecure.slio_biometrics.fpcore

class FPMatch {
    external fun InitMatch(): Int
    external fun MatchTemplate(piFeatureA: ByteArray?, piFeatureB: ByteArray?): Int

    companion object {
        private var mMatch: FPMatch? = null
        val instance: FPMatch?
            get() {
                if (mMatch == null) {
                    mMatch = FPMatch()
                }
                return mMatch
            }

        init {
            System.loadLibrary("fgtitalg")
            System.loadLibrary("fpcore")
        }
    }
}