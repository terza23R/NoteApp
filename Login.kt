package com.mahir.mahirapp

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.mahir.mahirapp.databinding.ActivityLoginBinding
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date


class Login : AppCompatActivity() {

    private var mAuth:FirebaseAuth?=null
    private var database=FirebaseDatabase.getInstance()
    private var myRef=database.reference
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mAuth=FirebaseAuth.getInstance()

        binding.ivImageView.setOnClickListener(View.OnClickListener {
            checkPermission()
        })


    }
    fun LoginToFireBase(email:String,password:String){

        mAuth!!.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener(this){ task ->

                if (task.isSuccessful){
                    Toast.makeText(applicationContext,"Successful login",Toast.LENGTH_LONG).show()

                    SaveImageInFirebase()

                }else
                {
                    Toast.makeText(applicationContext,"fail login",Toast.LENGTH_LONG).show()
                }

            }

    }

    fun SaveImageInFirebase(){
        var currentUser =mAuth!!.currentUser
        val email:String=currentUser!!.email.toString()
        val storage= FirebaseStorage.getInstance()
        val storgaRef=storage.getReferenceFromUrl("gs://mojaapp-f2fe6.appspot.com")
        val df=SimpleDateFormat("ddMMyyHHmmss")
        val dataobj=Date()
        val imagePath= SplitString(email) + "."+ df.format(dataobj)+ ".jpg"
        val ImageRef=storgaRef.child("images/"+imagePath )
        binding.ivImageView.isDrawingCacheEnabled=true
        binding.ivImageView.buildDrawingCache()

        val drawable=binding.ivImageView.drawable as BitmapDrawable
        val bitmap=drawable.bitmap
        val baos= ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data= baos.toByteArray()
        val uploadTask=ImageRef.putBytes(data)
        uploadTask.addOnFailureListener{
            Toast.makeText(applicationContext,"fail to upload",Toast.LENGTH_LONG).show()
        }.addOnSuccessListener { taskSnapshot ->

            var DownloadURL= taskSnapshot.storage.downloadUrl.result.toString()

            myRef.child("Users").child(currentUser.uid).child("email").setValue(currentUser.email)
            myRef.child("Users").child(currentUser.uid).child("ProfileImage").setValue(DownloadURL)
            LoadMain()
        }
        LoadMain()
    }

    fun SplitString(email:String):String{
        val split= email.split("@")
        return split[0]
    }

    override fun onStart() {
       super.onStart()
           // LoadMain()
    }

    fun LoadMain() {
        val currentUser = mAuth!!.currentUser

        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("email", currentUser.email)
            intent.putExtra("uid", currentUser.uid)


            startActivity(intent)

            finish()
        }
    }

    val READIMAGE:Int=253
    fun checkPermission(){
       if (Build.VERSION.SDK_INT>=23){
           if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)!=
               PackageManager.PERMISSION_GRANTED) {

               requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),READIMAGE)
               return
           }
       }
        loadImage()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            READIMAGE->{
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    loadImage()
                }
                else{
                    Toast.makeText(applicationContext,"Ne mogu pristupiti slikama",Toast.LENGTH_LONG).show()
                }
            }
            else->super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
    val PICK_IMAGE_CODE=111
    fun loadImage() {
        val intent=Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        this.startActivityForResult(intent,PICK_IMAGE_CODE)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode==PICK_IMAGE_CODE && data!=null && resultCode == RESULT_OK){
            val selectedImage = data.data
            val filePathColum=arrayOf(MediaStore.Images.Media.DATA)
            val cursor= contentResolver.query(selectedImage!!,filePathColum,null,null,null)
            cursor!!.moveToFirst()
            val columIndex= cursor!!.getColumnIndex(filePathColum[0])
            val picturePath=cursor!!.getString(columIndex)
            cursor!!.close()
            binding.ivImageView.setImageBitmap(BitmapFactory.decodeFile(picturePath))
        }
    }

    fun buLogin(view:View){
       LoginToFireBase(binding.etEmail.text.toString(),binding.etPassword.text.toString())
    }
}


