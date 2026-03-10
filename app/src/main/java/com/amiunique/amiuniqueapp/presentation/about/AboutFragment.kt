package com.amiunique.amiuniqueapp.presentation.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.amiunique.amiuniqueapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AboutFragment : Fragment() {
    private lateinit var mailBtn: FloatingActionButton
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_about, container, false)
        // Setup Fingerprints card
        setUpMailButton(view)
        return view
    }

    private fun setUpMailButton(view: View) {
        mailBtn = view.findViewById(R.id.mailBtn)
        // Open mail app when clicking on button
        mailBtn.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // Only allow mail apps
                putExtra(
                    Intent.EXTRA_EMAIL,
                    arrayOf("browser-fingerprinting-request@univ-lille.fr")
                ) // Replace with recipient email
            }

            // Check if there's an email app available
            try {
                startActivity(emailIntent)
            } catch (e: Throwable) {
                // Inform the user that no email app is available
                Toast.makeText(
                    view.context,
                    context?.getString(R.string.no_email_app),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}