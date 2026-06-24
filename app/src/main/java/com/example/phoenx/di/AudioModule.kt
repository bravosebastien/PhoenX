package com.example.phoenx.di

import android.content.Context
import com.example.phoenx.data.audio.PhoenXAudioRecorder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext context: Context): PhoenXAudioRecorder {
        return PhoenXAudioRecorder(context)
    }
}
