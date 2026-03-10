package com.amiunique.amiuniqueapp.presentation.privacy_policy

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.amiunique.amiuniqueapp.R


class PrivacyPolicyFragment : Fragment() {
    private lateinit var emailTV: TextView
    private lateinit var idTV: TextView
    private lateinit var copyEmail: ImageButton
    private lateinit var copyId: ImageButton
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_privacy_policy, container, false)
        // Init views
        emailTV = view.findViewById<TextView>(R.id.emailTV)
        emailTV.text = "browser-fingerprinting\n-request@univ-lille.fr"
        idTV = view.findViewById<TextView>(R.id.idTV)
        // get ANDROID ID value from SYSTEM
        idTV.text = android.provider.Settings.Secure.getString(
            requireContext().contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        copyEmail = view.findViewById<ImageButton>(R.id.copyEmail)
        // Copy email to clipboard when clicking on button
        copyEmail.setOnClickListener {
            var myClipboard = getSystemService(requireContext(), ClipboardManager::class.java) as ClipboardManager
            val clip = ClipData.newPlainText("Email", emailTV.text)
            myClipboard.setPrimaryClip(clip)
            Toast.makeText(
                view.context,
                emailTV.text,
                Toast.LENGTH_SHORT
            ).show()
        }
        copyId = view.findViewById<ImageButton>(R.id.copyID)
        // Copy ID to clipboard when clicking on button
        copyId.setOnClickListener {
            var myClipboard = getSystemService(requireContext(), ClipboardManager::class.java) as ClipboardManager
            val clip = ClipData.newPlainText("ID", idTV.text)
            myClipboard.setPrimaryClip(clip)
            Toast.makeText(
                view.context,
                idTV.text,
                Toast.LENGTH_SHORT
            ).show()
        }
        return view
    }
}