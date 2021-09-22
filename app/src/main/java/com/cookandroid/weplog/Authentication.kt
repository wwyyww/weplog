package com.cookandroid.weplog

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Authentication : AppCompatActivity() {

    lateinit var auth_layoutStep1: ConstraintLayout
    lateinit var auth_layoutStep2: ConstraintLayout

    lateinit var auth_layoutUpload: ConstraintLayout
    lateinit var auth_btnskip: Button
    lateinit var auth_step2Preview : ImageView
    lateinit var auth_step1TPlace : TextView
    lateinit var auth_step1txt : TextView

    lateinit var currentPhotoPath: String
    val REQUEST_TAKE_PHOTO = 1

    lateinit var trashPlace : String
    lateinit var fileName : String
    lateinit var photoURI : Uri
    var authStep1 = false
    var authStep2 = false

    var userNickname : String = ""
    //var userName : String ?= ""
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.authentication)

        auth_layoutStep1 = findViewById(R.id.auth_layoutStep1)
        auth_layoutStep2 = findViewById(R.id.auth_layoutStep2)
        auth_layoutUpload = findViewById(R.id.auth_layoutUpload)
        auth_btnskip = findViewById(R.id.auth_btnskip)
        auth_step1TPlace = findViewById(R.id.auth_step1TPlace)
        auth_step1txt = findViewById(R.id.auth_step1txt)

        auth_layoutUpload.visibility = View.INVISIBLE

        if(intent.hasExtra("data")){
            Toast.makeText(this, "내용 바꿈." + intent.getStringExtra("data"), Toast.LENGTH_SHORT).show()
            auth_step1txt.visibility = View.INVISIBLE
            auth_step1TPlace.visibility = View.VISIBLE

            trashPlace = intent.getStringExtra("data").toString()
            auth_step1TPlace.text = trashPlace
            authStep1 = true
            auth_layoutStep1.isClickable = false
        }

        // 인증 버튼 visible로 바꾸기
        checkAuth()


        // 1단계. 인증하기 버튼
        auth_layoutStep1.setOnClickListener {
            val intent = Intent(this, QRcodeScanner::class.java)
            intent.putExtra("page", "Authentication")
            startActivity(intent)
        }


        // 2단계. 사진찍기 버튼
        auth_layoutStep2.setOnClickListener {

            val cameraPermissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            if (cameraPermissionCheck != PackageManager.PERMISSION_GRANTED) { // 권한이 없는 경우
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 1000)
            } else { //권한이 있는 경우
                val REQUEST_IMAGE_CAPTURE = 1
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    takePictureIntent.resolveActivity(packageManager)?.also {
                        //startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)

                        dispatchTakePictureIntent()
                    }
                }
            }
        }


        // 최종 인증
        auth_layoutUpload.setOnClickListener {
            uploadImage(photoURI)
            uploadPost()
        }

        // skip 버튼
        auth_btnskip.setOnClickListener {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
        }

    }

    private fun uploadPost() {

        val user = Firebase.auth.currentUser
        database = Firebase.database.reference

        if (user != null) {
            val key = database.child("users").child(user.uid).push().key
            var post = Post()

            post.postId = key
            post.writerId = user?.uid


            val userRef = Firebase.database.getReference("users")

            lateinit var nick : String
            userRef.addValueEventListener(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    nick = snapshot.child(user?.uid).child("nickname").value.toString()
                    Log.e("snap",nick)
                    //post.writerNick = nick
                    userNickname = nick
                    Log.e("snap2",userNickname)
                }

                override fun onCancelled(error: DatabaseError) {
                    userNickname="ERROR"
                }
            })
            Log.e("snap3",userNickname)
            //post.writerNick = nick
            post.writerNick = userNickname
            post.timestamp = System.currentTimeMillis()
            post.photoUrl = fileName // storage에 업로드 한 사진 이름

            var postValues = post.toMap()

            if (key != null) {
                //database.child("users").child(user.uid).child("post").child(key).setValue(postValues) // 기존에는 post 객체 모두 저장
                database.child("users").child(user.uid).child("posts").child(key)  // 수정 후 : post객체 id만 저장
                database.child("community").child(key).setValue(postValues)
            }
        }


        Toast.makeText(this,"업로드 완료!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, NavigationActivity::class.java)
        startActivity(intent)


//        database = Firebase.database.reference
//
//        if (user != null) {
//            val key = database.child("users").child(user.uid).push().key
//            var post = Post()
//
//            post.postId = key
//            post.writerId = user?.uid
//            post.writerNick = userName
//            post.timestamp = System.currentTimeMillis()
//            post.photoUrl = photoURI.toString() // storage에 업로드 한 url
//
//            var postValues = post.toMap()
//
//            if (key != null) {
//                database.child("users").child(user.uid).child("post").child(key).setValue(postValues)
//                database.child("community").child(key).setValue(postValues)
//            }
//        }


    }


    // 이미지 저장은 storage 에 저장
    private fun uploadImage(uri : Uri) {

        var storage : FirebaseStorage ?= FirebaseStorage.getInstance()
        var uid = FirebaseAuth.getInstance().currentUser?.uid

        // 파일 이름 설정
        fileName = "IMAGE_${uid}_${SimpleDateFormat("yyyymmdd_HHmmss").format(Date())}_.png"

        // 클라우드 파일을 가리키는 포인터
        var imageRef = storage!!.reference.child("community").child(fileName)

        // 이미지 파일 업로드
        imageRef.putFile(uri!!).addOnSuccessListener {
            Toast.makeText(this, " 업로드 성공" + imageRef.toString() , Toast.LENGTH_SHORT).show()
        }.addOnFailureListener{
            Toast.makeText(this, " 업로드 실패" , Toast.LENGTH_SHORT).show()
        }
    }


    private fun checkAuth() {
        //Toast.makeText(this, authStep1.toString() + " " + authStep2.toString(), Toast.LENGTH_SHORT).show()
        if (authStep1 && authStep2){
            auth_layoutUpload.visibility = View.VISIBLE
        }

    }


    // 권한 없을 때
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1000) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // 사진 preview
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

//        auth_step2Preview = findViewById(R.id.auth_step2Preview)
//        auth_step2Preview.visibility = View.VISIBLE
//
//        val REQUEST_IMAGE_CAPTURE = 1
//
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
//            //Toast.makeText(this, "사진 Preview 함수 작동", Toast.LENGTH_SHORT).show()
//            val imageBitmap = data?.extras?.get("data") as Bitmap
//            auth_step2Preview.setImageBitmap(imageBitmap)
//            authStep2 = true
//            checkAuth()
//        }
        val REQUEST_IMAGE_CAPTURE = 1

        auth_step2Preview = findViewById(R.id.auth_step2Preview)
        auth_step2Preview.visibility = View.VISIBLE

        when (requestCode){
            1 -> {
                if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK){

                    // 카메라로부터 받은 데이터가 있을경우에만
                    val file = File(currentPhotoPath)
                    if (Build.VERSION.SDK_INT < 28) {
                        val bitmap = MediaStore.Images.Media
                                .getBitmap(contentResolver, Uri.fromFile(file))  //Deprecated
                        auth_step2Preview.setImageBitmap(bitmap)
                    }
                    else{
                        val decode = ImageDecoder.createSource(this.contentResolver,
                                Uri.fromFile(file))
                        val bitmap = ImageDecoder.decodeBitmap(decode)
                        auth_step2Preview.setImageBitmap(bitmap)
                    }

                    authStep2 = true
                    auth_layoutStep2.isClickable = false
                    checkAuth()
                }
            }
        }

    }



    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    photoURI = FileProvider.getUriForFile(
                            this,"com.cookandroid.weplog.fileprovider", it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File ?= getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }

    }



}