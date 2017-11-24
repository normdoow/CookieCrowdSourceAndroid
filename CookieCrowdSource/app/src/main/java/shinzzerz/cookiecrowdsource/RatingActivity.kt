package shinzzerz.cookiecrowdsource

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.RadioButton
import kotlinx.android.synthetic.main.activity_rating.view.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import shinzzerz.restapi.CookieAPI
import java.util.concurrent.TimeUnit

class RatingActivity : AppCompatActivity() {

    lateinit var view:View

    lateinit var cookieAPI:CookieAPI
    var time = 40

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = layoutInflater.inflate(R.layout.activity_rating, null) //as LinearLayout
        setContentView(view)

        val retrofit = Retrofit.Builder()
                .baseUrl(CookieAPI.BASE_URL)
                .build()

        cookieAPI = retrofit.create(CookieAPI::class.java)

        Observable.interval(60, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if(time >= 0) {
                        val text = time.toString() + " min."
                        view.time_text.text = text
                        time -= 1
                    }
                }


        view.submit_button.setOnClickListener {
            if(view.comments_text.text.toString().equals("")) {
                showNotification("No Comment", "Please add a comment to your rating. We really appreciate your feedback!")
            } else if(view.hot_group.checkedRadioButtonId == -1 || view.recommend_group.checkedRadioButtonId == -1) {
                showNotification("Not all questions answered", "Please answer the questions. We really appreciate your feedback!")
            } else {
                var radioButtonID = view.hot_group.checkedRadioButtonId
                var radioButton = view.hot_group.findViewById(radioButtonID) as RadioButton
                val isWarm = radioButton.text.toString()
                radioButtonID = view.recommend_group.checkedRadioButtonId
                val secRadioButton = view.recommend_group.findViewById(radioButtonID) as RadioButton
                val isRecommend = secRadioButton.text.toString()

                val callNewBaker = cookieAPI.sendRating(Math.round(view.rating.rating).toString(), view.comments_text.text.toString(), isWarm, isRecommend)
                callNewBaker.enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        finish()

                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        showNotification("Error", "Something bad happened with the connection. Try checking your internet connection.")
                    }
                })
            }
        }

        view.skip_button.setOnClickListener {
            finish()
        }
    }

    private fun showNotification(title:String, message:String) {

        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK"
        ) { dialog, which ->
            dialog.dismiss()
        }
        alertDialog.show()
    }
}
