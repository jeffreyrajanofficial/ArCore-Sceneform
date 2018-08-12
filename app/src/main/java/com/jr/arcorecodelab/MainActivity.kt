package com.jr.arcorecodelab

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.support.design.widget.Snackbar
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.PixelCopy
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.jr.arcorecodelab.R.drawable.*

import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var arFragment: ArFragment
    private val pointerDrawable = PointerDrawable()
    private var isTracking: Boolean = false;
    private var isHitting: Boolean = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        arFragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment

        arFragment.arSceneView.scene.addOnUpdateListener {
            arFragment.onUpdate(it)
            onUpdate()
        }

        initializeGallery()

        fab.setOnClickListener { view ->
            takePhoto()
        }
    }

    private fun onUpdate() {
        val trackChanged: Boolean = updateTracking()
        val contentView = findViewById<View>(android.R.id.content)
        if(trackChanged) {
            if (isTracking)
                contentView.overlay.add(pointerDrawable)
            else
                contentView.overlay.remove(pointerDrawable)

            contentView.invalidate()
        }

        if (isTracking) {
            val hitChanged = updateHitTest()
            if(hitChanged) {
                pointerDrawable.setEnabled(isHitting)
                contentView.invalidate()
            }
        }
    }

    private fun updateHitTest(): Boolean {
        val frame = arFragment.arSceneView.arFrame
        val point = getScreenCenter()
        val wasHitting = isHitting
        isHitting = false
        if (frame != null) {
            val hits = frame.hitTest(point.x.toFloat(), point.y.toFloat())
            for(hit in hits) {
                val trackable = hit.trackable
                if(trackable is Plane && (trackable as Plane).isPoseInPolygon(hit.hitPose)){
                    isHitting = true
                    break
                }
            }
        }

        return wasHitting != isHitting
    }

    private fun getScreenCenter(): Point {
        val view = findViewById<View>(android.R.id.content)
        return Point(view.width/2, view.height/2)
    }

    private fun updateTracking(): Boolean {
        val frame = arFragment.arSceneView.arFrame
        val wasTracking = isTracking
        isTracking = frame.camera.trackingState == TrackingState.TRACKING
        return isTracking != wasTracking
    }

    fun initializeGallery(): Unit {
        val gallery = findViewById<LinearLayout>(R.id.gallery_output)

        val andy = ImageView(this)
        andy.setImageResource(droid_thumb)
        andy.contentDescription = "andy"
        andy.setOnClickListener{
            addObject(Uri.parse("andy.sfb"))
        }
        gallery.addView(andy)

        val cabin = ImageView(this)
        cabin.setImageResource(cabin_thumb)
        cabin.contentDescription = "cabin"
        cabin.setOnClickListener{
            addObject(Uri.parse("Cabin.sfb"))
        }
        gallery.addView(cabin)

        val house = ImageView(this)
        house.setImageResource(house_thumb)
        house.contentDescription = "house"
        house.setOnClickListener{
            addObject(Uri.parse("House.sfb"))
        }
        gallery.addView(house)

        val igloo = ImageView(this)
        igloo.setImageResource(igloo_thumb)
        igloo.contentDescription = "igloo"
        igloo.setOnClickListener{
            addObject(Uri.parse("igloo.sfb"))
        }
        gallery.addView(igloo)
    }

    private fun addObject(parse: Uri?) {
        val frame = arFragment.arSceneView.arFrame
        val point = getScreenCenter()

        if(frame != null) {
            val hits = frame.hitTest(point.x.toFloat(), point.y.toFloat())

            for(hit in hits) {
                val trackable = hit.trackable

                if(trackable is Plane &&
                        (trackable as Plane).isPoseInPolygon(hit.hitPose)) {
                    placeObject(arFragment, hit.createAnchor(), parse)
                }
            }
        }
    }

    private fun placeObject(arFragment: ArFragment, createAnchor: Anchor?, parse: Uri?) {
        ModelRenderable.builder()
                .setSource(arFragment.context, parse)
                .build()
                .thenAccept{ renderable: Renderable ->
                    addNodeToScene(arFragment, createAnchor, renderable)
                }
                .exceptionally { throwable  ->
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle("Error")
                            .setMessage(throwable.message)
                    val dialog = builder.create()
                    dialog.show()
                    return@exceptionally null
                }
    }

    private fun addNodeToScene(arFragment: ArFragment, createAnchor: Anchor?, renderable: Renderable?) {
        val anchorNode = AnchorNode(createAnchor)
        val transformableNode = TransformableNode(arFragment.transformationSystem)
        transformableNode.renderable  = renderable
        transformableNode.setParent(anchorNode)
        arFragment.arSceneView.scene.addChild(anchorNode)
        transformableNode.select()
    }

    private fun generateFilename(): String {
        val date = SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(Date())
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).toString() + File.separator + "Sceneform/" + date + "_screenshot.jpg"
    }

    @Throws(IOException::class)
    private fun saveBitmapToDisk(bitmap: Bitmap, filename: String) {
        val out = File(filename)
        if (!out.parentFile.exists()) {
            out.parentFile.mkdirs()
        }
        try {
            FileOutputStream(filename).use { outputStream ->
                ByteArrayOutputStream().use { outputData ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData)
                    outputData.writeTo(outputStream)
                    outputStream.flush()
                    outputStream.close()
                }
            }
        } catch (ex: IOException) {
            throw IOException("Failed to save bitmap to disk", ex)
        }
    }

    private fun takePhoto() {
        val fileName = generateFilename()
        val view = arFragment.arSceneView

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        val handlerThread = HandlerThread("PixelCopier")

        handlerThread.start()

        PixelCopy.request(view, bitmap, {
            if(it == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, fileName)
                } catch (e: IOException) {
                    Toast.makeText(
                            baseContext,
                            e.message,
                            Toast.LENGTH_SHORT
                            ).show()
                }

                val snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Photo Saved", Snackbar.LENGTH_LONG)
                snackbar.setAction("Open in Photos") {
                    val photoFile = File(fileName)
                    val photoURI = FileProvider.getUriForFile(baseContext, packageName+".ar.name.provider",
                            photoFile)
                    val intent = Intent(Intent.ACTION_VIEW, photoURI)
                    intent.setDataAndType(photoURI, "image/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
                snackbar.show()
            } else {
                Toast.makeText(
                        baseContext,
                        "Failed to copyPixel: $it",
                        Toast.LENGTH_SHORT
                ).show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
