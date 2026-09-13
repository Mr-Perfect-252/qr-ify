package com.qrify.app

import androidx.core.content.FileProvider

/**
 * Dedicated FileProvider subclass so our <provider> entry has a unique
 * component name and does not collide with the ApexHub SDK's own
 * androidx.core.content.FileProvider declaration during manifest merge.
 */
class QrFileProvider : FileProvider()
