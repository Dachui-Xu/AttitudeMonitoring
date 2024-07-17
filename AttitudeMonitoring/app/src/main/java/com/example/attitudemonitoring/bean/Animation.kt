package com.example.attitudemonitoring.bean

import com.example.attitudemonitoring.R

interface Animation {
    val resourceId: Int
}

enum class WorkAnimation(override val resourceId: Int) : Animation {
    NORMAL(R.raw.normal),
    DOWN(R.raw.down),
    BIG_DOWN(R.raw.bigdown),
    RIGHT(R.raw.right),
    LEFT(R.raw.left),
    RISE(R.raw.rise)
}

enum class DriveAnimation(override val resourceId: Int) : Animation {
    DRIVE(R.raw.drive),
    LEFT(R.raw.cleft),
    RIGHT(R.raw.cright),
    DOWN(R.raw.cdown)
}
