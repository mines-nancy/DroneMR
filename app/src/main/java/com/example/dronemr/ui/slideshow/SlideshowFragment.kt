package com.example.dronemr.ui.slideshow

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.dronemr.MainActivity
import com.example.dronemr.R
import com.example.dronemr.databinding.FragmentSlideshowBinding
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.parrot.drone.groundsdk.Ref
//import com.parrot.drone.groundsdk.UnstableApi
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.MainCamera
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.camera.*
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.stream.GsdkStreamView
//import com.parrot.drone.groundsdk.stream.RawVideoSink
//import com.parrot.drone.groundsdk.stream.RawVideoSink.Companion.config
//import com.parrot.drone.groundsdk.stream.Stream.Sink
//import com.parrot.drone.groundsdk.stream.Stream.Sink.Config
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.atan2


class SlideshowFragment : Fragment() {
    companion object {
        const val TAG = "MLKit-ODT"
        const val REQUEST_IMAGE_CAPTURE: Int = 1
        private const val MAX_FONT_SIZE = 50F
    }


    private var _binding: FragmentSlideshowBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    /** Reference to the current drone stream server Peripheral. */
    private var streamServerRef: Ref<StreamServer>? = null

    /** Reference to the current drone live stream. */
    private var liveStreamRef: Ref<CameraLive>? = null

    /** Current drone live stream. */
    private var liveStream: CameraLive? = null



    /**
    /** Video sink */
    @OptIn(UnstableApi::class)
    private lateinit var streamSink: Sink
    private lateinit var sinkConfig: Config
    private lateinit var looper: Looper

    @OptIn(UnstableApi::class)
    private lateinit var callback: RawVideoSink.Callback
    */

    /** Reference on MainCamera peripheral. */
    private var cameraRef: Ref<MainCamera>? = null

    private lateinit var slideshowViewModel: SlideshowViewModel

    // User interface:
    /** Video stream view. */
    private lateinit var streamView: GsdkStreamView
    private lateinit var recordButton: Button
    private lateinit var detectObjectButton: Button
    private lateinit var detectPoseButton: Button
    private lateinit var mainActivity: MainActivity

    /** detection view */
    private lateinit var objectDetectionImageView: ImageView
    private var isObjectDetecting: Boolean = false
    private lateinit var poseDetectionImageView: ImageView
    private var isPoseDetecting: Boolean = false

    /** detection*/
    private lateinit var objectDetector: ObjectDetector
    private val labels: ArrayList<String> = arrayListOf("Person", "Car", "Truck", "Drone", "Vehicle", "Plane")
    /** JSONObject for drone position */
    private lateinit var dronesDetectedPosition: JSONObject

    /** pose detection*/
    private lateinit var poseDetector: PoseDetector

    /**transparent bitmap */
    private lateinit var mTransparentBackground: Bitmap
    private lateinit var objectDetectionBitmap : Bitmap
    private lateinit var poseDetectionBitmap : Bitmap

    /** adjust video size button */
    private lateinit var plusButton: Button
    private lateinit var minusButton: Button


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        slideshowViewModel =
            ViewModelProvider(this)[SlideshowViewModel::class.java]

        mainActivity = activity as MainActivity

        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textSlideshow

        streamView = root.findViewById(R.id.stream_view)
        streamView.paddingFill = GsdkStreamView.PADDING_FILL_BLUR_CROP


        recordButton = root.findViewById(R.id.record_button)
        detectObjectButton = root.findViewById(R.id.detect_object_button)
        detectPoseButton = root.findViewById(R.id.detect_pose_button)

        objectDetectionImageView= root.findViewById(R.id.objectDetectionImageView)
        poseDetectionImageView = root.findViewById(R.id.poseDetectionImageView)

        plusButton = root.findViewById(R.id.plus_button)
        minusButton = root.findViewById(R.id.minus_button)

        slideshowViewModel.text.observe(viewLifecycleOwner) {
            textView.text = "drone non connecté"
        }

        if (mainActivity.drone != null) {
            startVideoStream()
            recordButton.setOnClickListener {
                toggleStartStopRecord(mainActivity.drone!!)
            }

            detectObjectButton.setOnClickListener {
                MainScope().launch {
                    if (isObjectDetecting) {
                        isObjectDetecting = false
                        objectDetectionImageView.visibility = View.GONE
                    } else {
                        objectDetectionImageView.visibility = View.VISIBLE
                        isObjectDetecting = true

                        startObjectDetection()
                    }
                }
            }

            detectPoseButton.setOnClickListener {
                MainScope().launch {
                    if (isPoseDetecting) {
                        isPoseDetecting = false
                        poseDetectionImageView.visibility = View.GONE
                    } else {
                        poseDetectionImageView.visibility = View.VISIBLE
                        isPoseDetecting = true
                        startPoseDetection()
                    }
                }
            }
        } else {
            recordButton.isEnabled = false
            detectObjectButton.isEnabled = false
            detectPoseButton.isEnabled = false
        }

        plusButton.setOnClickListener {
            val layoutParams = ViewGroup.LayoutParams(streamView.layoutParams.width, streamView.layoutParams.height)
            layoutParams.height += 50
            streamView.layoutParams = layoutParams
            objectDetectionImageView.layoutParams = layoutParams
            poseDetectionImageView.layoutParams = layoutParams
        }
        minusButton.setOnClickListener {
            val layoutParams = ViewGroup.LayoutParams(streamView.layoutParams.width, streamView.layoutParams.height)
            layoutParams.height -= 50
            streamView.layoutParams = layoutParams
            objectDetectionImageView.layoutParams = layoutParams
            poseDetectionImageView.layoutParams = layoutParams
        }



        // looper = Looper.getMainLooper()
        //callback = StreamCallBack(mainActivity)

        //sinkConfig = config(looper, callback)

        dronesDetectedPosition = JSONObject()


        buildModel()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        isObjectDetecting = false
        isPoseDetecting = false

        objectDetectionImageView.visibility = View.GONE
        poseDetectionImageView.visibility = View.GONE

        streamView.visibility = View.GONE

        _binding = null
    }

    private fun startVideoStream() {
        // Monitor the stream server.

        streamServerRef =
            mainActivity.drone?.getPeripheral(StreamServer::class.java) { streamServer ->
                // Called when the stream server is available and when it changes.
                val textView: TextView = binding.textSlideshow
                slideshowViewModel.text.observe(viewLifecycleOwner) {
                    textView.text = "caméra"
                }

                streamServer?.run {
                    // Enable Streaming
                    if (!streamingEnabled()) {
                        enableStreaming(true)
                    }

                    // Monitor the live stream.
                    if (liveStreamRef == null) {
                        liveStreamRef = live { stream ->
                            // Called when the live stream is available and when it changes.

                            if (stream != null) {
                                if (liveStream == null) {
                                    // It is a new live stream.

                                    // Set the live stream as the stream
                                    // to be render by the stream view.

                                    streamView.setStream(stream)
                                }

                                // Play the live stream.
                                if (stream.playState() != CameraLive.PlayState.PLAYING) {
                                    stream.play()
                                }

                            } else {
                                // Stop rendering the stream
                                streamView.setStream(null)
                            }
                            // Keep the live stream to know if it is a new one or not.
                            liveStream = stream
                        }
                    }
                } ?: run {
                    // Stop monitoring the live stream
                    liveStreamRef?.close()
                    liveStreamRef = null
                    // Stop rendering the stream
                    streamView.setStream(null)
                }
            }
    }

    /** Monitors and prints whether video recording can be started or stopped. */
    fun monitorCanStartStopRecord(drone: Drone) {
        cameraRef = drone.getPeripheral(MainCamera::class.java) { camera ->
            // called on main thread when the camera peripheral changes
            val myToast =
                Toast.makeText(mainActivity, camera?.mode()?.value.toString(), Toast.LENGTH_SHORT)
            myToast.show()

            camera?.run {
                when {
                    canStartRecording() ->
                        println("Video recording can be started")

                    canStopRecording() ->
                        println("Video recording can be stopped")

                    else ->
                        println("Video recording can't be started or stopped")
                }
            }
        }
    }

    /** Starts or stops video recording. */
    private fun toggleStartStopRecord(drone: Drone) {
        drone.getPeripheral(MainCamera::class.java)?.run {
            when {
                // recording can be started
                canStartRecording() -> {
                    startRecording()
                    recordButton.text = "Stop Recording"
                    val myToast =
                        Toast.makeText(mainActivity, "starting recording", Toast.LENGTH_SHORT)
                    myToast.show()

                }

                // recording can be stopped
                canStopRecording() -> {
                    stopRecording()
                    recordButton.text = "Start Recording"
                    val myToast =
                        Toast.makeText(mainActivity, "stopped recording", Toast.LENGTH_SHORT)
                    myToast.show()
                }

                // recording can't be started or stopped
                else -> {
                    val myToast =
                        Toast.makeText(
                            mainActivity,
                            "cannot start or stop recording",
                            Toast.LENGTH_SHORT
                        )
                    myToast.show()
                }
            }
        }

    }

    private fun buildModel() {
        val localModel = LocalModel.Builder()
            .setAssetFilePath("lite-model_object_detection_mobile_object_labeler_v1_1.tflite")
            //.setAssetFilePath("v5s640-fp16.tflite")
            // or .setAbsoluteFilePath(absolute file path to model file)
            // or .setUri(URI to model file)
            .build()

        val poseDetectionOptions = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()


        // Step 2: acquire detector object
        val objectDetectionOptions = CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .setMaxPerObjectLabelCount(1)
            .enableClassification()
            .setClassificationConfidenceThreshold(0.7F)
            .build()

        objectDetector = ObjectDetection.getClient(objectDetectionOptions)
        poseDetector = PoseDetection.getClient(poseDetectionOptions)

    }
    private fun runObjectDetection(bitmap: Bitmap) {
        // Step 1: create ML Kit's InputImage object
        val image = InputImage.fromBitmap(bitmap, 0)

        dronesDetectedPosition = JSONObject()
        // Step 3: feed given image to detector and setup callback

        objectDetector.process(image)
            .addOnSuccessListener {
                // Task completed successfully
                debugPrint(it)

                // Parse ML Kit's DetectedObject and create corresponding visualization data for
                // wanted labels
                val detectedObjects = it
                    .map { obj ->
                        var text = "Unknown"
                        var label = ""
                        val id = obj.trackingId
                        // We will show the top confident detection result if it exist
                        if (obj.labels.isNotEmpty()) {
                            val firstLabel = obj.labels.first()
                            label = firstLabel.text
                            text =
                                "${firstLabel.text}_${id}, ${
                                    firstLabel.confidence.times(100).toInt()
                                }%"
                        }
                        if (label == "Drone") {
                            val dronePos = JSONObject()
                            dronePos.put("x", obj.boundingBox.centerX())
                            dronePos.put("y", obj.boundingBox.centerY())
                            dronesDetectedPosition.put("dronePos_${id}", dronePos)
                        }

                        BoxWithText(obj.boundingBox, text)
                    }

                // Draw the detection result on the input bitmap
                if (isObjectDetecting) {
                    objectDetectionBitmap = drawDetectionResult(bitmap, detectedObjects)

                    // Show the detection result on the app screen

                    mainActivity.runOnUiThread {
                        if (isObjectDetecting) {
                            objectDetectionImageView.setImageBitmap(objectDetectionBitmap)
                        }
                    }
                }


            }

            .addOnFailureListener {
                // Task failed with an exception
                val myToast =
                    Toast.makeText(mainActivity, it.message.toString(), Toast.LENGTH_SHORT)
                myToast.show()
                Log.e(TAG, it.message.toString())
            }



    }

    private fun runPoseDetection(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)

        if (isPoseDetecting) {
            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    // Task completed successfully
                    // ...

                    if (isPoseDetecting) {
                        poseDetectionBitmap = drawPoseResult(bitmap, pose)


                        mainActivity.runOnUiThread {
                            if (isPoseDetecting) {
                                poseDetectionImageView.setImageBitmap(poseDetectionBitmap)
                            }
                        }
                    }


                }
                .addOnFailureListener {
                    val myToast =
                        Toast.makeText(mainActivity, it.message.toString(), Toast.LENGTH_SHORT)
                    myToast.show()
                    Log.e(TAG, it.message.toString())
                }
        }

    }


    private fun debugPrint(detectedObjects: List<DetectedObject>) {
        detectedObjects.forEachIndexed { index, detectedObject ->
            val box = detectedObject.boundingBox

            Log.d(TAG, "Detected object: $index")
            Log.d(TAG, " trackingId: ${detectedObject.trackingId}")
            Log.d(TAG, " boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")
            detectedObject.labels.forEach {
                Log.d(TAG, " categories: ${it.text}")
                Log.d(TAG, " confidence: ${it.confidence}")
            }
        }
    }


    private suspend fun startObjectDetection() {
        while (isObjectDetecting) {
            streamView.capture {
                if (it != null) {
                    runObjectDetection(it)
                }
            }
            delay(100L)
        }
    }

    private suspend fun startPoseDetection() {
        while(isPoseDetecting) {
            streamView.capture {
                if (it != null) {
                    runPoseDetection(it)
                }
            }
            delay(100L)
        }
    }


    /**
     * Draw bounding boxes around objects together with the object's name.
     */
    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<BoxWithText>
    ): Bitmap {

        mTransparentBackground = Bitmap.createBitmap(
            bitmap.width, bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(mTransparentBackground)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT


        detectionResults.forEach {
            // draw bounding box
            pen.color = Color.RED
            pen.strokeWidth = 4F
            pen.style = Paint.Style.STROKE
            val box = it.box
            canvas.drawRect(box, pen)

            val tagSize = Rect(0, 0, 0, 0)

            // calculate the right font size
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.YELLOW
            pen.strokeWidth = 2F

            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(it.text, 0, it.text.length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.textSize) pen.textSize = fontSize

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F
            canvas.drawText(
                it.text, box.left + margin,
                box.top + tagSize.height().times(1F), pen
            )
        }
        return mTransparentBackground
    }

    private fun drawPoseResult(bitmap: Bitmap, pose: Pose) : Bitmap{

        val detectedPose = detectPose(pose)

        mTransparentBackground = Bitmap.createBitmap(
            bitmap.width, bitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(mTransparentBackground)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT
        pen.color = Color.RED

        val landmarks = pose.allPoseLandmarks
        for (landmark in landmarks) {
            canvas.drawCircle(landmark.position3D.x, landmark.position3D.y, 3.0F, pen)
        }

        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val leftEyeInner = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER)
        val leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)
        val leftEyeOuter = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER)
        val rightEyeInner = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER)
        val rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)
        val rightEyeOuter = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER)
        val leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR)
        val rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR)
        val leftMouth = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH)
        val rightMouth = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
        val rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)
        val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
        val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)
        val leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB)
        val rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)
        val leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL)
        val rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)
        val leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)
        val rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)

        // Face
        drawLine(canvas, nose, leftEyeInner, pen)
        drawLine(canvas, leftEyeInner, leftEye, pen)
        drawLine(canvas, leftEye, leftEyeOuter, pen)
        drawLine(canvas, leftEyeOuter, leftEar, pen)
        drawLine(canvas, nose, rightEyeInner, pen)
        drawLine(canvas, rightEyeInner, rightEye, pen)
        drawLine(canvas, rightEye, rightEyeOuter, pen)
        drawLine(canvas, rightEyeOuter, rightEar, pen)
        drawLine(canvas, leftMouth, rightMouth, pen)

        drawLine(canvas, leftShoulder, rightShoulder, pen)
        drawLine(canvas, leftHip, rightHip, pen)

        // Left body
        drawLine(canvas, leftShoulder, leftElbow, pen)
        drawLine(canvas, leftElbow, leftWrist, pen)
        drawLine(canvas, leftShoulder, leftHip, pen)
        drawLine(canvas, leftHip, leftKnee, pen)
        drawLine(canvas, leftKnee, leftAnkle, pen)
        drawLine(canvas, leftWrist, leftThumb, pen)
        drawLine(canvas, leftWrist, leftPinky, pen)
        drawLine(canvas, leftWrist, leftIndex, pen)
        drawLine(canvas, leftIndex, leftPinky, pen)
        drawLine(canvas, leftAnkle, leftHeel, pen)
        drawLine(canvas, leftHeel, leftFootIndex, pen)

        // Right body
        drawLine(canvas, rightShoulder, rightElbow, pen)
        drawLine(canvas, rightElbow, rightWrist, pen)
        drawLine(canvas, rightShoulder, rightHip, pen)
        drawLine(canvas, rightHip, rightKnee, pen)
        drawLine(canvas, rightKnee, rightAnkle, pen)
        drawLine(canvas, rightWrist, rightThumb, pen)
        drawLine(canvas, rightWrist, rightPinky, pen)
        drawLine(canvas, rightWrist, rightIndex, pen)
        drawLine(canvas, rightIndex, rightPinky, pen)
        drawLine(canvas, rightAnkle, rightHeel, pen)
        drawLine(canvas, rightHeel, rightFootIndex, pen)

        //write pose
        val tagSize = Rect(0, 0, 0, 0)

        pen.textSize = 3*MAX_FONT_SIZE/4
        pen.getTextBounds( detectedPose, 0, detectedPose.length, tagSize)

        if(detectedPose != "") {
            val margin = 10
            if(leftHip!!.position3D.x > rightHip!!.position3D.x) {
                canvas.drawText(
                    detectedPose, leftHip.position3D.x + margin,
                    leftHip.position3D.y - 2*margin, pen
                )
            } else {
                canvas.drawText(
                    detectedPose, rightHip.position3D.x + margin,
                    rightHip.position3D.y - 2*margin, pen
                )
            }
        }

        return mTransparentBackground
    }

    private fun detectPose(pose: Pose) : String {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

        var leftKneeAngle : Double = 0.0
        var rightKneeAngle : Double = 0.0
        if(leftKnee != null && leftHip !=null && leftAnkle != null) {
            leftKneeAngle = getAngle(leftHip, leftKnee, leftAnkle)
        }
        if(rightKnee != null && rightHip !=null && rightAnkle != null) {
            rightKneeAngle = getAngle(rightHip, rightKnee, rightAnkle)
        }
        var leftHipAngle : Double = 0.0
        var rightHipAngle : Double = 0.0
        if(leftKnee != null && leftHip !=null && leftShoulder != null) {
            leftHipAngle = getAngle(leftKnee, leftHip, leftShoulder)
        }
        if(rightKnee != null && rightHip !=null && rightShoulder != null) {
            rightHipAngle = getAngle(rightKnee, rightHip, rightShoulder)
        }

        if (leftKneeAngle == 0.0 || rightKneeAngle == 0.0 || leftHipAngle == 0.0 || rightHipAngle == 0.0) {
            return ""
        }

        if((leftKneeAngle < 90.0 && rightKneeAngle <90.0) && (leftHipAngle < 90.0 && rightHipAngle <90)){
            return  "Crouching"
        }
        if ((leftKneeAngle < 145.0 && rightKneeAngle < 145.0) && (leftHipAngle > 60.0 && rightHipAngle > 60.0)) {
            return "sitting"
            }
        else {
            return "standing"
        }

    }

    fun getAngle(firstPoint: PoseLandmark, midPoint: PoseLandmark, lastPoint: PoseLandmark): Double {
        var result = Math.toDegrees(
            (atan2(
                lastPoint.position.y - midPoint.position.y,
                lastPoint.position.x - midPoint.position.x)
                    - atan2(firstPoint.position.y - midPoint.position.y,
                firstPoint.position.x - midPoint.position.x)).toDouble()
        )
        result = abs(result) // Angle should never be negative
        if (result > 180) {
            result = 360.0 - result // Always get the acute representation of the angle
        }
        return result
    }

    private fun drawLine(canvas: Canvas, point1: PoseLandmark?, point2: PoseLandmark?, paint: Paint) {
        if (point1 != null) {
            if (point2 != null) {
                canvas.drawLine(point1.position3D.x, point1.position3D.y, point2.position3D.x, point2.position3D.y, paint)
            }
        }
    }

    /**
     * A general-purpose data class to store detection result for visualization
     */
    data class BoxWithText(val box: Rect, val text: String)

   /**
    @OptIn(UnstableApi::class)
    class StreamCallBack(activity: MainActivity) : RawVideoSink.Callback {

        private var width: Int = 0
        private var height: Int = 0
        private var mainActivity = activity

        override fun onFrame(sink: RawVideoSink, frame: RawVideoSink.Frame) {
            super.onFrame(sink, frame)

            try {
                val rel = frame.released
                val planes = frame.planes
                val myToast =
                    Toast.makeText(mainActivity, planes.toString(), Toast.LENGTH_SHORT)
                myToast.show()
            } catch (e: IllegalStateException) {
                val myToast =
                    Toast.makeText(mainActivity, e.toString(), Toast.LENGTH_SHORT)
                myToast.show()
            }


            //frame.release()
            //val myToast2 =
            //   Toast.makeText(mainActivity, frame.released.toString(), Toast.LENGTH_SHORT)
            //myToast2.show()

        }

        override fun onStart(sink: RawVideoSink, videoFormat: VideoFormat) {
            super.onStart(sink, videoFormat)
            width = videoFormat.resolution.width.toInt()
            height = videoFormat.resolution.height.toInt()
            val myToast =
                Toast.makeText(mainActivity, "$width $height", Toast.LENGTH_SHORT)
            myToast.show()
        }

        override fun onStop(sink: RawVideoSink) {
            super.onStop(sink)
        }

        private fun getBitmap(buffer: Buffer, width: Int, height: Int): Bitmap? {
            buffer.rewind()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            return bitmap
        }

    }
    */
}