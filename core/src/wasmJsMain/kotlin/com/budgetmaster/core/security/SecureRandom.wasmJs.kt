package com.budgetmaster.core.security

import kotlin.random.Random

// App lock is unsupported on web (AppLockPlatform.isSupported == false), so this is never
// exercised in production. Present only so shared code compiles across targets.
actual fun secureRandomBytes(size: Int): ByteArray = Random.nextBytes(size)
