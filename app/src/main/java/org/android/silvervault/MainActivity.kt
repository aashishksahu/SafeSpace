package org.android.silvervault

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import org.android.silvervault.crypto.Crypto

/*
    TODO:
        * Find encryption library
        * Find ways to encrypt data
 */

class MainActivity : AppCompatActivity() {

    private lateinit var logger: TextView
    private var files: ArrayList<Uri> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logger = findViewById(R.id.logger)

        val fileCount = findViewById<TextView>(R.id.fileCount)
        fileCount.text = "0"

        val selectFilesActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                fileCount.text = "0"

                if (result.resultCode == Activity.RESULT_OK) {
                    // empty the files queue
                    files.clear()

                    val data: Intent? = result.data

                    //If multiple files selected
                    if (data?.clipData != null) {
                        val count = data.clipData?.itemCount ?: 0
                        //set file count
                        fileCount.text = count.toString()
                        logInfoOnScreen(getString(R.string.files_selected))

                        for (i in 0 until count) {
                            files.add(data.clipData?.getItemAt(i)?.uri!!)
                        }
                    }

                    //If single file selected
                    else if (data?.data != null) {
                        fileCount.text = "1"
                        files.add(data.data!!)
                        logInfoOnScreen(getString(R.string.files_selected))
                    }
                }
            }


        val selectFilesEncrypt = findViewById<Button>(R.id.SelectFilesEncrypt)
        selectFilesEncrypt.setOnClickListener {

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.type = "*/*"
            selectFilesActivityResult.launch(intent)

        }

        // password field
        val passwordTextField = findViewById<TextInputLayout>(R.id.passwordInput)

        // Encrypt Button
        val encryptBtn = findViewById<Button>(R.id.startEncryption)
        encryptBtn.setOnClickListener {

            if (files.isEmpty()) {
                logInfoOnScreen(getString(R.string.files_count_error))

            } else if (validatePassword(passwordTextField)) {
                println("works")
                Crypto(applicationContext).encrypt(passwordTextField.editText?.text.toString(), files)
            }
        }

        // Decrypt Button
        val decryptBtn = findViewById<Button>(R.id.startDecryption)
        decryptBtn.setOnClickListener {

            if (files.isEmpty()) {
                logInfoOnScreen(getString(R.string.files_count_error))

            } else if (validatePassword(passwordTextField)) {
                Crypto(applicationContext).decrypt(passwordTextField.editText?.text.toString(), files)

            }
        }

        // about button
        val aboutBtn = findViewById<Button>(R.id.about)
        aboutBtn.setOnClickListener {
            // open new intent with MIT Licence and github link and library credits
        }
    }

    private fun validatePassword(passwordTextField: TextInputLayout): Boolean {
        val passwordRegex =
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}\$")

        passwordTextField.isErrorEnabled = false

        // password validation
        if (passwordTextField.editText?.text!!.isEmpty()) {
            passwordTextField.isErrorEnabled = true
            passwordTextField.error = getString(R.string.passwordInputError)
            return false

        } else if (!passwordRegex.containsMatchIn(passwordTextField.editText?.text!!)) {
            passwordTextField.isErrorEnabled = true
            passwordTextField.error = getString(R.string.passwordFormatError)
            return false
        }

        return true
    }

    private fun logInfoOnScreen(info: String) {

        logger.text = ""
        logger.append(info)
        logger.append("\n")

    }
}