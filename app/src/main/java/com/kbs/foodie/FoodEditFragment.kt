package com.kbs.foodie

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.util.*

class FoodEditFragment: Fragment(R.layout.food_edit_fragment) {

    private val db: FirebaseFirestore = Firebase.firestore
    private lateinit var foodEditViewModel:MyInfoViewModel
    private lateinit var foodContentCollectionRef: CollectionReference
    private lateinit var user:String

    var foodPhoto: Uri? = null
    var foodFileName : String? = null

    val storage = Firebase.storage
    val FoodStorageRef = storage.reference

    companion object {
        var PICK_PROFILE_FROM_ALBUM = 11
        const val UPLOAD_FOLDER = "contentImage/"
    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val main = activity as MainActivity
        main.hiddenMenu()
        val rootView = inflater.inflate(R.layout.food_edit_fragment, container, false) as ViewGroup
        user = main.user

        val foodPos = main.myInfoPos
        val editFoodNameText = rootView.findViewById<EditText>(R.id.nameEditText)
        val editFoodLocationText = rootView.findViewById<EditText>(R.id.locationEditText)
        val editFoodScoreEditText = rootView.findViewById<EditText>(R.id.scoreEditText)
        val editFoodReviewEditText = rootView.findViewById<EditText>(R.id.reviewEditText)
        val editFoodImage = rootView.findViewById<ImageView>(R.id.editFoodImage)
        val editFoodUpdateButton = rootView.findViewById<Button>(R.id.saveAndBackButton)
        val editFoodDeleteButton = rootView.findViewById<Button>(R.id.editFoodDeleteButton)

        foodEditViewModel = ViewModelProvider(requireActivity())[MyInfoViewModel::class.java]
        foodContentCollectionRef = db.collection("user").document(user)
            .collection("content")
        println(foodEditViewModel)

        //기존 foodContent SHOW
        if (main.FoodImageTrueFalse) {
            foodContentCollectionRef.get().addOnCompleteListener { task ->
                val getPositionFood = foodEditViewModel.getContent(foodPos)
                if (task.isSuccessful) {
                    val profileImageRef = FoodStorageRef.child("/${getPositionFood?.image}")
                    loadImage(profileImageRef, editFoodImage)
                }
            }
        }
        main.FoodImageTrueFalse = false

        foodContentCollectionRef.get().addOnCompleteListener { task ->
            val getPositionFood = foodEditViewModel.getContent(foodPos)
            if (task.isSuccessful) {
                editFoodNameText.text =
                    SpannableStringBuilder(getPositionFood?.name)
                editFoodLocationText.text =
                    SpannableStringBuilder(getPositionFood?.address)
                editFoodScoreEditText.text =
                    SpannableStringBuilder(getPositionFood?.score.toString())
                editFoodReviewEditText.text =
                    SpannableStringBuilder(getPositionFood?.review)
            }
        }
        editFoodImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = MediaStore.Images.Media.CONTENT_TYPE
            main.startActivityForResult(intent, FoodEditFragment.PICK_PROFILE_FROM_ALBUM)

            //이미지 띄우기
        }
        setFragmentResultListener("requestKey2") { requestKey, bundle ->
            //결과 값을 받는곳입니다.
            val userPPT = bundle.getString("bundleKey")
            foodPhoto = Uri.parse(userPPT)
            editFoodImage.setImageURI(foodPhoto)
        }

        //food Content Update
        editFoodUpdateButton.setOnClickListener {
            uploadFile()
            val mScore = editFoodScoreEditText.text.toString()
            val mReview = editFoodReviewEditText.text.toString()
            val mImage = "${UPLOAD_FOLDER}${foodFileName}"
            val map = hashMapOf<String, Any>()
            map["score"] = mScore
            map["review"] = mReview
            map["image"] = mImage
            foodContentCollectionRef.get().addOnCompleteListener { task ->
                val getPositionFood = foodEditViewModel.getContent(foodPos)
                val getPositionId = SpannableStringBuilder(getPositionFood?.id)
                if (task.isSuccessful) {
                    foodContentCollectionRef.document(getPositionId.toString()).update(map)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(
                                    requireContext(),
                                    "UPDATE FOOD CONTENT COMPLETE!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val foodShowFragment = FoodShowFragment()
                               // main.onChangeFragment(foodShowFragment)
                            }
                        }

                    }
            }
        }
        //food Content DELETE
        editFoodDeleteButton.setOnClickListener {

            val builder= AlertDialog.Builder(requireActivity())
            builder.setTitle("삭제하시겠습니까?")
                .setMessage("삭제하면 끝!")
                .setPositiveButton("확인",
                    DialogInterface.OnClickListener { dialog, id ->
                        foodContentCollectionRef.get().addOnCompleteListener { task ->
                            val getPositionFood = foodEditViewModel.getContent(foodPos)
                            val getPositionId = SpannableStringBuilder(getPositionFood?.id)
                            if (task.isSuccessful) {
                                foodContentCollectionRef.document(getPositionId.toString()).delete().addOnSuccessListener {

                                }
                                FoodStorageRef.child(getPositionFood?.image.toString()).delete()
                                    .addOnSuccessListener {

                                        val myInfoFragment = MyInfoFragment()
                                      //  main.onChangeFragment(myInfoFragment)
                                         }
                            }

                        //navigation 이동
                            //findNavController().navigate(R.id.action_foodEditFragment_to_foodShowFragment)
                        }.addOnFailureListener {}

                    })
                .setNegativeButton("취소",
                    DialogInterface.OnClickListener { dialog, id ->
                        Toast.makeText(requireContext(),"CANCEL", Toast.LENGTH_SHORT).show()

                    })
            // 다이얼로그를 띄워주기
            builder.show()

        }
        return rootView
    }
    private fun loadImage(imageRef: StorageReference, view: ImageView) {
        imageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener {
            val bmp = BitmapFactory.decodeByteArray(it, 0, it.size)
            view.setImageBitmap(bmp)
        }.addOnFailureListener {
            view.setImageResource(R.drawable.img)
        }
    }
    private fun uploadFile() {
        val timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        } else {
            Log.d(ContentValues.TAG, "version's low")
        }

        if(foodPhoto!=null) {
            foodFileName = "Content_$timestamp.png"
            val imageRef = storage.reference.child("${UPLOAD_FOLDER}${foodFileName}")
            imageRef.putFile(foodPhoto!!).addOnCompleteListener {
                println(foodFileName.toString())
            }
        }
        else{
            foodFileName="userDefaultImage.png"
        }

    }

}