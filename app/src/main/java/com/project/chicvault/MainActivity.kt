package com.project.chicvault

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
    }

//    private fun signOut() {
//        binding.googleSignInClient.signOut().addOnCompleteListener(this) {
//            // Sign out was successful, update UI
//            updateUI(null)
//        }
//    }

    private fun updateUI(account: GoogleSignInAccount?) {
//        if (account != null) {
//            // User is signed in, update the UI
//            binding.statusTextView.text = "Signed in as: ${account.displayName}"
//        } else {
//            // User is not signed in, show default UI
//            binding.statusTextView.text = "Sign-In failed"
//        }
    }
}