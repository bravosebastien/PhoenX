package com.example.phoenx.ui.screens.detective

import android.content.Context
import com.example.phoenx.R

data class InspirationCategory(
    val title: String,
    val emoji: String,
    val questions: List<String>
)

object InspirationData {
    fun getCategories(context: Context): List<InspirationCategory> {
        return listOf(
            InspirationCategory(context.getString(R.string.inspiration_cat_childhood), "🏠", context.resources.getStringArray(R.array.inspiration_questions_childhood).toList()),
            InspirationCategory(context.getString(R.string.inspiration_cat_our_story), "💕", context.resources.getStringArray(R.array.inspiration_questions_our_story).toList()),
            InspirationCategory(context.getString(R.string.inspiration_cat_places), "✈️", context.resources.getStringArray(R.array.inspiration_questions_places).toList()),
            InspirationCategory(context.getString(R.string.inspiration_cat_tastes), "🎭", context.resources.getStringArray(R.array.inspiration_questions_tastes).toList()),
            InspirationCategory(context.getString(R.string.inspiration_cat_habits), "🔍", context.resources.getStringArray(R.array.inspiration_questions_habits).toList()),
            InspirationCategory(context.getString(R.string.inspiration_cat_hard_times), "🤝", context.resources.getStringArray(R.array.inspiration_questions_hard_times).toList()),
            InspirationCategory(context.getString(R.string.inspiration_cat_dreams), "🌟", context.resources.getStringArray(R.array.inspiration_questions_dreams).toList()),
            InspirationCategory(context.getString(R.string.inspiration_cat_quotes), "💬", context.resources.getStringArray(R.array.inspiration_questions_quotes).toList()),
            InspirationCategory(context.getString(R.string.inspiration_cat_people), "👥", context.resources.getStringArray(R.array.inspiration_questions_people).toList()),
            InspirationCategory(context.getString(R.string.inspiration_cat_deep), "🔮", context.resources.getStringArray(R.array.inspiration_questions_deep).toList())
        )
    }
}
