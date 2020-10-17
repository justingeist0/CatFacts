package com.fantasmaplasma.catfacts

import android.app.Service
import android.content.*
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*


class FactService : Service() {
    private lateinit var mOffReceiver: BroadcastReceiver
    private lateinit var mOnReceiver: BroadcastReceiver

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        repository.initFacts(resources)
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE)
                    as ClipboardManager
        launchCopyPaste()
        return START_STICKY
    }

    private fun registerReceivers() {
        if(registered) return
        Log.d(TAG, "Registered Receivers")
        with(applicationContext) {
            mOffReceiver = CancelCopyPasteReceiver()
            registerReceiver(
                mOffReceiver,
                IntentFilter(
                    Intent.ACTION_SCREEN_OFF
                )
            )
            registerReceiver(
                mOffReceiver,
                IntentFilter(
                    ACTION_CANCEL
                )
            )
            mOnReceiver = ResumeCopyPasteReceiver()
            registerReceiver(
                mOnReceiver,
                IntentFilter(
                    Intent.ACTION_SCREEN_ON
                )
            )
            registerReceiver(
                mOnReceiver,
                IntentFilter(
                    ACTION_RESUME
                )
            )
        }
        registered = true
    }

    private fun launchCopyPaste() {
        if(copyPasteJob?.isActive == true) return
        copyPasteJob = GlobalScope.launch {
            while (true) {
                ensureActive()
                registerReceivers()
                moveTextToClipBoard(
                    repository.getNextFact()
                )
                Log.d(TAG, "hello fact in clipboard")
                delay(1000)
            }
        }
    }

    override fun onDestroy() {
        unregisterReceivers()
        super.onDestroy()
    }

    private fun unregisterReceivers() {
        with(applicationContext) {
            unregisterReceiver(mOnReceiver)
            unregisterReceiver(mOffReceiver)
        }
        registered = false
    }

    private fun moveTextToClipBoard(text: String) {
        val clip = ClipData.newPlainText(TAG, text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    inner class CancelCopyPasteReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent?.action == Intent.ACTION_SCREEN_OFF)
                    enableOnceOn =
                        copyPasteJob?.isActive == false
            copyPasteJob?.cancel()
        }
    }

    inner class ResumeCopyPasteReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent?.action != Intent.ACTION_SCREEN_ON && enableOnceOn)
                launchCopyPaste()
        }
    }

    companion object {
        const val TAG = "COPY_SERVICE"

        const val ACTION_CANCEL = "${BuildConfig.APPLICATION_ID}.ACTION_CANCEL"
        const val ACTION_RESUME = "${BuildConfig.APPLICATION_ID}.ACTION_RESUME"

        private lateinit var clipboard: ClipboardManager
        private val repository = Repository()
        private var copyPasteJob: Job? = null
        private var registered = false
        private var enableOnceOn = true
    }
}

