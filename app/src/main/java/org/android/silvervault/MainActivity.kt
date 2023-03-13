package org.android.silvervault

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.android.silvervault.lib.FileUtils


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fileUtils = FileUtils()

        val selectFilesActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == Activity.RESULT_OK) {

                    val data: Intent? = result.data

                    //If multiple files selected
                    if (data?.clipData != null) {
                        val count = data.clipData?.itemCount ?: 0

                        for (i in 0 until count) {
                            fileUtils.importFile(
                                applicationContext,
                                data.clipData?.getItemAt(i)?.uri!!
                            )
                        }
                    }

                    //If single file selected
                    else if (data?.data != null) {
                        fileUtils.importFile(applicationContext, data.data!!)
                    }
                }
            }


        val selectFiles = findViewById<Button>(R.id.selectFiles)
        selectFiles.setOnClickListener {

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "*/*"
            selectFilesActivityResult.launch(intent)

        }

        // about button
        val aboutBtn = findViewById<Button>(R.id.about)
        aboutBtn.setOnClickListener {
            // open new intent with MIT Licence and github link and library credits
        }
    }

    
}