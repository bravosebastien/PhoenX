package com.example.phoenx.ui.components

import androidx.lifecycle.ViewModel
import com.example.phoenx.data.model.PresentationVideo
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PhareViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _isOpen = MutableStateFlow(false)
    val isOpen: StateFlow<Boolean> = _isOpen.asStateFlow()

    private val _videos = MutableStateFlow<List<PresentationVideo>>(emptyList())
    val videos: StateFlow<List<PresentationVideo>> = _videos.asStateFlow()

    private val _selectedVideo = MutableStateFlow<PresentationVideo?>(null)
    val selectedVideo: StateFlow<PresentationVideo?> = _selectedVideo.asStateFlow()

    init {
        loadVideos()
    }

    private fun loadVideos() {
        db.collection("presentationVideos")
            .orderBy("slotIndex")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PresentationVideo::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                _videos.value = list
            }
    }

    fun openPhare() { _isOpen.value = true }
    fun closePhare() { _isOpen.value = false; _selectedVideo.value = null }

    fun selectVideo(video: PresentationVideo?) { _selectedVideo.value = video }

    fun getFilteredVideos(isCreator: Boolean): List<PresentationVideo> {
        val targetRole = if (isCreator) "CREATOR" else "RECIPIENT"
        return _videos.value.filter {
            it.targetAudience == "ALL" || it.targetAudience == targetRole
        }
    }
}
