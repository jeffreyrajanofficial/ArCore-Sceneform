package com.jr.arcorecodelab

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.drawable.Drawable

class PointerDrawable : Drawable() {
    private val paint: Paint = Paint()
    private var enabled: Boolean = false

    fun isEnabled(): Boolean = enabled

    fun setEnabled(enabled: Boolean) : Unit {
        this.enabled = enabled
    }

    override fun draw(p0: Canvas?) {
        val cx = p0!!.width/2
        val cy = p0.height/2

        if(enabled){
            paint.color = Color.GREEN
            p0.drawCircle(cx.toFloat(), cy.toFloat(), 10F, paint)
        } else {
            paint.color = Color.GRAY
            p0.drawText("X", cx.toFloat(), cy.toFloat(), paint)
        }

    }

    override fun setAlpha(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOpacity(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setColorFilter(p0: ColorFilter?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}