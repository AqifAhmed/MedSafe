package com.medsafe.app.ui.navigation

object Routes {
    const val TODAY = "today"
    const val MEDS = "meds"
    const val HISTORY = "history"
    const val PROFILES = "profiles"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
    const val ADD_EDIT_MED = "add_edit_med?medId={medId}"

    const val ARG_MED_ID = "medId"

    val bottomBarRoutes = setOf(TODAY, MEDS, HISTORY, SETTINGS)

    fun addEditMed(medId: Long = -1L): String = "add_edit_med?medId=$medId"
}
