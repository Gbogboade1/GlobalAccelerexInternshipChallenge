package com.example.dell.globalaccelerexinternshipchallenge

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import kotlinx.android.synthetic.main.activity_editor.*
import java.net.URL

class EditorActivity : AppCompatActivity() {

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item?.itemId){
            android.R.id.home -> finish()
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val data = intent.extras
        val person = Person(data.get("id") as String,
                data.get("name").toString(),
                data.get("age") as Int,
                data.get("photo_url") as String,
                data.get("photo_thumb_url") as String,
                data.get("description") as String)
        ImageLoadAsyncTask(person.photo_url,person_imageview,image_loading_progressbar)
        name_editext.setText(person.name)
        age_editext.setText(person.age.toString())
        description_editext.setText(person.description)
    }

    class ImageLoadAsyncTask(val photoUrl:String,val imageView: ImageView,val progressBar: ProgressBar): AsyncTask<Unit,Unit,Bitmap>(){
        override fun onPreExecute() {
            super.onPreExecute()
            showSpinner(true)
        }

        override fun doInBackground(vararg p0: Unit?): Bitmap {
            val imageUrl = URL(photoUrl)
            return BitmapFactory.decodeStream(imageUrl.openConnection().getInputStream())
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            imageView.setImageBitmap(result)
            showSpinner(false)
        }

        private fun showSpinner(done: Boolean){
            if (!done) {
                progressBar.visibility = View.INVISIBLE
            }else{
                progressBar.visibility = View.VISIBLE
            }
        }
    }
}
