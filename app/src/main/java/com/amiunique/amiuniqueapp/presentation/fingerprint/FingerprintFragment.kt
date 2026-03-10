package com.amiunique.amiuniqueapp.presentation.fingerprint

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amiunique.amiuniqueapp.R
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FingerprintFragment : Fragment() {
    private lateinit var attributeList: RecyclerView
    private lateinit var searchField: TextInputLayout
    //private lateinit var loadingIndicator: LottieAnimationView
    private lateinit var loadingIndicator: LinearProgressIndicator
    private lateinit var loadingTV: TextView

    private lateinit var attributeListAdapter: AttributeListAdapter
    private val fingerprintViewModel: FingerprintViewModel by viewModels { FingerprintViewModel.Factory }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_fingerprint, container, false)

        attributeList = view.findViewById(R.id.attributesList)
        searchField = view.findViewById(R.id.searchAttribute)
        loadingIndicator = view.findViewById(R.id.loadingAttributes)
        loadingIndicator.progress = 0
        loadingTV = view.findViewById(R.id.loadingTV)


        attributeList.layoutManager = LinearLayoutManager(requireActivity())
        attributeListAdapter = AttributeListAdapter(ArrayList())
        attributeList.adapter = attributeListAdapter

        searchField.editText?.doOnTextChanged { text, _, _, _ ->
            if (text != null) {
                attributeListAdapter.getFilter().filter(text)
            }
        }


        fingerprintViewModel.fingerprintLoadingStateData.observe(viewLifecycleOwner) { loadingState ->
            // Update the visibility of your loading indicator
            loadingIndicator.visibility = if (loadingState > 0) View.VISIBLE else View.GONE
            loadingIndicator.progress = loadingState
            loadingTV.visibility = if (loadingState > 0) View.VISIBLE else View.GONE
        }

        fingerprintViewModel.fingerprintLoadingStateString.observe(viewLifecycleOwner) { loadingStateString ->
            // Update the text of your loading indicator
            loadingTV.text = loadingStateString
        }
        fingerprintViewModel.fingerprintLiveData.observe(viewLifecycleOwner) {
            if (it != null) attributeListAdapter.updateInitialAttributeListData(it.attributes)
        }

        GlobalScope.launch (Dispatchers.IO){
            try {
                fingerprintViewModel.fetchFingerprint()
            }catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        return view
    }
}