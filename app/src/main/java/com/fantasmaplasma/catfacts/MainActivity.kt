package com.fantasmaplasma.catfacts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: ViewModel
    private lateinit var mNotificationReceiver: BroadcastReceiver
    private lateinit var mDeleteNotificationReceiver: BroadcastReceiver
    private lateinit var interstitialAd: InterstitialAd
    private var actions = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this)
                    .get(ViewModel::class.java)
        viewModel.checkIfInitNeeded(resources)
        setUpViews()
        setUpObservers()
        viewModel.setFact(
            getIdxOfLastSeenFact()
        )
        initNotification()
        initMonetization()
    }

    override fun onPause() {
        setLastFactIdx(
            viewModel.currentFactIdx
        )
        setActionCount(actions)
        super.onPause()
    }

    private fun setUpViews() {
        current_fact_tv
            .setOnClickListener {
                createShareIntent()
            }
        next_fact_btn
            .setOnClickListener {
                if(showAd()) return@setOnClickListener
                viewModel.nextFact()
            }
        enable_btn?.setOnClickListener {
            if(showAd()) return@setOnClickListener
            catFactsCopyPasteEnabled = !catFactsCopyPasteEnabled
            startOrStopService()
        }
        tutorial_button?.setOnClickListener {
            if(showAd()) return@setOnClickListener
            navigateToTutorialActivity()
        }
    }

    private fun initMonetization() {
        if(true)return

    }

    private fun showAd() : Boolean {
        actions++
        if(actions >= 10) {
            actions = 0
            interstitialAd.show()
            return true
        }
        return false
    }

    private fun createShareIntent() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_SUBJECT,
                current_fact_header_tv.text
            )
            putExtra(
                Intent.EXTRA_TEXT,
                current_fact_tv.text
            )
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_using)))
    }

    private fun setUpObservers() {
        with(viewModel) {
            currentFactLiveData.observe(this@MainActivity, Observer {
                changeFact(it)
            })
        }
    }

    private fun changeFact(newFact: String) {
        val delay = 1000L
        delay.setFactText(newFact)
        delay.setHeaderText(
            getString(
                R.string.current_fact_header,
                (viewModel.currentFactIdx+1).toString()
            )
        )
    }

    private fun Long.setFactText(newFact: String) {
        with(current_fact_tv) {
            animate().apply {
                duration = this@setFactText
                alpha(0f)
            }.withEndAction {
                text = newFact
                contentDescription = newFact
                animate().apply {
                    duration = this@setFactText
                    alpha(1f)
                }
            }
        }
    }

    private fun Long.setHeaderText(headerText: String) {
        with(current_fact_header_tv) {
            animate().apply {
                duration = this@setHeaderText
                translationX(
                    -header_container.width.toFloat()
                )
                scaleX(0.1f)
                scaleY(0.1f)
                alpha(0f)
            }.withEndAction {
                visibility = View.VISIBLE
                alpha = 1f
                text = headerText
                contentDescription = ""
                scroll_view.contentDescription =
                    headerText.replace("#", "")
                translationX =
                    header_container.width.toFloat()
                animate().apply {
                    duration = this@setHeaderText
                    translationX(0f)
                    scaleX(1f)
                    scaleY(1f)
                }

            }
        }
    }

    private fun startOrStopService() {
        if(catFactsCopyPasteEnabled) {
            startService(
                Intent(this, FactService::class.java)
            )
            createCatFactsNotification()
            showToast(
                getString(R.string.copy_paste_enabled)
            )
        } else {
            sendBroadcast(
                Intent(FactService.ACTION_CANCEL)
            )
            stopService(
                Intent(this, FactService::class.java)
            )
            cancelNotification()
            showToast(
                getString(R.string.copy_paste_disabled)
            )
        }
        setBtnText()
    }

    private fun initNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel (
                PRIMARY_CHANNEL_ID,
                "Cat Facts Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = "Important cat facts."
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationChannel.setSound(null, null)
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
        mNotificationReceiver = UpdateNotificationReceiver()
        registerReceiver(
            mNotificationReceiver,
            IntentFilter(ACTION_UPDATE_NOTIFICATION)
        )
        mDeleteNotificationReceiver = DeleteNotificationReceiver()
        registerReceiver(
            mDeleteNotificationReceiver,
            IntentFilter(ACTION_DELETE_NOTIFICATION)
        )
    }

    override fun onDestroy() {
        unregisterReceiver(mNotificationReceiver)
        unregisterReceiver(mDeleteNotificationReceiver)
        cancelNotification()
        stopService(
            Intent(this, FactService::class.java)
        )
        super.onDestroy()
    }

    private fun createCatFactsNotification() {
        val builder = getNotification()
        NotificationManagerCompat.from(this)
            .notify(NOTIFICATION_ID, builder.build())
    }

    private fun getNotification(): NotificationCompat.Builder {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingBodyClickIntent = PendingIntent.getActivity(this, NOTIFICATION_ID, activityIntent, PendingIntent.FLAG_ONE_SHOT)

        val deleteIntent = Intent(ACTION_DELETE_NOTIFICATION)
        val pendingDeleteIntent = PendingIntent.getBroadcast(this, NOTIFICATION_ID, deleteIntent, PendingIntent.FLAG_ONE_SHOT)

        val btnIntent = Intent(ACTION_UPDATE_NOTIFICATION)
        btnIntent.putExtra(EXTRA_ENABLED, !catFactsCopyPasteEnabled)
        val pendingBtnClickIntent = PendingIntent.getBroadcast(this, NOTIFICATION_ID, btnIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val view = RemoteViews(packageName,
            R.layout.cat_fact_notification
        )
        view.setTextViewText(R.id.notification_enable_btn, getNotificationBtnText())
        view.setOnClickPendingIntent(R.id.notification_enable_btn_layout, pendingBtnClickIntent)
        view.setImageViewResource(R.id.notification_circle_view,
                if (catFactsCopyPasteEnabled)
                    R.drawable.ic_circle_green
                else
                    R.drawable.ic_circle_red
            )

        return NotificationCompat.Builder(this, PRIMARY_CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSmallIcon(R.drawable.ic_notification)
            .setCustomContentView(view)
            .setContentIntent(pendingBodyClickIntent)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setAutoCancel(!catFactsCopyPasteEnabled)
            .setDeleteIntent(pendingDeleteIntent)
            .setNotificationSilent()
    }

    private fun cancelNotification() {
        with(NotificationManagerCompat.from(this)) {
            cancel(NOTIFICATION_ID)
        }
    }

    override fun onResume() {
        setBtnText()
        super.onResume()
    }

    private fun setBtnText() {
        enable_btn?.text =
            if (catFactsCopyPasteEnabled)
                getString(R.string.disable)
            else
                getString(R.string.enable)
    }

    private fun getNotificationBtnText() =
            if (catFactsCopyPasteEnabled)
                getString(R.string.enabled)
            else
                getString(R.string.disabled)

    private fun navigateToTutorialActivity() {
        if(enable_btn?.text == getString(R.string.enable)) {
            showToast(getString(R.string.step_zero, getString(R.string.enable)))
            return
        }
        val intent = Intent(this, TutorialActivity::class.java)
        startActivity(intent)
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun getIdxOfLastSeenFact(): Int {
        val prefs = getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_PREVIOUS_FACT, 0)
    }

    private fun setLastFactIdx(idx: Int) {
        val prefs = getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt(KEY_PREVIOUS_FACT, idx)
        editor.apply()
    }

    private fun getActionCount(): Int {
        val prefs = getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ACTION_COUNTER, 0)
    }

    private fun setActionCount(idx: Int) {
        val prefs = getSharedPreferences(KEY_PREFERENCES, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt(KEY_ACTION_COUNTER, idx)
        editor.apply()
    }

    inner class UpdateNotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val resume =
                intent?.getBooleanExtra(EXTRA_ENABLED, false)
            applicationContext.sendBroadcast(
                Intent(
                    if(resume == true)
                        FactService.ACTION_RESUME
                    else
                        FactService.ACTION_CANCEL
                )
            )
            catFactsCopyPasteEnabled = !catFactsCopyPasteEnabled
            createCatFactsNotification()
            setBtnText()
            Log.d(FactService.TAG, "Notification receiver received $resume")
        }
    }

    inner class DeleteNotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            applicationContext.sendBroadcast(
                Intent(
                    FactService.ACTION_CANCEL
                )
            )
            cancelNotification()
            catFactsCopyPasteEnabled = false
            setBtnText()
            Log.d(FactService.TAG, "Notification deleted")
        }
    }

    companion object {
        private const val KEY_PREFERENCES = "KEY_PREFERENCES"
        private const val KEY_PREVIOUS_FACT = "KEY_PREVIOUS_FACT"
        private const val KEY_ACTION_COUNTER = "KEY_ACTION_COUNTER"
        private const val PRIMARY_CHANNEL_ID = "primary_notification_channel"
        private const val NOTIFICATION_ID = 0
        private const val ACTION_UPDATE_NOTIFICATION =
            "${BuildConfig.APPLICATION_ID}.ACTION_UPDATE_NOTIFICATION"
        private const val EXTRA_ENABLED = "EXTRA_ENABLED"
        private const val ACTION_DELETE_NOTIFICATION =
            "${BuildConfig.APPLICATION_ID}.ACTION_DELETE_NOTIFICATION"
        private var catFactsCopyPasteEnabled = false
    }
}