package machikawa.hidemasa.techacademy.autoslideshowapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.content.ContentUris
import android.net.Uri
import android.os.Handler
import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), View.OnClickListener {

    // カメラへのアクセス許可のリクエストコード
    private val REQ_CODE = 47

    // 画像変更のTimer spam [単位：ミリ秒]
    private val timeSpan:Long = 2000  // 2秒

    // タイマーとハンドラーの生成
    private var mTimer: Timer? = null
    private var mHandler  = Handler()

    // 画像生成周りの変数
    private val imageUriArray:ArrayList<Uri> = arrayListOf()
    private var imageIndex:Int = 0
    private var isPlay:Boolean = false

    // ストレージへのアクセス可否
    private var isGranted:Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ストレージへのアクセス権の確認
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                getContentsInfo()
            } else {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQ_CODE)
            }
        } else {
            getContentsInfo()
        }
        // ボタンへのListenerの登録
        forwardBtn.setOnClickListener(this)
        backBtn.setOnClickListener(this)
        playBtn.setOnClickListener(this)
    }

    // パーミッション許可/拒否時の処理
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQ_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    getContentsInfo()
                } else {
                    isGranted = grantResults[0]
                }
        }
    }

    // コンテンツの取得
    private fun getContentsInfo(){
        val resolver = contentResolver
        this.imageIndex = 0

        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,null,null,null
        )

        if (cursor.moveToFirst()){
            do {
                val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val id = cursor.getLong(fieldIndex)
                val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                imageUriArray.add(imageUri)
            } while(cursor.moveToNext())
        }
        cursor.close()
        // 初期の画像設定。画像の配列があるときだけイメージを生成
        if (imageUriArray.size > 0 ) {
            imageView.setImageURI(imageUriArray[this.imageIndex])
        }
    }

    // 各ボタンクリック時の処理わけ
    override fun onClick(v: View) {

        // 画像イメージ、アクセス権がなければスナックバーで注意する.　
        if (isGranted == PackageManager.PERMISSION_DENIED) {
            Snackbar.make(v, "ストレージへのアクセスが拒否されています", Snackbar.LENGTH_LONG).show()
        }else if (imageUriArray.size == 0) {
            Snackbar.make(v, "画像がありません", Snackbar.LENGTH_LONG).show()
        } else {
            if (v.id == R.id.backBtn) {
                showPreviousImage()
            } else if (v.id == R.id.playBtn) {
                doPlay()
            } else if (v.id == R.id.forwardBtn) {
                showNextImage()
            }
        }
    }

    // 前へ⏪ボタン押下時の処理
    private fun showPreviousImage(){
        if (this.imageIndex == 0 ) {
            this.imageIndex = imageUriArray.size - 1
        } else {
            this.imageIndex -= 1
        }
        imageView.setImageURI(imageUriArray[this.imageIndex])
    }

    // 次へ⏩ボタン押下時の処理
    private fun showNextImage(){
        if (this.imageIndex == imageUriArray.size -1 ) {
            this.imageIndex = 0
        } else {
            this.imageIndex += 1
        }
        imageView.setImageURI(imageUriArray[this.imageIndex])
    }

    // 再生▶️ボタン押下時の処理
    private fun doPlay(){
        // すでに再生ボタンが押下されている場合の処理
        if (this.isPlay) {
            //タイマーの停止
            stopChangingImagesByTimer()
            // 両サイドボタンの再活性化
            forwardBtn.isEnabled = true
            backBtn.isEnabled = true
            // ボタン画像の変更 (再生へ)
            playBtn.setText("再生")
            // ステータスの反転
            this.isPlay = false
            // 再生がまだされていない場合の処理
        } else {
            //タイマーの開始
            changeImagesByTimer()
            // 両サイドボタンのdis-activate
            forwardBtn.isEnabled = false
            backBtn.isEnabled = false
            // ボタン画像の変更 (停止へ)
            playBtn.setText("停止")
            // ステータスの反転
            this.isPlay = true
        }
    }

    // 再生ボタンによるタイマーでの画像先送り
    private fun changeImagesByTimer(){
        if (mTimer == null) {
            mTimer = Timer()
            mTimer!!.schedule(
                object : TimerTask() {
                    override fun run() {
                        mHandler.post {
                            showNextImage()
                        }
                    }
                },
                100,
                timeSpan    /// 自動切り替えのタイムスパン
            )
        }
    }

    // 画像コマ送りの終了
    private fun stopChangingImagesByTimer() {
        if (mTimer != null){
            mTimer!!.cancel()
            mTimer =null
        }
    }
}