package com.example.ai_productivityenhancer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AIBlockingWorkerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAIBlockingWorker() {
        val worker = TestListenableWorkerBuilder<AIBlockingWorker>(context).build()
        val result = worker.startWork().get()
        assertTrue(result is ListenableWorker.Result.Success)
    }
}
