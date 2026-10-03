# Only applied when the instrumented tests run against the minified release build
# (-PtestBuildType=release). Never ship an APK built with that flag.
#
# The test APK leaves out every library the app already has and uses the app's copy at runtime, so
# what the test runner needs must survive R8. So must the app code the tests call by name.
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep class androidx.tracing.** { *; }
-keep class androidx.concurrent.** { *; }
-keep class com.erela.fixme.custom_views.SignaturePadView { public *; }
