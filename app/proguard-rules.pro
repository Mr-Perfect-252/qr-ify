# ApexHub SDK — keep public API and Gson models (mirrors the SDK's own rules).
-keep class com.apexhub.sdk.ApexHubUpdater { *; }
-keep class com.apexhub.sdk.ApexHubConfig { *; }
-keep class com.apexhub.sdk.UpdateInfo { *; }
-keep class com.apexhub.sdk.UpdateCheckResult { *; }
-keep class com.apexhub.sdk.UpdateStrategy { *; }
-keep class com.apexhub.sdk.ApexHubException { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.gson.**
