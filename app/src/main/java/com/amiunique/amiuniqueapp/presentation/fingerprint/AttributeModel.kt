package com.amiunique.amiuniqueapp.presentation.fingerprint

import com.google.gson.annotations.Expose

data class AttributeModel(
    val attribute: String,
    val value: String,
    @Expose(serialize = false)
    val description: String = "",
    @Expose(serialize = false)
    var showDetails: Boolean = false
)
