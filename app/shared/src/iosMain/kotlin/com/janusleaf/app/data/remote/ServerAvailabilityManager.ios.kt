package com.janusleaf.app.data.remote

import kotlinx.datetime.Clock

actual fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
