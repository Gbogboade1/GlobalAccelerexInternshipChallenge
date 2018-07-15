package com.example.dell.globalaccelerexinternshipchallenge

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.Loader
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<List<Person>> {

    companion object {
        val TAG = "MainActivity"
    }

    var personAdapter: PersonAdapter? = null
    var personList: List<Person>? = mutableListOf()
    val LOADER_ID = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        personAdapter = PersonAdapter(applicationContext, arrayListOf())
        main_listview.adapter = personAdapter
        main_listview.emptyView = LayoutInflater.from(applicationContext).inflate(R.layout.empty_view, null)
        main_listview.setOnItemClickListener { adapterView, view, i, l -> startEditorActivity(i) }
        supportLoaderManager.initLoader(LOADER_ID, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<Person>> {
        Log.v(TAG, "Create Loader")
        return PersonAsyncTaskLoader(this, main_loading_progressbar,no_network_imageview)
    }

    override fun onLoadFinished(loader: Loader<List<Person>>, data: List<Person>?) {
        Log.v(TAG, "Loader finished")
        personList = data
//        personAdapter?.clear()
        personAdapter?.addAll(data)
        hideSpinner()
    }

    override fun onLoaderReset(loader: Loader<List<Person>>) {
        Log.v(TAG, "Loader reset")
    }

    fun startEditorActivity(i: Int) {
        val intent = Intent(baseContext, EditorActivity::class.java)
        val person = personList?.get(i)
        intent.putExtra("id", person?.id)
        intent.putExtra("name", person?.name)
        intent.putExtra("age", person?.age)
        intent.putExtra("photo_thumb_url", person?.photo_thumb_url)
        intent.putExtra("photo_url", person?.photo_url)
        intent.putExtra("description", person?.description)
        startActivity(intent)
    }

    private fun hideSpinner() {
        main_loading_progressbar.visibility = View.GONE
    }

    private class PersonAsyncTaskLoader(context: Context, val progressBar: ProgressBar,val networkImageView: ImageView) : AsyncTaskLoader<List<Person>>(context) {
        companion object {
            val TAG = "PersonAsyncTaskLoader"
        }

        override fun onStartLoading() {
            super.onStartLoading()
            Log.v(TAG, "Started -> $TAG")
            showSpinner(true)
            forceLoad()
        }

        override fun loadInBackground(): List<Person> {
            val list = mutableListOf<Person>()
            if (isNetworkAvailable()) {
                val queryURL = "https://intern-challenge.herokuapp.com/persons"
                val url = URL(queryURL)
                val httpClient = url.openConnection() as HttpURLConnection
                if (httpClient.responseCode == HttpURLConnection.HTTP_OK) {
                    Log.v(TAG, "connected to $url")
                    try {
                        val stream = BufferedInputStream(httpClient.inputStream)
                        val data = readStream(stream)
                        val json = JSONObject(data)
                        val personsArray = json.getJSONArray("persons")
                        for (i in 0..personsArray.length() - 1) {
                            val p = personsArray.getJSONObject(i)
                            val id = p.getString("id")
                            val name = p.getString("name")
                            val age = p.getInt("age")
                            val photoUrl = p.getString("name")
                            val photoThumbUrl = p.getString("photo_thumb")
                            val description = p.getString("description")

                            list.add(Person(id, name, age, photoUrl, photoThumbUrl, description))
                        }
                        Log.e(TAG, "ERROR: ${list.size}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e(TAG, "ERROR: $e")
                    } finally {
                        Log.v(TAG, "done")
                        httpClient.disconnect()
                    }
                } else {
                    Log.e(TAG, "Unable to connect to $url")
                }

                networkImageView.visibility = View.GONE
            }else{
                networkImageView.visibility = View.VISIBLE
            }
            return list
        }


        /**
         * helper function to read input steam
         */
        private fun readStream(stream: BufferedInputStream): String {
            val bufferedReader = BufferedReader(InputStreamReader(stream))
            val stringBuilder = StringBuilder()
            bufferedReader.forEachLine { stringBuilder.append(it) }
            return stringBuilder.toString()
        }

        private fun showSpinner(done: Boolean) {
            if (!done) {
                progressBar.visibility = View.GONE
            } else {
                progressBar.visibility = View.VISIBLE
            }
        }

        fun isNetworkAvailable(): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            return if (connectivityManager is ConnectivityManager) {
                val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
                networkInfo?.isConnected ?: false
            } else false
        }
    }

}

data class Person(val id: String, val name: String, val age: Int,
                  val photo_url: String, val photo_thumb_url: String, val description: String)

class PersonAdapter(context: Context, var list: List<Person>?) : ArrayAdapter<Person>(context, 0, list) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        Log.v("ADAPTER", "View $position")
        var view: View? = null
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        } else {
            view = convertView
        }
        val person: Person = getItem(position)
        val nameTextView = view?.findViewById(R.id.name_textview) as TextView
        val ageTextView = view?.findViewById(R.id.age_textview) as TextView
        val photoThumbImageView = view?.findViewById(R.id.thumbnail_imageview) as ImageView
        val progressBar = view?.findViewById(R.id.thumbnail_loading_progressbar) as ProgressBar
        val name = person.name
        val age = person.age

        val photoThumbUrl = person.photo_thumb_url
        BitMapAsyncTack(photoThumbUrl, photoThumbImageView, progressBar).execute()
        nameTextView.text = name
        ageTextView.text = age.toString()

        return view
    }

    class BitMapAsyncTack(val urlString: String, val imageView: ImageView, val progressBar: ProgressBar) : AsyncTask<Unit, Unit, Bitmap>() {
        override fun onPreExecute() {
            super.onPreExecute()
            showSpinner(true)
        }

        override fun doInBackground(vararg p0: Unit?): Bitmap {
            val photoThumbUrl = URL(urlString)
            return BitmapFactory.decodeStream(photoThumbUrl.openConnection().getInputStream())
        }

        override fun onPostExecute(result: Bitmap?) {
            super.onPostExecute(result)
            imageView.setImageBitmap(result)
            showSpinner(false)
        }


        private fun showSpinner(done: Boolean) {
            if (!done) {
                progressBar.visibility = View.INVISIBLE
            } else {
                progressBar.visibility = View.VISIBLE
            }
        }
    }

}
