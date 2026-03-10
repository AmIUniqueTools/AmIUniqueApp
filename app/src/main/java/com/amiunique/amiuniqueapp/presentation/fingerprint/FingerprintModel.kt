package com.amiunique.amiuniqueapp.presentation.fingerprint

import java.util.*
import kotlin.collections.ArrayList

data class FingerprintModel(
    val attributes: ArrayList<AttributeModel>,
    val capturedAt: Date = Date()
)
